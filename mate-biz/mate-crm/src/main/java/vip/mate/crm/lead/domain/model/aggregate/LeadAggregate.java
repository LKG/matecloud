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
package vip.mate.crm.lead.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.lead.domain.model.entity.Lead;
import vip.mate.crm.lead.types.enums.LeadStatus;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.time.LocalDateTime;

/**
 * 线索聚合根: 创建即 FOLLOWING; 转化/废弃为终态, 终态不可再变更;
 * 已转化线索必须记录 converted_customer_id。
 *
 * @author mateaix
 */
@Getter
@Builder
public class LeadAggregate {

    private final Lead lead;

    public static LeadAggregate create(String leadName, String contactName, String mobile,
                                       String telephone, String email, String industry,
                                       String source, String address, String remark,
                                       LocalDateTime nextTime, String ownerId, String ownerName,
                                       String deptId, String creatorId, String creatorName) {
        requireText(leadName, "线索名称不能为空");
        requireText(contactName, "联系人不能为空");
        Lead lead = Lead.builder()
                .leadName(leadName)
                .contactName(contactName)
                .mobile(mobile)
                .telephone(telephone)
                .email(email)
                .industry(industry)
                .source(source == null || source.isBlank() ? "OTHER" : source)
                .status(LeadStatus.FOLLOWING.name())
                .address(address)
                .remark(remark)
                .nextTime(nextTime)
                .ownerId(ownerId)
                .ownerName(ownerName)
                .deptId(deptId)
                .creatorId(creatorId)
                .creatorName(creatorName)
                .build();
        lead.setId(SnowflakeUtil.newSnowflakeId());
        return LeadAggregate.builder().lead(lead).build();
    }

    public static LeadAggregate of(Lead lead) {
        if (lead == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return LeadAggregate.builder().lead(lead).build();
    }

    /** 编辑基本信息 — 终态线索不可编辑。 */
    public void updateInfo(String leadName, String contactName, String mobile, String telephone,
                           String email, String industry, String source, String address,
                           String remark, LocalDateTime nextTime) {
        assertFollowing();
        requireText(leadName, "线索名称不能为空");
        requireText(contactName, "联系人不能为空");
        lead.setLeadName(leadName);
        lead.setContactName(contactName);
        lead.setMobile(mobile);
        lead.setTelephone(telephone);
        lead.setEmail(email);
        lead.setIndustry(industry);
        if (source != null && !source.isBlank()) {
            lead.setSource(source);
        }
        lead.setAddress(address);
        lead.setRemark(remark);
        lead.setNextTime(nextTime);
    }

    /** 跟进后更新下次跟进时间。 */
    public void touchFollow(LocalDateTime nextTime) {
        assertFollowing();
        if (nextTime != null) {
            lead.setNextTime(nextTime);
        }
    }

    /** 变更负责人(线索转移)。 */
    public void changeOwner(String ownerId, String ownerName, String deptId) {
        assertFollowing();
        requireText(ownerId, "负责人不能为空");
        lead.setOwnerId(ownerId);
        lead.setOwnerName(ownerName);
        lead.setDeptId(deptId);
    }

    /** 转化为客户 — 记录客户ID并进入终态。 */
    public void markConverted(String customerId) {
        assertFollowing();
        requireText(customerId, "转化目标客户不能为空");
        lead.setStatus(LeadStatus.CONVERTED.name());
        lead.setConvertedCustomerId(customerId);
    }

    /** 废弃线索。 */
    public void discard() {
        assertFollowing();
        lead.setStatus(LeadStatus.INVALID.name());
    }

    public String getId() {
        return lead.getId();
    }

    private void assertFollowing() {
        if (LeadStatus.CONVERTED.name().equals(lead.getStatus())) {
            throw new BizException(CrmErrorCode.LEAD_ALREADY_CONVERTED);
        }
        if (LeadStatus.INVALID.name().equals(lead.getStatus())) {
            throw new BizException(CrmErrorCode.LEAD_INVALID);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
