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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;
import vip.mate.crm.trade.domain.adapter.repository.IContractRepository;
import vip.mate.crm.trade.domain.model.aggregate.ContractAggregate;
import vip.mate.crm.trade.domain.model.entity.Contract;
import vip.mate.crm.trade.domain.model.entity.ContractProduct;
import vip.mate.crm.trade.domain.model.valobj.ContractQuery;
import vip.mate.crm.trade.infrastructure.dao.ContractDao;
import vip.mate.crm.trade.infrastructure.dao.ContractProductDao;
import vip.mate.crm.trade.infrastructure.dao.po.ContractPO;
import vip.mate.crm.trade.infrastructure.dao.po.ContractProductPO;
import vip.mate.crm.common.CrmScope;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 合同仓储实现: 主表 + 产品行(更新时明细行差量同步)。 */
@Repository
@RequiredArgsConstructor
public class ContractRepositoryImpl implements IContractRepository {

    private final ContractDao contractDao;
    private final ContractProductDao contractProductDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(ContractAggregate aggregate) {
        contractDao.insert(toPO(aggregate.getContract()));
        saveProducts(aggregate.getProducts());
    }

    @Override
    public void update(ContractAggregate aggregate) {
        contractDao.updateById(toPO(aggregate.getContract()));
        // 明细行差量同步: @TableLogic 下删后重插会与软删残留行主键冲突
        List<ContractProduct> existingProducts = loadProducts(aggregate.getId());
        Set<String> existing = existingProducts.stream().map(ContractProduct::getId).collect(Collectors.toSet());
        Set<String> keep = aggregate.getProducts() == null ? Set.of()
                : aggregate.getProducts().stream().map(ContractProduct::getId).collect(Collectors.toSet());
        for (ContractProduct p : existingProducts) {
            if (!keep.contains(p.getId())) {
                contractProductDao.deleteById(p.getId());
            }
        }
        if (aggregate.getProducts() == null) {
            return;
        }
        for (ContractProduct p : aggregate.getProducts()) {
            if (existing.contains(p.getId())) {
                contractProductDao.updateById(toProductPO(p));
            } else {
                contractProductDao.insert(toProductPO(p));
            }
        }
    }

    @Override
    public ContractAggregate findById(String id) {
        ContractPO po = contractDao.selectById(id);
        if (po == null) {
            return null;
        }
        return ContractAggregate.of(toEntity(po), loadProducts(id));
    }

    @Override
    public void deleteById(String id) {
        contractDao.deleteById(id);
        LambdaQueryWrapper<ContractProductPO> del = new LambdaQueryWrapper<>();
        del.eq(ContractProductPO::getContractId, id);
        contractProductDao.delete(del);
    }

    @Override
    public PageResult<ContractAggregate> page(ContractQuery query) {
        LambdaQueryWrapper<ContractPO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(query.keyword())) {
            String kw = query.keyword().trim();
            wrapper.and(w -> w.like(ContractPO::getContractName, kw)
                    .or().like(ContractPO::getContractNum, kw)
                    .or().like(ContractPO::getCustomerName, kw));
        }
        wrapper.eq(notBlank(query.status()), ContractPO::getStatus, query.status())
                .eq(notBlank(query.customerId()), ContractPO::getCustomerId, query.customerId())
                .eq(notBlank(query.ownerId()), ContractPO::getOwnerId, query.ownerId());
        (query.scope() == null ? CrmScope.all() : query.scope())
                .apply(wrapper, ContractPO::getOwnerId, ContractPO::getDeptId);
        wrapper.orderByDesc(ContractPO::getUpdatedAt);

        Page<ContractPO> result = contractDao.selectPage(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        List<ContractAggregate> list = result.getRecords().stream()
                .map(po -> ContractAggregate.of(toEntity(po), null))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<ContractAggregate> search(AdvancedQuery query, CrmScope scope) {
        Page<ContractPO> result = crmSearchSupport.search(contractDao, "contract", query, scope, "owner_id", "dept_id", "updated_at");
        List<ContractAggregate> list = result.getRecords().stream()
                .map(po -> ContractAggregate.of(toEntity(po), null))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public boolean existsByNum(String contractNum, String excludeId) {
        if (!notBlank(contractNum)) {
            return false;
        }
        LambdaQueryWrapper<ContractPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractPO::getContractNum, contractNum)
                .ne(excludeId != null, ContractPO::getId, excludeId);
        return contractDao.selectCount(wrapper) > 0;
    }

    @Override
    public List<Contract> listByCustomerId(String customerId) {
        LambdaQueryWrapper<ContractPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractPO::getCustomerId, customerId).orderByDesc(ContractPO::getUpdatedAt);
        return contractDao.selectList(wrapper).stream().map(this::toEntity).toList();
    }

    @Override
    public void updateReceivedMoney(String id, BigDecimal receivedMoney) {
        LambdaUpdateWrapper<ContractPO> update = new LambdaUpdateWrapper<>();
        update.eq(ContractPO::getId, id).set(ContractPO::getReceivedMoney, receivedMoney);
        contractDao.update(null, update);
    }

    private void saveProducts(List<ContractProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (ContractProduct p : products) {
            contractProductDao.insert(toProductPO(p));
        }
    }

    private List<ContractProduct> loadProducts(String contractId) {
        LambdaQueryWrapper<ContractProductPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContractProductPO::getContractId, contractId);
        return contractProductDao.selectList(wrapper).stream().map(this::toProductEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private ContractPO toPO(Contract c) {
        ContractPO po = new ContractPO();
        po.setId(c.getId());
        po.setContractNum(c.getContractNum());
        po.setContractName(c.getContractName());
        po.setCustomerId(c.getCustomerId());
        po.setCustomerName(c.getCustomerName());
        po.setBusinessId(c.getBusinessId());
        po.setContactName(c.getContactName());
        po.setMoney(c.getMoney());
        po.setReceivedMoney(c.getReceivedMoney());
        po.setStartTime(c.getStartTime());
        po.setEndTime(c.getEndTime());
        po.setSignTime(c.getSignTime());
        po.setStatus(c.getStatus());
        po.setRemark(c.getRemark());
        po.setAttachment(c.getAttachment());
        po.setOwnerId(c.getOwnerId());
        po.setOwnerName(c.getOwnerName());
        po.setDeptId(c.getDeptId());
        po.setCreatorId(c.getCreatorId());
        po.setCreatorName(c.getCreatorName());
        return po;
    }

    private Contract toEntity(ContractPO po) {
        return Contract.builder()
                .id(po.getId())
                .contractNum(po.getContractNum())
                .contractName(po.getContractName())
                .customerId(po.getCustomerId())
                .customerName(po.getCustomerName())
                .businessId(po.getBusinessId())
                .contactName(po.getContactName())
                .money(po.getMoney())
                .receivedMoney(po.getReceivedMoney())
                .startTime(po.getStartTime())
                .endTime(po.getEndTime())
                .signTime(po.getSignTime())
                .status(po.getStatus())
                .remark(po.getRemark())
                .attachment(po.getAttachment())
                .ownerId(po.getOwnerId())
                .ownerName(po.getOwnerName())
                .deptId(po.getDeptId())
                .creatorId(po.getCreatorId())
                .creatorName(po.getCreatorName())
                .lockVersion(po.getLockVersion())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private ContractProductPO toProductPO(ContractProduct p) {
        ContractProductPO po = new ContractProductPO();
        po.setId(p.getId());
        po.setContractId(p.getContractId());
        po.setProductId(p.getProductId());
        po.setProductName(p.getProductName());
        po.setUnitPrice(p.getUnitPrice());
        po.setQuantity(p.getQuantity());
        po.setDiscount(p.getDiscount());
        po.setSubtotal(p.getSubtotal());
        return po;
    }

    private ContractProduct toProductEntity(ContractProductPO po) {
        return ContractProduct.builder()
                .id(po.getId())
                .contractId(po.getContractId())
                .productId(po.getProductId())
                .productName(po.getProductName())
                .unitPrice(po.getUnitPrice())
                .quantity(po.getQuantity())
                .discount(po.getDiscount())
                .subtotal(po.getSubtotal())
                .build();
    }
}
