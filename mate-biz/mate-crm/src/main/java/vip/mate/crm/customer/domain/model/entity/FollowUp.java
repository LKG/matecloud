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
package vip.mate.crm.customer.domain.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跟进记录实体: 多态挂载(LEAD/CUSTOMER/CONTACT/BUSINESS)。
 *
 * @author mateaix
 */
@Data
@Builder
public class FollowUp {

    private String id;
    /** LEAD/CUSTOMER/CONTACT/BUSINESS */
    private String bizType;
    private String bizId;
    private String bizName;
    /** 归属客户(公海回收统计; bizType=CUSTOMER 时=bizId) */
    private String customerId;
    private String content;
    /** PHONE/VISIT/WECHAT/EMAIL/MEETING/OTHER */
    private String followType;
    private String nextContent;
    private LocalDateTime nextTime;
    private String userId;
    private String userName;
    private String deptId;
    private LocalDateTime createdAt;
}
