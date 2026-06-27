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
package vip.mate.system.admin.trigger.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reverse-engineered code generator for the admin UI (RFC-050 G5). Reads the
 * application DataSource metadata to list tables, show columns, and produce
 * boilerplate DDD code as plain strings. The frontend renders these in tabs
 * and offers copy-to-clipboard / download. We deliberately do NOT touch the
 * filesystem here — that's reserved for {@code mate-cli gen code}.
 *
 * @author mateaix
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/codegen")
@RequiredArgsConstructor
public class CodeGenController {

    private final DataSource dataSource;

    @GetMapping("/tables")
    public Result<List<TableSummary>> listTables(
            @RequestParam(required = false) String prefix) {
        List<TableSummary> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (prefix != null && !prefix.isBlank() && !name.startsWith(prefix)) continue;
                    tables.add(new TableSummary(name, rs.getString("REMARKS")));
                }
            }
        } catch (SQLException e) {
            log.warn("[codegen] listTables failed: {}", e.getMessage());
            return Result.fail("Failed to list tables: " + e.getMessage());
        }
        return Result.ok(tables);
    }

    @GetMapping("/tables/{tableName}")
    public Result<TableDetail> describe(@PathVariable String tableName) {
        if (!isSafeIdentifier(tableName)) {
            return Result.fail("Invalid table name");
        }
        TableDetail detail = new TableDetail();
        detail.tableName = tableName;
        detail.columns = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, null)) {
                while (rs.next()) {
                    ColumnInfo c = new ColumnInfo();
                    c.name = rs.getString("COLUMN_NAME");
                    c.typeName = rs.getString("TYPE_NAME");
                    c.javaType = sqlTypeToJava(rs.getInt("DATA_TYPE"));
                    c.size = rs.getInt("COLUMN_SIZE");
                    c.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    c.comment = rs.getString("REMARKS");
                    detail.columns.add(c);
                }
            }
        } catch (SQLException e) {
            log.warn("[codegen] describe failed: {}", e.getMessage());
            return Result.fail("Failed to describe table: " + e.getMessage());
        }
        return Result.ok(detail);
    }

    @PostMapping("/preview")
    public Result<Map<String, String>> preview(@RequestBody PreviewRequest request) {
        Result<TableDetail> detailResult = describe(request.tableName);
        if (Boolean.FALSE.equals(detailResult.getSuccess())) {
            return Result.fail(detailResult.getCode(), detailResult.getMsg());
        }
        TableDetail detail = detailResult.getData();
        String entity = request.entityName != null && !request.entityName.isBlank()
                ? request.entityName
                : tableToClass(request.tableName);
        String pkg = request.basePackage != null && !request.basePackage.isBlank()
                ? request.basePackage
                : "vip.mate." + entity.toLowerCase();

        Map<String, String> files = new LinkedHashMap<>();
        files.put(entity + "PO.java", renderPO(detail, pkg, entity));
        files.put(entity + ".java", renderEntity(detail, pkg, entity));
        files.put(entity + "Dao.java", renderDao(pkg, entity));
        files.put(entity + "Controller.java", renderController(pkg, entity));
        return Result.ok(files);
    }

    // ---------- Renderers ----------

    private String renderPO(TableDetail detail, String pkg, String entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".infrastructure.dao.po;\n\n")
          .append("import com.baomidou.mybatisplus.annotation.*;\n")
          .append("import lombok.Data;\n")
          .append("import java.time.LocalDateTime;\n\n")
          .append("@Data\n@TableName(\"").append(detail.tableName).append("\")\n")
          .append("public class ").append(entity).append("PO {\n\n")
          .append("    @TableId(type = IdType.ASSIGN_ID)\n")
          .append("    private String id;\n");
        for (ColumnInfo c : detail.columns) {
            if (isBaseField(c.name)) continue;
            if (c.comment != null && !c.comment.isBlank()) {
                sb.append("    /** ").append(c.comment).append(" */\n");
            }
            sb.append("    private ").append(c.javaType).append(" ").append(toCamel(c.name)).append(";\n");
        }
        sb.append("    @TableField(fill = FieldFill.INSERT)\n    private LocalDateTime createdAt;\n")
          .append("    @TableField(fill = FieldFill.INSERT_UPDATE)\n    private LocalDateTime updatedAt;\n")
          .append("    @TableLogic\n    private Integer deleted;\n")
          .append("}\n");
        return sb.toString();
    }

    private String renderEntity(TableDetail detail, String pkg, String entity) {
        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(pkg).append(".domain.model.entity;\n\n")
          .append("import lombok.Data;\nimport lombok.EqualsAndHashCode;\n")
          .append("import lombok.NoArgsConstructor;\nimport lombok.experimental.SuperBuilder;\n")
          .append("import vip.mate.base.model.entity.BaseEntity;\n\n")
          .append("@Data\n@SuperBuilder\n@NoArgsConstructor\n@EqualsAndHashCode(callSuper = true)\n")
          .append("public class ").append(entity).append(" extends BaseEntity {\n");
        for (ColumnInfo c : detail.columns) {
            if (isBaseField(c.name) || "id".equals(c.name)) continue;
            sb.append("    private ").append(c.javaType).append(" ").append(toCamel(c.name)).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }

    private String renderDao(String pkg, String entity) {
        return "package " + pkg + ".infrastructure.dao;\n\n"
                + "import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n"
                + "import org.apache.ibatis.annotations.Mapper;\n"
                + "import " + pkg + ".infrastructure.dao.po." + entity + "PO;\n\n"
                + "@Mapper\n"
                + "public interface " + entity + "Dao extends BaseMapper<" + entity + "PO> {\n}\n";
    }

    private String renderController(String pkg, String entity) {
        String lc = Character.toLowerCase(entity.charAt(0)) + entity.substring(1);
        return "package " + pkg + ".trigger.controller;\n\n"
                + "import lombok.RequiredArgsConstructor;\n"
                + "import org.springframework.web.bind.annotation.*;\n"
                + "import vip.mate.base.result.Result;\n\n"
                + "@RestController\n"
                + "@RequestMapping(\"/api/v1/" + lc + "s\")\n"
                + "@RequiredArgsConstructor\n"
                + "public class " + entity + "Controller {\n\n"
                + "    @GetMapping(\"/{id}\")\n"
                + "    public Result<?> getById(@PathVariable String id) {\n"
                + "        return Result.ok(null);\n"
                + "    }\n"
                + "}\n";
    }

    // ---------- Helpers ----------

    private static boolean isSafeIdentifier(String name) {
        return name != null && name.matches("[A-Za-z0-9_]+");
    }

    private static boolean isBaseField(String columnName) {
        return columnName.equalsIgnoreCase("created_at")
                || columnName.equalsIgnoreCase("updated_at")
                || columnName.equalsIgnoreCase("deleted")
                || columnName.equalsIgnoreCase("lock_version");
    }

    private static String toCamel(String snake) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private static String tableToClass(String tableName) {
        String stripped = tableName.startsWith("mate_") ? tableName.substring(5) : tableName;
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : stripped.toCharArray()) {
            if (c == '_') { upper = true; continue; }
            sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }

    private static String sqlTypeToJava(int sqlType) {
        return switch (sqlType) {
            case Types.BIT, Types.BOOLEAN -> "Boolean";
            case Types.TINYINT, Types.SMALLINT -> "Integer";
            case Types.INTEGER -> "Integer";
            case Types.BIGINT -> "Long";
            case Types.FLOAT, Types.REAL -> "Float";
            case Types.DOUBLE -> "Double";
            case Types.NUMERIC, Types.DECIMAL -> "java.math.BigDecimal";
            case Types.DATE -> "java.time.LocalDate";
            case Types.TIME -> "java.time.LocalTime";
            case Types.TIMESTAMP -> "java.time.LocalDateTime";
            default -> "String";
        };
    }

    // ---------- DTOs ----------

    public record TableSummary(String name, String comment) {}

    public static class TableDetail {
        public String tableName;
        public List<ColumnInfo> columns;
    }

    public static class ColumnInfo {
        public String name;
        public String typeName;
        public String javaType;
        public int size;
        public boolean nullable;
        public String comment;
    }

    public static class PreviewRequest {
        public String tableName;
        public String entityName;
        public String basePackage;
    }
}
