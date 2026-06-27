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
package vip.mate.starter.datascope;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * Auto-configuration for data scope (data permission) support.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class DataScopeAutoConfiguration {

    /**
     * Exposed as an ordered {@code InnerInterceptor} bean so ds-starter
     * collects it into the MyBatis-Plus chain after the tenant-line
     * interceptor ({@code @Order(10)}) and before pagination. Only rewrites
     * SQL when a {@code DataScopeContext} is set on the current thread.
     */
    @Bean
    @Order(20)
    public DataScopeInterceptor dataScopeInterceptor() {
        return new DataScopeInterceptor();
    }

    @Bean
    public DataScopeAspect dataScopeAspect() {
        return new DataScopeAspect();
    }
}
