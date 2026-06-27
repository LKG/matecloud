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
import vip.mate.auth.application.command.SsoLoginCommand;
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
 * Third-party SSO login (企业微信 / 钉钉 / 飞书). Delegates the external
 * authentication to mate-sso-starter's {@link SsoTemplate}, resolves the bound
 * local user via {@link IdentityMappingPort}, and lets {@link AbstractLoginStrategy}
 * issue the Sa-Token session + audit.
 *
 * <p>Only active when {@code mate.feature.sso.enabled=true} (same flag that
 * activates the starter, so {@link SsoTemplate} is guaranteed present).
 *
 * <p>The user must already be bound (provisioned by an organization sync) — an
 * unbound external identity is rejected, mirroring PlayEdu's behaviour.
 *
 * @author mateaix
 */
@Component
@ConditionalOnProperty(prefix = "mate.feature.sso", name = "enabled", havingValue = "true")
public class SsoLoginStrategy extends AbstractLoginStrategy<SsoLoginCommand> {

    private final SsoTemplate ssoTemplate;
    private final IdentityMappingPort identityMappingPort;
    private final IAuthDomainService authDomainService;

    public SsoLoginStrategy(TokenIssuerPort tokenIssuer,
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
        return LoginType.SSO;
    }

    @Override
    protected String accountOf(SsoLoginCommand command) {
        return command.getProviderCode();
    }

    @Override
    protected AuthUser verify(SsoLoginCommand command) {
        ExternalUser ext = ssoTemplate.authenticate(command.getProviderCode(),
                AuthRequest.ofCode(command.getCode(), command.getState(), command.getRedirectUri()));
        String localUserId = identityMappingPort
                .resolveUser(command.getProviderCode(), ext.externalId(), ext.unionId())
                .orElseThrow(() -> new BizException(ResponseCode.UNAUTHORIZED.getCode(),
                        "当前账号未绑定系统用户,请联系管理员同步组织"));
        AuthUser user = authDomainService.loadById(localUserId);
        user.ensureActive();
        return user;
    }
}
