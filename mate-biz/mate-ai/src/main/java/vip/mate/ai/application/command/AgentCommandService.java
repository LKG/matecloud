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
package vip.mate.ai.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.ai.domain.adapter.repository.IAgentRepository;
import vip.mate.ai.domain.model.aggregate.AgentAggregate;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class AgentCommandService {

    private final IAgentRepository agentRepository;

    @Transactional(rollbackFor = Exception.class)
    public String create(AgentAggregate input) {
        if (agentRepository.findByCode(input.getCode()) != null) {
            throw new BizException(AiErrorCode.DUPLICATE_AGENT_CODE);
        }
        AgentAggregate fresh = AgentAggregate.newOne(input.getCode(), input.getName());
        fresh.applyUpdate(input);
        agentRepository.save(fresh);
        return fresh.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String id, AgentAggregate input) {
        AgentAggregate existing = agentRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.AGENT_NOT_EXIST);
        }
        existing.applyUpdate(input);
        agentRepository.update(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        AgentAggregate existing = agentRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.AGENT_NOT_EXIST);
        }
        if (existing.isBuiltIn()) {
            throw new BizException(AiErrorCode.CANNOT_DELETE_BUILTIN_AGENT);
        }
        agentRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(String id, boolean enabled) {
        AgentAggregate existing = agentRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.AGENT_NOT_EXIST);
        }
        if (enabled) {
            existing.enable();
        } else {
            existing.disable();
        }
        agentRepository.update(existing);
    }
}
