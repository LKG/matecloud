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
 * Generates an aggregate root + repository + controller skeleton in an existing module.
 *
 * @author mateaix
 */
public class AggregateTemplate {

    private final Path projectRoot;
    private final String moduleName;
    private final String aggregateName; // e.g., Order

    public AggregateTemplate(Path projectRoot, String moduleName, String aggregateName) {
        this.projectRoot = projectRoot;
        this.moduleName = moduleName;
        this.aggregateName = aggregateName;
    }

    public void generate() throws IOException {
        String slug = moduleName.replace("mate-", "").replace("-", "");
        String basePackage = "vip.mate." + slug;
        Path moduleRoot = projectRoot.resolve("mate-biz").resolve(moduleName);
        Path javaRoot = moduleRoot.resolve("src/main/java").resolve(basePackage.replace('.', '/'));
        if (!Files.exists(javaRoot)) {
            throw new IOException("Module does not exist: " + moduleRoot);
        }

        Path aggregateDir = javaRoot.resolve("domain/model/aggregate");
        Path entityDir = javaRoot.resolve("domain/model/entity");
        Path repoDir = javaRoot.resolve("domain/adapter/repository");
        Path ctrlDir = javaRoot.resolve("trigger/controller");
        Files.createDirectories(aggregateDir);
        Files.createDirectories(entityDir);
        Files.createDirectories(repoDir);
        Files.createDirectories(ctrlDir);

        String aggregateClass = aggregateName + "Aggregate";
        String entityClass = aggregateName;
        String repoClass = aggregateName + "Repository";
        String ctrlClass = aggregateName + "Controller";

        Files.writeString(aggregateDir.resolve(aggregateClass + ".java"),
                """
                package %s.domain.model.aggregate;

                import lombok.Builder;
                import lombok.Data;
                import %s.domain.model.entity.%s;

                @Data
                @Builder
                public class %s {
                    private %s %s;

                    public static %s create(%s entity) {
                        return %s.builder().%s(entity).build();
                    }
                }
                """.formatted(basePackage, basePackage, entityClass, aggregateClass,
                        entityClass, toCamel(entityClass), aggregateClass, entityClass,
                        aggregateClass, toCamel(entityClass)));

        Files.writeString(entityDir.resolve(entityClass + ".java"),
                """
                package %s.domain.model.entity;

                import lombok.Data;
                import lombok.EqualsAndHashCode;
                import lombok.NoArgsConstructor;
                import lombok.experimental.SuperBuilder;
                import vip.mate.base.model.entity.BaseEntity;

                @Data
                @SuperBuilder
                @NoArgsConstructor
                @EqualsAndHashCode(callSuper = true)
                public class %s extends BaseEntity {
                    // Domain fields go here. Use `mate gen code --table <table>` to
                    // reverse-generate this from a database table once the schema
                    // is in place.
                }
                """.formatted(basePackage, entityClass));

        Files.writeString(repoDir.resolve(repoClass + ".java"),
                """
                package %s.domain.adapter.repository;

                import %s.domain.model.aggregate.%s;

                public interface %s {
                    void save(%s aggregate);
                    %s findById(String id);
                }
                """.formatted(basePackage, basePackage, aggregateClass, repoClass,
                        aggregateClass, aggregateClass));

        Files.writeString(ctrlDir.resolve(ctrlClass + ".java"),
                """
                package %s.trigger.controller;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                import vip.mate.base.result.Result;

                @RestController
                @RequestMapping("/api/v1/%s")
                public class %s {

                    @GetMapping("/{id}")
                    public Result<String> get(@PathVariable String id) {
                        return Result.ok(id);
                    }
                }
                """.formatted(basePackage, toCamel(entityClass) + "s", ctrlClass));
    }

    private static String toCamel(String pascal) {
        if (pascal == null || pascal.isEmpty()) return pascal;
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}
