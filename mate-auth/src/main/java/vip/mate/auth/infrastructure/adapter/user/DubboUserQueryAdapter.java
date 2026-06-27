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
package vip.mate.auth.infrastructure.adapter.user;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.admin.service.IRpcPermissionService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.base.result.Result;

/**
 * Infrastructure implementation of {@link UserQueryPort}. Authenticates against
 * the back-office account store (<b>mate_admin</b>) via mate-system's
 * {@link IRpcPermissionService} and wraps each response in {@link AuthUser}.
 *
 * <p>Roles/permissions are NOT carried in these responses — they are resolved
 * separately at token-issue time (SaTokenIssuer → getRoleKeysByUsername).
 *
 * @author mateaix
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboUserQueryAdapter implements UserQueryPort {

    @DubboReference(check = false, timeout = 5000, retries = 1,
            version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
    private IRpcPermissionService rpcPermissionService;

    @Override
    public AuthUser findByAccount(Account account) {
        Result<UserInfoResponse> result = switch (account.kind()) {
            case USERNAME -> safeCall(() -> rpcPermissionService.getAdminForAuthByUsername(account.value()), "getAdminForAuthByUsername");
            case MOBILE   -> safeCall(() -> rpcPermissionService.getAdminForAuthByMobile(account.value()),   "getAdminForAuthByMobile");
            case EMAIL    ->
                    // EMAIL is not a native admin lookup — fall through to username
                    // as a heuristic. Add getAdminForAuthByEmail if a dedicated
                    // path is needed.
                    safeCall(() -> rpcPermissionService.getAdminForAuthByUsername(account.value()), "getAdminForAuthByUsername(email)");
        };
        return unwrap(result);
    }

    @Override
    public AuthUser findById(String userId) {
        Result<UserInfoResponse> result = safeCall(
                () -> rpcPermissionService.getAdminForAuthById(userId), "getAdminForAuthById");
        return unwrap(result);
    }

    private Result<UserInfoResponse> safeCall(DubboCall call, String label) {
        try {
            return call.invoke();
        } catch (Exception e) {
            log.error("[auth] Dubbo call {} failed: {}", label, e.getMessage(), e);
            throw new BizException(ResponseCode.INTERNAL_ERROR.getCode(),
                    "Failed to query user service");
        }
    }

    private AuthUser unwrap(Result<UserInfoResponse> result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess()) || result.getData() == null) {
            return null;
        }
        return AuthUser.from(result.getData());
    }

    @FunctionalInterface
    private interface DubboCall {
        Result<UserInfoResponse> invoke();
    }
}
