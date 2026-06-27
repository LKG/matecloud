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
package vip.mate.ai.domain.adapter.repository;

import vip.mate.ai.domain.model.entity.MessageEntity;

import java.util.List;

public interface IMessageRepository {

    void append(MessageEntity message);

    void deleteByConversation(String conversationId);

    /** All messages for a conversation, oldest first. */
    List<MessageEntity> listByConversation(String conversationId);
}
