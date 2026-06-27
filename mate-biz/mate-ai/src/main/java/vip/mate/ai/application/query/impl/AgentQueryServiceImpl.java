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
package vip.mate.ai.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.ai.application.query.IAgentQueryService;
import vip.mate.ai.domain.adapter.repository.IAgentRepository;
import vip.mate.ai.domain.model.aggregate.AgentAggregate;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentQueryServiceImpl implements IAgentQueryService {

    private final IAgentRepository agentRepository;

    @Override
    public PageResult<AgentView> page(int pageNum, int pageSize, String keyword, Integer enabled) {
        PageResult<AgentAggregate> raw = agentRepository.page(pageNum, pageSize, keyword, enabled);
        List<AgentView> list = raw.getList().stream().map(this::toView).toList();
        return PageResult.of(list, raw.getTotal());
    }

    @Override
    public List<AgentView> listEnabled() {
        return agentRepository.listEnabled().stream().map(this::toView).toList();
    }

    @Override
    public AgentView detail(String id) {
        AgentAggregate a = agentRepository.findById(id);
        if (a == null) {
            throw new BizException(AiErrorCode.AGENT_NOT_EXIST);
        }
        return toView(a);
    }

    private AgentView toView(AgentAggregate a) {
        return new AgentView(
                a.getId(), a.getCode(), a.getName(), a.getNameEn(), a.getCategory(),
                a.getProvider(), a.getDescription(), a.getIcon(),
                a.getInstallCmd(), a.getLaunchCmd(), a.getDefaultModel(),
                a.getSystemPrompt(), a.getEnabled(), a.getBuiltIn(), a.getSort());
    }
}
