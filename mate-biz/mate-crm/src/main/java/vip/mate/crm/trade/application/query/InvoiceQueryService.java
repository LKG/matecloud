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
package vip.mate.crm.trade.application.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IInvoiceRepository;
import vip.mate.crm.trade.domain.model.aggregate.InvoiceAggregate;
import vip.mate.crm.trade.domain.model.entity.Invoice;

import java.util.List;

/** 发票读侧查询服务。 */
@Service
@RequiredArgsConstructor
public class InvoiceQueryService {

    private final IInvoiceRepository invoiceRepository;
    private final CurrentUserResolver currentUserResolver;

    public PageResult<Invoice> page(String keyword, String contractId, Integer status,
                                    Boolean mineOnly, int pageNum, int pageSize) {
        String ownerId = Boolean.TRUE.equals(mineOnly) ? currentUserResolver.require().userId() : null;
        return invoiceRepository.page(keyword, contractId, status, ownerId,
                currentUserResolver.scope(), pageNum, pageSize);
    }

    public Invoice findById(String id) {
        InvoiceAggregate agg = invoiceRepository.findById(id);
        return agg == null ? null : agg.getInvoice();
    }

    public List<Invoice> listByContract(String contractId) {
        return invoiceRepository.listByContract(contractId);
    }
}
