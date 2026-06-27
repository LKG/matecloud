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
package vip.mate.starter.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.base.model.ModelConfigStore;
import vip.mate.base.model.ModelEndpoint;
import vip.mate.starter.channel.ChannelCrypto;

import java.util.List;
import java.util.Map;

/**
 * Shared, DB-backed {@link ModelConfigStore}. Reads the admin-managed model config from
 * the (shared) {@code mate_model_*} tables via {@link JdbcTemplate} — no MyBatis, no
 * Spring AI — so any service can include this starter and resolve the active endpoint
 * plus decrypted credentials that were configured in the admin console.
 *
 * <p>Resolution: if a gateway is enabled for the tenant, all types route through it;
 * otherwise the per-type default is looked up {@code TENANT} → {@code GLOBAL}, and the
 * referenced provider's {@code config_json} is decrypted with {@link ChannelCrypto}.
 *
 * @author mateaix
 */
public class DbModelConfigStore implements ModelConfigStore {

    private static final Logger log = LoggerFactory.getLogger(DbModelConfigStore.class);

    private final JdbcTemplate jdbc;
    private final ChannelCrypto crypto;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DbModelConfigStore(JdbcTemplate jdbc, ChannelCrypto crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    @Override
    public ModelEndpoint resolve(String modelType, String tenantId) {
        String tenant = (tenantId == null || tenantId.isBlank()) ? "0" : tenantId;
        try {
            ModelEndpoint gateway = resolveGateway(tenant);
            if (gateway != null) {
                return gateway;
            }
            return resolveLocal(modelType, tenant);
        } catch (Exception e) {
            log.debug("[model] resolve failed for {}/{}: {}", modelType, tenant, e.getMessage());
            return null;
        }
    }

    private ModelEndpoint resolveGateway(String tenant) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT base_url, token_cipher FROM mate_model_gateway "
                        + "WHERE tenant_id = ? AND enabled = 1 AND deleted = 0 LIMIT 1", tenant);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> r = rows.get(0);
        String baseUrl = asString(r.get("base_url"));
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String key = crypto.decrypt(asString(r.get("token_cipher")));
        return new ModelEndpoint(ModelEndpoint.SUPPLY_GATEWAY, "GATEWAY", baseUrl, key, null);
    }

    private ModelEndpoint resolveLocal(String modelType, String tenant) {
        Map<String, Object> def = queryDefault(modelType, tenant);
        if (def == null && !"0".equals(tenant)) {
            def = queryDefault(modelType, "0");
        }
        if (def == null) {
            return null;
        }
        String providerId = asString(def.get("provider_id"));
        String model = asString(def.get("model"));

        List<Map<String, Object>> providers = jdbc.queryForList(
                "SELECT vendor, config_json FROM mate_model_provider "
                        + "WHERE id = ? AND deleted = 0 LIMIT 1", providerId);
        if (providers.isEmpty()) {
            return null;
        }
        Map<String, Object> p = providers.get(0);
        String vendor = asString(p.get("vendor"));
        Map<String, Object> cfg = parse(asString(p.get("config_json")));
        String baseUrl = firstNonBlank(asString(cfg.get("baseUrl")), asString(cfg.get("endpoint")));
        String apiKey = firstNonBlank(asString(cfg.get("apiKey")), asString(cfg.get("token")));
        return new ModelEndpoint(ModelEndpoint.SUPPLY_LOCAL, vendor, baseUrl, apiKey, model);
    }

    private Map<String, Object> queryDefault(String modelType, String tenant) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT provider_id, model FROM mate_model_default "
                        + "WHERE tenant_id = ? AND model_type = ? AND deleted = 0 LIMIT 1",
                tenant, modelType);
        return rows.isEmpty() ? null : rows.get(0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String stored) {
        if (stored == null || stored.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(crypto.decrypt(stored), Map.class);
        } catch (Exception e) {
            log.warn("[model] failed to parse provider config_json: {}", e.getMessage());
            return Map.of();
        }
    }

    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
