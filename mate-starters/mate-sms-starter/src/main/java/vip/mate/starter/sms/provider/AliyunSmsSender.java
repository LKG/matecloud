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
package vip.mate.starter.sms.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.sms.annotation.SmsProvider;
import vip.mate.starter.sms.config.SmsProperties;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;
import vip.mate.starter.sms.spi.AbstractSmsSender;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Aliyun SMS provider via the Dysmsapi (POP) v1 API. No SDK dependency.
 *
 * <p>Effective config = admin-managed config (from {@link ChannelConfigStore}) over
 * static {@link SmsProperties.Aliyun}, so credentials/templates are editable in the
 * console. Activated when {@code mate.sms.provider=aliyun}.
 *
 * @author mateaix
 */
@Slf4j
@SmsProvider(value = "aliyun", describe = "Aliyun Dysmsapi")
public class AliyunSmsSender extends AbstractSmsSender {

    private static final DateTimeFormatter ISO_8601 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final SmsProperties.Aliyun staticCfg;
    private final ObjectProvider<ChannelConfigStore> configStore;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public AliyunSmsSender(SmsProperties properties, ObjectProvider<ChannelConfigStore> configStore) {
        this.staticCfg = properties.getAliyun();
        this.configStore = configStore;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("aliyun", "阿里云短信")
                .describe("Dysmsapi")
                .text("accessKeyId", "AccessKeyId", true)
                .secret("accessKeySecret", "AccessKeySecret", true)
                .text("signName", "签名", true, "MateCloud")
                .text("endpoint", "Endpoint", false, "https://dysmsapi.aliyuncs.com")
                .map("templates", "模板映射", "业务类型 → 模板 Code,如 VERIFY_CODE → SMS_123456")
                .build();
    }

    private Map<String, String> stored() {
        ChannelConfigStore store = configStore == null ? null : configStore.getIfAvailable();
        Map<String, String> s = store == null ? null : store.config(CHANNEL, type());
        return s == null ? Map.of() : s;
    }

    private String pick(Map<String, String> s, String key, String fallback) {
        String v = s.get(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> templates(Map<String, String> s) {
        String raw = s.get("templates");
        if (raw != null && !raw.isBlank()) {
            try {
                return json.readValue(raw, Map.class);
            } catch (Exception e) {
                log.warn("[mate-sms:aliyun] invalid templates json, using static: {}", e.getMessage());
            }
        }
        return staticCfg.getTemplates() == null ? Map.of() : staticCfg.getTemplates();
    }

    @Override
    public SmsResult send(SmsMessage message) {
        Map<String, String> s = stored();
        String businessType = message.getBusinessType();
        String templateCode = templates(s).get(businessType);
        if (templateCode == null) {
            log.warn("[mate-sms:aliyun] no template mapped for businessType={}", businessType);
            return SmsResult.fail(type(), "no template for businessType=" + businessType);
        }
        String accessKeyId = pick(s, "accessKeyId", staticCfg.getAccessKeyId());
        String accessKeySecret = pick(s, "accessKeySecret", staticCfg.getAccessKeySecret());
        String signName = pick(s, "signName", staticCfg.getSignName());
        String endpoint = pick(s, "endpoint", staticCfg.getEndpoint());
        try {
            String templateParam = buildTemplateParam(message);
            Map<String, String> params = new TreeMap<>();
            params.put("AccessKeyId", accessKeyId);
            params.put("Action", "SendSms");
            params.put("Format", "JSON");
            params.put("PhoneNumbers", message.getMobile());
            params.put("RegionId", "cn-hangzhou");
            params.put("SignName", signName);
            params.put("SignatureMethod", "HMAC-SHA1");
            params.put("SignatureNonce", UUID.randomUUID().toString());
            params.put("SignatureVersion", "1.0");
            params.put("TemplateCode", templateCode);
            params.put("TemplateParam", templateParam);
            params.put("Timestamp", ISO_8601.format(Instant.now()));
            params.put("Version", "2017-05-25");

            params.put("Signature", sign(params, accessKeySecret));
            String body = canonicalQuery(params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<?, ?> result = json.readValue(response.body(), Map.class);
                if ("OK".equals(result.get("Code"))) {
                    Object bizId = result.get("BizId");
                    log.info("[mate-sms:aliyun] sent: mobile={} bizId={}", message.getMobile(), bizId);
                    return SmsResult.ok(type(), bizId == null ? null : bizId.toString());
                }
                log.warn("[mate-sms:aliyun] gateway rejected: code={} message={}",
                        result.get("Code"), result.get("Message"));
                return SmsResult.fail(type(), String.valueOf(result.get("Message")));
            }
            log.warn("[mate-sms:aliyun] HTTP {} body={}", response.statusCode(), response.body());
            return SmsResult.fail(type(), "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.error("[mate-sms:aliyun] dispatch failed: mobile={}", message.getMobile(), e);
            return SmsResult.fail(type(), e.getMessage());
        }
    }

    private String buildTemplateParam(SmsMessage message) throws Exception {
        if (message.getTemplateParams() != null && !message.getTemplateParams().isEmpty()) {
            return json.writeValueAsString(message.getTemplateParams());
        }
        Map<String, String> params = new LinkedHashMap<>();
        String code = extractCode(message.getContent());
        if (code != null) {
            params.put("code", code);
        } else {
            params.put("content", message.getContent() == null ? "" : message.getContent());
        }
        return json.writeValueAsString(params);
    }

    private static String sign(Map<String, String> params, String accessKeySecret) throws Exception {
        String canonical = canonicalQuery(params);
        String stringToSign = "POST&" + percentEncode("/") + "&" + percentEncode(canonical);
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec((accessKeySecret + "&").getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] sig = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(sig);
    }

    private static String canonicalQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(percentEncode(e.getKey())).append('=').append(percentEncode(e.getValue()));
        }
        return sb.toString();
    }

    private static String percentEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
                .replace("%7E", "~");
    }
}
