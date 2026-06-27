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
package vip.mate.ai.domain.model.aggregate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.ai.types.enums.ConversationStatus;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;

/**
 * Chat session aggregate. Owns its header (title, agent, provider, model)
 * and a counter of attached messages. The actual message stream is stored
 * in a separate entity ({@link vip.mate.ai.domain.model.entity.MessageEntity})
 * loaded on demand from the repository.
 *
 * @author mateaix
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConversationAggregate extends BaseEntity {

    private String tenantId;
    private String ownerId;
    private String ownerName;
    private String title;
    private String agentId;
    private String agentCode;
    private String providerId;
    private String model;
    private String systemPrompt;
    private ConversationStatus status;
    private Integer messageCount;
    private LocalDateTime lastActiveAt;

    /** Create a fresh conversation owned by the given admin. */
    public static ConversationAggregate create(String ownerId, String ownerName,
                                               String tenantId, String title,
                                               String agentId, String agentCode,
                                               String providerId, String model,
                                               String systemPrompt) {
        return ConversationAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .tenantId(tenantId)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .title(title == null || title.isBlank() ? "新会话" : title)
                .agentId(agentId)
                .agentCode(agentCode)
                .providerId(providerId)
                .model(model)
                .systemPrompt(systemPrompt)
                .status(ConversationStatus.ACTIVE)
                .messageCount(0)
                .lastActiveAt(LocalDateTime.now())
                .build();
    }

    public void touchActivity() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void incrementMessages(int delta) {
        this.messageCount = (this.messageCount == null ? 0 : this.messageCount) + delta;
    }

    public void rename(String newTitle) {
        if (newTitle != null && !newTitle.isBlank()) {
            this.title = newTitle.trim();
        }
    }

    public void archive() {
        this.status = ConversationStatus.ARCHIVED;
    }

    public boolean isOwnedBy(String adminId) {
        return this.ownerId != null && this.ownerId.equals(adminId);
    }
}
