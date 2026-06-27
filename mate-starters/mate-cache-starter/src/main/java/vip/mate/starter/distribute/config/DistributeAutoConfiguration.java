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
package vip.mate.starter.distribute.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.distribute.worker.WorkerIdHolder;

/**
 * Auto-configuration for distributed ID generation (Snowflake).
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnBean(RedissonClient.class)
public class DistributeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WorkerIdHolder workerIdHolder(RedissonClient redissonClient) {
        return new WorkerIdHolder(redissonClient);
    }
}
