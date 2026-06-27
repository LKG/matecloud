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
package vip.mate.ai.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.ai.application.command.McpServerCommandService;
import vip.mate.ai.application.query.IMcpServerQueryService;
import vip.mate.ai.application.query.IMcpServerQueryService.McpServerView;
import vip.mate.ai.domain.model.aggregate.McpServerAggregate;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerCommandService mcpServerCommandService;
    private final IMcpServerQueryService mcpServerQueryService;

    @GetMapping
    public Result<PageResult<McpServerView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(mcpServerQueryService.page(pageNum, pageSize, keyword, status));
    }

    @GetMapping("/enabled")
    public Result<List<McpServerView>> listEnabled() {
        return Result.ok(mcpServerQueryService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<McpServerView> detail(@PathVariable String id) {
        return Result.ok(mcpServerQueryService.detail(id));
    }

    @PostMapping
    public Result<String> create(@RequestBody McpServerAggregate input) {
        return Result.ok(mcpServerCommandService.create(input));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody McpServerAggregate input) {
        mcpServerCommandService.update(id, input);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        mcpServerCommandService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/enabled")
    public Result<Void> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        mcpServerCommandService.toggleEnabled(id, enabled);
        return Result.ok();
    }
}
