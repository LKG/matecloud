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
package vip.mate.starter.sms.model;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.HashMap;
import java.util.Map;

/**
 * An SMS send request. Built via chained setters so options can grow without
 * breaking call sites.
 *
 * @author mateaix
 */
@Data
@Accessors(chain = true)
public class SmsMessage {

    /** Recipient phone number. */
    private String mobile;

    /** Rendered message body — used by log providers and as a fallback for cloud providers. */
    private String content;

    /** Business code (VERIFY_CODE / ORDER_NOTIFY / ...) — maps to a template at the provider. */
    private String businessType;

    /** Structured template parameters (e.g. {@code {"code":"123456"}}); cloud providers prefer these. */
    private Map<String, String> templateParams;

    /** Free-form extension data for custom providers. */
    private Map<String, Object> extendData = new HashMap<>();

    public static SmsMessage of(String mobile, String content, String businessType) {
        return new SmsMessage().setMobile(mobile).setContent(content).setBusinessType(businessType);
    }
}
