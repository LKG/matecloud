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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.domain.adapter.repository.IQueryFieldRepository;
import vip.mate.crm.query.domain.model.QueryField;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.dto.ConditionGroup;
import vip.mate.crm.query.dto.QueryCondition;
import vip.mate.crm.query.types.enums.FieldType;
import vip.mate.crm.query.types.enums.FilterOperator;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 安全动态查询翻译器: 把 {@link AdvancedQuery} 翻译成按列名的 {@link QueryWrapper}。
 *
 * <p>注入防护三重保障: ① 列名只能来自 {@code mate_crm_query_field} 白名单(前端永远只传
 * 逻辑 fieldKey); ② 列名再叠加 {@code ^[a-z_][a-z0-9_]*$} 正则兜底; ③ 所有值经 MyBatis
 * 参数绑定(eq/like/in/between...), 绝不字符串拼接。
 *
 * <p>组合语义: 组间 OR、组内 AND, 整体与数据权限/mineOnly 以 AND 结合。
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class CrmDynamicQueryBuilder {

    private static final Pattern SAFE_COLUMN = Pattern.compile("^[a-z_][a-z0-9_]*$");

    private final IQueryFieldRepository queryFieldRepository;

    /**
     * @param moduleKey   模块 key(白名单作用域)
     * @param query       高级查询请求体
     * @param scope       数据权限(可为 null 按全部)
     * @param ownerColumn 负责人物理列名(如 owner_id; 无则传 null 跳过范围/mine)
     * @param deptColumn  部门物理列名(如 dept_id; 无则传 null)
     * @return 仅含 where + order 的 wrapper(分页由调用方 selectPage 承载)
     */
    public <T> QueryWrapper<T> build(String moduleKey, AdvancedQuery query, CrmScope scope,
                                     String ownerColumn, String deptColumn) {
        return build(moduleKey, query, scope, ownerColumn, deptColumn, "created_at");
    }

    public <T> QueryWrapper<T> build(String moduleKey, AdvancedQuery query, CrmScope scope,
                                     String ownerColumn, String deptColumn, String defaultOrderColumn) {
        QueryWrapper<T> wrapper = new QueryWrapper<>();
        Map<String, QueryField> resolved = new HashMap<>();

        CrmScope effectiveScope = scope == null ? CrmScope.all() : scope;

        // 数据权限(AND): 负责人/部门列存在时才过滤
        if (ownerColumn != null && deptColumn != null) {
            effectiveScope.apply(wrapper, ownerColumn, deptColumn);
        }

        // mineOnly(AND): 仅看自己负责
        if (Boolean.TRUE.equals(query.getMineOnly()) && ownerColumn != null) {
            String uid = effectiveScope.userId();
            if (uid != null && !uid.isBlank()) {
                wrapper.eq(ownerColumn, uid);
            }
        }

        // 条件组: 整体作为单一单元与上面的 AND 结合 → scope AND ((g1) OR (g2) ...)
        List<ConditionGroup> groups = query.getGroups();
        if (groups != null && !groups.isEmpty()) {
            wrapper.and(outer -> {
                boolean first = true;
                for (ConditionGroup group : groups) {
                    if (group == null || group.getConditions() == null || group.getConditions().isEmpty()) {
                        continue;
                    }
                    if (first) {
                        outer.and(inner -> applyGroup(inner, moduleKey, group, resolved));
                        first = false;
                    } else {
                        outer.or(inner -> applyGroup(inner, moduleKey, group, resolved));
                    }
                }
            });
        }

        // 排序: orderField 走白名单解析, 否则默认列倒序
        String orderField = query.getOrderField();
        if (orderField != null && !orderField.isBlank()) {
            QueryField sortField = resolve(moduleKey, orderField, resolved);
            boolean asc = Boolean.TRUE.equals(query.getOrderAsc());
            wrapper.orderBy(true, asc, safeColumn(sortField));
        } else {
            wrapper.orderByDesc(defaultOrderColumn);
        }
        return wrapper;
    }

    /** 组内 AND。 */
    private <T> void applyGroup(QueryWrapper<T> inner, String moduleKey, ConditionGroup group,
                                Map<String, QueryField> resolved) {
        for (QueryCondition condition : group.getConditions()) {
            applyCondition(inner, moduleKey, condition, resolved);
        }
    }

    private <T> void applyCondition(QueryWrapper<T> inner, String moduleKey, QueryCondition condition,
                                    Map<String, QueryField> resolved) {
        QueryField field = resolve(moduleKey, condition.getFieldKey(), resolved);
        String column = safeColumn(field);
        FilterOperator operator = parseOperator(condition.getOperator());
        if (!field.allows(operator)) {
            throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(),
                    CrmErrorCode.OPERATOR_TYPE_MISMATCH.getMessage() + ": " + condition.getFieldKey());
        }
        List<String> values = condition.getValues();
        FieldType type = field.fieldType();
        switch (operator) {
            case IS_NULL -> inner.isNull(column);
            case IS_NOT_NULL -> inner.isNotNull(column);
            case IN -> inner.in(column, coerce(type, nonEmpty(values)));
            case BETWEEN -> {
                List<String> pair = require(values, 2);
                inner.between(column, coerce(type, pair.get(0)), coerce(type, pair.get(1)));
            }
            case EQ -> inner.eq(column, coerce(type, first(values)));
            case NE -> inner.ne(column, coerce(type, first(values)));
            case LIKE -> inner.like(column, first(values));
            case GT -> inner.gt(column, coerce(type, first(values)));
            case GE -> inner.ge(column, coerce(type, first(values)));
            case LT -> inner.lt(column, coerce(type, first(values)));
            case LE -> inner.le(column, coerce(type, first(values)));
        }
    }

    /** 白名单解析: 命中不了即拒绝(防越权探测列名)。 */
    private QueryField resolve(String moduleKey, String fieldKey, Map<String, QueryField> cache) {
        if (fieldKey == null || fieldKey.isBlank()) {
            throw deny();
        }
        String ck = moduleKey + "#" + fieldKey;
        if (cache.containsKey(ck)) {
            return cache.get(ck);
        }
        QueryField field = queryFieldRepository.find(moduleKey, fieldKey);
        if (field == null || !field.visible()) {
            throw deny();
        }
        cache.put(ck, field);
        return field;
    }

    private BizException deny() {
        return new BizException(CrmErrorCode.QUERY_FIELD_NOT_ALLOWED.getCode(),
                CrmErrorCode.QUERY_FIELD_NOT_ALLOWED.getMessage());
    }

    private String safeColumn(QueryField field) {
        String column = field.columnName();
        if (column == null || !SAFE_COLUMN.matcher(column).matches()) {
            throw deny();
        }
        return column;
    }

    private FilterOperator parseOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(),
                    CrmErrorCode.OPERATOR_TYPE_MISMATCH.getMessage());
        }
        try {
            return FilterOperator.parse(operator);
        } catch (IllegalArgumentException e) {
            throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(),
                    CrmErrorCode.OPERATOR_TYPE_MISMATCH.getMessage() + ": " + operator);
        }
    }

    /** NUMBER 字段转 BigDecimal, 其余按原值(字符串参数绑定, MySQL 侧安全比较)。 */
    private Object coerce(FieldType type, String value) {
        if (value == null) {
            return null;
        }
        if (type == FieldType.NUMBER) {
            try {
                return new BigDecimal(value.trim());
            } catch (NumberFormatException e) {
                throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(),
                        CrmErrorCode.OPERATOR_TYPE_MISMATCH.getMessage() + ": 非法数值 " + value);
            }
        }
        return value;
    }

    private List<Object> coerce(FieldType type, List<String> values) {
        return values.stream().map(v -> coerce(type, v)).map(Object.class::cast).toList();
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static List<String> nonEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private static List<String> require(List<String> values, int min) {
        if (values == null || values.size() < min) {
            throw new BizException(CrmErrorCode.OPERATOR_TYPE_MISMATCH.getCode(),
                    CrmErrorCode.OPERATOR_TYPE_MISMATCH.getMessage() + ": 需要至少 " + min + " 个值");
        }
        return values;
    }
}
