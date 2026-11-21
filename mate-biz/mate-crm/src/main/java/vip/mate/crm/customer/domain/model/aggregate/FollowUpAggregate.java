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
package vip.mate.crm.customer.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.starter.distribute.util.SnowflakeUtil;

/**
 * 跟进记录聚合根(纯创建, 不可变; 删除即逻辑删)。
 *
 * @author mateaix
 */
@Getter
@Builder
public class FollowUpAggregate {

    private final FollowUp followUp;

    public static FollowUpAggregate create(String bizType, String bizId, String bizName, String customerId,
                                           String content, String followType, String nextContent,
                                           java.time.LocalDateTime nextTime, CrmCurrentUser operator) {
        if (bizType == null || bizType.isBlank() || bizId == null || bizId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "跟进对象不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "跟进内容不能为空");
        }
        FollowUp followUp = FollowUp.builder()
                .bizType(bizType)
                .bizId(bizId)
                .bizName(bizName)
                .customerId(customerId != null && !customerId.isBlank() ? customerId
                        : ("CUSTOMER".equals(bizType) ? bizId : null))
                .content(content)
                .followType(followType == null || followType.isBlank() ? "PHONE" : followType)
                .nextContent(nextContent)
                .nextTime(nextTime)
                .userId(operator.userId())
                .userName(operator.displayName())
                .deptId(operator.deptId())
                .build();
        followUp.setId(SnowflakeUtil.newSnowflakeId());
        return FollowUpAggregate.builder().followUp(followUp).build();
    }

    public static FollowUpAggregate of(FollowUp followUp) {
        if (followUp == null) {
            throw new BizException(vip.mate.crm.types.exception.CrmErrorCode.RECORD_NOT_FOUND);
        }
        return FollowUpAggregate.builder().followUp(followUp).build();
    }

    public String getId() {
        return followUp.getId();
    }
}
