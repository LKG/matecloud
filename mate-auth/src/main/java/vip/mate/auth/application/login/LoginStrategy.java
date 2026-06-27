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

import vip.mate.auth.application.command.LoginCommand;
import vip.mate.auth.domain.model.valobj.LoginResult;
import vip.mate.auth.domain.model.valobj.LoginType;

/**
 * Strategy contract for one login flavor (password / SMS / ...).
 * <p>
 * The {@link #supportedType()} method replaces niuyin's string-keyed
 * {@code @PostConstruct} registration — Spring auto-wires every
 * {@code LoginStrategy} into {@link LoginStrategyFactory} and keys them by
 * their enum type, so there is no magic string.
 *
 * @author mateaix
 */
public interface LoginStrategy<T extends LoginCommand> {

    /** Which {@link LoginType} this strategy handles. */
    LoginType supportedType();

    /**
     * Execute the login. Implementations should delegate every verification
     * step to the {@code AuthUser} aggregate and rely on injected ports for
     * side effects.
     */
    LoginResult login(T command);
}
