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
package vip.mate.crm.trade.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;
import vip.mate.crm.trade.domain.adapter.repository.IInvoiceRepository;
import vip.mate.crm.trade.domain.model.aggregate.InvoiceAggregate;
import vip.mate.crm.trade.domain.model.entity.Invoice;
import vip.mate.crm.trade.infrastructure.dao.InvoiceDao;
import vip.mate.crm.trade.infrastructure.dao.po.InvoicePO;

import java.util.List;

/** 发票仓储实现。 */
@Repository
@RequiredArgsConstructor
public class InvoiceRepositoryImpl implements IInvoiceRepository {

    private final InvoiceDao invoiceDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(InvoiceAggregate aggregate) {
        invoiceDao.insert(toPO(aggregate.getInvoice()));
    }

    @Override
    public void update(InvoiceAggregate aggregate) {
        invoiceDao.updateById(toPO(aggregate.getInvoice()));
    }

    @Override
    public InvoiceAggregate findById(String id) {
        InvoicePO po = invoiceDao.selectById(id);
        return po == null ? null : InvoiceAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        invoiceDao.deleteById(id);
    }

    @Override
    public PageResult<Invoice> page(String keyword, String contractId, Integer status,
                                    String ownerId, CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<InvoicePO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(InvoicePO::getTitle, kw)
                    .or().like(InvoicePO::getContractNum, kw)
                    .or().like(InvoicePO::getInvoiceNo, kw));
        }
        wrapper.eq(notBlank(contractId), InvoicePO::getContractId, contractId)
                .eq(status != null, InvoicePO::getStatus, status)
                .eq(notBlank(ownerId), InvoicePO::getOwnerId, ownerId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, InvoicePO::getOwnerId, InvoicePO::getDeptId);
        wrapper.orderByDesc(InvoicePO::getUpdatedAt);

        Page<InvoicePO> result = invoiceDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Invoice> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<Invoice> search(AdvancedQuery query, CrmScope scope) {
        Page<InvoicePO> result = crmSearchSupport.search(invoiceDao, "invoice", query, scope, "owner_id", "dept_id", "updated_at");
        List<Invoice> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<Invoice> listByContract(String contractId) {
        LambdaQueryWrapper<InvoicePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(InvoicePO::getContractId, contractId).orderByDesc(InvoicePO::getUpdatedAt);
        return invoiceDao.selectList(wrapper).stream().map(this::toEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private InvoicePO toPO(Invoice i) {
        InvoicePO po = new InvoicePO();
        po.setId(i.getId());
        po.setContractId(i.getContractId());
        po.setContractNum(i.getContractNum());
        po.setCustomerId(i.getCustomerId());
        po.setInvoiceType(i.getInvoiceType());
        po.setTitle(i.getTitle());
        po.setTaxNo(i.getTaxNo());
        po.setBankName(i.getBankName());
        po.setBankAccount(i.getBankAccount());
        po.setAddressInfo(i.getAddressInfo());
        po.setMoney(i.getMoney());
        po.setTaxRate(i.getTaxRate());
        po.setTaxMoney(i.getTaxMoney());
        po.setStatus(i.getStatus());
        po.setInvoiceNo(i.getInvoiceNo());
        po.setInvoiceDate(i.getInvoiceDate());
        po.setRemark(i.getRemark());
        po.setOwnerId(i.getOwnerId());
        po.setOwnerName(i.getOwnerName());
        po.setDeptId(i.getDeptId());
        po.setCreatorId(i.getCreatorId());
        po.setCreatorName(i.getCreatorName());
        return po;
    }

    private Invoice toEntity(InvoicePO po) {
        return Invoice.builder()
                .id(po.getId())
                .contractId(po.getContractId())
                .contractNum(po.getContractNum())
                .customerId(po.getCustomerId())
                .invoiceType(po.getInvoiceType())
                .title(po.getTitle())
                .taxNo(po.getTaxNo())
                .bankName(po.getBankName())
                .bankAccount(po.getBankAccount())
                .addressInfo(po.getAddressInfo())
                .money(po.getMoney())
                .taxRate(po.getTaxRate())
                .taxMoney(po.getTaxMoney())
                .status(po.getStatus())
                .invoiceNo(po.getInvoiceNo())
                .invoiceDate(po.getInvoiceDate())
                .remark(po.getRemark())
                .ownerId(po.getOwnerId())
                .ownerName(po.getOwnerName())
                .deptId(po.getDeptId())
                .creatorId(po.getCreatorId())
                .creatorName(po.getCreatorName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
