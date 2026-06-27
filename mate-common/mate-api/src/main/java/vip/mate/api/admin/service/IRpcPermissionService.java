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
package vip.mate.api.admin.service;

import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.Result;
import vip.mate.base.tenant.CrossTenantRpc;

import java.util.List;

/**
 * Dubbo RPC interface for permission operations.
 * Implemented by mate-admin, consumed by other services needing permission checks.
 *
 * <p>All lookups are authentication/authorization resolutions that occur before
 * a tenant context exists, so they carry {@link CrossTenantRpc} (allowed to run
 * without a tenant attachment in SCHEMA/DATASOURCE mode).
 *
 * @author mateaix
 */
public interface IRpcPermissionService {

    @CrossTenantRpc
    Result<List<String>> getPermissionsByAdminId(String adminId);

    @CrossTenantRpc
    Result<List<String>> getRoleKeysByAdminId(String adminId);

    /**
     * Lookup by username — used when the caller only has a username (e.g., auth
     * service authenticating through mate-system's user store but needing
     * mate-admin's RBAC).
     */
    @CrossTenantRpc
    Result<List<String>> getRoleKeysByUsername(String username);

    @CrossTenantRpc
    Result<List<String>> getPermissionsByUsername(String username);

    // ---- Back-office account authentication (mate_admin is the auth subject) ----
    // Return the full admin account (incl. password hash + status) so mate-auth
    // can verify credentials. Resolved before any tenant context exists.

    @CrossTenantRpc
    Result<UserInfoResponse> getAdminForAuthByUsername(String username);

    @CrossTenantRpc
    Result<UserInfoResponse> getAdminForAuthByMobile(String mobile);

    @CrossTenantRpc
    Result<UserInfoResponse> getAdminForAuthById(String id);
}
