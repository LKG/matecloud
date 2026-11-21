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
package vip.mate.crm.customer.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.customer.domain.model.aggregate.ContactAggregate;

import java.util.List;

/**
 * 联系人仓储接口。
 *
 * @author mateaix
 */
public interface IContactRepository {

    void save(ContactAggregate aggregate);

    void update(ContactAggregate aggregate);

    ContactAggregate findById(String id);

    void deleteById(String id);

    PageResult<ContactAggregate> page(String keyword, String customerId, CrmScope scope, int pageNum, int pageSize);

    /** 某客户下全部联系人(客户 360 用)。 */
    List<ContactAggregate> listByCustomerId(String customerId);

    /** 清空某客户下所有主联系人标记(设新主前置操作)。 */
    void clearPrimary(String customerId);
}
