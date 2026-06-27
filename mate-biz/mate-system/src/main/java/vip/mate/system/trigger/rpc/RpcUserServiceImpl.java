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
package vip.mate.system.trigger.rpc;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.api.system.service.IRpcUserService;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;

/**
 * Identity lookups consumed by the auth service and gateway. These resolve a
 * user by a globally-unique identifier (id / username / mobile) as part of
 * authentication, which happens BEFORE a tenant is established — so they run
 * cross-tenant via {@link TenantHelper#withoutTenant}. Without that, the
 * fail-closed tenant interceptor would reject these no-context mate_user reads.
 *
 * @author mateaix
 */
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
@RequiredArgsConstructor
public class RpcUserServiceImpl implements IRpcUserService {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    @Override
    public Result<UserInfoResponse> getUserById(String userId) {
        UserInfoResponse info = TenantHelper.withoutTenant(() -> userQueryService.findById(userId));
        return info != null ? Result.ok(info) : Result.fail(ResponseCode.DATA_NOT_FOUND);
    }

    @Override
    public Result<UserInfoResponse> getUserByUsername(String username) {
        // Return password for auth RPC
        UserInfoResponse info = TenantHelper.withoutTenant(
                () -> userQueryService.findByUsernameForAuth(username));
        return info != null ? Result.ok(info) : Result.fail(ResponseCode.DATA_NOT_FOUND);
    }

    @Override
    public Result<UserInfoResponse> getUserByMobile(String mobile) {
        // Return password for auth RPC
        UserInfoResponse info = TenantHelper.withoutTenant(
                () -> userQueryService.findByMobileForAuth(mobile));
        return info != null ? Result.ok(info) : Result.fail(ResponseCode.DATA_NOT_FOUND);
    }

    @Override
    public Result<UserInfoResponse> registerUser(RegisterUserCommand command) {
        UserInfoResponse info = TenantHelper.withoutTenant(() -> {
            String userId = userCommandService.createUser(command);
            return userQueryService.findById(userId);
        });
        return Result.ok(info);
    }
}
