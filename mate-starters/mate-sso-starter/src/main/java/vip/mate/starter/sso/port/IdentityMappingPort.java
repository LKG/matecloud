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
package vip.mate.starter.sso.port;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for the external-id ↔ local-id mapping. Unlike
 * {@link OrgProvisionPort}, the starter ships a default JDBC implementation over
 * the {@code mate_identity_*} tables, so a consumer normally need not implement it.
 *
 * <p>Local principal / department ids are {@code String} to match MateCloud's
 * id convention (VARCHAR(32) UUID / snowflake-as-string).
 *
 * @author mateaix
 */
public interface IdentityMappingPort {

    /** Resolve a local user id by external id, falling back to unionId. */
    Optional<String> resolveUser(String provider, String externalId, String unionId);

    /** Bind (or rebind) an external identity to a local user id. */
    void bindUser(String provider, String externalId, String unionId, String localUserId);

    /** Remove a user mapping (used by the UNLINK leave strategy). */
    void unbindUser(String provider, String externalId);

    /** All user mappings for a provider — used by FULL-sync prune (diff). */
    List<UserMapping> listUserMappings(String provider);

    /** Resolve a local department id by external id. */
    Optional<String> resolveDept(String provider, String externalId);

    /** Bind an external department to a local department id. */
    void bindDept(String provider, String externalId, String localDeptId);

    /** Remove a department mapping. */
    void unbindDept(String provider, String externalId);

    /** All department mappings for a provider — used by FULL-sync prune. */
    List<DeptMapping> listDeptMappings(String provider);

    /** A persisted user mapping row (external id ↔ local principal). */
    record UserMapping(String externalId, String unionId, String principalId) {
    }

    /** A persisted department mapping row. */
    record DeptMapping(String externalId, String localDeptId) {
    }
}
