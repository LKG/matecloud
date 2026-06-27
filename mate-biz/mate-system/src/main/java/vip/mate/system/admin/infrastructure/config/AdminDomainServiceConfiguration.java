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
package vip.mate.system.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.MenuRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;
import vip.mate.system.admin.domain.permission.service.IPermissionDomainService;
import vip.mate.system.admin.domain.permission.service.impl.PermissionDomainServiceImpl;

/**
 * Wires the (framework-free) domain services of the {@code admin} context as
 * Spring beans, keeping the domain layer free of {@code @Service}.
 *
 * @author mateaix
 */
@Configuration
public class AdminDomainServiceConfiguration {

    @Bean
    public IPermissionDomainService permissionDomainService(AdminRepository adminRepository,
                                                            RoleRepository roleRepository,
                                                            MenuRepository menuRepository) {
        return new PermissionDomainServiceImpl(adminRepository, roleRepository, menuRepository);
    }
}
