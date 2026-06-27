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
package vip.mate.system.admin.infrastructure.datascope;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.mate.system.admin.domain.permission.adapter.repository.AdminRepository;
import vip.mate.system.admin.domain.permission.adapter.repository.RoleRepository;

/**
 * Registers {@link DataScopeSessionInterceptor} so every request resolves the
 * current admin's data scope into the Sa-Token session before any
 * {@code @DataPermission} query runs.
 *
 * @author mateaix
 */
@Configuration
@RequiredArgsConstructor
public class DataScopeWebConfig implements WebMvcConfigurer {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DataScopeSessionInterceptor(adminRepository, roleRepository))
                .addPathPatterns("/**");
    }
}
