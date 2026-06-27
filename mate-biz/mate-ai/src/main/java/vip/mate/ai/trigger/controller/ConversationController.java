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
package vip.mate.ai.trigger.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import vip.mate.ai.application.command.ChatCommandService;
import vip.mate.ai.application.query.IConversationQueryService;
import vip.mate.ai.application.query.IConversationQueryService.ConversationDetail;
import vip.mate.ai.application.query.IConversationQueryService.ConversationView;
import vip.mate.ai.domain.model.aggregate.ConversationAggregate;
import vip.mate.ai.domain.model.entity.MessageEntity;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

/**
 * Persisted-chat endpoints. The starter still exposes a stateless
 * {@code /api/v1/ai/chat} for one-shot conversations; this controller is
 * scoped to {@code /api/v1/ai/conversations/**} and owns the message
 * transcript shown in the UI's "Library" / "Workspace" pages.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatCommandService chatCommandService;
    private final IConversationQueryService conversationQueryService;

    @Data
    public static class CreateRequest {
        private String title;
        private String agentId;
        private String agentCode;
        private String providerId;
        private String model;
        private String systemPrompt;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private Boolean archive;
    }

    @Data
    public static class MessageRequest {
        private String message;
    }

    @GetMapping
    public Result<PageResult<ConversationView>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(conversationQueryService.page(pageNum, pageSize, status, keyword));
    }

    @PostMapping
    public Result<ConversationView> create(@RequestBody CreateRequest req) {
        ConversationAggregate c = chatCommandService.createConversation(
                req.getTitle(), req.getAgentId(), req.getAgentCode(),
                req.getProviderId(), req.getModel(), req.getSystemPrompt());
        // Return as view for symmetry with the list endpoint
        return Result.ok(conversationQueryService.detail(c.getId()).header());
    }

    @GetMapping("/{id}")
    public Result<ConversationDetail> detail(@PathVariable String id) {
        return Result.ok(conversationQueryService.detail(id));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateRequest req) {
        chatCommandService.updateConversation(id, req.getTitle(), req.getArchive());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        chatCommandService.deleteConversation(id);
        return Result.ok();
    }

    @PostMapping("/{id}/messages")
    public Result<MessageEntity> sendMessage(@PathVariable String id, @RequestBody MessageRequest req) {
        return Result.ok(chatCommandService.sendMessage(id, req.getMessage()));
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMessage(@PathVariable String id, @RequestBody MessageRequest req) {
        return chatCommandService.streamMessage(id, req.getMessage());
    }
}
