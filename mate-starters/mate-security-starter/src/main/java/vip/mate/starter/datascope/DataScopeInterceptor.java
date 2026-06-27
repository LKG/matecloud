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
package vip.mate.starter.datascope;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * MyBatis Plus InnerInterceptor that appends data scope WHERE conditions.
 *
 * @author mateaix
 */
@Slf4j
public class DataScopeInterceptor implements InnerInterceptor {

    /**
     * Ids in this platform are either Snowflake numbers or 32-char UUID hex
     * strings — both alphanumeric. Restrict to {@code [A-Za-z0-9]} so quotes /
     * spaces / semicolons can never appear, then emit the value as a quoted SQL
     * string literal (dept_id / user columns are VARCHAR). This both avoids SQL
     * injection AND works for the project's non-numeric ids.
     */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9]+");

    /** Condition that matches no rows; used when an id value fails validation (fail-closed). */
    private static final String DENY_ALL = " AND 1 = 0";

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms,
                            Object parameter, RowBounds rowBounds,
                            ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        DataScopeContext ctx = DataScopeHolder.get();
        if (ctx == null || ctx.getScope() == DataScope.ALL) {
            return;
        }
        String originalSql = boundSql.getSql();
        // Table targeting: when includeTables is set, only rewrite a query that
        // references one of those tables (word boundary, so `mate_admin` does
        // NOT match `mate_admin_role`). Prevents corrupting unrelated queries
        // (e.g. per-row join lookups) that the annotated method also runs.
        if (!referencesIncludedTable(originalSql, ctx.getIncludeTables())) {
            return;
        }
        String scopeSql = buildScopeSql(originalSql, ctx);
        try {
            Field sqlField = BoundSql.class.getDeclaredField("sql");
            sqlField.setAccessible(true);
            sqlField.set(boundSql, scopeSql);
        } catch (Exception e) {
            log.error("Failed to set data scope SQL: {}", e.getMessage(), e);
        }
    }

    /** Empty/null includeTables → apply to every query (legacy). Otherwise require a word-boundary table match. */
    static boolean referencesIncludedTable(String sql, String[] includeTables) {
        if (includeTables == null || includeTables.length == 0) {
            return true;
        }
        if (sql == null) {
            return false;
        }
        String lower = sql.toLowerCase();
        for (String table : includeTables) {
            if (table == null || table.isBlank()) {
                continue;
            }
            if (Pattern.compile("\\b" + Pattern.quote(table.toLowerCase()) + "\\b").matcher(lower).find()) {
                return true;
            }
        }
        return false;
    }

    String buildScopeSql(String originalSql, DataScopeContext ctx) {
        return switch (ctx.getScope()) {
            case DEPT -> {
                String deptId = safeId(ctx.getDeptId());
                if (deptId == null) {
                    yield originalSql + DENY_ALL;
                }
                yield originalSql + " AND " + ctx.getDeptAlias() + " = '" + deptId + "'";
            }
            case DEPT_AND_CHILD -> {
                String deptId = safeId(ctx.getDeptId());
                if (deptId == null) {
                    yield originalSql + DENY_ALL;
                }
                yield originalSql + " AND " + ctx.getDeptAlias()
                        + " IN (SELECT id FROM mate_dept WHERE id = '" + deptId
                        + "' OR ancestors LIKE CONCAT('" + deptId + "', ',%'))";
            }
            case SELF -> {
                String userId = safeId(ctx.getUserId());
                if (userId == null) {
                    yield originalSql + DENY_ALL;
                }
                yield originalSql + " AND " + ctx.getUserAlias() + " = '" + userId + "'";
            }
            case CUSTOM -> {
                String inList = safeIdList(ctx.getCustomDeptIds());
                if (inList == null) {
                    yield originalSql + DENY_ALL;
                }
                yield originalSql + " AND " + ctx.getDeptAlias() + " IN (" + inList + ")";
            }
            default -> originalSql;
        };
    }

    /** Trimmed value if it is a bare alphanumeric id, otherwise null (rejects injection). */
    private static String safeId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (!SAFE_ID.matcher(trimmed).matches()) {
            log.warn("Rejected invalid data-scope id value: {}", value);
            return null;
        }
        return trimmed;
    }

    /** Comma-separated ids → quoted SQL IN list ({@code 'a', 'b'}); null if empty or any token invalid. */
    private static String safeIdList(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String token : csv.split(",")) {
            String id = safeId(token);
            if (id == null) {
                return null;
            }
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append('\'').append(id).append('\'');
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
