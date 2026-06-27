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
import vip.mate.ai.application.query.IConversationQueryService;
import vip.mate.ai.domain.adapter.repository.IConversationRepository;
import vip.mate.ai.domain.adapter.repository.IMessageRepository;
import vip.mate.ai.domain.model.aggregate.ConversationAggregate;
import vip.mate.ai.domain.model.entity.MessageEntity;
import vip.mate.ai.infrastructure.support.AuthContext;
import vip.mate.ai.types.enums.ConversationStatus;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationQueryServiceImpl implements IConversationQueryService {

    private final IConversationRepository conversationRepository;
    private final IMessageRepository messageRepository;

    @Override
    public PageResult<ConversationView> page(int pageNum, int pageSize, Integer status, String keyword) {
        PageResult<ConversationAggregate> raw = conversationRepository.pageByOwner(
                AuthContext.currentUserId(), pageNum, pageSize, status, keyword);
        List<ConversationView> list = raw.getList().stream().map(this::toView).toList();
        return PageResult.of(list, raw.getTotal());
    }

    @Override
    public ConversationDetail detail(String id) {
        ConversationAggregate c = conversationRepository.findById(id);
        if (c == null) {
            throw new BizException(AiErrorCode.CONVERSATION_NOT_EXIST);
        }
        // Fail closed: no resolved user (request outside the authenticated
        // gateway path) must NOT be allowed to read another user's conversation.
        String currentUser = AuthContext.currentUserId();
        if (currentUser == null || !c.isOwnedBy(currentUser)) {
            throw new BizException(AiErrorCode.NOT_OWNER_OF_CONVERSATION);
        }
        List<MessageEntity> msgs = messageRepository.listByConversation(id);
        List<MessageView> mv = msgs.stream().map(this::toMessageView).toList();
        return new ConversationDetail(toView(c), mv);
    }

    private ConversationView toView(ConversationAggregate c) {
        ConversationStatus st = c.getStatus() == null ? ConversationStatus.ACTIVE : c.getStatus();
        return new ConversationView(
                c.getId(), c.getTitle(),
                c.getAgentId(), c.getAgentCode(),
                c.getProviderId(), c.getModel(),
                st.getCode(), st.getDescription(),
                c.getMessageCount(), c.getLastActiveAt(),
                c.getCreatedAt());
    }

    private MessageView toMessageView(MessageEntity m) {
        return new MessageView(
                m.getId(),
                m.getRole() == null ? null : m.getRole().name(),
                m.getContent(),
                m.getToolName(),
                m.getLatencyMs(),
                m.getFinishReason(),
                m.getCreatedAt());
    }
}
