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
import vip.mate.ai.domain.adapter.repository.IAgentRepository;
import vip.mate.ai.domain.model.aggregate.AgentAggregate;
import vip.mate.ai.infrastructure.adapter.repository.convertor.AgentInfraConvertor;
import vip.mate.ai.infrastructure.dao.AgentDao;
import vip.mate.ai.infrastructure.dao.po.AgentPO;
import vip.mate.base.result.PageResult;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements IAgentRepository {

    private static final AgentInfraConvertor CONVERTOR = AgentInfraConvertor.INSTANCE;

    private final AgentDao agentDao;

    @Override public void save(AgentAggregate agent)    { agentDao.insert(CONVERTOR.toPO(agent)); }
    @Override public void update(AgentAggregate agent)  { agentDao.updateById(CONVERTOR.toPO(agent)); }
    @Override public void deleteById(String id)         { agentDao.deleteById(id); }
    @Override public AgentAggregate findById(String id) { return CONVERTOR.toDomain(agentDao.selectById(id)); }

    @Override
    public AgentAggregate findByCode(String code) {
        return CONVERTOR.toDomain(agentDao.selectOne(new LambdaQueryWrapper<AgentPO>()
                .eq(AgentPO::getCode, code).last("LIMIT 1")));
    }

    @Override
    public PageResult<AgentAggregate> page(int pageNum, int pageSize, String keyword, Integer enabled) {
        LambdaQueryWrapper<AgentPO> w = new LambdaQueryWrapper<AgentPO>()
                .eq(enabled != null, AgentPO::getEnabled, enabled)
                .and(keyword != null && !keyword.isBlank(),
                        q -> q.like(AgentPO::getName, keyword).or().like(AgentPO::getCode, keyword))
                .orderByAsc(AgentPO::getSort)
                .orderByDesc(AgentPO::getCreatedAt);
        Page<AgentPO> r = agentDao.selectPage(new Page<>(pageNum, pageSize), w);
        return PageResult.of(r.getRecords().stream().map(CONVERTOR::toDomain).toList(), r.getTotal());
    }

    @Override
    public List<AgentAggregate> listEnabled() {
        return agentDao.selectList(new LambdaQueryWrapper<AgentPO>()
                        .eq(AgentPO::getEnabled, 1).orderByAsc(AgentPO::getSort))
                .stream().map(CONVERTOR::toDomain).toList();
    }
}
