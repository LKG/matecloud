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
package vip.mate.crm.trade.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.adapter.repository.IInvoiceRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.aggregate.InvoiceAggregate;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.time.LocalDate;

/**
 * 发票写侧应用服务: 开票申请 / 开票 / 驳回 / 作废编排。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class InvoiceCommandService {

    private final IInvoiceRepository invoiceRepository;
    private final IContractRepository contractRepository;
    private final CurrentUserResolver currentUserResolver;

    @Transactional
    public String apply(InvoiceCommand cmd) {
        CrmCurrentUser operator = currentUserResolver.require();
        ContractAggregate contract = contractRepository.findById(cmd.getContractId());
        if (contract == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        InvoiceAggregate agg = InvoiceAggregate.apply(
                contract.getId(), contract.getContract().getContractNum(), contract.getContract().getCustomerId(),
                cmd.getInvoiceType(), cmd.getTitle(), cmd.getTaxNo(), cmd.getBankName(), cmd.getBankAccount(),
                cmd.getAddressInfo(), cmd.getMoney(), cmd.getTaxRate(), cmd.getRemark(), operator);
        invoiceRepository.save(agg);
        return agg.getId();
    }

    @Transactional
    public void issue(String id, String invoiceNo, LocalDate invoiceDate) {
        InvoiceAggregate agg = findOrThrow(id);
        agg.issue(invoiceNo, invoiceDate);
        invoiceRepository.update(agg);
    }

    @Transactional
    public void reject(String id) {
        InvoiceAggregate agg = findOrThrow(id);
        agg.reject();
        invoiceRepository.update(agg);
    }

    @Transactional
    public void voidInvoice(String id) {
        InvoiceAggregate agg = findOrThrow(id);
        agg.voidInvoice();
        invoiceRepository.update(agg);
    }

    @Transactional
    public void delete(String id) {
        findOrThrow(id);
        invoiceRepository.deleteById(id);
    }

    private InvoiceAggregate findOrThrow(String id) {
        InvoiceAggregate agg = invoiceRepository.findById(id);
        if (agg == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return agg;
    }
}
