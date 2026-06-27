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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tencent Cloud SMS provider (TC3-HMAC-SHA256). No SDK dependency.
 *
 * <p>Effective config = admin-managed config (from {@link ChannelConfigStore}) over
 * static {@link SmsProperties.Tencent}. Activated when {@code mate.sms.provider=tencent}.
 *
 * @author mateaix
 */
@Slf4j
@SmsProvider(value = "tencent", describe = "Tencent Cloud SMS (TC3)")
public class TencentSmsSender extends AbstractSmsSender {

    private static final String SERVICE = "sms";
    private static final String VERSION = "2021-01-11";
    private static final String ACTION = "SendSms";
    private static final DateTimeFormatter UTC_DAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final SmsProperties.Tencent staticCfg;
    private final ObjectProvider<ChannelConfigStore> configStore;
    private final HttpClient http;
    private final ObjectMapper json = new ObjectMapper();

    public TencentSmsSender(SmsProperties properties, ObjectProvider<ChannelConfigStore> configStore) {
        this.staticCfg = properties.getTencent();
        this.configStore = configStore;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("tencent", "腾讯云短信")
                .describe("TC3 签名")
                .text("secretId", "SecretId", true)
                .secret("secretKey", "SecretKey", true)
                .text("sdkAppId", "SdkAppId", true)
                .text("signName", "签名", true)
                .text("region", "Region", false, "ap-guangzhou")
                .map("templates", "模板映射", "业务类型 → 模板 ID")
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
                log.warn("[mate-sms:tencent] invalid templates json, using static: {}", e.getMessage());
            }
        }
        return staticCfg.getTemplates() == null ? Map.of() : staticCfg.getTemplates();
    }

    @Override
    public SmsResult send(SmsMessage message) {
        Map<String, String> s = stored();
        String templateId = templates(s).get(message.getBusinessType());
        if (templateId == null) {
            log.warn("[mate-sms:tencent] no template mapped for businessType={}", message.getBusinessType());
            return SmsResult.fail(type(), "no template for businessType=" + message.getBusinessType());
        }
        String secretId = pick(s, "secretId", staticCfg.getSecretId());
        String secretKey = pick(s, "secretKey", staticCfg.getSecretKey());
        String sdkAppId = pick(s, "sdkAppId", staticCfg.getSdkAppId());
        String signName = pick(s, "signName", staticCfg.getSignName());
        String region = pick(s, "region", staticCfg.getRegion());
        String endpoint = pick(s, "endpoint", staticCfg.getEndpoint());
        try {
            String[] templateParams = (message.getTemplateParams() != null && !message.getTemplateParams().isEmpty())
                    ? message.getTemplateParams().values().toArray(new String[0])
                    : buildTemplateParams(message.getContent());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("PhoneNumberSet", new String[]{normalize(message.getMobile())});
            payload.put("SmsSdkAppId", sdkAppId);
            payload.put("SignName", signName);
            payload.put("TemplateId", templateId);
            payload.put("TemplateParamSet", templateParams);
            String body = json.writeValueAsString(payload);

            long timestamp = System.currentTimeMillis() / 1000;
            String date = UTC_DAY.format(Instant.ofEpochSecond(timestamp));
            String host = URI.create(endpoint).getHost();
            String hashedPayload = sha256Hex(body);
            String canonicalRequest = "POST\n/\n\n"
                    + "content-type:application/json; charset=utf-8\n"
                    + "host:" + host + "\n"
                    + "x-tc-action:" + ACTION.toLowerCase() + "\n\n"
                    + "content-type;host;x-tc-action\n"
                    + hashedPayload;
            String credentialScope = date + "/" + SERVICE + "/tc3_request";
            String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n"
                    + sha256Hex(canonicalRequest);

            byte[] secretDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
            byte[] secretService = hmacSha256(secretDate, SERVICE);
            byte[] secretSigning = hmacSha256(secretService, "tc3_request");
            String signature = hex(hmacSha256(secretSigning, stringToSign));

            String authorization = "TC3-HMAC-SHA256 Credential=" + secretId + "/" + credentialScope
                    + ", SignedHeaders=content-type;host;x-tc-action, Signature=" + signature;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", authorization)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Host", host)
                    .header("X-TC-Action", ACTION)
                    .header("X-TC-Timestamp", String.valueOf(timestamp))
                    .header("X-TC-Version", VERSION)
                    .header("X-TC-Region", region)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Map<?, ?> resp = json.readValue(response.body(), Map.class);
                Map<?, ?> envelope = (Map<?, ?>) resp.get("Response");
                if (envelope != null && envelope.get("Error") == null) {
                    log.info("[mate-sms:tencent] sent: mobile={}", message.getMobile());
                    return SmsResult.ok(type(), null);
                }
                log.warn("[mate-sms:tencent] gateway rejected: {}", response.body());
                return SmsResult.fail(type(), "gateway rejected");
            }
            log.warn("[mate-sms:tencent] HTTP {} body={}", response.statusCode(), response.body());
            return SmsResult.fail(type(), "HTTP " + response.statusCode());
        } catch (Exception e) {
            log.error("[mate-sms:tencent] dispatch failed: mobile={}", message.getMobile(), e);
            return SmsResult.fail(type(), e.getMessage());
        }
    }

    private String[] buildTemplateParams(String content) {
        String code = extractCode(content);
        return new String[]{code != null ? code : (content == null ? "" : content)};
    }

    private static String normalize(String mobile) {
        if (mobile == null) return "";
        return mobile.startsWith("+") ? mobile : "+86" + mobile;
    }

    private static byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return hex(md.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
