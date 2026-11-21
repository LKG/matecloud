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
import vip.mate.crm.customer.domain.model.entity.Contact;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

/**
 * 联系人聚合根: 必须归属有效客户; 是否主联系人由应用层在同一客户内保证唯一。
 *
 * @author mateaix
 */
@Getter
@Builder
public class ContactAggregate {

    private final Contact contact;

    public static ContactAggregate create(String customerId, String customerName, String contactName,
                                          String mobile, String telephone, String email, String position,
                                          String gender, Integer isPrimary, String wechat, String remark,
                                          CrmCurrentUser operator) {
        if (customerId == null || customerId.isBlank()) {
            throw new BizException(CrmErrorCode.CONTACT_CUSTOMER_REQUIRED);
        }
        requireText(contactName, "联系人姓名不能为空");
        Contact contact = Contact.builder()
                .customerId(customerId)
                .customerName(customerName)
                .contactName(contactName)
                .mobile(mobile)
                .telephone(telephone)
                .email(email)
                .position(position)
                .gender(gender)
                .isPrimary(isPrimary == null ? 0 : isPrimary)
                .wechat(wechat)
                .remark(remark)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        contact.setId(SnowflakeUtil.newSnowflakeId());
        return ContactAggregate.builder().contact(contact).build();
    }

    public static ContactAggregate of(Contact contact) {
        if (contact == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return ContactAggregate.builder().contact(contact).build();
    }

    public void updateInfo(String contactName, String mobile, String telephone, String email,
                           String position, String gender, Integer isPrimary, String wechat, String remark) {
        requireText(contactName, "联系人姓名不能为空");
        contact.setContactName(contactName);
        contact.setMobile(mobile);
        contact.setTelephone(telephone);
        contact.setEmail(email);
        contact.setPosition(position);
        contact.setGender(gender);
        if (isPrimary != null) {
            contact.setIsPrimary(isPrimary);
        }
        contact.setWechat(wechat);
        contact.setRemark(remark);
    }

    public void markPrimary(int primary) {
        contact.setIsPrimary(primary);
    }

    public String getId() {
        return contact.getId();
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
