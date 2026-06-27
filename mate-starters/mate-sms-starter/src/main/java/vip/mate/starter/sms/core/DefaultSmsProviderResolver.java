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

import org.springframework.beans.factory.ObjectProvider;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.sms.config.SmsProperties;
import vip.mate.starter.sms.spi.AbstractSmsSender;

/**
 * Default resolver — prefers the admin-managed active provider from the
 * {@link ChannelConfigStore} (if present), else falls back to the statically
 * configured {@code mate.sms.provider}.
 *
 * @author mateaix
 */
public class DefaultSmsProviderResolver implements SmsProviderResolver {

    private final SmsProperties properties;
    private final ObjectProvider<ChannelConfigStore> configStore;

    public DefaultSmsProviderResolver(SmsProperties properties,
                                      ObjectProvider<ChannelConfigStore> configStore) {
        this.properties = properties;
        this.configStore = configStore;
    }

    @Override
    public String resolveProvider() {
        ChannelConfigStore store = configStore == null ? null : configStore.getIfAvailable();
        if (store != null) {
            String active = store.activeType(AbstractSmsSender.CHANNEL);
            if (active != null && !active.isBlank()) {
                return active;
            }
        }
        return properties.getProvider();
    }
}
