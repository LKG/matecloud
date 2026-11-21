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
package vip.mate.crm.trade.domain.model.aggregate;

import lombok.Builder;
import lombok.Getter;
import vip.mate.base.exception.BizException;
import vip.mate.base.response.ResponseCode;
import vip.mate.crm.common.CrmCurrentUser;
import vip.mate.crm.trade.domain.model.entity.Invoice;
import vip.mate.crm.trade.types.enums.InvoiceStatus;
import vip.mate.crm.types.exception.CrmErrorCode;
import vip.mate.starter.distribute.util.SnowflakeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 发票聚合根: 已申请 → 已开票/已驳回; 已开票/已申请可作废。
 *
 * <p>税额不变式: taxMoney = money * taxRate(申请时快照)。开票动作补录发票号与开票日期。
 *
 * @author mateaix
 */
@Getter
@Builder
public class InvoiceAggregate {

    private final Invoice invoice;

    public static InvoiceAggregate apply(String contractId, String contractNum, String customerId,
                                         String invoiceType, String title, String taxNo, String bankName,
                                         String bankAccount, String addressInfo, BigDecimal money,
                                         BigDecimal taxRate, String remark, CrmCurrentUser operator) {
        if (contractId == null || contractId.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "开票申请必须关联合同");
        }
        requireText(title, "发票抬头不能为空");
        if (money == null || money.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), "开票金额必须大于 0");
        }
        BigDecimal rate = taxRate == null ? new BigDecimal("0.06") : taxRate;
        Invoice invoice = Invoice.builder()
                .contractId(contractId)
                .contractNum(contractNum)
                .customerId(customerId)
                .invoiceType(invoiceType == null || invoiceType.isBlank() ? "GENERAL" : invoiceType)
                .title(title)
                .taxNo(taxNo)
                .bankName(bankName)
                .bankAccount(bankAccount)
                .addressInfo(addressInfo)
                .money(money)
                .taxRate(rate)
                .taxMoney(money.multiply(rate).setScale(2, RoundingMode.HALF_UP))
                .status(InvoiceStatus.APPLIED.code())
                .remark(remark)
                .ownerId(operator.userId())
                .ownerName(operator.displayName())
                .deptId(operator.deptId())
                .creatorId(operator.userId())
                .creatorName(operator.displayName())
                .build();
        invoice.setId(SnowflakeUtil.newSnowflakeId());
        return InvoiceAggregate.builder().invoice(invoice).build();
    }

    public static InvoiceAggregate of(Invoice invoice) {
        if (invoice == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        return InvoiceAggregate.builder().invoice(invoice).build();
    }

    /** 开票: 已申请 → 已开票, 补录发票号/日期。 */
    public void issue(String invoiceNo, LocalDate invoiceDate) {
        assertProcessable();
        invoice.setStatus(InvoiceStatus.ISSUED.code());
        invoice.setInvoiceNo(invoiceNo);
        invoice.setInvoiceDate(invoiceDate == null ? LocalDate.now() : invoiceDate);
    }

    /** 驳回: 已申请 → 已驳回。 */
    public void reject() {
        assertProcessable();
        invoice.setStatus(InvoiceStatus.REJECTED.code());
    }

    /** 作废: 非已作废/已驳回的发票均可作废。 */
    public void voidInvoice() {
        InvoiceStatus current = status();
        if (current == InvoiceStatus.VOID || current == InvoiceStatus.REJECTED) {
            throw new BizException(CrmErrorCode.INVOICE_LOCKED);
        }
        invoice.setStatus(InvoiceStatus.VOID.code());
    }

    public String getId() {
        return invoice.getId();
    }

    private InvoiceStatus status() {
        return InvoiceStatus.of(invoice.getStatus() == null ? 0 : invoice.getStatus());
    }

    private void assertProcessable() {
        if (!status().canProcess()) {
            throw new BizException(CrmErrorCode.INVOICE_LOCKED);
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BizException(ResponseCode.PARAM_VALID_ERROR.getCode(), message);
        }
    }
}
