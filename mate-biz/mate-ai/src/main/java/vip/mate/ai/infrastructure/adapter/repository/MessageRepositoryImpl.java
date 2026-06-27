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
package vip.mate.ai.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.ai.domain.adapter.repository.IMessageRepository;
import vip.mate.ai.domain.model.entity.MessageEntity;
import vip.mate.ai.infrastructure.dao.MessageDao;
import vip.mate.ai.infrastructure.dao.po.MessagePO;
import vip.mate.ai.types.enums.MessageRole;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements IMessageRepository {

    private final MessageDao messageDao;

    @Override
    public void append(MessageEntity message) {
        messageDao.insert(toPO(message));
    }

    @Override
    public void deleteByConversation(String conversationId) {
        messageDao.delete(new LambdaQueryWrapper<MessagePO>()
                .eq(MessagePO::getConversationId, conversationId));
    }

    @Override
    public List<MessageEntity> listByConversation(String conversationId) {
        return messageDao.selectList(new LambdaQueryWrapper<MessagePO>()
                        .eq(MessagePO::getConversationId, conversationId)
                        .orderByAsc(MessagePO::getCreatedAt))
                .stream().map(this::toDomain).toList();
    }

    private MessagePO toPO(MessageEntity m) {
        MessagePO po = new MessagePO();
        po.setId(m.getId());
        po.setConversationId(m.getConversationId());
        po.setRole(m.getRole() == null ? null : m.getRole().name());
        po.setContent(m.getContent());
        po.setToolName(m.getToolName());
        po.setToolPayload(m.getToolPayload());
        po.setPromptTokens(m.getPromptTokens());
        po.setCompletionTokens(m.getCompletionTokens());
        po.setTotalTokens(m.getTotalTokens());
        po.setLatencyMs(m.getLatencyMs());
        po.setFinishReason(m.getFinishReason());
        return po;
    }

    private MessageEntity toDomain(MessagePO po) {
        if (po == null) return null;
        return MessageEntity.builder()
                .id(po.getId())
                .conversationId(po.getConversationId())
                .role(MessageRole.parse(po.getRole()))
                .content(po.getContent())
                .toolName(po.getToolName())
                .toolPayload(po.getToolPayload())
                .promptTokens(po.getPromptTokens())
                .completionTokens(po.getCompletionTokens())
                .totalTokens(po.getTotalTokens())
                .latencyMs(po.getLatencyMs())
                .finishReason(po.getFinishReason())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
