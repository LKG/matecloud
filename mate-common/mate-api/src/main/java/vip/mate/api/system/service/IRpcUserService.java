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
package vip.mate.api.system.service;

import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.Result;
import vip.mate.base.tenant.CrossTenantRpc;

/**
 * Dubbo RPC interface for user operations.
 *
 * <p>These resolve a user by a global identifier as part of authentication,
 * before a tenant is established — hence {@link CrossTenantRpc}. See that
 * annotation for the SCHEMA/DATASOURCE fail-closed semantics.
 *
 * @author mateaix
 */
public interface IRpcUserService {

    @CrossTenantRpc
    Result<UserInfoResponse> getUserById(String userId);

    @CrossTenantRpc
    Result<UserInfoResponse> getUserByUsername(String username);

    @CrossTenantRpc
    Result<UserInfoResponse> getUserByMobile(String mobile);

    /**
     * NOT {@link CrossTenantRpc}: registration is a write, not an
     * authentication lookup. In SCHEMA/DATASOURCE mode it must run with a tenant
     * context (so the user lands in that tenant's database); a no-tenant call is
     * rejected by the provider filter rather than writing to master.
     */
    Result<UserInfoResponse> registerUser(RegisterUserCommand command);
}
