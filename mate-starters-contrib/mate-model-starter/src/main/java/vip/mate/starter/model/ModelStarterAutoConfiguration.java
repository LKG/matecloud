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
package vip.mate.starter.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.base.model.ModelConfigStore;
import vip.mate.starter.channel.ChannelCrypto;

import javax.sql.DataSource;

/**
 * Registers the DB-backed {@link ModelConfigStore} and the {@link ModelFactory} when a
 * {@link DataSource} is present. Any service that includes this starter (and shares the
 * database where the {@code mate_model_*} tables live) can resolve admin-managed model
 * config and build OpenAI-compatible clients.
 *
 * <p>Reuses the shared {@link ChannelCrypto} (same {@code mate.security.encrypt.key}) so
 * credentials encrypted by the admin write path decrypt here.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class ModelStarterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChannelCrypto.class)
    public ChannelCrypto modelChannelCrypto(@Value("${mate.security.encrypt.key:}") String key) {
        return new ChannelCrypto(key);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(ModelConfigStore.class)
    public ModelConfigStore dbModelConfigStore(JdbcTemplate jdbcTemplate, ChannelCrypto channelCrypto) {
        return new DbModelConfigStore(jdbcTemplate, channelCrypto);
    }

    @Bean
    @ConditionalOnMissingBean(ModelFactory.class)
    public ModelFactory modelFactory() {
        return new ModelFactory();
    }
}
