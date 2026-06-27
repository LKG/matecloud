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
import vip.mate.auth.application.command.LoginCommand;
import vip.mate.auth.domain.model.valobj.LoginType;
import vip.mate.base.exception.BizException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Spring-wired replacement for niuyin's static {@code LoginStrategyFactory}.
 * <p>
 * Spring injects every {@link LoginStrategy} bean, we key them by
 * {@link LoginStrategy#supportedType()} — no static Map, no {@code @PostConstruct}
 * registration, no magic strings.
 *
 * @author mateaix
 */
@Component
public class LoginStrategyFactory {

    private final Map<LoginType, LoginStrategy<? extends LoginCommand>> strategies;

    public LoginStrategyFactory(List<LoginStrategy<? extends LoginCommand>> beans) {
        Map<LoginType, LoginStrategy<? extends LoginCommand>> map = new EnumMap<>(LoginType.class);
        for (LoginStrategy<? extends LoginCommand> s : beans) {
            LoginType type = s.supportedType();
            if (map.containsKey(type)) {
                throw new IllegalStateException(
                        "Duplicate LoginStrategy for " + type
                                + ": " + s.getClass().getName()
                                + " vs " + map.get(type).getClass().getName());
            }
            map.put(type, s);
        }
        this.strategies = Collections.unmodifiableMap(map);
    }

    @SuppressWarnings("unchecked")
    public <T extends LoginCommand> LoginStrategy<T> get(LoginType type) {
        LoginStrategy<? extends LoginCommand> strategy = strategies.get(type);
        if (strategy == null) {
            throw new BizException("LOGIN_TYPE_UNSUPPORTED",
                    "No login strategy registered for " + type);
        }
        return (LoginStrategy<T>) strategy;
    }
}
