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
package vip.mate.ai.application.query;

import vip.mate.base.result.PageResult;

import java.util.List;

public interface IAgentQueryService {

    PageResult<AgentView> page(int pageNum, int pageSize, String keyword, Integer enabled);

    List<AgentView> listEnabled();

    AgentView detail(String id);

    record AgentView(String id, String code, String name, String nameEn, String category,
                     String provider, String description, String icon,
                     String installCmd, String launchCmd, String defaultModel,
                     String systemPrompt, Integer enabled, Integer builtIn, Integer sort) {}
}
