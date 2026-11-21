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
package vip.mate.crm.customer.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.domain.adapter.repository.IContactRepository;
import vip.mate.crm.customer.domain.model.aggregate.ContactAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

/**
 * 联系人写侧应用服务: 主联系人唯一性编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class ContactCommandService {

    private final IContactRepository contactRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String create(ContactCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        ContactAggregate agg = ContactAggregate.create(
                cmd.getCustomerId(), cmd.getCustomerName(), cmd.getContactName(), cmd.getMobile(),
                cmd.getTelephone(), cmd.getEmail(), cmd.getPosition(), cmd.getGender(), cmd.getIsPrimary(),
                cmd.getWechat(), cmd.getRemark(), operator);
        if (Integer.valueOf(1).equals(cmd.getIsPrimary())) {
            contactRepository.clearPrimary(cmd.getCustomerId());
        }
        contactRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void update(String id, ContactCommand cmd) {
        ContactAggregate agg = findOrThrow(id);
        if (Integer.valueOf(1).equals(cmd.getIsPrimary())) {
            contactRepository.clearPrimary(agg.getContact().getCustomerId());
        }
        agg.updateInfo(cmd.getContactName(), cmd.getMobile(), cmd.getTelephone(), cmd.getEmail(),
                cmd.getPosition(), cmd.getGender(), cmd.getIsPrimary(), cmd.getWechat(), cmd.getRemark());
        contactRepository.update(agg);
    }

    /** 设为主联系人: 清空同客户其它主标记后置本条为 1。 */
    @Transactional
    public void setPrimary(String id) {
        ContactAggregate agg = findOrThrow(id);
        contactRepository.clearPrimary(agg.getContact().getCustomerId());
        agg.markPrimary(1);
        contactRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        contactRepository.deleteById(id);
    }

    private ContactAggregate findOrThrow(String id) {
        ContactAggregate agg = contactRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
