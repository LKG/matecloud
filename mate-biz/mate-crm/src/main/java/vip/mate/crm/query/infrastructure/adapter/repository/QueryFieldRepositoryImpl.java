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
package vip.mate.crm.query.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.crm.query.domain.adapter.repository.IQueryFieldRepository;
import vip.mate.crm.query.domain.model.QueryField;
import vip.mate.crm.query.infrastructure.dao.QueryFieldDao;
import vip.mate.crm.query.infrastructure.dao.po.QueryFieldPO;
import vip.mate.crm.query.types.enums.FieldType;

import java.util.List;

/** 可查字段元数据仓储实现: 只读配置表, PO -> 值对象映射。 */
@Repository
@RequiredArgsConstructor
public class QueryFieldRepositoryImpl implements IQueryFieldRepository {

    private final QueryFieldDao queryFieldDao;

    @Override
    public List<QueryField> listByModule(String moduleKey) {
        LambdaQueryWrapper<QueryFieldPO> wrapper = new LambdaQueryWrapper<QueryFieldPO>()
                .eq(QueryFieldPO::getModuleKey, moduleKey)
                .orderByAsc(QueryFieldPO::getSort);
        return queryFieldDao.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public QueryField find(String moduleKey, String fieldKey) {
        LambdaQueryWrapper<QueryFieldPO> wrapper = new LambdaQueryWrapper<QueryFieldPO>()
                .eq(QueryFieldPO::getModuleKey, moduleKey)
                .eq(QueryFieldPO::getFieldKey, fieldKey)
                .last("LIMIT 1");
        QueryFieldPO po = queryFieldDao.selectOne(wrapper);
        return po == null ? null : toDomain(po);
    }

    private QueryField toDomain(QueryFieldPO po) {
        return new QueryField(
                po.getModuleKey(),
                po.getFieldKey(),
                po.getColumnName(),
                po.getLabel(),
                FieldType.ofNullable(po.getFieldType()),
                po.getOptionsJson(),
                po.getSort() == null ? 0 : po.getSort(),
                po.getVisible() == null || po.getVisible() == 1);
    }
}
