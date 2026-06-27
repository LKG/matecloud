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
package vip.mate.system.admin.trigger.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vip.mate.system.admin.application.query.IDictQueryService;
import vip.mate.system.admin.domain.dict.model.entity.DictData;

import java.util.List;

/**
 * Sample AI tools exposing dict-subdomain reads to Claude via Spring AI.
 * <p>
 * Any bean method annotated with Spring AI's {@code @Tool} is auto-discovered
 * by {@link vip.mate.starter.ai.tool.AiToolRegistry} and becomes callable
 * through:
 * <ul>
 *   <li>{@code POST /api/v1/ai/chat} (Claude picks which tool to call)</li>
 *   <li>{@code POST /api/v1/ai/tools/{name}/invoke} (direct REST invocation)</li>
 *   <li>{@code mate-cli ai chat} and {@code mate-cli --mcp} (stdio MCP bridge)</li>
 * </ul>
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class DictAiTools {

    private final IDictQueryService dictQueryService;

    @Tool(description = "List all dict entries for a given dictType. "
                      + "Useful when you need to know what options are available "
                      + "for a code/value field.")
    public List<DictData> listDictByType(
            @ToolParam(description = "Dict type code, e.g. 'user_status' or 'gender'")
            String dictType) {
        return dictQueryService.findByType(dictType);
    }

    @Tool(description = "Look up a single dict label by its dictType and dictValue. "
                      + "Returns null if no entry exists.")
    public String findDictLabel(
            @ToolParam(description = "Dict type code")
            String dictType,
            @ToolParam(description = "Dict value (the code stored in the DB)")
            String dictValue) {
        return dictQueryService.findLabel(dictType, dictValue);
    }
}
