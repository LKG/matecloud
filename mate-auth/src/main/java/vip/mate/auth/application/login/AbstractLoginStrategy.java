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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vip.mate.auth.application.command.LoginCommand;
import vip.mate.auth.domain.adapter.port.LoginAttemptPort;
import vip.mate.auth.domain.adapter.port.LoginAuditPort;
import vip.mate.auth.domain.adapter.port.TokenIssuerPort;
import vip.mate.auth.domain.event.UserLoggedInEvent;
import vip.mate.auth.domain.event.UserLoginFailedEvent;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.LoginResult;

import java.time.Instant;

/**
 * Template-method base for every login strategy. Subclasses implement the
 * {@link #verify(LoginCommand)} step; everything else (attempt tracking,
 * token issuance, audit) is handled here once.
 *
 * <p>Compared to niuyin's {@code AbstractLoginStrategyService}:
 * <ul>
 *   <li>No static factory / {@code @PostConstruct} registration — Spring
 *       auto-wires every strategy into {@link LoginStrategyFactory}.</li>
 *   <li>Token issuance is delegated to {@link TokenIssuerPort}, not hard-coded
 *       to a JwtUtil.</li>
 *   <li>Failed attempts emit an audit event and increment a counter so we
 *       can lock accounts under brute force.</li>
 * </ul>
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLoginStrategy<T extends LoginCommand> implements LoginStrategy<T> {

    protected final TokenIssuerPort tokenIssuer;
    protected final LoginAttemptPort attemptPort;
    protected final LoginAuditPort auditPort;

    /**
     * Extract the account identifier used for attempt tracking. For password
     * login this is the username; for SMS login this is the mobile number.
     */
    protected abstract String accountOf(T command);

    /**
     * Verify credentials and return the matching aggregate.
     */
    protected abstract AuthUser verify(T command);

    @Override
    public final LoginResult login(T command) {
        String account = accountOf(command);
        attemptPort.ensureNotLocked(account);

        AuthUser user;
        try {
            user = verify(command);
        } catch (RuntimeException ex) {
            int failures = attemptPort.recordFailure(account);
            auditPort.recordFailure(UserLoginFailedEvent.builder()
                    .account(account)
                    .loginType(supportedType())
                    .reason(ex.getMessage())
                    .at(Instant.now())
                    .build());
            log.warn("[auth] Login failed: type={} account={} failures={}",
                    supportedType(), account, failures);
            throw ex;
        }

        attemptPort.recordSuccess(account);
        LoginResult result = tokenIssuer.issue(user);
        auditPort.recordSuccess(UserLoggedInEvent.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .loginType(supportedType())
                .at(Instant.now())
                .build());
        log.info("[auth] Login succeeded: type={} userId={} username={}",
                supportedType(), user.getUserId(), user.getUsername());
        return result;
    }
}
