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
package vip.mate.base.model;

/**
 * Read-only seam between the AI execution layer (mate-model-starter) and the
 * admin-managed model configuration persisted by mate-system.
 *
 * <p>Mirrors {@link vip.mate.base.channel.ChannelConfigStore}: the starter depends only
 * on this interface, while a DB-backed implementation (pure {@code JdbcTemplate} + AES
 * decryption, no Spring AI) supplies the resolved endpoint so credentials come from
 * runtime config instead of static properties and can be scoped per tenant.
 *
 * <p>Resolution order is {@code TENANT} → {@code GLOBAL}: a tenant-scoped default
 * overrides the global one for the same {@code modelType}.
 *
 * @author mateaix
 */
public interface ModelConfigStore {

    /**
     * Resolve the active endpoint for a model type within a tenant.
     *
     * @param modelType logical model type, e.g. {@code LLM} / {@code EMBEDDING} /
     *                  {@code RERANK} / {@code TTS} / {@code STT} / {@code IMAGE} /
     *                  {@code VIDEO} / {@code MODERATION}
     * @param tenantId  current tenant id ({@code "0"} when multi-tenancy is off)
     * @return the resolved {@link ModelEndpoint}, or {@code null} when nothing is
     *         configured for the given type/tenant
     */
    ModelEndpoint resolve(String modelType, String tenantId);
}
