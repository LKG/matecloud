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

import org.springframework.stereotype.Component;
import vip.mate.auth.application.command.SmsLoginCommand;
import vip.mate.auth.domain.adapter.port.LoginAttemptPort;
import vip.mate.auth.domain.adapter.port.LoginAuditPort;
import vip.mate.auth.domain.adapter.port.SmsCodePort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginType;
import vip.mate.auth.domain.service.IAuthDomainService;

/**
 * Mobile + SMS verification code login strategy.
 *
 * @author mateaix
 */
@Component
public class SmsLoginStrategy extends AbstractLoginStrategy<SmsLoginCommand> {

    private final IAuthDomainService authDomainService;
    private final SmsCodePort smsCodePort;

    public SmsLoginStrategy(TokenIssuerPort tokenIssuer,
                            LoginAttemptPort attemptPort,
                            LoginAuditPort auditPort,
                            IAuthDomainService authDomainService,
                            SmsCodePort smsCodePort) {
        super(tokenIssuer, attemptPort, auditPort);
        this.authDomainService = authDomainService;
        this.smsCodePort = smsCodePort;
    }

    @Override
    public LoginType supportedType() {
        return LoginType.SMS;
    }

    @Override
    protected String accountOf(SmsLoginCommand command) {
        return command.getMobile();
    }

    @Override
    protected AuthUser verify(SmsLoginCommand command) {
        // One-time-use consume; throws on mismatch/expired
        smsCodePort.verifyLoginCode(command.getMobile(), command.getCode());
        return authDomainService.loadByMobileForSmsLogin(command.getMobile());
    }
}
