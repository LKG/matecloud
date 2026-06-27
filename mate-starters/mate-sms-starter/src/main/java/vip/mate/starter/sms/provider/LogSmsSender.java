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

import lombok.extern.slf4j.Slf4j;
import vip.mate.base.channel.ProviderDescriptor;
import vip.mate.starter.sms.annotation.SmsProvider;
import vip.mate.starter.sms.model.SmsMessage;
import vip.mate.starter.sms.model.SmsResult;
import vip.mate.starter.sms.spi.AbstractSmsSender;

/**
 * Default provider — logs the message instead of dispatching it. Safe for local
 * dev / demo (verification codes are still stored in Redis by the caller).
 *
 * @author mateaix
 */
@Slf4j
@SmsProvider(value = "log", describe = "Log only (no real dispatch) — default for dev")
public class LogSmsSender extends AbstractSmsSender {

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.builder("log", "日志(开发)")
                .describe("仅打印日志,不真实下发(本地/演示)")
                .build();
    }

    @Override
    public SmsResult send(SmsMessage message) {
        log.info("[mate-sms:log] businessType={} mobile={} content={}",
                message.getBusinessType(), message.getMobile(), message.getContent());
        return SmsResult.ok(type(), null);
    }
}
