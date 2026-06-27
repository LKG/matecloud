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
package vip.mate.system.admin.application.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.channel.FieldSpec;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.base.exception.BizException;
import org.springframework.beans.factory.ObjectProvider;
import vip.mate.starter.channel.ChannelCrypto;
import vip.mate.starter.file.core.FileStorageRegistry;
import vip.mate.starter.sso.core.ProviderRegistry;
import vip.mate.starter.file.model.UploadCommand;
import vip.mate.starter.sms.core.SmsSenderRegistry;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;
import vip.mate.system.admin.infrastructure.dao.ChannelConfigDao;
import vip.mate.system.admin.infrastructure.dao.po.ChannelConfigPO;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin management of channel (storage / sms) provider configuration.
 *
 * <p>Merges each provider's self-described fields ({@link ProviderDescriptor}) with
 * the stored config in {@code mate_channel_config}. Secret fields are masked on read
 * and preserved on write (a blank / masked secret means "keep the stored value").
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelConfigService {

    /** Masked placeholder returned for secret fields that have a stored value. */
    public static final String MASK = "******";

    public static final String STORAGE = "storage";
    public static final String SMS = "sms";
    /** SSO / 身份接入渠道:provider 元数据来自 SSO ProviderRegistry,与 storage/sms 共用这套配置机制。 */
    public static final String IDENTITY = "identity";

    private final ChannelConfigDao dao;
    private final ObjectMapper objectMapper;
    private final FileStorageRegistry fileRegistry;
    private final SmsSenderRegistry smsRegistry;
    private final ChannelCrypto crypto;
    /** Optional: present only when the SSO starter is on the classpath + enabled. */
    private final ObjectProvider<ProviderRegistry> ssoRegistry;

    // ---- views ----

    public record ProviderView(String type, String name, String describe, boolean enabled,
                               List<FieldSpec> fields, Map<String, Object> values) {}

    public record ChannelView(String channel, String active, List<ProviderView> providers) {}

    public record TestResult(boolean success, String message) {}

    // ---- list ----

    public ChannelView list(String channel) {
        List<ProviderDescriptor> descriptors = descriptors(channel);
        List<ProviderView> providers = new ArrayList<>(descriptors.size());
        String active = null;
        for (ProviderDescriptor d : descriptors) {
            ChannelConfigPO po = find(channel, d.getType());
            boolean enabled = po != null && Integer.valueOf(1).equals(po.getEnabled());
            if (enabled) {
                active = d.getType();
            }
            providers.add(new ProviderView(d.getType(), d.getName(), d.getDescribe(), enabled,
                    d.getFields(), maskedValues(d, po)));
        }
        return new ChannelView(channel, active, providers);
    }

    // ---- save ----

    @Transactional(rollbackFor = Exception.class)
    public void save(String channel, String type, boolean enabled, Map<String, Object> values) {
        ProviderDescriptor descriptor = descriptor(channel, type);
        ChannelConfigPO po = find(channel, type);
        Map<String, Object> stored = parse(po == null ? null : po.getConfigJson());

        // Merge incoming over stored; for secrets, blank/masked means "keep existing".
        Map<String, Object> merged = new LinkedHashMap<>(stored);
        if (values != null) {
            for (FieldSpec f : descriptor.getFields()) {
                Object incoming = values.get(f.key());
                if (f.secret()) {
                    String s = incoming == null ? null : String.valueOf(incoming);
                    if (s == null || s.isBlank() || MASK.equals(s)) {
                        continue; // keep stored secret
                    }
                    merged.put(f.key(), s);
                } else if (values.containsKey(f.key())) {
                    merged.put(f.key(), incoming);
                }
            }
        }

        String json;
        try {
            json = crypto.encrypt(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            throw new BizException("SYSA002", "Invalid channel config: " + e.getMessage());
        }

        if (enabled) {
            // single active provider per channel
            dao.update(null, new LambdaUpdateWrapper<ChannelConfigPO>()
                    .eq(ChannelConfigPO::getChannel, channel)
                    .set(ChannelConfigPO::getEnabled, 0));
        }

        if (po == null) {
            po = new ChannelConfigPO();
            po.setChannel(channel);
            po.setProviderType(type);
            po.setScope("GLOBAL");
            po.setConfigJson(json);
            po.setEnabled(enabled ? 1 : 0);
            dao.insert(po);
        } else {
            po.setConfigJson(json);
            po.setEnabled(enabled ? 1 : 0);
            dao.updateById(po);
        }
        log.info("[channel] saved {}:{} enabled={}", channel, type, enabled);
    }

    // ---- test ----

    public TestResult test(String channel, String type, String target) {
        try {
            if (STORAGE.equals(channel)) {
                return testStorage(type);
            }
            if (SMS.equals(channel)) {
                return testSms(type, target);
            }
            if (IDENTITY.equals(channel)) {
                // 身份渠道无轻量探针(凭证有效性需真正调用),交由「同步组织」验证。
                return new TestResult(true, "配置已就绪,请用「同步组织」验证凭证与拉取通讯录");
            }
            throw new BizException("SYSA001", "Unknown channel: " + channel);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            String detail = explain(e);
            // Full stacktrace to the server log so ops can dig deeper; concise
            // root-cause string to the UI so the operator can self-diagnose.
            log.warn("[channel] test failed {}:{} — {}", channel, type, detail, e);
            return new TestResult(false, detail);
        }
    }

    /**
     * Build a UI-friendly failure message that includes the ROOT cause, not just
     * the outermost wrapper. Without this, a storage/SMS probe failure surfaces
     * only a generic "Failed to upload to OSS: probe-xxx.txt" and hides the
     * actionable reason (AccessDenied / NoSuchBucket / SignatureDoesNotMatch /
     * UnknownHost / Connection refused …).
     */
    private static String explain(Throwable e) {
        String top = e.getMessage();
        Throwable root = e;
        Set<Throwable> seen = new HashSet<>();
        while (root.getCause() != null && root.getCause() != root && seen.add(root)) {
            root = root.getCause();
        }
        String rootMsg = root.getMessage();
        String rootDesc = (rootMsg == null || rootMsg.isBlank())
                ? root.getClass().getSimpleName()
                : root.getClass().getSimpleName() + ": " + rootMsg;

        if (top == null || top.isBlank()) {
            return rootDesc;
        }
        // Avoid duplicating when the wrapper already carries the root text.
        return top.contains(rootDesc) || (rootMsg != null && top.contains(rootMsg))
                ? top
                : top + " — " + rootDesc;
    }

    private TestResult testStorage(String type) {
        var storage = fileRegistry.get(type);
        String objectName = "mate-channel-test/probe-" + System.currentTimeMillis() + ".txt";
        byte[] data = "mate-channel-test".getBytes(StandardCharsets.UTF_8);
        storage.upload(UploadCommand.of(new ByteArrayInputStream(data), data.length, "text/plain", objectName));
        try {
            storage.delete(null, objectName);
        } catch (Exception ignored) {
            // probe uploaded fine; cleanup failure is non-fatal for the test verdict
        }
        return new TestResult(true, "上传探测对象成功");
    }

    private TestResult testSms(String type, String target) {
        if (target == null || target.isBlank()) {
            throw new BizException("SYSA001", "请输入测试手机号");
        }
        SmsResult r = smsRegistry.get(type).send(
                SmsMessage.of(target, "【MateCloud】测试验证码 123456", "VERIFY_CODE"));
        return new TestResult(r.success(), r.success() ? "短信已发送" : r.message());
    }

    // ---- helpers ----

    private List<ProviderDescriptor> descriptors(String channel) {
        if (STORAGE.equals(channel)) {
            return fileRegistry.all().stream().map(s -> s.descriptor()).toList();
        }
        if (SMS.equals(channel)) {
            return smsRegistry.all().stream().map(s -> s.descriptor()).toList();
        }
        if (IDENTITY.equals(channel)) {
            ProviderRegistry reg = ssoRegistry.getIfAvailable();
            if (reg == null) {
                throw new BizException("SYSA001", "SSO 未启用(mate.feature.sso.enabled)");
            }
            return reg.all().stream().map(p -> p.descriptor()).toList();
        }
        throw new BizException("SYSA001", "Unknown channel: " + channel);
    }

    private ProviderDescriptor descriptor(String channel, String type) {
        return descriptors(channel).stream()
                .filter(d -> d.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new BizException("SYSA001", "Unknown provider: " + channel + "/" + type));
    }

    private ChannelConfigPO find(String channel, String type) {
        return dao.selectOne(new LambdaQueryWrapper<ChannelConfigPO>()
                .eq(ChannelConfigPO::getChannel, channel)
                .eq(ChannelConfigPO::getProviderType, type)
                .last("LIMIT 1"));
    }

    private Map<String, Object> maskedValues(ProviderDescriptor descriptor, ChannelConfigPO po) {
        Map<String, Object> stored = parse(po == null ? null : po.getConfigJson());
        Map<String, Object> out = new LinkedHashMap<>();
        for (FieldSpec f : descriptor.getFields()) {
            Object v = stored.get(f.key());
            if (f.secret()) {
                out.put(f.key(), (v != null && !String.valueOf(v).isBlank()) ? MASK : "");
            } else if (v != null) {
                out.put(f.key(), v);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(crypto.decrypt(json), Map.class);
        } catch (Exception e) {
            log.warn("[channel] bad config_json, treating as empty: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }
}
