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
package vip.mate.starter.tenant.core;

/**
 * Multi-tenant isolation strategy.
 *
 * <p>Only {@link #COLUMN} is implemented today. {@link #SCHEMA} and
 * {@link #DATASOURCE} are reserved for a follow-up phase that introduces
 * dynamic-datasource routing; declaring them here lets configuration and the
 * resolver chain stay forward-compatible.
 *
 * @author mateaix
 */
public enum MultiTenantType {

    /** No isolation — single-tenant deployment. */
    NONE,

    /** Row-level: append {@code WHERE tenant_id = ?} to whitelisted tables. */
    COLUMN,

    /** One schema per tenant (reserved, not yet implemented). */
    SCHEMA,

    /** One datasource per tenant (reserved, not yet implemented). */
    DATASOURCE
}
