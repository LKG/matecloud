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
package vip.mate.starter.sso.core;

import org.springframework.beans.factory.ObjectProvider;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.sso.spi.model.ProviderConfig;

/**
 * Reads decrypted per-provider config from {@code mate_channel_config}
 * (channel = {@code "identity"}) via the shared {@link ChannelConfigStore}, so SSO
 * credentials are admin-managed + AES-encrypted + tenant-scoped — exactly like
 * file/sms starters. When no store is present, returns an empty config.
 *
 * @author mateaix
 */
public class SsoConfigStore {

    /** Logical channel name in {@code mate_channel_config}. */
    public static final String CHANNEL = "identity";

    private final ObjectProvider<ChannelConfigStore> delegate;

    public SsoConfigStore(ObjectProvider<ChannelConfigStore> delegate) {
        this.delegate = delegate;
    }

    /** Decrypted config for a provider (e.g. {@code "wechat_work"}). */
    public ProviderConfig config(String providerCode) {
        ChannelConfigStore store = delegate.getIfAvailable();
        if (store == null) {
            return new ProviderConfig(null);
        }
        return new ProviderConfig(store.config(CHANNEL, providerCode));
    }

    /** The active/default provider code for the identity channel, if configured. */
    public String activeProvider() {
        ChannelConfigStore store = delegate.getIfAvailable();
        return store == null ? null : store.activeType(CHANNEL);
    }
}
