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
package vip.mate.cli.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads CLI-wide configuration from {@code ~/.matecloud/config} (Java
 * {@code Properties} format) with environment-variable fallback. Keys:
 * <pre>
 *   nacos.server-addr = 127.0.0.1:8848
 *   nacos.namespace   = dev
 *   nacos.group       = DEFAULT_GROUP
 *   nacos.username    = nacos
 *   nacos.password    = nacos
 *   admin.base-url    = http://127.0.0.1:9030
 * </pre>
 *
 * @author mateaix
 */
public final class CliConfig {

    private static final Path CONFIG_PATH = Paths.get(
            System.getProperty("user.home"), ".matecloud", "config");

    private static Properties properties;

    private CliConfig() {
    }

    private static Properties load() {
        if (properties != null) return properties;
        Properties p = new Properties();
        if (Files.isReadable(CONFIG_PATH)) {
            try (var in = Files.newInputStream(CONFIG_PATH)) {
                p.load(in);
            } catch (IOException ignored) {
            }
        }
        properties = p;
        return p;
    }

    public static String get(String key, String envVar, String defaultValue) {
        String env = System.getenv(envVar);
        if (env != null && !env.isBlank()) return env;
        String prop = load().getProperty(key);
        if (prop != null && !prop.isBlank()) return prop;
        return defaultValue;
    }

    public static String nacosServerAddr() {
        return get("nacos.server-addr", "NACOS_SERVER_ADDR", "127.0.0.1:8848");
    }

    public static String nacosNamespace() {
        return get("nacos.namespace", "NACOS_NAMESPACE", "dev");
    }

    public static String nacosGroup() {
        return get("nacos.group", "NACOS_GROUP", "DEFAULT_GROUP");
    }

    public static String nacosUsername() {
        return get("nacos.username", "NACOS_USERNAME", "nacos");
    }

    public static String nacosPassword() {
        return get("nacos.password", "NACOS_PASSWORD", "nacos");
    }

    public static String adminBaseUrl() {
        return get("admin.base-url", "MATE_ADMIN_URL", "http://127.0.0.1:9030");
    }

    public static String gatewayBaseUrl() {
        return get("gateway.base-url", "MATE_GATEWAY_URL", "http://127.0.0.1:9010");
    }
}
