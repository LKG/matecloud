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
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;

/**
 * 条件查询共享支撑: 组合 {@link CrmDynamicQueryBuilder} 与分页, 供各模块仓储复用。
 *
 * <p>调用方(仓储实现)传入自己的 {@link BaseMapper} + moduleKey + 负责人/部门列名,
 * 得到 {@code Page<PO>}, 再用现成 toEntity 映射为聚合。
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class CrmSearchSupport {

    private final CrmDynamicQueryBuilder builder;

    /** 执行分页条件查询(默认按 created_at 倒序)。 */
    public <T> Page<T> search(BaseMapper<T> dao, String moduleKey, AdvancedQuery query,
                              CrmScope scope, String ownerColumn, String deptColumn) {
        return search(dao, moduleKey, query, scope, ownerColumn, deptColumn, "created_at");
    }

    public <T> Page<T> search(BaseMapper<T> dao, String moduleKey, AdvancedQuery query,
                              CrmScope scope, String ownerColumn, String deptColumn, String defaultOrderColumn) {
        QueryWrapper<T> wrapper = builder.build(moduleKey, query, scope, ownerColumn, deptColumn, defaultOrderColumn);
        int pageNum = query.getPageNum() <= 0 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() <= 0 ? 20 : query.getPageSize();
        return dao.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }
}
