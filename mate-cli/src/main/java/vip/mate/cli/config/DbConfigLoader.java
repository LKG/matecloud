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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;

public class DbConfigLoader {

    private static final String CONFIG_PATH = System.getProperty("user.home") + "/.mate/db.yml";

    public static DbConfig load() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            System.err.println("Database config not found: " + CONFIG_PATH);
            System.err.println("Create it with service connection details. See 'mate db --help'.");
            System.exit(1);
        }
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(file, DbConfig.class);
        } catch (IOException e) {
            System.err.println("Failed to parse " + CONFIG_PATH + ": " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    public static DbConfig.ServiceDb getServiceDb(String service) {
        DbConfig config = load();
        DbConfig.ServiceDb db = config.getServices().get(service);
        if (db == null) {
            System.err.println("Service '" + service + "' not found in " + CONFIG_PATH);
            System.err.println("Available: " + config.getServices().keySet());
            System.exit(1);
        }
        return db;
    }
}
