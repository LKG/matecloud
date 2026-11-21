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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 条件算子: 后端据字段类型校验合法性, 再翻译成 QueryWrapper 对应方法(值全部参数绑定)。
 *
 * @author mateaix
 */
@Getter
@RequiredArgsConstructor
public enum FilterOperator {

    EQ("等于", 1),
    NE("不等于", 1),
    LIKE("包含", 1),
    GT("大于", 1),
    GE("大于等于", 1),
    LT("小于", 1),
    LE("小于等于", 1),
    IN("属于(多值)", 2),
    BETWEEN("区间(两值)", 2),
    IS_NULL("为空", 0),
    IS_NOT_NULL("不为空", 0);

    private final String label;
    /** 最少需要的值个数(多值算子用 values 列表承载)。 */
    private final int minValues;

    /** 该算子是否为空判定(无需值)。 */
    public boolean isNullCheck() {
        return this == IS_NULL || this == IS_NOT_NULL;
    }

    public static FilterOperator parse(String value) {
        return FilterOperator.valueOf(value.trim().toUpperCase());
    }
}
