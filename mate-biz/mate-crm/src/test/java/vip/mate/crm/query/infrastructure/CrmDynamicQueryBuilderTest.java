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
package vip.mate.crm.query.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.domain.adapter.repository.IQueryFieldRepository;
import vip.mate.crm.query.domain.model.QueryField;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.dto.ConditionGroup;
import vip.mate.crm.query.dto.QueryCondition;
import vip.mate.crm.query.types.enums.FieldType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CrmDynamicQueryBuilder translation & injection-guard tests")
class CrmDynamicQueryBuilderTest {

    /** lead 模块白名单: 文本/枚举/金额/时间各一。 */
    private static Map<String, QueryField> whitelist() {
        Map<String, QueryField> m = new HashMap<>();
        m.put("lead#leadName", field("lead", "leadName", "lead_name", FieldType.TEXT));
        m.put("lead#status", field("lead", "status", "status", FieldType.SELECT));
        m.put("lead#money", field("lead", "money", "money", FieldType.NUMBER));
        m.put("lead#createdAt", field("lead", "createdAt", "created_at", FieldType.DATETIME));
        m.put("lead#hiddenField", field("lead", "hiddenField", "hidden_field", FieldType.TEXT, false));
        return m;
    }

    private static QueryField field(String module, String key, String column, FieldType type) {
        return field(module, key, column, type, true);
    }

    private static QueryField field(String module, String key, String column, FieldType type, boolean visible) {
        return new QueryField(module, key, column, key, type, null, 1, visible);
    }

    private static CrmDynamicQueryBuilder builder(Map<String, QueryField> wl) {
        IQueryFieldRepository repo = new IQueryFieldRepository() {
            @Override
            public List<QueryField> listByModule(String moduleKey) {
                return List.of();
            }

            @Override
            public QueryField find(String moduleKey, String fieldKey) {
                return wl.get(moduleKey + "#" + fieldKey);
            }
        };
        return new CrmDynamicQueryBuilder(repo);
    }

    private static QueryCondition cond(String fieldKey, String operator, String... values) {
        QueryCondition c = new QueryCondition();
        c.setFieldKey(fieldKey);
        c.setOperator(operator);
        c.setValues(List.of(values));
        return c;
    }

    private static ConditionGroup group(QueryCondition... conditions) {
        ConditionGroup g = new ConditionGroup();
        g.setConditions(List.of(conditions));
        return g;
    }

    private static AdvancedQuery query(ConditionGroup... groups) {
        AdvancedQuery q = new AdvancedQuery();
        q.setGroups(List.of(groups));
        return q;
    }

    @Test
    @DisplayName("LIKE maps whitelisted column and binds value as parameter")
    void like_bound_as_param() {
        QueryWrapper<Object> w = builder(whitelist())
                .build("lead", query(group(cond("leadName", "LIKE", "张"))), CrmScope.all(), "owner_id", "dept_id");

        assertTrue(w.getSqlSegment().contains("lead_name LIKE"), w.getSqlSegment());
        assertFalse(w.getSqlSegment().contains("张"), "value must not be concatenated into SQL");
        assertTrue(w.getParamNameValuePairs().containsValue("%张%"));
    }

    @Test
    @DisplayName("Injection payload stays inside bound parameters")
    void injection_only_bound() {
        String evil = "x' OR '1'='1";
        QueryWrapper<Object> w = builder(whitelist())
                .build("lead", query(group(cond("leadName", "LIKE", evil))), CrmScope.all(), "owner_id", "dept_id");

        assertFalse(w.getSqlSegment().contains("OR '1'='1"));
        assertTrue(w.getParamNameValuePairs().containsValue("%" + evil + "%"));
    }

    @Test
    @DisplayName("Unknown or invisible fieldKey rejected with CRMB100")
    void unknown_field_denied() {
        CrmDynamicQueryBuilder b = builder(whitelist());
        BizException e1 = assertThrows(BizException.class, () -> b.build("lead",
                query(group(cond("notMapped", "EQ", "v"))), CrmScope.all(), "owner_id", "dept_id"));
        assertEquals("CRMB100", e1.getCode());

        BizException e2 = assertThrows(BizException.class, () -> b.build("lead",
                query(group(cond("hiddenField", "EQ", "v"))), CrmScope.all(), "owner_id", "dept_id"));
        assertEquals("CRMB100", e2.getCode());
    }

    @Test
    @DisplayName("Operator not allowed by field type rejected with CRMB101")
    void operator_type_mismatch() {
        // SELECT 字段不允许 LIKE
        BizException e = assertThrows(BizException.class, () -> builder(whitelist()).build("lead",
                query(group(cond("status", "LIKE", "A"))), CrmScope.all(), "owner_id", "dept_id"));
        assertEquals("CRMB101", e.getCode());
    }

    @Test
    @DisplayName("Groups OR-ed, conditions inside a group AND-ed")
    void group_semantics() {
        QueryWrapper<Object> w = builder(whitelist()).build("lead",
                query(
                        group(cond("leadName", "LIKE", "a"), cond("status", "EQ", "FOLLOWING")),
                        group(cond("money", "GE", "100"))),
                CrmScope.all(), "owner_id", "dept_id");

        String sql = w.getSqlSegment();
        assertTrue(sql.contains(" AND "), sql);
        assertTrue(sql.contains(" OR "), sql);
        // 两个条件组都出现在同一个外层括号单元里
        assertTrue(sql.contains("lead_name LIKE") && sql.contains("money >="));
    }

    @Test
    @DisplayName("BETWEEN on NUMBER coerces both values to BigDecimal")
    void between_number() {
        QueryWrapper<Object> w = builder(whitelist()).build("lead",
                query(group(cond("money", "BETWEEN", "100", "200"))), CrmScope.all(), "owner_id", "dept_id");

        assertTrue(w.getSqlSegment().contains("money BETWEEN"));
        assertTrue(w.getParamNameValuePairs().containsValue(new BigDecimal("100")));
        assertTrue(w.getParamNameValuePairs().containsValue(new BigDecimal("200")));
    }

    @Test
    @DisplayName("mineOnly filters by current user on owner column")
    void mine_only() {
        CrmScope scope = new CrmScope(CrmScope.ALL, "u9", "d1", List.of());
        AdvancedQuery q = query(group(cond("leadName", "LIKE", "a")));
        q.setMineOnly(true);

        QueryWrapper<Object> w = builder(whitelist()).build("lead", q, scope, "owner_id", "dept_id");
        assertTrue(w.getSqlSegment().contains("owner_id ="));
        assertTrue(w.getParamNameValuePairs().containsValue("u9"));
    }

    @Test
    @DisplayName("orderField resolved via whitelist; unknown order field denied")
    void order_field() {
        AdvancedQuery q = query(group(cond("leadName", "LIKE", "a")));
        q.setOrderField("createdAt");
        q.setOrderAsc(true);
        QueryWrapper<Object> w = builder(whitelist()).build("lead", q, CrmScope.all(), "owner_id", "dept_id");
        assertTrue(w.getSqlSegment().contains("ORDER BY created_at ASC"), w.getSqlSegment());

        AdvancedQuery bad = query(group(cond("leadName", "LIKE", "a")));
        bad.setOrderField("password");
        assertThrows(BizException.class,
                () -> builder(whitelist()).build("lead", bad, CrmScope.all(), "owner_id", "dept_id"));
    }

    @Test
    @DisplayName("No groups falls back to default created_at desc ordering")
    void default_order() {
        AdvancedQuery q = new AdvancedQuery();
        QueryWrapper<Object> w = builder(whitelist()).build("lead", q, CrmScope.all(), "owner_id", "dept_id");
        assertTrue(w.getSqlSegment().contains("ORDER BY created_at DESC"), w.getSqlSegment());
    }
}
