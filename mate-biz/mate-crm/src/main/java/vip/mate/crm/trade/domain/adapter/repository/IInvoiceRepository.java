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
package vip.mate.crm.trade.domain.adapter.repository;

import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.trade.domain.model.aggregate.InvoiceAggregate;
import vip.mate.crm.trade.domain.model.entity.Invoice;

import java.util.List;

/**
 * 发票仓储接口。
 *
 * @author mateaix
 */
public interface IInvoiceRepository {

    void save(InvoiceAggregate aggregate);

    void update(InvoiceAggregate aggregate);

    InvoiceAggregate findById(String id);

    void deleteById(String id);

    PageResult<Invoice> page(String keyword, String contractId, Integer status,
                             String ownerId, CrmScope scope, int pageNum, int pageSize);

    List<Invoice> listByContract(String contractId);
}
