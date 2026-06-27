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
package vip.mate.ai.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.ai.domain.adapter.repository.IMcpServerRepository;
import vip.mate.ai.domain.model.aggregate.McpServerAggregate;
import vip.mate.ai.infrastructure.adapter.repository.convertor.McpServerInfraConvertor;
import vip.mate.ai.infrastructure.dao.McpServerDao;
import vip.mate.ai.infrastructure.dao.po.McpServerPO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class McpServerRepositoryImpl implements IMcpServerRepository {

    private static final McpServerInfraConvertor CONVERTOR = McpServerInfraConvertor.INSTANCE;

    private final McpServerDao mcpServerDao;

    @Override public void save(McpServerAggregate s)       { mcpServerDao.insert(CONVERTOR.toPO(s)); }
    @Override public void update(McpServerAggregate s)     { mcpServerDao.updateById(CONVERTOR.toPO(s)); }
    @Override public void deleteById(String id)            { mcpServerDao.deleteById(id); }
    @Override public McpServerAggregate findById(String id){ return CONVERTOR.toDomain(mcpServerDao.selectById(id)); }

    @Override
    public McpServerAggregate findByCode(String code) {
        return CONVERTOR.toDomain(mcpServerDao.selectOne(new LambdaQueryWrapper<McpServerPO>()
                .eq(McpServerPO::getCode, code).last("LIMIT 1")));
    }

    @Override
    public PageResult<McpServerAggregate> page(int pageNum, int pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<McpServerPO> w = new LambdaQueryWrapper<McpServerPO>()
                .eq(status != null, McpServerPO::getStatus, status)
                .and(keyword != null && !keyword.isBlank(),
                        q -> q.like(McpServerPO::getName, keyword).or().like(McpServerPO::getCode, keyword))
                .orderByAsc(McpServerPO::getSort)
                .orderByDesc(McpServerPO::getCreatedAt);
        Page<McpServerPO> r = mcpServerDao.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(r.getRecords().stream().map(CONVERTOR::toDomain).toList(), r.getTotal());
    }

    @Override
    public List<McpServerAggregate> listEnabled() {
        return mcpServerDao.selectList(new LambdaQueryWrapper<McpServerPO>()
                        .eq(McpServerPO::getStatus, 1).orderByAsc(McpServerPO::getSort))
                .stream().map(CONVERTOR::toDomain).toList();
    }
}
