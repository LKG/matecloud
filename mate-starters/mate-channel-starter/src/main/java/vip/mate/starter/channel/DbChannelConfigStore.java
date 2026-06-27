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
package vip.mate.starter.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.base.channel.ChannelConfigStore;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared, DB-backed {@link ChannelConfigStore}. Reads admin-managed config from the
 * (shared) {@code mate_channel_config} table via {@link JdbcTemplate} — no MyBatis
 * mapper, so any service can include this starter and immediately resolve the active
 * provider + decrypted credentials that were configured in the admin console.
 *
 * <p>{@code config_json} is decrypted with {@link ChannelCrypto}; MAP/object fields
 * are re-serialized to JSON strings so providers parse them uniformly.
 *
 * @author mateaix
 */
public class DbChannelConfigStore implements ChannelConfigStore {

    private static final Logger log = LoggerFactory.getLogger(DbChannelConfigStore.class);

    private final JdbcTemplate jdbc;
    private final ChannelCrypto crypto;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DbChannelConfigStore(JdbcTemplate jdbc, ChannelCrypto crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    @Override
    public String activeType(String channel) {
        try {
            List<String> rows = jdbc.query(
                    "SELECT provider_type FROM mate_channel_config "
                            + "WHERE channel = ? AND enabled = 1 AND deleted = 0 LIMIT 1",
                    (rs, i) -> rs.getString(1), channel);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.debug("[channel] activeType query failed for {}: {}", channel, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, String> config(String channel, String type) {
        String json;
        try {
            List<String> rows = jdbc.query(
                    "SELECT config_json FROM mate_channel_config "
                            + "WHERE channel = ? AND provider_type = ? AND deleted = 0 LIMIT 1",
                    (rs, i) -> rs.getString(1), channel, type);
            if (rows.isEmpty() || rows.get(0) == null) {
                return Map.of();
            }
            json = crypto.decrypt(rows.get(0));
        } catch (Exception e) {
            log.debug("[channel] config query failed for {}:{}: {}", channel, type, e.getMessage());
            return Map.of();
        }
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(json, Map.class);
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                Object v = e.getValue();
                if (v == null) {
                    continue;
                }
                out.put(e.getKey(), v instanceof String s ? s : objectMapper.writeValueAsString(v));
            }
            return out;
        } catch (Exception e) {
            log.warn("[channel] failed to parse config_json for {}:{} — {}", channel, type, e.getMessage());
            return Map.of();
        }
    }
}
