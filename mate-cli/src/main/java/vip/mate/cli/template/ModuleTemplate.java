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
package vip.mate.cli.template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Generates a DDD business module skeleton under mate-biz/ that already follows the
 * pluggable-module conventions (docs/conventions/pluggable-module-guide.md):
 *
 * <ul>
 *   <li>§1 per-service Flyway history — sets {@code mate.module.code} + a {@code V1__} migration
 *       on an independent version line;</li>
 *   <li>§2 menu self-registration — ships a {@code menu-manifest.yml} + depends on mate-menu-starter;</li>
 *   <li>§3 gateway dynamic routing — declares {@code gateway-path} discovery metadata
 *       (no mate-gateway change needed);</li>
 *   <li>auto-registers the module into {@code mate-biz/pom.xml} so no central pom edit is needed.</li>
 * </ul>
 *
 * The result is a module that compiles and self-wires on first boot; replace the demo
 * table / menu / package stubs with real content.
 *
 * @author mateaix
 */
public class ModuleTemplate {

    private final Path projectRoot;
    private final String moduleName;   // e.g., mate-order
    private final String basePackage;  // e.g., vip.mate.order
    private final String code;         // e.g., order  (module.code + url segment + history table suffix)
    private final int port;

    public ModuleTemplate(Path projectRoot, String moduleName, String basePackage, int port) {
        this.projectRoot = projectRoot;
        this.moduleName = moduleName;
        this.basePackage = basePackage;
        this.code = moduleName.replace("mate-", "");
        this.port = port;
    }

    public void generate() throws IOException {
        Path moduleRoot = projectRoot.resolve("mate-biz").resolve(moduleName);
        Path javaRoot = moduleRoot.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        Path resourcesRoot = moduleRoot.resolve("src/main/resources");

        Files.createDirectories(javaRoot.resolve("trigger/controller"));
        Files.createDirectories(javaRoot.resolve("application/command"));
        Files.createDirectories(javaRoot.resolve("application/query"));
        Files.createDirectories(javaRoot.resolve("application/convertor"));
        Files.createDirectories(javaRoot.resolve("domain/model/aggregate"));
        Files.createDirectories(javaRoot.resolve("domain/model/entity"));
        Files.createDirectories(javaRoot.resolve("domain/adapter/repository"));
        Files.createDirectories(javaRoot.resolve("domain/service"));
        Files.createDirectories(javaRoot.resolve("infrastructure/adapter/repository"));
        Files.createDirectories(javaRoot.resolve("infrastructure/dao/po"));
        Files.createDirectories(javaRoot.resolve("types/exception"));
        Files.createDirectories(resourcesRoot.resolve("db/migration"));

        writePom(moduleRoot);
        writeApplication(javaRoot);
        writeApplicationYml(resourcesRoot);
        writeMigration(resourcesRoot);
        writeMenuManifest(resourcesRoot);
        registerInParentPom();
    }

    private void writePom(Path moduleRoot) throws IOException {
        String pom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>vip.mate</groupId>
                        <artifactId>mate-biz</artifactId>
                        <version>1.0.0</version>
                        <relativePath>../pom.xml</relativePath>
                    </parent>
                    <artifactId>%s</artifactId>
                    <packaging>jar</packaging>
                    <dependencies>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-base</artifactId></dependency>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-api</artifactId></dependency>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-ds-starter</artifactId></dependency>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-web-starter</artifactId></dependency>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-nacos-starter</artifactId></dependency>
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-rpc-starter</artifactId></dependency>
                        <!-- menu self-registration (docs/conventions/pluggable-module-guide.md §2) -->
                        <dependency><groupId>vip.mate</groupId><artifactId>mate-menu-starter</artifactId></dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.springframework.boot</groupId>
                                <artifactId>spring-boot-maven-plugin</artifactId>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """.formatted(moduleName);
        Files.writeString(moduleRoot.resolve("pom.xml"), pom);
    }

    private void writeApplication(Path javaRoot) throws IOException {
        String className = toPascal(code) + "Application";
        String content = """
                package %s;

                import org.springframework.boot.SpringApplication;
                import org.springframework.boot.autoconfigure.SpringBootApplication;
                import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

                /**
                 * Dubbo is enabled via mate-rpc-starter's RpcAutoConfiguration.
                 * Menus self-register via mate-menu-starter (reads menu-manifest.yml).
                 */
                @SpringBootApplication
                @EnableDiscoveryClient
                public class %s {
                    public static void main(String[] args) {
                        SpringApplication.run(%s.class, args);
                    }
                }
                """.formatted(basePackage, className, className);
        Files.writeString(javaRoot.resolve(className + ".java"), content);
    }

    /**
     * application.yml following the repo convention (imports mate-defaults + Nacos) and
     * wiring the pluggable knobs: mate.module.code (§1) and the gateway-path discovery
     * metadata (§3).
     */
    private void writeApplicationYml(Path resourcesRoot) throws IOException {
        String content = """
                spring:
                  application:
                    name: %s
                  config:
                    import:
                      - classpath:mate-defaults.yml
                      - optional:nacos:mate-infra-${spring.profiles.active:dev}.yml
                      - optional:nacos:${spring.application.name}-${spring.profiles.active:dev}.yml
                  cloud:
                    nacos:
                      discovery:
                        metadata:
                          # §3 gateway dynamic routing: mate-gateway auto-builds a route from
                          # this metadata. No mate-gateway change needed to add this service.
                          gateway-path: /api/v1/%s/**

                mate:
                  # §1 per-service Flyway history table = flyway_history_%s (independent version line).
                  module:
                    code: %s

                server:
                  port: %d
                """.formatted(moduleName, code, code, code, port);
        Files.writeString(resourcesRoot.resolve("application.yml"), content);
    }

    /** First migration on this service's own version line — starts at V1 (no global coordination). */
    private void writeMigration(Path resourcesRoot) throws IOException {
        String content = """
                -- %s schema V1 — INDEPENDENT per-service Flyway line (table flyway_history_%s),
                -- so versions start at V1 and never collide with other services.
                -- Dual-dialect: H2 (MySQL mode) + MySQL; CREATE TABLE IF NOT EXISTS, no ALTER.
                -- See docs/conventions/pluggable-module-guide.md §1. Replace this demo table.
                CREATE TABLE IF NOT EXISTS `mate_%s_demo` (
                    `id`           VARCHAR(64)  NOT NULL COMMENT 'Snowflake id',
                    `tenant_id`    VARCHAR(64)  NOT NULL DEFAULT '0' COMMENT 'Tenant id ("0" when off)',
                    `name`         VARCHAR(128) NOT NULL COMMENT 'Demo name',
                    `created_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    `updated_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    `deleted`      TINYINT      NOT NULL DEFAULT 0,
                    PRIMARY KEY (`id`),
                    KEY `idx_%s_tenant` (`tenant_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='%s demo table (replace me)';
                """.formatted(moduleName, code, code, code, moduleName);
        Files.writeString(resourcesRoot.resolve("db/migration/V1__" + code + "_schema.sql"), content);
    }

    /** Menu manifest self-registered at startup (§2). Stable string codes; mate-system assigns ids. */
    private void writeMenuManifest(Path resourcesRoot) throws IOException {
        String pascal = toPascal(code);
        String content = """
                # %s menu manifest — self-registered into mate-system at startup (§2).
                # Stable string `code` is the identity; mate-system assigns numeric ids.
                # NEVER write menus into mate-system migrations; declare them here instead.
                menus:
                  - code: %s
                    name: %s
                    nameEn: %s
                    type: M            # M=directory C=menu F=button
                    icon: Boxes
                    sort: 20
                    children:
                      - code: %s.home
                        parentCode: %s
                        name: %s 首页
                        nameEn: %s Home
                        path: /%s
                        component: %s/Index
                        perms: %s:home:view
                        type: C
                        sort: 1
                """.formatted(moduleName, code, pascal, pascal,
                code, code, pascal, pascal, code, code, code);
        Files.writeString(resourcesRoot.resolve("menu-manifest.yml"), content);
    }

    /** Add {@code <module>moduleName</module>} to mate-biz/pom.xml (idempotent). */
    private void registerInParentPom() throws IOException {
        Path parentPom = projectRoot.resolve("mate-biz").resolve("pom.xml");
        if (!Files.exists(parentPom)) {
            return;
        }
        String pom = Files.readString(parentPom);
        String entry = "<module>" + moduleName + "</module>";
        if (pom.contains(entry)) {
            return; // already registered
        }
        int close = pom.indexOf("</modules>");
        if (close < 0) {
            return; // no <modules> block — leave for manual edit
        }
        // Preserve the indentation of the existing module entries.
        String indent = "        ";
        int firstModule = pom.indexOf("<module>");
        if (firstModule >= 0 && firstModule < close) {
            int lineStart = pom.lastIndexOf('\n', firstModule);
            if (lineStart >= 0) {
                indent = pom.substring(lineStart + 1, firstModule);
            }
        }
        String insertion = indent + entry + System.lineSeparator();
        String updated = pom.substring(0, close) + insertion + pom.substring(close);
        Files.writeString(parentPom, updated);
    }

    private static String toPascal(String input) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : input.toCharArray()) {
            if (c == '-' || c == '_') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
