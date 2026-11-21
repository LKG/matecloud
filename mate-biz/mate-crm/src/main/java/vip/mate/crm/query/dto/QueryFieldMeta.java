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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可查字段元数据(下发给前端的筛选器配置)。
 *
 * <p>刻意不含 {@code columnName} —— 真实列名永不出网, 前端只认 {@code fieldKey}。
 *
 * @author mateaix
 */
@Data
public class QueryFieldMeta {

    private String fieldKey;
    private String label;
    /** TEXT/NUMBER/DATE/DATETIME/SELECT/USER/DEPT/BOOLEAN。 */
    private String fieldType;
    /** 允许的算子(EQ/LIKE/GT/...), 前端按此渲染算子下拉。 */
    private List<String> operators;
    /** SELECT 可选值, 其余为 null。 */
    private List<OptionItem> options;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private String value;
        private String label;
    }
}
