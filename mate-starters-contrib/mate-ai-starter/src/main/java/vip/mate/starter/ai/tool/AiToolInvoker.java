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
package vip.mate.starter.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import vip.mate.base.exception.BizException;

import java.util.Map;

/**
 * Invokes a Spring AI {@link ToolCallback} by name with a JSON-style input map.
 * Used by {@code POST /api/v1/ai/tools/{name}/invoke}. The LLM-driven path
 * (via {@code ChatClient}) doesn't need this — Spring AI dispatches tool calls
 * internally.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class AiToolInvoker {

    private final AiToolRegistry registry;
    private final ObjectMapper objectMapper;

    public String invoke(String toolName, Map<String, Object> input) {
        ToolCallback callback = registry.get(toolName);
        if (callback == null) {
            throw new BizException("AI_TOOL_NOT_FOUND", "Unknown AI tool: " + toolName);
        }
        try {
            String inputJson = objectMapper.writeValueAsString(input == null ? Map.of() : input);
            return callback.call(inputJson);
        } catch (Exception e) {
            log.warn("[mate-ai] Tool invocation failed: {}", toolName, e);
            throw new BizException("AI_TOOL_INVOCATION_FAILED",
                    "Tool '" + toolName + "' failed: " + e.getMessage());
        }
    }
}
