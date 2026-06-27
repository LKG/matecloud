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
package vip.mate.starter.sms.core;

import java.util.Map;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;
import vip.mate.starter.sms.spi.SmsSender;

/**
 * Application-facing facade for sending SMS. Resolves the active provider per
 * call (via {@link SmsProviderResolver}) and delegates — callers never depend on
 * a specific gateway.
 *
 * <pre>{@code
 *   smsTemplate.send("13800138000", "code 123456", "VERIFY_CODE");
 * }</pre>
 *
 * @author mateaix
 */
public class SmsTemplate {

    private final SmsSenderRegistry registry;
    private final SmsProviderResolver resolver;

    public SmsTemplate(SmsSenderRegistry registry, SmsProviderResolver resolver) {
        this.registry = registry;
        this.resolver = resolver;
    }

    /** The currently active provider. */
    public SmsSender active() {
        return registry.get(resolver.resolveProvider());
    }

    public SmsResult send(SmsMessage message) {
        return active().send(message);
    }

    public SmsResult send(String mobile, String content, String businessType) {
        return send(SmsMessage.of(mobile, content, businessType));
    }

    public SmsResult send(String mobile, String content, String businessType,
                          Map<String, String> templateParams) {
        return send(SmsMessage.of(mobile, content, businessType).setTemplateParams(templateParams));
    }
}
