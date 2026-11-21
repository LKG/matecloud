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
package vip.mate.crm.customer.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.domain.adapter.repository.IContactRepository;
import vip.mate.crm.customer.domain.adapter.repository.ICustomerRepository;
import vip.mate.crm.customer.domain.adapter.repository.IFollowUpRepository;
import vip.mate.crm.customer.domain.model.aggregate.ContactAggregate;
import vip.mate.crm.customer.domain.model.aggregate.CustomerAggregate;
import vip.mate.crm.customer.domain.model.aggregate.FollowUpAggregate;
import vip.mate.crm.customer.domain.model.entity.Contact;
import vip.mate.crm.customer.domain.model.entity.Customer;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.crm.customer.domain.model.valobj.CustomerQuery;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.util.List;

/** 客户读侧实现: 公海/私海分页 + 客户 360 聚合。 */
@Service
@RequiredArgsConstructor
public class CustomerQueryService implements ICustomerQueryService {

    private final ICustomerRepository customerRepository;
    private final IContactRepository contactRepository;
    private final IFollowUpRepository followUpRepository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    public PageResult<Customer> page(String keyword, String industry, String level, String status,
                                     Integer poolStatus, Boolean mineOnly, int pageNum, int pageSize) {
        CrmScope scope = currentUserResolver.scope();
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        CustomerQuery query = new CustomerQuery(keyword, industry, level, status, poolStatus, ownerId,
                scope, pageNum, pageSize);
        PageResult<CustomerAggregate> result = customerRepository.page(query);
        List<Customer> list = result.getList().stream().map(CustomerAggregate::getCustomer).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public Customer findById(String id) {
        CustomerAggregate agg = customerRepository.findById(id);
        return agg == null ? null : agg.getCustomer();
    }

    @Override
    public CustomerDetail detail(String id) {
        CustomerAggregate agg = customerRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<Contact> contacts = contactRepository.listByCustomerId(id).stream()
                .map(ContactAggregate::getContact).toList();
        List<FollowUp> followUps = followUpRepository.listByCustomerId(id).stream()
                .map(FollowUpAggregate::getFollowUp).toList();
        return new CustomerDetail(agg.getCustomer(), contacts, followUps);
    }
}
