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
package vip.mate.crm.query.dto;

import lombok.Data;

/**
 * 单个查询条件: 逻辑字段 key + 算子 + 值列表。
 *
 * <p>前端只传 {@code fieldKey}(如 status), 绝不传列名; 真实列由后端元数据白名单解析。
 *
 * @author mateaix
 */
@Data
public class QueryCondition {

    /** 逻辑字段名(元数据 field_key)。 */
    private String fieldKey;

    /** 算子(EQ/LIKE/GT/BETWEEN/IN/..., 见 {@code FilterOperator})。 */
    private String operator;

    /** 值列表: 单值算子取第 0 个, BETWEEN 取两值, IN 取多值; 空判定忽略。 */
    private java.util.List<String> values;
}
