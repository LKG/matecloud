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
package vip.mate.starter.idempotent.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.idempotent.aspect.IdempotentAspect;
import vip.mate.starter.idempotent.controller.IdempotentTokenController;

@AutoConfiguration
@EnableConfigurationProperties(IdempotentProperties.class)
@ConditionalOnProperty(prefix = "mate.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IdempotentAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public IdempotentAspect idempotentAspect(RedissonClient redissonClient,
                                             IdempotentProperties properties) {
        return new IdempotentAspect(redissonClient, properties);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    public IdempotentTokenController idempotentTokenController(RedissonClient redissonClient,
                                                               IdempotentProperties properties) {
        return new IdempotentTokenController(redissonClient, properties);
    }
}
