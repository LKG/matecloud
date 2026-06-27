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
package vip.mate.ai.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.ai.types.enums.MessageRole;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;

/**
 * A single message inside a conversation. Append-only — once written it is
 * never updated in place (the message log is the auditable conversation
 * history). Token / latency / finish-reason fields are populated only for
 * ASSISTANT turns when known.
 *
 * @author mateaix
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    private String id;
    private String conversationId;
    private MessageRole role;
    private String content;
    private String toolName;
    private String toolPayload;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Integer latencyMs;
    private String finishReason;
    private LocalDateTime createdAt;

    public static MessageEntity userMessage(String conversationId, String content) {
        return MessageEntity.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .conversationId(conversationId)
                .role(MessageRole.USER)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static MessageEntity assistantMessage(String conversationId, String content,
                                                 Integer latencyMs, String finishReason) {
        return MessageEntity.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .conversationId(conversationId)
                .role(MessageRole.ASSISTANT)
                .content(content)
                .latencyMs(latencyMs)
                .finishReason(finishReason)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
