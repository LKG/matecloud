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
package vip.mate.crm.query.types.enums;

import java.util.Set;

/**
 * 可查字段类型: 决定前端值控件与后端允许的算子集合。
 *
 * @author mateaix
 */
public enum FieldType {

    /** 文本(输入框): EQ/NE/LIKE/IN/空判定。 */
    TEXT,
    /** 数值(数字框): 支持比较与区间。 */
    NUMBER,
    /** 日期(日期选择器)。 */
    DATE,
    /** 日期时间(日期时间选择器)。 */
    DATETIME,
    /** 枚举下拉(options_json 提供可选值)。 */
    SELECT,
    /** 人员(负责人/处理人等, 按姓名列过滤)。 */
    USER,
    /** 部门。 */
    DEPT,
    /** 布尔。 */
    BOOLEAN;

    /** 该类型允许的算子集合。 */
    public Set<FilterOperator> allowedOperators() {
        return switch (this) {
            case TEXT, USER -> Set.of(FilterOperator.EQ, FilterOperator.NE, FilterOperator.LIKE,
                    FilterOperator.IN, FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
            case NUMBER -> Set.of(FilterOperator.EQ, FilterOperator.NE, FilterOperator.GT, FilterOperator.GE,
                    FilterOperator.LT, FilterOperator.LE, FilterOperator.BETWEEN, FilterOperator.IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
            case DATE, DATETIME -> Set.of(FilterOperator.EQ, FilterOperator.GT, FilterOperator.GE,
                    FilterOperator.LT, FilterOperator.LE, FilterOperator.BETWEEN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
            case SELECT, DEPT -> Set.of(FilterOperator.EQ, FilterOperator.NE, FilterOperator.IN,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
            case BOOLEAN -> Set.of(FilterOperator.EQ, FilterOperator.NE,
                    FilterOperator.IS_NULL, FilterOperator.IS_NOT_NULL);
        };
    }

    public boolean supports(FilterOperator operator) {
        return allowedOperators().contains(operator);
    }

    /** 宽松解析, 未知类型按 TEXT 处理(不因元数据脏值整体失败)。 */
    public static FieldType ofNullable(String value) {
        if (value == null || value.isBlank()) {
            return TEXT;
        }
        try {
            return FieldType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
