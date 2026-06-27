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
package vip.mate.auth.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vip.mate.starter.sso.core.SsoTemplate;

import java.util.List;
import java.util.Map;
import vip.mate.auth.application.command.LdapLoginCommand;
import vip.mate.auth.application.command.PasswordLoginCommand;
import vip.mate.auth.application.command.SmsLoginCommand;
import vip.mate.auth.application.command.SsoLoginCommand;
import vip.mate.auth.application.login.LoginContext;
import vip.mate.auth.domain.adapter.port.SmsCodePort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;

import java.util.ArrayList;
import java.util.Set;

/**
 * Application service: single entry point for login / logout / currentUser.
 * Delegates to {@link LoginContext} for the strategy-based login pipeline.
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class AuthAppService {

    private static final String ROLE_KEY_PREFIX = "mate:user:role:";
    private static final String PERM_KEY_PREFIX = "mate:user:perm:";

    private final LoginContext loginContext;
    private final TokenIssuerPort tokenIssuer;
    private final IAuthDomainService authDomainService;
    private final SmsCodePort smsCodePort;
    private final StringRedisTemplate stringRedisTemplate;
    /** Present only when mate.feature.sso.enabled=true (mate-sso-starter). */
    private final ObjectProvider<SsoTemplate> ssoTemplateProvider;

    public LoginResult passwordLogin(PasswordLoginCommand command) {
        return loginContext.execute(command);
    }

    public LoginResult smsLogin(SmsLoginCommand command) {
        return loginContext.execute(command);
    }

    public LoginResult ssoLogin(SsoLoginCommand command) {
        return loginContext.execute(command);
    }

    public LoginResult ldapLogin(LdapLoginCommand command) {
        return loginContext.execute(command);
    }

    /**
     * Public list of configured SSO providers (non-secret config) for the login
     * page. Empty when SSO is disabled.
     */
    public List<Map<String, Object>> ssoProviders() {
        SsoTemplate ssoTemplate = ssoTemplateProvider.getIfAvailable();
        return ssoTemplate == null ? List.of() : ssoTemplate.publicProviders();
    }

    public void sendSmsCode(String mobile) {
        smsCodePort.sendLoginCode(mobile);
    }

    public void logout() {
        tokenIssuer.revokeCurrentSession();
    }

    public AuthUser currentUser() {
        String userId = tokenIssuer.currentUserId();
        if (userId == null) {
            throw new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                    "Not logged in");
        }
        AuthUser user = authDomainService.loadById(userId);

        // Enrich with cached permissions/roles from Redis (written by SaTokenIssuer at login)
        Set<String> perms = stringRedisTemplate.opsForSet().members(PERM_KEY_PREFIX + userId);
        Set<String> roles = stringRedisTemplate.opsForSet().members(ROLE_KEY_PREFIX + userId);
        return AuthUser.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .mobile(user.getMobile())
                .email(user.getEmail())
                .passwordHash(null) // never expose
                .status(user.getStatus())
                .roleCodes(roles != null ? new ArrayList<>(roles) : new ArrayList<>())
                .permissions(perms != null ? new ArrayList<>(perms) : new ArrayList<>())
                .build();
    }
}
