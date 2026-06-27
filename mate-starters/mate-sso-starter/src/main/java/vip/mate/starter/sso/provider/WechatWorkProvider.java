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
package vip.mate.starter.sso.provider;

import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.sso.core.AccessTokenCache;
import vip.mate.starter.sso.core.http.HttpExecutor;
import vip.mate.starter.sso.core.util.Sha1;
import vip.mate.starter.sso.spi.AbstractOAuthIdentityProvider;
import vip.mate.starter.sso.spi.JsSdkProvider;
import vip.mate.starter.sso.spi.annotation.IdentityProvider;
import vip.mate.starter.sso.spi.model.ExternalDept;
import vip.mate.starter.sso.spi.model.ExternalUser;
import vip.mate.starter.sso.spi.model.JsSdkConfig;
import vip.mate.starter.sso.spi.model.JsSdkType;
import vip.mate.starter.sso.spi.model.ProviderConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 企业微信(WeChat Work)provider. OAuth login + contacts sync over the qyapi
 * endpoints — borrows PlayEdu's WechatHttpUtil logic, refactored behind the
 * template method (the three vendor steps) + the neutral SPI.
 *
 * <p>Config keys (see {@link #descriptor()}): {@code corpId}, {@code agentId},
 * {@code secret}(应用 secret,用于登录 token)、{@code contactsSecret}(通讯录同步
 * secret)、{@code trustDomain}.
 *
 * @author mateaix
 */
@IdentityProvider(value = "wechat_work", describe = "企业微信")
public class WechatWorkProvider extends AbstractOAuthIdentityProvider implements JsSdkProvider {

    private static final String BASE = "https://qyapi.weixin.qq.com/cgi-bin";
    /** WeChat Work 通讯录里的"未分配/待设置"部门,同步时跳过(对齐 PlayEdu OTHER_DEP)。 */
    private static final String OTHER_DEP = "其他（待设置部门）";

    public WechatWorkProvider(HttpExecutor http, AccessTokenCache tokenCache) {
        super(http, tokenCache);
    }

    @Override
    public String code() {
        return "wechat_work";
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("wechat_work", "企业微信")
                .describe("企业微信扫码/H5 登录 + 通讯录同步")
                .text("corpId", "企业 ID (CorpID)", true)
                .text("agentId", "应用 AgentId", true)
                .secret("secret", "应用 Secret", true)
                .secret("contactsSecret", "通讯录同步 Secret", false)
                .text("trustDomain", "可信域名", false)
                .secret("callbackToken", "通讯录回调 Token", false)
                .secret("callbackAesKey", "通讯录回调 EncodingAESKey", false)
                .build();
    }

    // ---- OAuth template steps ----

    @Override
    protected AccessTokenCache.Token requestAccessToken(ProviderConfig cfg) {
        Map<String, Object> r = http.getJson(BASE + "/gettoken?corpid=" + cfg.require("corpId")
                + "&corpsecret=" + cfg.require("secret"));
        return new AccessTokenCache.Token(str(r.get("access_token")), asLong(r.get("expires_in"), 7200));
    }

    @Override
    protected String exchangeCodeForUserId(String code, String accessToken, ProviderConfig cfg) {
        Map<String, Object> r = http.getJson(BASE + "/auth/getuserinfo?access_token=" + accessToken + "&code=" + code);
        Object userid = r.getOrDefault("userid", r.get("UserId"));
        return str(userid);
    }

    @Override
    protected ExternalUser fetchUserDetail(String externalId, String accessToken, ProviderConfig cfg) {
        Map<String, Object> r = http.getJson(BASE + "/user/get?access_token=" + accessToken + "&userid=" + externalId);
        return toUser(r);
    }

    // ---- organization sync (uses the contacts-sync token) ----

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalDept> fetchDepartments(ProviderConfig cfg) {
        String token = syncToken(cfg);
        Map<String, Object> r = http.getJson(BASE + "/department/list?access_token=" + token);
        List<Map<String, Object>> list = (List<Map<String, Object>>) r.getOrDefault("department", List.of());
        List<ExternalDept> out = new ArrayList<>();
        for (Map<String, Object> d : list) {
            String name = str(d.get("name"));
            if (OTHER_DEP.equals(name)) {
                continue; // 跳过"其他（待设置部门）"
            }
            out.add(new ExternalDept(
                    str(d.get("id")),
                    name,
                    str(d.get("parentid")),
                    (int) asLong(d.get("order"), 0),
                    d));
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<ExternalUser> fetchUsers(ProviderConfig cfg, List<ExternalDept> departments) {
        String token = syncToken(cfg);
        List<ExternalUser> out = new ArrayList<>();
        // Query only ROOT departments with fetch_child=1, so the whole sub-tree comes
        // back per call — turns the old O(department-count) N+1 into O(root-count)
        // (usually 1). Robust to multi-root corps and to corps whose root id != "1".
        for (ExternalDept dept : rootDepartments(departments)) {
            Map<String, Object> r = http.getJson(BASE + "/user/list?access_token=" + token
                    + "&department_id=" + dept.externalId() + "&fetch_child=1");
            List<Map<String, Object>> list = (List<Map<String, Object>>) r.getOrDefault("userlist", List.of());
            for (Map<String, Object> u : list) {
                out.add(toUser(u));
            }
        }
        return dedup(out); // fetch_child sub-trees overlap across roots → dedup stays as a safety net
    }

    /** Top-level departments: parent missing/"0", or parent not in the synced set. */
    private List<ExternalDept> rootDepartments(List<ExternalDept> departments) {
        Set<String> ids = new HashSet<>();
        for (ExternalDept d : departments) {
            ids.add(d.externalId());
        }
        List<ExternalDept> roots = new ArrayList<>();
        for (ExternalDept d : departments) {
            String parent = d.parentExternalId();
            if (parent == null || parent.isBlank() || "0".equals(parent) || !ids.contains(parent)) {
                roots.add(d);
            }
        }
        return roots;
    }

    private String syncToken(ProviderConfig cfg) {
        String contactsSecret = cfg.get("contactsSecret");
        if (contactsSecret == null || contactsSecret.isBlank()) {
            return accessToken(cfg); // fall back to the app secret token
        }
        return tokenCache.get("sso:token:wechat_work:contacts", () -> {
            Map<String, Object> r = http.getJson(BASE + "/gettoken?corpid=" + cfg.require("corpId")
                    + "&corpsecret=" + contactsSecret);
            return new AccessTokenCache.Token(str(r.get("access_token")), asLong(r.get("expires_in"), 7200));
        });
    }

    // ---- H5 JS-SDK signature (wx.config / wx.agentConfig) ----

    @Override
    public JsSdkConfig jsConfig(JsSdkType type, String url, ProviderConfig cfg) {
        String ticket = jsapiTicket(type, cfg);
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long timestamp = System.currentTimeMillis() / 1000L;
        String raw = "jsapi_ticket=" + ticket
                + "&noncestr=" + nonceStr
                + "&timestamp=" + timestamp
                + "&url=" + url;
        String signature = Sha1.hex(raw);
        return new JsSdkConfig(cfg.require("corpId"), cfg.get("agentId"), timestamp, nonceStr, signature, url);
    }

    /** jsapi_ticket(企业 get_jsapi_ticket / 应用 ticket/get?type=agent_config),缓存 ~7200s。 */
    private String jsapiTicket(JsSdkType type, ProviderConfig cfg) {
        String token = accessToken(cfg);
        String key = "sso:jsticket:wechat_work:" + (type == JsSdkType.AGENT ? "agent" : "corp");
        String endpoint = type == JsSdkType.AGENT
                ? BASE + "/ticket/get?access_token=" + token + "&type=agent_config"
                : BASE + "/get_jsapi_ticket?access_token=" + token;
        return tokenCache.get(key, () -> {
            Map<String, Object> r = http.getJson(endpoint);
            return new AccessTokenCache.Token(str(r.get("ticket")), asLong(r.get("expires_in"), 7200));
        });
    }

    @SuppressWarnings("unchecked")
    private ExternalUser toUser(Map<String, Object> u) {
        List<String> deptIds = new ArrayList<>();
        Object dept = u.get("department");
        if (dept instanceof List<?> l) {
            for (Object o : l) {
                deptIds.add(str(o));
            }
        }
        boolean enabled = asLong(u.getOrDefault("status", 1), 1) == 1; // 1=已激活
        return new ExternalUser(
                str(u.getOrDefault("userid", u.get("UserId"))),
                str(u.get("unionid")),
                str(u.get("name")),
                str(u.get("mobile")),
                str(u.get("email")),
                str(u.get("avatar")),
                deptIds,
                enabled,
                u);
    }

    private List<ExternalUser> dedup(List<ExternalUser> in) {
        List<ExternalUser> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ExternalUser u : in) {
            if (u.externalId() != null && seen.add(u.externalId())) {
                out.add(u);
            }
        }
        return out;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static long asLong(Object o, long def) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return o == null ? def : Long.parseLong(String.valueOf(o));
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
