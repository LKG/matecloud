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
package vip.mate.auth.application.login;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.auth.application.command.LdapLoginCommand;
import vip.mate.auth.domain.adapter.port.LoginAttemptPort;
import vip.mate.auth.domain.adapter.port.LoginAuditPort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginType;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.starter.sso.core.SsoTemplate;
import vip.mate.starter.sso.port.IdentityMappingPort;
import vip.mate.starter.sso.spi.model.AuthRequest;
import vip.mate.starter.sso.spi.model.ExternalUser;

/**
 * LDAP / AD login. Verifies username + password against the directory (bind) via
 * mate-sso-starter's LdapProvider, resolves the bound local user, and issues the
 * session. Only active when {@code mate.feature.sso.enabled=true}.
 *
 * @author mateaix
 */
@Component
@ConditionalOnProperty(prefix = "mate.feature.sso", name = "enabled", havingValue = "true")
public class LdapLoginStrategy extends AbstractLoginStrategy<LdapLoginCommand> {

    private static final String PROVIDER = "ldap";

    private final SsoTemplate ssoTemplate;
    private final IdentityMappingPort identityMappingPort;
    private final IAuthDomainService authDomainService;

    public LdapLoginStrategy(TokenIssuerPort tokenIssuer,
                             LoginAttemptPort attemptPort,
                             LoginAuditPort auditPort,
                             SsoTemplate ssoTemplate,
                             IdentityMappingPort identityMappingPort,
                             IAuthDomainService authDomainService) {
        super(tokenIssuer, attemptPort, auditPort);
        this.ssoTemplate = ssoTemplate;
        this.identityMappingPort = identityMappingPort;
        this.authDomainService = authDomainService;
    }

    @Override
    public LoginType supportedType() {
        return LoginType.LDAP;
    }

    @Override
    protected String accountOf(LdapLoginCommand command) {
        return command.getUsername();
    }

    @Override
    protected AuthUser verify(LdapLoginCommand command) {
        ExternalUser ext = ssoTemplate.authenticate(PROVIDER,
                AuthRequest.ofCredentials(command.getUsername(), command.getPassword()));
        String localUserId = identityMappingPort
                .resolveUser(PROVIDER, ext.externalId(), ext.unionId())
                .orElseThrow(() -> new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                        "当前 LDAP 账号未录入系统,请联系管理员同步"));
        AuthUser user = authDomainService.loadById(localUserId);
        user.ensureActive();
        return user;
    }
}
