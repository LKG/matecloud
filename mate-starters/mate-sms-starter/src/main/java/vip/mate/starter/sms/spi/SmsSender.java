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
package vip.mate.starter.sms.spi;

import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.sms.annotation.SmsProvider;
import vip.mate.starter.sms.core.SmsTemplate;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;

/**
 * SMS gateway SPI. Implement once per gateway (Log, Aliyun, Tencent, …),
 * annotate with {@link SmsProvider} to declare its {@link #type()}, register as
 * a Spring bean. Application code sends through {@link SmsTemplate}, never a
 * provider directly.
 *
 * @author mateaix
 */
public interface SmsSender {

    /** Provider type key, e.g. {@code "aliyun"} (mirrors {@link SmsProvider#value()}). */
    String type();

    /** Self-description (display name + config field metadata) for the admin UI. */
    ProviderDescriptor descriptor();

    /** Dispatch a single message. */
    SmsResult send(SmsMessage message);
}
