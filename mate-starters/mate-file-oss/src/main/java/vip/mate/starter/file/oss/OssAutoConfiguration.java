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
package vip.mate.starter.file.oss;

import com.aliyun.oss.OSS;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import vip.mate.base.channel.ChannelConfigStore;

/**
 * Registers the Aliyun OSS {@link vip.mate.starter.file.spi.FileStorage} provider
 * whenever the OSS SDK is on the classpath — <b>unconditionally</b> (no
 * {@code mate.file.oss.endpoint} gate), so it shows up in the admin "渠道配置"
 * list and can be configured from the UI. The OSS client itself is built lazily
 * from the effective (DB / static) config inside {@link OssFileStorage}.
 *
 * @author mateaix
 */
@AutoConfiguration
@ConditionalOnClass(OSS.class)
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OssFileStorage.class)
    public OssFileStorage ossFileStorage(OssProperties properties,
                                         ObjectProvider<ChannelConfigStore> configStore) {
        return new OssFileStorage(properties, configStore);
    }
}
