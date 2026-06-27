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
package vip.mate.starter.security.config;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.mate.starter.security.audit.AuditLogAspect;
import vip.mate.starter.security.encrypt.EncryptProperties;
import vip.mate.starter.security.encrypt.EncryptTypeHandler;
import vip.mate.starter.security.ratelimit.RateLimitAspect;
import vip.mate.starter.security.repeatsubmit.RepeatSubmitAspect;
import vip.mate.starter.security.sign.ApiSignInterceptor;
import vip.mate.starter.security.sign.AppKeyProvider;
import vip.mate.starter.security.sign.CachedBodyFilter;

/**
 * Auto-configuration for mate-security-starter.
 *
 * @author mateaix
 */
@AutoConfiguration
@RequiredArgsConstructor
@EnableConfigurationProperties(EncryptProperties.class)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private final ObjectProvider<ApiSignInterceptor> apiSignInterceptorProvider;

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RateLimitAspect rateLimitAspect(RedissonClient redissonClient) {
        return new RateLimitAspect(redissonClient);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public RepeatSubmitAspect repeatSubmitAspect(RedissonClient redissonClient) {
        return new RepeatSubmitAspect(redissonClient);
    }

    @Bean
    public AuditLogAspect auditLogAspect(ApplicationEventPublisher publisher) {
        return new AuditLogAspect(publisher);
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.ibatis.type.BaseTypeHandler")
    public EncryptTypeHandler encryptTypeHandler(Environment environment) {
        return new EncryptTypeHandler(environment);
    }

    @Bean
    public CachedBodyFilter cachedBodyFilter() {
        return new CachedBodyFilter();
    }

    @Bean
    @ConditionalOnBean({AppKeyProvider.class, RedissonClient.class})
    public ApiSignInterceptor apiSignInterceptor(AppKeyProvider appKeyProvider, RedissonClient redissonClient) {
        return new ApiSignInterceptor(appKeyProvider, redissonClient);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        apiSignInterceptorProvider.ifAvailable(interceptor ->
                registry.addInterceptor(interceptor).addPathPatterns("/api/**")
        );
    }
}
