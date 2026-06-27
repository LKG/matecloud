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
package vip.mate.notice.domain.model.aggregate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.notice.types.enums.BusinessType;
import vip.mate.notice.types.enums.NoticeChannel;
import vip.mate.notice.types.enums.NoticeStatus;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NoticeAggregate extends BaseEntity {

    public static final int MAX_RETRY_COUNT = 3;

    private NoticeChannel channel;
    private String target;
    private NoticeStatus status;
    private BusinessType businessType;
    private String content;
    private String resultMessage;
    private LocalDateTime sentAt;
    private Integer retryCount;

    /**
     * Optional structured template parameters carried from the upstream API
     * call. SMS gateways like Aliyun Dysmsapi require parameters as a JSON
     * object ({@code {"code":"123456"}}) rather than a rendered string.
     * Not persisted — set by the application layer, consumed by the channel
     * adapter, then dropped.
     */
    private transient Map<String, String> templateParams;

    public static NoticeAggregate create(NoticeChannel channel, String target,
                                         BusinessType businessType, String content) {
        return NoticeAggregate.builder()
                .id(SnowflakeUtil.newSnowflakeId())
                .channel(channel)
                .target(target)
                .status(NoticeStatus.PENDING)
                .businessType(businessType)
                .content(content)
                .retryCount(0)
                .build();
    }

    public void markAsSuccess(String resultMessage) {
        this.status = NoticeStatus.SUCCESS;
        this.resultMessage = resultMessage;
        this.sentAt = LocalDateTime.now();
    }

    public void markAsFailed(String resultMessage) {
        this.status = NoticeStatus.FAILED;
        this.resultMessage = resultMessage;
    }

    public void incrementRetry() {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    public boolean canRetry() {
        return (this.retryCount == null ? 0 : this.retryCount) < MAX_RETRY_COUNT
                && this.status == NoticeStatus.FAILED;
    }
}
