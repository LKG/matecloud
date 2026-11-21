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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 个人常用筛选(domain 实体, 无框架注解): 保存一份序列化的 AdvancedQuery, 仅本人可见。
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilter {

    private String id;
    private String userId;
    private String moduleKey;
    private String name;
    /** 序列化的 AdvancedQuery(groups/order/paging)。 */
    private String queryJson;
    private int sort;
}
