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

import org.springframework.boot.context.properties.ConfigurationProperties;
import vip.mate.starter.file.annotation.FileProvider;

/**
 * Top-level file-storage properties. Provider-specific credentials live under
 * each provider's own prefix (e.g. {@code minio.*}).
 *
 * @author mateaix
 */
@ConfigurationProperties(prefix = "mate.file")
public class FileProperties {

    /** Active provider type. Must match a registered {@code @FileProvider} value. */
    private String type = "minio";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
