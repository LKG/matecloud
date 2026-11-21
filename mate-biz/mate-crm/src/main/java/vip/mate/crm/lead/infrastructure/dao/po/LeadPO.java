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
package vip.mate.crm.lead.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 线索持久化对象(mate_crm_lead)。 */
@Data
@TableName("mate_crm_lead")
public class LeadPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** 租户列, 依赖 DB 默认值 '1' */
    private String tenantId;

    private String leadName;
    private String contactName;
    private String mobile;
    private String telephone;
    private String email;
    private String industry;

    /** SEARCH/OFFICIAL_WEBSITE/EXHIBITION/REFERRAL/PHONE/OTHER */
    private String source;

    /** FOLLOWING/CONVERTED/INVALID */
    private String status;

    private String address;
    private String remark;
    private LocalDateTime nextTime;
    private String convertedCustomerId;

    /** 数据权限过滤列 */
    private String ownerId;
    private String ownerName;
    private String deptId;

    private String creatorId;
    private String creatorName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
