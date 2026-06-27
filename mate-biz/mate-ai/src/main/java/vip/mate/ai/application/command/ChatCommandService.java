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
package vip.mate.ai.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import vip.mate.ai.domain.adapter.repository.IConversationRepository;
import vip.mate.ai.domain.adapter.repository.IMessageRepository;
import vip.mate.ai.domain.model.aggregate.ConversationAggregate;
import vip.mate.ai.domain.model.entity.MessageEntity;
import vip.mate.ai.infrastructure.support.AuthContext;
import vip.mate.ai.types.exception.AiErrorCode;
import vip.mate.base.exception.BizException;
import vip.mate.starter.ai.chat.AiChatService;

/**
 * Orchestrates chat turns: persists the user prompt + the assistant reply
 * around a call into {@link AiChatService}. The starter's in-memory
 * {@code MessageChatMemoryAdvisor} provides the rolling context window
 * Spring AI needs; this command service owns the durable transcript
 * shown in the UI's "task library".
 *
 * @author mateaix
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCommandService {

    private final IConversationRepository conversationRepository;
    private final IMessageRepository messageRepository;
    private final AiChatService aiChatService;

    /**
     * Self-injected proxy so {@link #persistFailedTurn} runs through Spring AOP
     * in its own {@code REQUIRES_NEW} transaction (a plain {@code this.} call
     * would be self-invocation and bypass the proxy). {@code @Lazy} breaks the
     * self-reference bean cycle.
     */
    @Lazy
    @Autowired
    private ChatCommandService self;

    /** Create a new conversation owned by the current admin. */
    @Transactional(rollbackFor = Exception.class)
    public ConversationAggregate createConversation(String title, String agentId, String agentCode,
                                                    String providerId, String model,
                                                    String systemPrompt) {
        ConversationAggregate c = ConversationAggregate.create(
                AuthContext.currentUserId(),
                AuthContext.currentUserName(),
                AuthContext.currentTenantId(),
                title, agentId, agentCode, providerId, model, systemPrompt);
        conversationRepository.save(c);
        log.info("[mate-ai] conversation created id={} owner={}", c.getId(), c.getOwnerId());
        return c;
    }

    /** Rename / archive an existing conversation. */
    @Transactional(rollbackFor = Exception.class)
    public void updateConversation(String id, String newTitle, Boolean archive) {
        ConversationAggregate c = mustOwn(id);
        if (newTitle != null && !newTitle.isBlank()) {
            c.rename(newTitle);
        }
        if (Boolean.TRUE.equals(archive)) {
            c.archive();
        }
        conversationRepository.update(c);
    }

    /** Delete a conversation along with its message log. */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String id) {
        ConversationAggregate c = mustOwn(id);
        messageRepository.deleteByConversation(c.getId());
        conversationRepository.deleteById(c.getId());
    }

    /**
     * Blocking send: persists the user message, runs the chat call, persists
     * the assistant reply, then updates the conversation's activity counter.
     */
    @Transactional(rollbackFor = Exception.class)
    public MessageEntity sendMessage(String conversationId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new BizException(AiErrorCode.EMPTY_USER_MESSAGE);
        }
        ConversationAggregate c = mustOwn(conversationId);

        MessageEntity userMsg = MessageEntity.userMessage(c.getId(), userMessage);
        messageRepository.append(userMsg);

        long t0 = System.currentTimeMillis();
        String reply;
        try {
            reply = aiChatService.chat(c.getId(), c.getSystemPrompt(), userMessage);
        } catch (RuntimeException e) {
            log.error("[mate-ai] chat call failed for conversation={}", c.getId(), e);
            // Persist the failed turn in a SEPARATE (REQUIRES_NEW) transaction so it
            // survives the rollback that the rethrow below triggers on THIS method's
            // transaction — otherwise both the user message and the error reply would
            // vanish and the transcript would not show what went wrong.
            self.persistFailedTurn(c.getId(), userMessage, e.getMessage(),
                    (int) (System.currentTimeMillis() - t0));
            throw new BizException("AIE001", "AI provider call failed: " + e.getMessage(), e);
        }
        int latency = (int) (System.currentTimeMillis() - t0);
        MessageEntity assistantMsg = MessageEntity.assistantMessage(c.getId(), reply, latency, "STOP");
        messageRepository.append(assistantMsg);

        c.touchActivity();
        c.incrementMessages(2);
        conversationRepository.update(c);
        return assistantMsg;
    }

    /**
     * Persist a failed chat turn (user prompt + error reply) durably, in its own
     * transaction. Invoked via the {@link #self} proxy from {@link #sendMessage}'s
     * failure path so the record commits even though the caller's transaction
     * rolls back. Best-effort: never throws, so it can't mask the original error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void persistFailedTurn(String conversationId, String userMessage,
                                  String errorMessage, int latency) {
        try {
            ConversationAggregate c = conversationRepository.findById(conversationId);
            if (c == null) {
                return;
            }
            messageRepository.append(MessageEntity.userMessage(conversationId, userMessage));
            messageRepository.append(MessageEntity.assistantMessage(conversationId,
                    "[模型调用失败] " + errorMessage, latency, "ERROR"));
            c.touchActivity();
            c.incrementMessages(2);
            conversationRepository.update(c);
        } catch (Exception persistEx) {
            log.error("[mate-ai] failed to persist failed turn for {}", conversationId, persistEx);
        }
    }

    /**
     * Streaming send: persists the user message immediately, then returns a
     * Flux that fans out the assistant fragments. When the stream completes
     * we persist the joined reply + bump activity counters. Errors are
     * persisted too so the transcript stays consistent with what was shown.
     */
    public Flux<String> streamMessage(String conversationId, String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return Flux.error(new BizException(AiErrorCode.EMPTY_USER_MESSAGE));
        }
        ConversationAggregate c = mustOwn(conversationId);

        MessageEntity userMsg = MessageEntity.userMessage(c.getId(), userMessage);
        messageRepository.append(userMsg);

        long t0 = System.currentTimeMillis();
        StringBuilder collected = new StringBuilder();
        return aiChatService.stream(c.getId(), c.getSystemPrompt(), userMessage)
                .doOnNext(collected::append)
                .doOnComplete(() -> finalizeStream(c, collected.toString(), t0, "STOP"))
                .doOnError(err -> finalizeStream(c,
                        collected.length() == 0
                                ? "[模型调用失败] " + err.getMessage()
                                : collected + "\n\n[流被中断] " + err.getMessage(),
                        t0, "ERROR"));
    }

    private void finalizeStream(ConversationAggregate c, String fullReply, long t0, String finish) {
        try {
            MessageEntity assistantMsg = MessageEntity.assistantMessage(
                    c.getId(), fullReply, (int) (System.currentTimeMillis() - t0), finish);
            messageRepository.append(assistantMsg);
            c.touchActivity();
            c.incrementMessages(2);
            conversationRepository.update(c);
        } catch (Exception persistEx) {
            log.error("[mate-ai] failed to persist streamed assistant reply for {}", c.getId(), persistEx);
        }
    }

    private ConversationAggregate mustOwn(String id) {
        ConversationAggregate c = conversationRepository.findById(id);
        if (c == null) {
            throw new BizException(AiErrorCode.CONVERSATION_NOT_EXIST);
        }
        // Fail closed: a missing user context must deny access, never grant it.
        String currentUser = AuthContext.currentUserId();
        if (currentUser == null || !c.isOwnedBy(currentUser)) {
            throw new BizException(AiErrorCode.NOT_OWNER_OF_CONVERSATION);
        }
        return c;
    }
}
