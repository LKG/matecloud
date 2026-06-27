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
package vip.mate.starter.sso.port.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.starter.sso.port.IdentityMappingPort;

/**
 * Default {@link IdentityMappingPort} backed by {@code mate_identity_user_mapping}
 * / {@code mate_identity_dept_mapping}. Plain JdbcTemplate (same approach as
 * mate-channel-starter's {@code DbChannelConfigStore}) — no ORM dependency.
 *
 * <p>NOTE: first cut keeps it tenant-agnostic (GLOBAL). Multi-tenant scoping is a
 * follow-up: add {@code tenant_id} to the predicates via the tenant context.
 *
 * @author mateaix
 */
public class JdbcIdentityMappingAdapter implements IdentityMappingPort {

    private final JdbcTemplate jdbc;

    public JdbcIdentityMappingAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> resolveUser(String provider, String externalId, String unionId) {
        String byExt = queryUser("provider = ? AND external_id = ?", provider, externalId);
        if (byExt != null) {
            return Optional.of(byExt);
        }
        if (unionId != null && !unionId.isBlank()) {
            return Optional.ofNullable(queryUser("provider = ? AND union_id = ?", provider, unionId));
        }
        return Optional.empty();
    }

    private String queryUser(String where, Object... args) {
        List<String> ids = jdbc.query(
                "SELECT principal_id FROM mate_identity_user_mapping WHERE " + where + " AND deleted = 0 LIMIT 1",
                (rs, rn) -> rs.getString(1), args);
        return ids.isEmpty() ? null : ids.get(0);
    }

    @Override
    public void bindUser(String provider, String externalId, String unionId, String localUserId) {
        int updated = jdbc.update(
                "UPDATE mate_identity_user_mapping SET principal_id = ?, union_id = ? "
                        + "WHERE provider = ? AND external_id = ? AND deleted = 0",
                localUserId, unionId, provider, externalId);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO mate_identity_user_mapping "
                            + "(id, provider, external_id, union_id, principal_id, deleted) VALUES (?, ?, ?, ?, ?, 0)",
                    uuid(), provider, externalId, unionId, localUserId);
        }
    }

    @Override
    public void unbindUser(String provider, String externalId) {
        jdbc.update("UPDATE mate_identity_user_mapping SET deleted = 1 "
                + "WHERE provider = ? AND external_id = ? AND deleted = 0", provider, externalId);
    }

    @Override
    public List<UserMapping> listUserMappings(String provider) {
        return jdbc.query(
                "SELECT external_id, union_id, principal_id FROM mate_identity_user_mapping "
                        + "WHERE provider = ? AND deleted = 0",
                (rs, rn) -> new UserMapping(rs.getString(1), rs.getString(2), rs.getString(3)),
                provider);
    }

    @Override
    public Optional<String> resolveDept(String provider, String externalId) {
        List<String> ids = jdbc.query(
                "SELECT local_dept_id FROM mate_identity_dept_mapping "
                        + "WHERE provider = ? AND external_id = ? AND deleted = 0 LIMIT 1",
                (rs, rn) -> rs.getString(1), provider, externalId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    @Override
    public void bindDept(String provider, String externalId, String localDeptId) {
        int updated = jdbc.update(
                "UPDATE mate_identity_dept_mapping SET local_dept_id = ? "
                        + "WHERE provider = ? AND external_id = ? AND deleted = 0",
                localDeptId, provider, externalId);
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO mate_identity_dept_mapping "
                            + "(id, provider, external_id, local_dept_id, deleted) VALUES (?, ?, ?, ?, 0)",
                    uuid(), provider, externalId, localDeptId);
        }
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void unbindDept(String provider, String externalId) {
        jdbc.update("UPDATE mate_identity_dept_mapping SET deleted = 1 "
                + "WHERE provider = ? AND external_id = ? AND deleted = 0", provider, externalId);
    }

    @Override
    public List<DeptMapping> listDeptMappings(String provider) {
        return jdbc.query(
                "SELECT external_id, local_dept_id FROM mate_identity_dept_mapping "
                        + "WHERE provider = ? AND deleted = 0",
                (rs, rn) -> new DeptMapping(rs.getString(1), rs.getString(2)),
                provider);
    }
}
