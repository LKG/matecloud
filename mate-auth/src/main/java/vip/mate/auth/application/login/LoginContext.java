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
import org.springframework.stereotype.Service;
import vip.mate.auth.application.command.LoginCommand;
import vip.mate.auth.domain.model.valobj.LoginResult;

/**
 * Façade over the strategy factory. Callers (REST controllers, services, ...)
 * hand in any {@link LoginCommand} and get back a {@link LoginResult}.
 * <p>
 * The whole login pipeline — attempt tracking, credential verification,
 * token issuance, audit — runs inside the chosen strategy.
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class LoginContext {

    private final LoginStrategyFactory factory;

    public <T extends LoginCommand> LoginResult execute(T command) {
        LoginStrategy<T> strategy = factory.get(command.loginType());
        return strategy.login(command);
    }
}
