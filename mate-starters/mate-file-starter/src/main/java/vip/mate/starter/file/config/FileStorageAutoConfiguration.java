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
package vip.mate.starter.file.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.util.List;
import vip.mate.base.channel.ChannelConfigStore;
import vip.mate.starter.file.core.DefaultFileStorageResolver;
import vip.mate.starter.file.core.FileStorageRegistry;
import vip.mate.starter.file.core.FileStorageResolver;
import vip.mate.starter.file.core.FileTemplate;
import vip.mate.starter.file.provider.MinioFileStorage;
import vip.mate.starter.file.spi.FileStorage;

/**
 * Wires the pluggable file-storage layer: the active-provider resolver, the
 * provider registry (collecting every {@link FileStorage} bean), the
 * {@link FileTemplate} facade, and the bundled MinIO / S3-compatible provider
 * (registered when {@code minio.endpoint} is configured).
 *
 * <p>Additional providers (OSS, S3, …) only need to add their own
 * {@code @FileProvider} bean on the classpath — they are picked up automatically.
 *
 * @author mateaix
 */
@AutoConfiguration
@EnableConfigurationProperties({FileProperties.class, MinioProperties.class})
public class FileStorageAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "minio", name = "endpoint")
    @ConditionalOnMissingBean(MinioFileStorage.class)
    public MinioFileStorage minioFileStorage(MinioProperties properties,
                                             ObjectProvider<ChannelConfigStore> configStore) {
        return new MinioFileStorage(properties, configStore);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageResolver.class)
    public FileStorageResolver fileStorageResolver(FileProperties properties,
                                                   ObjectProvider<ChannelConfigStore> configStore) {
        return new DefaultFileStorageResolver(properties, configStore);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageRegistry.class)
    public FileStorageRegistry fileStorageRegistry(List<FileStorage> storages) {
        return new FileStorageRegistry(storages);
    }

    @Bean
    @ConditionalOnMissingBean(FileTemplate.class)
    public FileTemplate fileTemplate(FileStorageRegistry registry, FileStorageResolver resolver) {
        return new FileTemplate(registry, resolver);
    }
}
