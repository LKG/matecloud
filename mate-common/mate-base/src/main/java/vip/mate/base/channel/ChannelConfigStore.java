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
package vip.mate.base.channel;

import java.util.Map;

/**
 * Seam between the capability starters (file / sms) and a persisted, admin-managed
 * configuration. Starters depend only on this interface; a business module (e.g.
 * mate-system) supplies a DB-backed bean so that the <b>active provider</b> and its
 * <b>credentials</b> come from runtime config instead of static properties — and can
 * therefore be edited in the admin console and scoped per tenant.
 *
 * <p>When no implementation is present (or it returns empty), starters fall back to
 * their static {@code *Properties}, so each starter still works standalone.
 *
 * @author mateaix
 */
public interface ChannelConfigStore {

    /**
     * The active provider type for a channel, or {@code null} to fall back to the
     * statically configured type.
     *
     * @param channel logical channel, e.g. {@code "storage"} or {@code "sms"}
     */
    String activeType(String channel);

    /**
     * The stored (decrypted) config for a specific provider, or an empty map to fall
     * back to static properties. Keys match the provider's {@link FieldSpec#key()}.
     *
     * @param channel logical channel
     * @param type    provider type, e.g. {@code "minio"} / {@code "aliyun"}
     */
    Map<String, String> config(String channel, String type);
}
