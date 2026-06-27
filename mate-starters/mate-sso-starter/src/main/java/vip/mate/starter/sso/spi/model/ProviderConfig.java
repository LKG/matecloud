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
package vip.mate.starter.sso.spi.model;

import java.util.Collections;
import java.util.Map;

/**
 * Decrypted, per-provider configuration (corpId / secret / agentId / ldap url …),
 * sourced from {@code mate_channel_config} (channel = {@code "identity"}) via
 * {@link vip.mate.base.channel.ChannelConfigStore}. Thin typed wrapper over a map
 * whose keys match the provider's {@code ProviderDescriptor} field keys.
 *
 * @author mateaix
 */
public final class ProviderConfig {

    private final Map<String, String> values;

    public ProviderConfig(Map<String, String> values) {
        this.values = values == null ? Map.of() : Collections.unmodifiableMap(values);
    }

    public String get(String key) {
        return values.get(key);
    }

    public String get(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public String require(String key) {
        String v = values.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required SSO config: " + key);
        }
        return v;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Map<String, String> asMap() {
        return values;
    }
}
