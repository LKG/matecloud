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
package vip.mate.starter.file.core;

import org.springframework.beans.factory.ObjectProvider;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.file.config.FileProperties;
import vip.mate.starter.file.spi.AbstractFileStorage;

/**
 * Default resolver — prefers the admin-managed active type from the
 * {@link ChannelConfigStore} (if present), else falls back to the statically
 * configured {@code mate.file.type}.
 *
 * @author mateaix
 */
public class DefaultFileStorageResolver implements FileStorageResolver {

    private final FileProperties properties;
    private final ObjectProvider<ChannelConfigStore> configStore;

    public DefaultFileStorageResolver(FileProperties properties,
                                      ObjectProvider<ChannelConfigStore> configStore) {
        this.properties = properties;
        this.configStore = configStore;
    }

    @Override
    public String resolveType() {
        ChannelConfigStore store = configStore == null ? null : configStore.getIfAvailable();
        if (store != null) {
            String active = store.activeType(AbstractFileStorage.CHANNEL);
            if (active != null && !active.isBlank()) {
                return active;
            }
        }
        return properties.getType();
    }
}
