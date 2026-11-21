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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

/**
 * 高级(条件)查询统一请求体: 所有列表页筛选的单一来源。
 *
 * <p>语义: {@code groups} 组间 OR, 缺省视为单组; {@code orderField} 为逻辑 field_key,
 * 由后端元数据白名单解析成真实列; {@code mineOnly} 为真时按负责人=当前用户过滤。
 *
 * @author mateaix
 */
@Data
public class AdvancedQuery {

    /** 条件组(组间 OR)。 */
    private List<ConditionGroup> groups;

    /** 排序字段(逻辑 field_key)。 */
    private String orderField;

    /** 是否升序(默认按创建时间倒序, 由 orderField 为空体现)。 */
    private Boolean orderAsc;

    /** 仅看我负责(折算为 owner 列 = 当前用户)。 */
    private Boolean mineOnly;

    private int pageNum = 1;

    private int pageSize = 20;

    /** 无任何有效条件(groups 为空或全部空组)。 */
    @JsonIgnore
    public boolean isEmpty() {
        if (groups == null || groups.isEmpty()) {
            return true;
        }
        return groups.stream().allMatch(g -> g == null || g.getConditions() == null || g.getConditions().isEmpty());
    }
}
