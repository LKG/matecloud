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
import vip.mate.ai.domain.adapter.repository.IMcpServerRepository;
import vip.mate.ai.domain.model.aggregate.McpServerAggregate;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class McpServerCommandService {

    private final IMcpServerRepository mcpServerRepository;

    @Transactional(rollbackFor = Exception.class)
    public String create(McpServerAggregate input) {
        if (mcpServerRepository.findByCode(input.getCode()) != null) {
            throw new BizException(AiErrorCode.DUPLICATE_MCP_CODE);
        }
        McpServerAggregate fresh = McpServerAggregate.newOne(
                input.getCode(), input.getName(), input.getTransport());
        fresh.applyUpdate(input);
        mcpServerRepository.save(fresh);
        return fresh.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String id, McpServerAggregate input) {
        McpServerAggregate existing = mcpServerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.MCP_NOT_EXIST);
        }
        existing.applyUpdate(input);
        mcpServerRepository.update(existing);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        McpServerAggregate existing = mcpServerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.MCP_NOT_EXIST);
        }
        mcpServerRepository.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void toggleEnabled(String id, boolean enabled) {
        McpServerAggregate existing = mcpServerRepository.findById(id);
        if (existing == null) {
            throw new BizException(AiErrorCode.MCP_NOT_EXIST);
        }
        if (enabled) {
            existing.enable();
        } else {
            existing.disable();
        }
        mcpServerRepository.update(existing);
    }
}
