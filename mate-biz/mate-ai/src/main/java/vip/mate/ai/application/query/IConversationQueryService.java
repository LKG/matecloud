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

public interface IConversationQueryService {

    PageResult<ConversationView> page(int pageNum, int pageSize, Integer status, String keyword);

    /** Header + full message list (oldest first). */
    ConversationDetail detail(String id);

    record ConversationView(String id, String title, String agentId, String agentCode,
                            String providerId, String model, Integer status, String statusLabel,
                            Integer messageCount, LocalDateTime lastActiveAt, LocalDateTime createdAt) {}

    record MessageView(String id, String role, String content, String toolName,
                       Integer latencyMs, String finishReason, LocalDateTime createdAt) {}

    record ConversationDetail(ConversationView header, List<MessageView> messages) {}
}
