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
package vip.mate.system.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.mate.system.domain.adapter.port.PasswordEncoderPort;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.service.IUserDomainService;
import vip.mate.system.domain.service.impl.UserDomainServiceImpl;

/**
 * Wires the (framework-free) domain services of the {@code system} context as
 * Spring beans. Keeping the {@code @Bean} factory here — not {@code @Service} on
 * the domain class — preserves the rule that the domain layer carries zero
 * framework dependencies.
 *
 * @author mateaix
 */
@Configuration
public class DomainServiceConfiguration {

    @Bean
    public IUserDomainService userDomainService(UserRepository userRepository,
                                                PasswordEncoderPort passwordEncoder) {
        return new UserDomainServiceImpl(userRepository, passwordEncoder);
    }
}
