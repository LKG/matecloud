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
package vip.mate.starter.menu;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Auto-configuration for startup-time menu self-registration. Active when Dubbo
 * is on the classpath and {@code mate.menu.auto-register} is not explicitly
 * disabled (defaults to enabled).
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.dubbo.config.annotation.DubboReference")
@ConditionalOnProperty(prefix = "mate.menu", name = "auto-register",
        havingValue = "true", matchIfMissing = true)
public class MenuStarterAutoConfiguration {

    @Bean
    public MenuAutoRegistrar menuAutoRegistrar(Environment environment) {
        return new MenuAutoRegistrar(environment);
    }
}
