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
import vip.mate.ai.application.query.IMcpServerQueryService;
import vip.mate.ai.domain.adapter.repository.IMcpServerRepository;
import vip.mate.ai.domain.model.aggregate.McpServerAggregate;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class McpServerQueryServiceImpl implements IMcpServerQueryService {

    private final IMcpServerRepository mcpServerRepository;

    @Override
    public PageResult<McpServerView> page(int pageNum, int pageSize, String keyword, Integer status) {
        PageResult<McpServerAggregate> raw = mcpServerRepository.page(pageNum, pageSize, keyword, status);
        List<McpServerView> list = raw.getList().stream().map(this::toView).toList();
        return PageResult.of(list, raw.getTotal());
    }

    @Override
    public List<McpServerView> listEnabled() {
        return mcpServerRepository.listEnabled().stream().map(this::toView).toList();
    }

    @Override
    public McpServerView detail(String id) {
        McpServerAggregate s = mcpServerRepository.findById(id);
        if (s == null) {
            throw new BizException(AiErrorCode.MCP_NOT_EXIST);
        }
        return toView(s);
    }

    private McpServerView toView(McpServerAggregate s) {
        return new McpServerView(
                s.getId(), s.getCode(), s.getName(), s.getTransport(),
                s.getCommand(), s.getArgs(), s.getEndpoint(), s.getEnvJson(),
                s.getDescription(), s.getToolCount(), s.getStatus(),
                s.getLastCheckAt(), s.getLastError(), s.getSort());
    }
}
