# RFC-042: CLI db + gen Commands — 让脚手架真正干活

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 9
- **Dependencies**: RFC-038, RFC-041

## 背景

`mate-cli` 有 6 个 TODO 桩命令（db、gen、cache、config、rpc、ai），本 RFC 实现最有价值的两个：

1. **`mate db`**：连接 MySQL 执行查询、查看表结构、触发 Flyway 迁移
2. **`mate gen code`**：读取数据库表元数据，反向生成完整 DDD 4 层代码

其他命令（cache、config、rpc、ai）留到后续 RFC。

## 设计方案

### Part 1: mate db 命令实现

#### Change 1: 数据库连接配置

CLI 需要知道各服务的数据库连接信息。通过 `~/.mate/db.yml` 配置文件读取。

File: `mate-cli/src/main/java/vip/mate/cli/config/DbConfig.java`

```java
package vip.mate.cli.config;

import lombok.Data;
import java.util.Map;

/**
 * Database connection config, loaded from ~/.mate/db.yml
 *
 * Example ~/.mate/db.yml:
 * <pre>
 * services:
 *   mate-admin:
 *     url: jdbc:mysql://localhost:3306/mate_admin
 *     username: root
 *     password: root
 *   mate-system:
 *     url: jdbc:mysql://localhost:3306/mate_system
 *     username: root
 *     password: root
 * </pre>
 */
@Data
public class DbConfig {
    private Map<String, ServiceDb> services;

    @Data
    public static class ServiceDb {
        private String url;
        private String username;
        private String password;
    }
}
```

File: `mate-cli/src/main/java/vip/mate/cli/config/DbConfigLoader.java`

```java
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
```

#### Change 2: DbCommand 实现

File: `mate-cli/src/main/java/vip/mate/cli/command/DbCommand.java`

```java
package vip.mate.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vip.mate.cli.config.DbConfig;
import vip.mate.cli.config.DbConfigLoader;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Command(name = "db", description = "Database operations",
        subcommands = {DbCommand.QuerySub.class, DbCommand.DescribeSub.class, DbCommand.MigrateSub.class})
public class DbCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate db <query|describe|migrate>");
    }

    @Command(name = "query", description = "Execute a SQL query")
    public static class QuerySub implements Runnable {
        @Parameters(index = "0", description = "SQL query")
        String sql;

        @Option(names = "--service", description = "Target service", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
            try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword());
                 Statement stmt = conn.createStatement()) {

                if (sql.trim().toUpperCase().startsWith("SELECT") || sql.trim().toUpperCase().startsWith("SHOW")) {
                    ResultSet rs = stmt.executeQuery(sql);
                    printResultSet(rs);
                } else {
                    int affected = stmt.executeUpdate(sql);
                    System.out.println("Affected rows: " + affected);
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    @Command(name = "describe", description = "Show table structure")
    public static class DescribeSub implements Runnable {
        @Parameters(index = "0", description = "Table name")
        String table;

        @Option(names = "--service", description = "Target service", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(service);
            try (Connection conn = DriverManager.getConnection(db.getUrl(), db.getUsername(), db.getPassword())) {
                // Columns
                System.out.println("=== Columns ===");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW FULL COLUMNS FROM `" + table + "`")) {
                    printResultSet(rs);
                }

                // Indexes
                System.out.println("\n=== Indexes ===");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW INDEX FROM `" + table + "`")) {
                    printResultSet(rs);
                }

                // Row count
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS row_count FROM `" + table + "`")) {
                    if (rs.next()) {
                        System.out.println("\nRow count: " + rs.getLong("row_count"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("SQL error: " + e.getMessage());
            }
        }
    }

    @Command(name = "migrate", description = "Run Flyway migrations for a module")
    public static class MigrateSub implements Runnable {
        @Option(names = "--module", description = "Target module (e.g. mate-admin)", required = true)
        String module;

        @Override
        public void run() {
            DbConfig.ServiceDb db = DbConfigLoader.getServiceDb(module);
            try {
                org.flywaydb.core.Flyway flyway = org.flywaydb.core.Flyway.configure()
                        .dataSource(db.getUrl(), db.getUsername(), db.getPassword())
                        .locations("filesystem:" + findMigrationDir(module))
                        .baselineOnMigrate(true)
                        .load();
                var result = flyway.migrate();
                System.out.println("Applied " + result.migrationsExecuted + " migration(s)");
                result.migrations.forEach(m ->
                        System.out.println("  " + m.version + " - " + m.description));
            } catch (Exception e) {
                System.err.println("Migration failed: " + e.getMessage());
            }
        }

        private String findMigrationDir(String module) {
            // Try common paths
            String[] candidates = {
                    module + "/src/main/resources/db/migration",
                    "mate-biz/" + module + "/src/main/resources/db/migration"
            };
            for (String candidate : candidates) {
                if (new java.io.File(candidate).isDirectory()) return candidate;
            }
            System.err.println("Migration directory not found for module: " + module);
            System.exit(1);
            return null;
        }
    }

    static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        // Collect data
        List<String[]> rows = new ArrayList<>();
        String[] headers = new String[colCount];
        int[] widths = new int[colCount];

        for (int i = 1; i <= colCount; i++) {
            headers[i - 1] = meta.getColumnLabel(i);
            widths[i - 1] = headers[i - 1].length();
        }

        while (rs.next()) {
            String[] row = new String[colCount];
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                row[i - 1] = val != null ? val : "NULL";
                widths[i - 1] = Math.max(widths[i - 1], Math.min(row[i - 1].length(), 50));
            }
            rows.add(row);
        }

        // Print header
        StringBuilder sep = new StringBuilder("+");
        StringBuilder hdr = new StringBuilder("|");
        for (int i = 0; i < colCount; i++) {
            sep.append("-".repeat(widths[i] + 2)).append("+");
            hdr.append(" ").append(String.format("%-" + widths[i] + "s", headers[i])).append(" |");
        }
        System.out.println(sep);
        System.out.println(hdr);
        System.out.println(sep);

        // Print rows
        for (String[] row : rows) {
            StringBuilder line = new StringBuilder("|");
            for (int i = 0; i < colCount; i++) {
                String val = row[i].length() > 50 ? row[i].substring(0, 47) + "..." : row[i];
                line.append(" ").append(String.format("%-" + widths[i] + "s", val)).append(" |");
            }
            System.out.println(line);
        }
        System.out.println(sep);
        System.out.println(rows.size() + " row(s)");
    }
}
```

---

### Part 2: mate gen code 命令实现

#### Change 3: 代码生成器核心

从数据库表元数据生成完整 DDD 4 层代码：PO、DAO、Entity、Repository (interface + impl)、CommandService、QueryService (interface + impl)、Controller。

File: `mate-cli/src/main/java/vip/mate/cli/template/GenCodeTemplate.java`

```java
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
                import java.util.Date;
                
                @Data
                @TableName("%s")
                public class %sPO {
                
                    @TableId(type = IdType.ASSIGN_ID)
                    private String id;
                %s
                    @TableField(fill = FieldFill.INSERT)
                    private Date createdAt;
                    @TableField(fill = FieldFill.INSERT_UPDATE)
                    private Date updatedAt;
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
                
                    // TODO: Implement toPO() and toEntity() mapping
                    private %sPO toPO(%s entity) {
                        %sPO po = new %sPO();
                        po.setId(entity.getId());
                        return po;
                    }
                
                    private %s toEntity(%sPO po) {
                        return %s.builder()
                                .id(po.getId())
                                .build();
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
                entity, entity, entity, entity,
                entity, entity, entity);
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
            case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "Date";
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
```

#### Change 4: 更新 GenCommand

File: `mate-cli/src/main/java/vip/mate/cli/command/GenCommand.java`

```java
package vip.mate.cli.command;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import vip.mate.cli.template.GenCodeTemplate;

import java.nio.file.Path;

@Command(name = "gen", description = "Code generation",
        subcommands = {GenCommand.CodeSub.class})
public class GenCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Usage: mate gen code --table <table> --module <module>");
    }

    @Command(name = "code", description = "Reverse-generate DDD code from a DB table")
    public static class CodeSub implements Runnable {
        @Option(names = "--table", required = true, description = "Database table name (e.g. mate_order)")
        String table;

        @Option(names = "--module", required = true, description = "Target module (e.g. mate-order)")
        String module;

        @Option(names = "--service", description = "Service for DB connection", defaultValue = "mate-admin")
        String service;

        @Override
        public void run() {
            try {
                Path projectRoot = Path.of(System.getProperty("user.dir"));
                GenCodeTemplate template = new GenCodeTemplate(table, module, service, projectRoot);
                template.generate();
            } catch (Exception e) {
                System.err.println("Code generation failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
```

### Change 5: CLI pom.xml 增加 Flyway 和 YAML 依赖

File: `mate-cli/pom.xml`（增加依赖）

```xml
<!-- 在 <dependencies> 中添加 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-cli/src/.../config/DbConfig.java` | New | 数据库配置模型 |
| `mate-cli/src/.../config/DbConfigLoader.java` | New | 配置加载器 |
| `mate-cli/src/.../command/DbCommand.java` | Modify | 实现 query/describe/migrate |
| `mate-cli/src/.../template/GenCodeTemplate.java` | New | DDD 代码生成器 |
| `mate-cli/src/.../command/GenCommand.java` | Modify | 调用 GenCodeTemplate |
| `mate-cli/pom.xml` | Modify | 增加 Flyway + YAML 依赖 |

## 验证方案

1. `mvn clean compile -pl mate-cli -am` — 编译通过
2. 创建 `~/.mate/db.yml` 配置文件
3. `java -jar mate-cli.jar db describe mate_admin --service mate-admin` — 应输出表结构
4. `java -jar mate-cli.jar db query "SELECT COUNT(*) FROM mate_admin" --service mate-admin` — 应输出行数
5. `java -jar mate-cli.jar db migrate --module mate-admin` — 应执行 Flyway 迁移
6. `java -jar mate-cli.jar gen code --table mate_config --module mate-admin --service mate-admin` — 应生成 8 个 Java 文件
7. 检查生成的文件编译通过

## 注意事项

- **~/.mate/db.yml** 不提交到 Git，包含敏感的数据库密码
- **gen code 生成的是骨架**：Repository 的 toPO/toEntity 映射需要开发者手动补全字段
- **表名前缀处理**：`mate_` 前缀会被自动去除，`mate_order` → `Order`
- **不覆盖已有文件**：如果目标文件已存在，应跳过（当前实现会覆盖，正式版需加判断）
- **QueryService 返回 Entity**：简化实现直接返回 domain entity，正式项目应返回 VO record
