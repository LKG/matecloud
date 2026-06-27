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
package vip.mate.starter.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.starter.ai.tool.AiToolInvoker;
import vip.mate.starter.ai.tool.AiToolRegistry;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints exposing the AI tool registry.
 * <ul>
 *   <li>{@code GET  /api/v1/ai/tools}                — list every registered tool</li>
 *   <li>{@code POST /api/v1/ai/tools/{name}/invoke}  — invoke one by name</li>
 * </ul>
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/ai/tools")
@RequiredArgsConstructor
public class AiToolController {

    private final AiToolRegistry registry;
    private final AiToolInvoker invoker;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(registry.describeAll());
    }

    @PostMapping("/{name}/invoke")
    public Result<String> invoke(@PathVariable String name,
                                 @RequestBody(required = false) Map<String, Object> input) {
        return Result.ok(invoker.invoke(name, input));
    }
}
