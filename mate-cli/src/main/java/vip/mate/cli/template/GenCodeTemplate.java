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

import vip.mate.cli.config.DbConfig;
import vip.mate.cli.config.DbConfigLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reverse-generates DDD 4-layer code from a database table.
 *
 * @author mateaix
 */
public class GenCodeTemplate {

    private final String tableName;      // e.g. "mate_order"
    private final String moduleName;     // e.g. "mate-order"
    private final String service;        // e.g. "mate-admin" (for DB connection)
    private final Path projectRoot;

    public GenCodeTemplate(String tableName, String moduleName, String service, Path projectRoot) {
        this.tableName = tableName;
        this.moduleName = moduleName;
        this.service = service;
        this.projectRoot = projectRoot;
    }

    public void generate() throws Exception {
        DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
        List<ColumnMeta> columns = loadColumns(db);

        String entityName = tableToClass(tableName);     // e.g. "Order"
        String basePackage = "vip.mate." + moduleName.replace("mate-", "");

        Path moduleRoot = resolveModuleRoot();
        Path javaRoot = moduleRoot.resolve("src/main/java").resolve(basePackage.replace('.', '/'));

        generatePO(javaRoot, basePackage, entityName, columns);
        generateDAO(javaRoot, basePackage, entityName);
        generateEntity(javaRoot, basePackage, entityName, columns);
        generateRepository(javaRoot, basePackage, entityName);
        generateRepositoryImpl(javaRoot, basePackage, entityName, columns);
        generateCommandService(javaRoot, basePackage, entityName);
        generateQueryService(javaRoot, basePackage, entityName, columns);
        generateController(javaRoot, basePackage, entityName);

        System.out.println("Generated DDD code for table '" + tableName + "' in module '" + moduleName + "'");
        System.out.println("  Entity: " + entityName);
        System.out.println("  Package: " + basePackage);
        System.out.println("  Files: 8 (PO, DAO, Entity, Repository, RepositoryImpl, CommandService, QueryService, Controller)");
    }

    private List<ColumnMeta> loadColumns(DbConfig.ServiceDb db) throws SQLException {
        List<ColumnMeta> columns = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
            DatabaseMetaData dbMeta = conn.getMetaData();
            try (ResultSet rs = dbMeta.getColumns(null, null, tableName, null)) {
                while (rs.next()) {
                    ColumnMeta col = new ColumnMeta();
                    col.columnName = rs.getString("COLUMN_NAME");
                    col.dataType = rs.getInt("DATA_TYPE");
                    col.typeName = rs.getString("TYPE_NAME");
                    col.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    col.comment = rs.getString("REMARKS");
                    columns.add(col);
                }
            }
        }
        if (columns.isEmpty()) {
            throw new RuntimeException("Table '" + tableName + "' not found or has no columns");
        }
        return columns;
    }

    private void generatePO(Path javaRoot, String pkg, String entity, List<ColumnMeta> columns) throws IOException {
        Path dir = javaRoot.resolve("infrastructure/dao/po");
        Files.createDirectories(dir);

        StringBuilder fields = new StringBuilder();
        for (ColumnMeta col : columns) {
            if (isBaseField(col.columnName)) continue;
            String javaType = sqlTypeToJava(col.dataType);
            String fieldName = columnToField(col.columnName);
            if (col.comment != null && !col.comment.isBlank()) {
                fields.append("    /** ").append(col.comment).append(" */\n");
            }
            fields.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n");
        }

        String content = """
                package %s.infrastructure.dao.po;

                import com.baomidou.mybatisplus.annotation.*;
                import lombok.Data;
                import java.time.LocalDateTime;

                @Data
                @TableName("%s")
                public class %sPO {

                    @TableId(type = IdType.ASSIGN_ID)
                    private String id;
                %s
                    @TableField(fill = FieldFill.INSERT)
                    private LocalDateTime createdAt;
                    @TableField(fill = FieldFill.INSERT_UPDATE)
                    private LocalDateTime updatedAt;
                    @TableLogic
                    private Integer deleted;
                }
                """.formatted(pkg, tableName, entity, fields);
        Files.writeString(dir.resolve(entity + "PO.java"), content);
    }

    private void generateDAO(Path javaRoot, String pkg, String entity) throws IOException {
        Path dir = javaRoot.resolve("infrastructure/dao");
        Files.createDirectories(dir);
        String content = """
                package %s.infrastructure.dao;

                import com.baomidou.mybatisplus.core.mapper.BaseMapper;
                import org.apache.ibatis.annotations.Mapper;
                import %s.infrastructure.dao.po.%sPO;

                @Mapper
                public interface %sDao extends BaseMapper<%sPO> {
                }
                """.formatted(pkg, pkg, entity, entity, entity);
        Files.writeString(dir.resolve(entity + "Dao.java"), content);
    }

    private void generateEntity(Path javaRoot, String pkg, String entity, List<ColumnMeta> columns) throws IOException {
        Path dir = javaRoot.resolve("domain/model/entity");
        Files.createDirectories(dir);

        StringBuilder fields = new StringBuilder();
        for (ColumnMeta col : columns) {
            if (isBaseField(col.columnName) || "id".equals(col.columnName)) continue;
            String javaType = sqlTypeToJava(col.dataType);
            String fieldName = columnToField(col.columnName);
            fields.append("    private ").append(javaType).append(" ").append(fieldName).append(";\n");
        }

        String content = """
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

                %s}
                """.formatted(pkg, entity, fields);
        Files.writeString(dir.resolve(entity + ".java"), content);
    }

    private void generateRepository(Path javaRoot, String pkg, String entity) throws IOException {
        Path dir = javaRoot.resolve("domain/adapter/repository");
        Files.createDirectories(dir);
        String content = """
                package %s.domain.adapter.repository;

                import %s.domain.model.entity.%s;
                import java.util.List;

                public interface %sRepository {
                    void save(%s entity);
                    void update(%s entity);
                    void deleteById(String id);
                    %s findById(String id);
                    List<%s> pageQuery(int pageNum, int pageSize);
                }
                """.formatted(pkg, pkg, entity, entity, entity, entity, entity, entity);
        Files.writeString(dir.resolve(entity + "Repository.java"), content);
    }

    private void generateRepositoryImpl(Path javaRoot, String pkg, String entity, List<ColumnMeta> columns) throws IOException {
        Path dir = javaRoot.resolve("infrastructure/adapter/repository");
        Files.createDirectories(dir);

        String lcEntity = entity.substring(0, 1).toLowerCase() + entity.substring(1);

        // Build field-by-field mapping for toPO / toEntity from the column list.
        // BaseEntity owns id/createdAt/updatedAt (domain audit fields); the PO
        // additionally owns the DB-managed soft-delete flag. So id is set
        // explicitly and the audit/delete columns are filled by MyBatis Plus handlers.
        StringBuilder toPoSetters = new StringBuilder();
        StringBuilder toEntityBuilder = new StringBuilder();
        for (ColumnMeta col : columns) {
            if (isBaseField(col.columnName) || "id".equals(col.columnName)) continue;
            String fieldName = columnToField(col.columnName);
            String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            toPoSetters.append("        po.set").append(capitalized)
                       .append("(entity.get").append(capitalized).append("());\n");
            toEntityBuilder.append("                .").append(fieldName)
                           .append("(po.get").append(capitalized).append("())\n");
        }

        String content = """
                package %s.infrastructure.adapter.repository;

                import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Repository;
                import %s.domain.adapter.repository.%sRepository;
                import %s.domain.model.entity.%s;
                import %s.infrastructure.dao.%sDao;
                import %s.infrastructure.dao.po.%sPO;

                import java.util.List;

                @Repository
                @RequiredArgsConstructor
                public class %sRepositoryImpl implements %sRepository {

                    private final %sDao %sDao;

                    @Override
                    public void save(%s entity) {
                        %sDao.insert(toPO(entity));
                    }

                    @Override
                    public void update(%s entity) {
                        %sDao.updateById(toPO(entity));
                    }

                    @Override
                    public void deleteById(String id) {
                        %sDao.deleteById(id);
                    }

                    @Override
                    public %s findById(String id) {
                        %sPO po = %sDao.selectById(id);
                        return po != null ? toEntity(po) : null;
                    }

                    @Override
                    public List<%s> pageQuery(int pageNum, int pageSize) {
                        Page<%sPO> page = %sDao.selectPage(new Page<>(pageNum, pageSize), null);
                        return page.getRecords().stream().map(this::toEntity).toList();
                    }

                    private %sPO toPO(%s entity) {
                        %sPO po = new %sPO();
                        po.setId(entity.getId());
                %s        return po;
                    }

                    private %s toEntity(%sPO po) {
                        return %s.builder()
                                .id(po.getId())
                %s                .build();
                    }
                }
                """.formatted(pkg, pkg, entity, pkg, entity, pkg, entity, pkg, entity,
                entity, entity,
                entity, lcEntity,
                entity, lcEntity,
                entity, lcEntity,
                lcEntity,
                entity, entity, lcEntity,
                entity, entity, lcEntity,
                entity, entity, entity, entity, toPoSetters,
                entity, entity, entity, toEntityBuilder);
        Files.writeString(dir.resolve(entity + "RepositoryImpl.java"), content);
    }

    private void generateCommandService(Path javaRoot, String pkg, String entity) throws IOException {
        Path dir = javaRoot.resolve("application/command");
        Files.createDirectories(dir);
        String content = """
                package %s.application.command;

                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Service;
                import org.springframework.transaction.annotation.Transactional;
                import %s.domain.adapter.repository.%sRepository;
                import %s.domain.model.entity.%s;

                @Service
                @RequiredArgsConstructor
                public class %sCommandService {

                    private final %sRepository repository;

                    @Transactional
                    public String create(%s entity) {
                        repository.save(entity);
                        return entity.getId();
                    }

                    @Transactional
                    public void update(%s entity) {
                        repository.update(entity);
                    }

                    @Transactional
                    public void delete(String id) {
                        repository.deleteById(id);
                    }
                }
                """.formatted(pkg, pkg, entity, pkg, entity, entity, entity, entity, entity);
        Files.writeString(dir.resolve(entity + "CommandService.java"), content);
    }

    private void generateQueryService(Path javaRoot, String pkg, String entity, List<ColumnMeta> columns) throws IOException {
        Path dir = javaRoot.resolve("application/query");
        Files.createDirectories(dir);
        String content = """
                package %s.application.query;

                import java.util.List;

                public interface I%sQueryService {
                    %s findById(String id);
                    List<%s> pageQuery(int pageNum, int pageSize);
                }
                """.formatted(pkg, entity, entity, entity);
        Files.writeString(dir.resolve("I" + entity + "QueryService.java"), content);

        // Impl
        Path implDir = dir.resolve("impl");
        Files.createDirectories(implDir);
        String implContent = """
                package %s.application.query.impl;

                import lombok.RequiredArgsConstructor;
                import org.springframework.stereotype.Service;
                import %s.application.query.I%sQueryService;
                import %s.domain.adapter.repository.%sRepository;
                import %s.domain.model.entity.%s;

                import java.util.List;

                @Service
                @RequiredArgsConstructor
                public class %sQueryServiceImpl implements I%sQueryService {

                    private final %sRepository repository;

                    @Override
                    public %s findById(String id) {
                        return repository.findById(id);
                    }

                    @Override
                    public List<%s> pageQuery(int pageNum, int pageSize) {
                        return repository.pageQuery(pageNum, pageSize);
                    }
                }
                """.formatted(pkg, pkg, entity, pkg, entity, pkg, entity,
                entity, entity, entity, entity, entity);
        Files.writeString(implDir.resolve(entity + "QueryServiceImpl.java"), implContent);
    }

    private void generateController(Path javaRoot, String pkg, String entity) throws IOException {
        Path dir = javaRoot.resolve("trigger/controller");
        Files.createDirectories(dir);
        String lcEntity = entity.substring(0, 1).toLowerCase() + entity.substring(1);
        String pluralPath = lcEntity + "s";

        String content = """
                package %s.trigger.controller;

                import lombok.RequiredArgsConstructor;
                import org.springframework.web.bind.annotation.*;
                import %s.application.command.%sCommandService;
                import %s.application.query.I%sQueryService;
                import %s.domain.model.entity.%s;
                import vip.mate.base.result.Result;

                import java.util.List;

                @RestController
                @RequestMapping("/api/v1/%s")
                @RequiredArgsConstructor
                public class %sController {

                    private final %sCommandService commandService;
                    private final I%sQueryService queryService;

                    @GetMapping("/{id}")
                    public Result<%s> getById(@PathVariable String id) {
                        return Result.ok(queryService.findById(id));
                    }

                    @GetMapping
                    public Result<List<%s>> list(@RequestParam(defaultValue = "1") int pageNum,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
                        return Result.ok(queryService.pageQuery(pageNum, pageSize));
                    }

                    @DeleteMapping("/{id}")
                    public Result<Void> delete(@PathVariable String id) {
                        commandService.delete(id);
                        return Result.ok();
                    }
                }
                """.formatted(pkg, pkg, entity, pkg, entity, pkg, entity,
                pluralPath, entity, entity, entity, entity, entity);
        Files.writeString(dir.resolve(entity + "Controller.java"), content);
    }

    private Path resolveModuleRoot() {
        // Try mate-biz/module first, then top-level
        Path bizPath = projectRoot.resolve("mate-biz").resolve(moduleName);
        if (Files.isDirectory(bizPath)) return bizPath;
        Path topPath = projectRoot.resolve(moduleName);
        if (Files.isDirectory(topPath)) return topPath;
        throw new RuntimeException("Module directory not found: " + moduleName);
    }

    // ---- Utility methods ----

    private static String tableToClass(String table) {
        // Remove mate_ prefix and convert to PascalCase
        String name = table.startsWith("mate_") ? table.substring(5) : table;
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : name.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private static String columnToField(String column) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : column.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private static String sqlTypeToJava(int sqlType) {
        return switch (sqlType) {
            case Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.NVARCHAR -> "String";
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT -> "Integer";
            case Types.BIGINT -> "Long";
            case Types.DECIMAL, Types.NUMERIC -> "java.math.BigDecimal";
            case Types.BOOLEAN, Types.BIT -> "Boolean";
            case Types.DATE -> "java.time.LocalDate";
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "java.time.LocalDateTime";
            case Types.DOUBLE, Types.FLOAT, Types.REAL -> "Double";
            case Types.CLOB, Types.NCLOB, Types.BLOB -> "String";
            default -> "String";
        };
    }

    private static boolean isBaseField(String column) {
        return switch (column) {
            case "id", "created_at", "updated_at", "deleted", "lock_version" -> true;
            default -> false;
        };
    }

    static class ColumnMeta {
        String columnName;
        int dataType;
        String typeName;
        boolean nullable;
        String comment;
    }
}
