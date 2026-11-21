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
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.domain.adapter.repository.IContactRepository;
import vip.mate.crm.customer.domain.model.aggregate.ContactAggregate;
import vip.mate.crm.customer.domain.model.entity.Contact;
import vip.mate.crm.query.dto.AdvancedQuery;

import java.util.List;

/** 联系人读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class ContactQueryService {

    private final IContactRepository contactRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<Contact> page(String keyword, String customerId, int pageNum, int pageSize) {
        PageResult<ContactAggregate> result = contactRepository.page(
                keyword, customerId, currentUserResolver.scope(), pageNum, pageSize);
        List<Contact> list = result.getList().stream().map(ContactAggregate::getContact).toList();
        return PageResult.of(list, result.getTotal());
    }

    public Contact findById(String id) {
        ContactAggregate agg = contactRepository.findById(id);
        return agg == null ? null : agg.getContact();
    }

    public PageResult<Contact> search(AdvancedQuery query) {
        PageResult<ContactAggregate> result = contactRepository.search(query, currentUserResolver.scope());
        List<Contact> list = result.getList().stream().map(ContactAggregate::getContact).toList();
        return PageResult.of(list, result.getTotal());
    }
}
