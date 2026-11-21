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
package vip.mate.crm.business.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.business.domain.adapter.repository.IBusinessRepository;
import vip.mate.crm.business.domain.model.aggregate.BusinessAggregate;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.business.domain.model.entity.BusinessProduct;
import vip.mate.crm.business.domain.model.valobj.BusinessQuery;
import vip.mate.crm.business.infrastructure.dao.BusinessDao;
import vip.mate.crm.business.infrastructure.dao.BusinessProductDao;
import vip.mate.crm.business.infrastructure.dao.po.BusinessPO;
import vip.mate.crm.business.infrastructure.dao.po.BusinessProductPO;
import vip.mate.crm.business.types.enums.BusinessStage;
import vip.mate.crm.common.CrmScope;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 商机仓储实现: 主表 + 产品行(更新时明细行差量同步)。 */
@Repository
@RequiredArgsConstructor
public class BusinessRepositoryImpl implements IBusinessRepository {

    private final BusinessDao businessDao;
    private final BusinessProductDao businessProductDao;

    @Override
    public void save(BusinessAggregate aggregate) {
        businessDao.insert(toPO(aggregate.getBusiness()));
        saveProducts(aggregate.getProducts());
    }

    @Override
    public void update(BusinessAggregate aggregate) {
        businessDao.updateById(toPO(aggregate.getBusiness()));
        // 明细行差量同步: @TableLogic 下删后重插会与软删残留行主键冲突
        List<BusinessProduct> existingProducts = loadProducts(aggregate.getId());
        Set<String> existing = existingProducts.stream().map(BusinessProduct::getId).collect(Collectors.toSet());
        Set<String> keep = aggregate.getProducts() == null ? Set.of()
                : aggregate.getProducts().stream().map(BusinessProduct::getId).collect(Collectors.toSet());
        for (BusinessProduct p : existingProducts) {
            if (!keep.contains(p.getId())) {
                businessProductDao.deleteById(p.getId());
            }
        }
        if (aggregate.getProducts() == null) {
            return;
        }
        for (BusinessProduct p : aggregate.getProducts()) {
            if (existing.contains(p.getId())) {
                businessProductDao.updateById(toProductPO(p));
            } else {
                businessProductDao.insert(toProductPO(p));
            }
        }
    }

    @Override
    public BusinessAggregate findById(String id) {
        BusinessPO po = businessDao.selectById(id);
        if (po == null) {
            return null;
        }
        return BusinessAggregate.of(toEntity(po), loadProducts(id));
    }

    @Override
    public void deleteById(String id) {
        businessDao.deleteById(id);
        LambdaQueryWrapper<BusinessProductPO> del = new LambdaQueryWrapper<>();
        del.eq(BusinessProductPO::getBusinessId, id);
        businessProductDao.delete(del);
    }

    @Override
    public PageResult<BusinessAggregate> page(BusinessQuery query) {
        LambdaQueryWrapper<BusinessPO> wrapper = new LambdaQueryWrapper<>();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String kw = query.keyword().trim();
            wrapper.and(w -> w.like(BusinessPO::getBusinessName, kw)
                    .or().like(BusinessPO::getCustomerName, kw));
        }
        wrapper.eq(notBlank(query.stage()), BusinessPO::getStage, query.stage())
                .eq(notBlank(query.customerId()), BusinessPO::getCustomerId, query.customerId())
                .eq(query.statusFlag() != null, BusinessPO::getStatusFlag, query.statusFlag())
                .eq(notBlank(query.ownerId()), BusinessPO::getOwnerId, query.ownerId());
        (query.scope() == null ? CrmScope.all() : query.scope())
                .apply(wrapper, BusinessPO::getOwnerId, BusinessPO::getDeptId);
        wrapper.orderByDesc(BusinessPO::getUpdatedAt);

        Page<BusinessPO> result = businessDao.selectPage(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        List<BusinessAggregate> list = result.getRecords().stream()
                .map(po -> BusinessAggregate.of(toEntity(po), null))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<BusinessAggregate> listByCustomerId(String customerId) {
        LambdaQueryWrapper<BusinessPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessPO::getCustomerId, customerId).orderByDesc(BusinessPO::getUpdatedAt);
        return businessDao.selectList(wrapper).stream()
                .map(po -> BusinessAggregate.of(toEntity(po), null))
                .toList();
    }

    @Override
    public List<BusinessAggregate> findStale(int days, int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<BusinessPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessPO::getStatusFlag, 1)
                .ne(BusinessPO::getStage, BusinessStage.NEGOTIATION.name())
                .lt(BusinessPO::getUpdatedAt, cutoff)
                .orderByAsc(BusinessPO::getUpdatedAt)
                .last("limit " + limit);
        return businessDao.selectList(wrapper).stream()
                .map(po -> BusinessAggregate.of(toEntity(po), null))
                .toList();
    }

    private void saveProducts(List<BusinessProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        for (BusinessProduct p : products) {
            businessProductDao.insert(toProductPO(p));
        }
    }

    private List<BusinessProduct> loadProducts(String businessId) {
        LambdaQueryWrapper<BusinessProductPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessProductPO::getBusinessId, businessId);
        return businessProductDao.selectList(wrapper).stream().map(this::toProductEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private BusinessPO toPO(Business b) {
        BusinessPO po = new BusinessPO();
        po.setId(b.getId());
        po.setBusinessName(b.getBusinessName());
        po.setCustomerId(b.getCustomerId());
        po.setCustomerName(b.getCustomerName());
        po.setContactName(b.getContactName());
        po.setSource(b.getSource());
        po.setStage(b.getStage());
        po.setStatusFlag(b.getStatusFlag());
        po.setMoney(b.getMoney());
        po.setDealProb(b.getDealProb());
        po.setDealDate(b.getDealDate());
        po.setNextStep(b.getNextStep());
        po.setEndRemark(b.getEndRemark());
        po.setRemark(b.getRemark());
        po.setOwnerId(b.getOwnerId());
        po.setOwnerName(b.getOwnerName());
        po.setDeptId(b.getDeptId());
        po.setCreatorId(b.getCreatorId());
        po.setCreatorName(b.getCreatorName());
        return po;
    }

    private Business toEntity(BusinessPO po) {
        return Business.builder()
                .id(po.getId())
                .businessName(po.getBusinessName())
                .customerId(po.getCustomerId())
                .customerName(po.getCustomerName())
                .contactName(po.getContactName())
                .source(po.getSource())
                .stage(po.getStage())
                .statusFlag(po.getStatusFlag())
                .money(po.getMoney())
                .dealProb(po.getDealProb())
                .dealDate(po.getDealDate())
                .nextStep(po.getNextStep())
                .endRemark(po.getEndRemark())
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

    private BusinessProductPO toProductPO(BusinessProduct p) {
        BusinessProductPO po = new BusinessProductPO();
        po.setId(p.getId());
        po.setBusinessId(p.getBusinessId());
        po.setProductId(p.getProductId());
        po.setProductName(p.getProductName());
        po.setUnitPrice(p.getUnitPrice());
        po.setQuantity(p.getQuantity());
        po.setSubtotal(p.getSubtotal());
        return po;
    }

    private BusinessProduct toProductEntity(BusinessProductPO po) {
        return BusinessProduct.builder()
                .id(po.getId())
                .businessId(po.getBusinessId())
                .productId(po.getProductId())
                .productName(po.getProductName())
                .unitPrice(po.getUnitPrice())
                .quantity(po.getQuantity())
                .subtotal(po.getSubtotal())
                .build();
    }
}
