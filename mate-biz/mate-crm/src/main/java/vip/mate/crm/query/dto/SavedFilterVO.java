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

/**
 * 个人常用筛选(下发前端, query 已反序列化为结构化 {@link AdvancedQuery})。
 *
 * @author mateaix
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilterVO {

    private String id;
    private String moduleKey;
    private String name;
    private AdvancedQuery query;
    private int sort;
}
