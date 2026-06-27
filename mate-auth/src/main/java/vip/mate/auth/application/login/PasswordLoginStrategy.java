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
import vip.mate.auth.application.command.PasswordLoginCommand;
import vip.mate.auth.domain.adapter.port.LoginAttemptPort;
import vip.mate.auth.domain.adapter.port.LoginAuditPort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.auth.domain.model.valobj.LoginType;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.auth.infrastructure.adapter.security.PasswordTransportDecryptor;

/**
 * Username / mobile / email + password login strategy.
 *
 * @author mateaix
 */
@Component
public class PasswordLoginStrategy extends AbstractLoginStrategy<PasswordLoginCommand> {

    private final IAuthDomainService authDomainService;
    private final PasswordTransportDecryptor transportDecryptor;

    public PasswordLoginStrategy(TokenIssuerPort tokenIssuer,
                                 LoginAttemptPort attemptPort,
                                 LoginAuditPort auditPort,
                                 IAuthDomainService authDomainService,
                                 PasswordTransportDecryptor transportDecryptor) {
        super(tokenIssuer, attemptPort, auditPort);
        this.authDomainService = authDomainService;
        this.transportDecryptor = transportDecryptor;
    }

    @Override
    public LoginType supportedType() {
        return LoginType.PASSWORD;
    }

    @Override
    protected String accountOf(PasswordLoginCommand command) {
        return command.getAccount();
    }

    @Override
    protected AuthUser verify(PasswordLoginCommand command) {
        Account account = Account.of(command.getAccount());
        // Decrypt AES-CFB transport-encrypted password from frontend
        String rawPassword = transportDecryptor.decrypt(command.getPassword());
        return authDomainService.loadAndVerify(account, rawPassword);
    }
}
