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
package vip.mate.auth.domain.model.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import vip.mate.api.system.enums.UserStatus;
import vip.mate.auth.domain.adapter.port.PasswordEncoderPort;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

import java.util.List;

/**
 * Authentication-side view of a user. This is a DDD aggregate root that
 * encapsulates the invariants mate-auth cares about (active status, password
 * match, role list). The write model lives in mate-system; we only read.
 *
 * <p>The ctor is private — construct via {@link #from(UserInfoResponse)} so
 * that any {@code null}s from the RPC boundary are handled in one place.
 *
 * @author mateaix
 */
@Getter
@Builder
@AllArgsConstructor
public class AuthUser {

    private final String userId;
    private final String username;
    private final String realName;
    private final String mobile;
    private final String email;
    /** BCrypt-hashed password. Non-null only on the auth query path. */
    @JsonIgnore
    private final String passwordHash;
    private final UserStatus status;
    private final List<String> roleCodes;
    private final List<String> permissions;
    /**
     * Tenant ID the user belongs to. {@code null} for legacy single-tenant
     * deployments; downstream consumers (Sa-Token session) substitute the
     * default system tenant.
     */
    private final String tenantId;

    /**
     * Build an {@code AuthUser} from the shared {@link UserInfoResponse} DTO.
     */
    public static AuthUser from(UserInfoResponse response) {
        if (response == null) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "Invalid account or password");
        }
        return AuthUser.builder()
                .userId(response.getUserId())
                .username(response.getUsername())
                .realName(response.getRealName())
                .mobile(response.getMobile())
                .email(response.getEmail())
                .passwordHash(response.getPassword())
                .status(response.getStatus())
                .roleCodes(response.getRoleCodes())
                .permissions(response.getPermissions())
                .tenantId(response.getTenantId())
                .build();
    }

    /**
     * Throws {@link BizException} if the account is not in a state that
     * allows logging in.
     */
    public void ensureActive() {
        if (status == null || status != UserStatus.ACTIVE) {
            throw new BizException(ResponseCode.FORBIDDEN.getCode(),
                    "Account is frozen, disabled, or deleted");
        }
    }

    /**
     * Verify a plain-text password against the stored hash.
     * @param rawPassword the plain-text password to verify
     * @param encoder port for password matching (injected from infrastructure)
     * @throws BizException if the password does not match
     */
    public void verifyPassword(String rawPassword, PasswordEncoderPort encoder) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "Invalid account or password");
        }
        if (passwordHash == null || !encoder.matches(rawPassword, passwordHash)) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "Invalid account or password");
        }
    }

    public boolean hasMobile() {
        return mobile != null && !mobile.isBlank();
    }
}
