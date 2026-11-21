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
package vip.mate.crm.query.domain.model;

import vip.mate.crm.query.types.enums.FieldType;
import vip.mate.crm.query.types.enums.FilterOperator;

/**
 * 可查字段元数据(domain 值对象, 无框架注解)。
 *
 * <p>列名白名单的唯一来源: 翻译器只会使用这里的 {@code columnName}, 前端永远看不到真实列。
 *
 * @param moduleKey   模块 key
 * @param fieldKey    逻辑字段 key
 * @param columnName  真实物理列名(受信任的内部配置)
 * @param label       中文名
 * @param fieldType   字段类型
 * @param optionsJson SELECT 可选值 JSON(其余为 null)
 * @param sort        排序
 * @param visible     是否可见
 * @author mateaix
 */
public record QueryField(String moduleKey, String fieldKey, String columnName, String label,
                         FieldType fieldType, String optionsJson, int sort, boolean visible) {

    /** 该字段是否允许指定算子。 */
    public boolean allows(FilterOperator operator) {
        return fieldType.supports(operator);
    }
}
