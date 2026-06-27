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

import java.time.LocalDateTime;
import java.util.List;

public interface IMcpServerQueryService {

    PageResult<McpServerView> page(int pageNum, int pageSize, String keyword, Integer status);

    List<McpServerView> listEnabled();

    McpServerView detail(String id);

    record McpServerView(String id, String code, String name, String transport,
                         String command, String args, String endpoint, String envJson,
                         String description, Integer toolCount, Integer status,
                         LocalDateTime lastCheckAt, String lastError, Integer sort) {}
}
