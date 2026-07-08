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
package vip.mate.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.mate.auth.domain.adapter.port.PasswordEncoderPort;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.service.IAuthDomainService;
import vip.mate.auth.domain.service.impl.AuthDomainServiceImpl;

/**
 * Wires the (framework-free) domain services of the {@code auth} context as
 * Spring beans. Keeping the {@code @Bean} factory here — not {@code @Service} on
 * the domain class — preserves the rule that the domain layer carries zero
 * framework dependencies. Mirrors mate-system's {@code DomainServiceConfiguration}.
 *
 * @author mateaix
 */
@Configuration
public class DomainServiceConfiguration {

    @Bean
    public IAuthDomainService authDomainService(UserQueryPort userQueryPort,
                                                PasswordEncoderPort passwordEncoder) {
        return new AuthDomainServiceImpl(userQueryPort, passwordEncoder);
    }
}
