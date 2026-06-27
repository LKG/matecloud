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
package vip.mate.starter.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import vip.mate.base.channel.ChannelConfigStore;

import javax.sql.DataSource;

/**
 * Registers the shared {@link ChannelCrypto} and the DB-backed
 * {@link ChannelConfigStore} when a {@link DataSource} is present. Any service that
 * includes this starter (and shares the database where {@code mate_channel_config}
 * lives) thereby resolves admin-managed storage/SMS config.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(JdbcTemplate.class)
public class ChannelConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ChannelCrypto.class)
    public ChannelCrypto channelCrypto(@Value("${mate.security.encrypt.key:}") String key) {
        return new ChannelCrypto(key);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean(ChannelConfigStore.class)
    public ChannelConfigStore dbChannelConfigStore(JdbcTemplate jdbcTemplate, ChannelCrypto channelCrypto) {
        return new DbChannelConfigStore(jdbcTemplate, channelCrypto);
    }
}
