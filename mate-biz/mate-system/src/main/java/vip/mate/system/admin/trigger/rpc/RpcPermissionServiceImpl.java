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
package vip.mate.system.admin.trigger.rpc;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.system.admin.application.query.IAdminQueryService;
import vip.mate.system.admin.domain.permission.service.IPermissionDomainService;
import vip.mate.api.admin.service.IRpcPermissionService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.Result;
import vip.mate.starter.tenant.core.TenantHelper;

import java.util.List;

/**
 * Role/permission lookup for gateway / auth Sa-Token integration. Resolves by
 * admin id or username during authentication — before a tenant context exists —
 * so it runs cross-tenant via {@link TenantHelper#withoutTenant}; otherwise the
 * fail-closed tenant interceptor would reject these no-context mate_admin /
 * mate_role reads.
 *
 * @author mateaix
 */
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
@RequiredArgsConstructor
public class RpcPermissionServiceImpl implements IRpcPermissionService {

    private final IPermissionDomainService permissionDomainService;
    private final IAdminQueryService adminQueryService;

    @Override
    public Result<List<String>> getPermissionsByAdminId(String adminId) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> permissionDomainService.findPermissionsByAdminId(adminId)));
    }

    @Override
    public Result<List<String>> getRoleKeysByAdminId(String adminId) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> permissionDomainService.findRoleKeysByAdminId(adminId)));
    }

    @Override
    public Result<List<String>> getRoleKeysByUsername(String username) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> permissionDomainService.findRoleKeysByUsername(username)));
    }

    @Override
    public Result<List<String>> getPermissionsByUsername(String username) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> permissionDomainService.findPermissionsByUsername(username)));
    }

    @Override
    public Result<UserInfoResponse> getAdminForAuthByUsername(String username) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> adminQueryService.findForAuthByUsername(username)));
    }

    @Override
    public Result<UserInfoResponse> getAdminForAuthByMobile(String mobile) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> adminQueryService.findForAuthByMobile(mobile)));
    }

    @Override
    public Result<UserInfoResponse> getAdminForAuthById(String id) {
        return Result.ok(TenantHelper.withoutTenant(
                () -> adminQueryService.findForAuthById(id)));
    }
}
