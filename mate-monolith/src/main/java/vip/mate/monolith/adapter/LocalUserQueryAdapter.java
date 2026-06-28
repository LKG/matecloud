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
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.starter.tenant.core.TenantHelper;
import vip.mate.system.admin.application.query.IAdminQueryService;

/**
 * In-process implementation of {@link UserQueryPort} for monolith mode.
 * <p>
 * Authenticates against the back-office account store (<b>mate_admin</b>) via
 * mate-system's {@link IAdminQueryService} — the same source that
 * {@code DubboUserQueryAdapter} reaches over Dubbo through
 * {@code IRpcPermissionService#getAdminForAuthByUsername}. (NOT the end-user
 * store {@code mate_user} / {@code IUserQueryService} — admins like the seeded
 * {@code admin} account live in {@code mate_admin}.) {@code mate_admin} is a
 * global table, so lookups run under {@link TenantHelper#withoutTenant}.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalUserQueryAdapter implements UserQueryPort {

    private final IAdminQueryService adminQueryService;

    @Override
    public AuthUser findByAccount(Account account) {
        UserInfoResponse response = TenantHelper.withoutTenant(() -> switch (account.kind()) {
            case USERNAME -> adminQueryService.findForAuthByUsername(account.value());
            case MOBILE   -> adminQueryService.findForAuthByMobile(account.value());
            // EMAIL is not a native admin lookup — fall back to username (matches
            // DubboUserQueryAdapter's heuristic).
            case EMAIL    -> adminQueryService.findForAuthByUsername(account.value());
        });
        return response == null ? null : AuthUser.from(response);
    }

    @Override
    public AuthUser findById(String userId) {
        UserInfoResponse response = TenantHelper.withoutTenant(
                () -> adminQueryService.findForAuthById(userId));
        return response == null ? null : AuthUser.from(response);
    }
}
