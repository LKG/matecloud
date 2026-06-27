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
package vip.mate.starter.sms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * SMS configuration, bound to {@code mate.sms.*}.
 *
 * <pre>
 * mate:
 *   sms:
 *     provider: aliyun
 *     aliyun:
 *       access-key-id: LTAI5t...
 *       access-key-secret: xxx
 *       sign-name: MateCloud
 *       templates:
 *         VERIFY_CODE: SMS_123456
 * </pre>
 *
 * @author mateaix
 */
@Data
@ConfigurationProperties(prefix = "mate.sms")
public class SmsProperties {

    /** Active provider type (log / aliyun / tencent). Defaults to log for safe local dev. */
    private String provider = "log";

    private Aliyun aliyun = new Aliyun();
    private Tencent tencent = new Tencent();

    @Data
    public static class Aliyun {
        private String accessKeyId;
        private String accessKeySecret;
        /** Signature name (approved in the Aliyun console). */
        private String signName;
        private String endpoint = "https://dysmsapi.aliyuncs.com";
        /** businessType -> Aliyun template code mapping. */
        private Map<String, String> templates = new HashMap<>();
    }

    @Data
    public static class Tencent {
        private String secretId;
        private String secretKey;
        private String sdkAppId;
        private String signName;
        private String endpoint = "https://sms.tencentcloudapi.com";
        private String region = "ap-guangzhou";
        private Map<String, String> templates = new HashMap<>();
    }
}
