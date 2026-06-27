/*
 * Copyright (c) 2024-2026 Beijing Daotiandi Technology Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vip.mate.starter.sso.web;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.exception.BizException;
import vip.mate.starter.sso.core.SsoConfigStore;
import vip.mate.starter.sso.core.SsoTemplate;
import vip.mate.starter.sso.core.callback.WeComCallbackCrypto;
import vip.mate.starter.sso.core.sync.SyncMode;
import vip.mate.starter.sso.spi.model.ProviderConfig;

/**
 * 企业微信通讯录变更回调入口 — 准实时同步,替代纯定时全量。
 *
 * <ul>
 *   <li>GET:URL 有效性验证 — 验签后解密 {@code echostr} 原样返回。</li>
 *   <li>POST:变更事件 — 验签 + 解密,若为通讯录变更则<b>异步</b>触发一次全量同步
 *       (并发回调由 {@code SyncLock} 合并,WeChat 侧立即拿到 200)。</li>
 * </ul>
 *
 * <p>回调由企业微信服务器调用、不携带 sa-token;网关须放行 {@code /api/v1/sso/callback/**},
 * 鉴权完全靠回调自身的 {@code Token}/{@code EncodingAESKey} 验签。配置键:
 * {@code callbackToken}、{@code callbackAesKey}、{@code corpId}(作为 receiveId)。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/sso/callback")
public class SsoCallbackController {

    private static final Logger log = LoggerFactory.getLogger(SsoCallbackController.class);

    private final SsoTemplate ssoTemplate;
    private final SsoConfigStore configStore;
    private final ExecutorService syncExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "sso-callback-sync");
                t.setDaemon(true);
                return t;
            });

    public SsoCallbackController(SsoTemplate ssoTemplate, SsoConfigStore configStore) {
        this.ssoTemplate = ssoTemplate;
        this.configStore = configStore;
    }

    /** URL 验证:验签通过则解密 echostr 原样回显(企业微信要求返回明文)。 */
    @GetMapping("/{provider}")
    public String verify(@PathVariable String provider,
                         @RequestParam("msg_signature") String msgSignature,
                         @RequestParam String timestamp,
                         @RequestParam String nonce,
                         @RequestParam String echostr) {
        ProviderConfig cfg = configStore.config(provider);
        String token = cfg.require("callbackToken");
        if (!WeComCallbackCrypto.verify(token, timestamp, nonce, echostr, msgSignature)) {
            throw new BizException("SSOE001", "回调验签失败");
        }
        return WeComCallbackCrypto.decrypt(cfg.require("callbackAesKey"), echostr, cfg.get("corpId"));
    }

    /** 变更事件:验签 + 解密;通讯录变更则异步触发全量同步。 */
    @PostMapping("/{provider}")
    public String event(@PathVariable String provider,
                        @RequestParam("msg_signature") String msgSignature,
                        @RequestParam String timestamp,
                        @RequestParam String nonce,
                        @RequestBody String body) {
        ProviderConfig cfg = configStore.config(provider);
        String token = cfg.require("callbackToken");
        String encrypt = WeComCallbackCrypto.extractEncrypt(body);
        if (encrypt == null || !WeComCallbackCrypto.verify(token, timestamp, nonce, encrypt, msgSignature)) {
            throw new BizException("SSOE001", "回调验签失败");
        }
        String xml = WeComCallbackCrypto.decrypt(cfg.require("callbackAesKey"), encrypt, cfg.get("corpId"));
        if (xml.contains("change_contact") || xml.contains("ChangeType")) {
            triggerSync(provider);
        }
        return ""; // 企业微信对回调只需 200,无需回复内容
    }

    private void triggerSync(String provider) {
        syncExecutor.submit(() -> {
            try {
                ssoTemplate.sync(provider, SyncMode.FULL);
            } catch (BizException e) {
                // SSO_B_SYNC_RUNNING:已有同步在跑,变更会被那次覆盖,忽略即可
                log.debug("[sso-callback] sync skipped for {}: {}", provider, e.getMessage());
            } catch (Exception e) {
                log.warn("[sso-callback] sync failed for {}: {}", provider, e.getMessage());
            }
        });
    }

    @PreDestroy
    void shutdown() {
        syncExecutor.shutdownNow();
    }
}
