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
 * 保存/更新个人常用筛选请求体。{@code id} 为空视为新增, 非空视为覆盖更新。
 *
 * @author mateaix
 */
@Data
public class SavedFilterRequest {

    private String id;
    private String moduleKey;
    private String name;
    private AdvancedQuery query;
    private Integer sort;
}
