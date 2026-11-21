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
package vip.mate.crm.customer.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.customer.domain.adapter.repository.ICustomerRepository;
import vip.mate.crm.customer.domain.model.aggregate.CustomerAggregate;
import vip.mate.crm.customer.domain.model.entity.Customer;
import vip.mate.crm.customer.domain.model.valobj.CustomerQuery;
import vip.mate.crm.customer.infrastructure.dao.CustomerDao;
import vip.mate.crm.customer.infrastructure.dao.po.CustomerPO;

import java.time.LocalDateTime;
import java.util.List;

/** 客户仓储实现: PO<->聚合转换 + 公海/私海查询 + 查重 + 回收扫描。 */
@Repository
@RequiredArgsConstructor
public class CustomerRepositoryImpl implements ICustomerRepository {

    private final CustomerDao customerDao;

    @Override
    public void save(CustomerAggregate aggregate) {
        customerDao.insert(toPO(aggregate.getCustomer()));
    }

    @Override
    public void update(CustomerAggregate aggregate) {
        customerDao.updateById(toPO(aggregate.getCustomer()));
    }

    @Override
    public CustomerAggregate findById(String id) {
        CustomerPO po = customerDao.selectById(id);
        return po == null ? null : CustomerAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        customerDao.deleteById(id);
    }

    @Override
    public PageResult<CustomerAggregate> page(CustomerQuery query) {
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<>();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String kw = query.keyword().trim();
            wrapper.and(w -> w.like(CustomerPO::getCustomerName, kw)
                    .or().like(CustomerPO::getPhone, kw));
        }
        wrapper.eq(notBlank(query.industry()), CustomerPO::getIndustry, query.industry())
                .eq(notBlank(query.level()), CustomerPO::getLevel, query.level())
                .eq(notBlank(query.status()), CustomerPO::getStatus, query.status())
                .eq(query.poolStatus() != null, CustomerPO::getPoolStatus, query.poolStatus())
                .eq(notBlank(query.ownerId()), CustomerPO::getOwnerId, query.ownerId());
        // 公海(pool_status=1)对所有可见, 不加数据权限; 私海按 owner/dept 范围过滤。
        boolean publicPool = query.poolStatus() != null && query.poolStatus() == CustomerAggregate.POOL_PUBLIC;
        if (!publicPool) {
            (query.scope() == null ? CrmScope.all() : query.scope())
                    .apply(wrapper, CustomerPO::getOwnerId, CustomerPO::getDeptId);
        }
        wrapper.orderByDesc(CustomerPO::getUpdatedAt);

        Page<CustomerPO> result = customerDao.selectPage(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        List<CustomerAggregate> list = result.getRecords().stream()
                .map(po -> CustomerAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public boolean existsByName(String customerName, String excludeId) {
        if (!notBlank(customerName)) {
            return false;
        }
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerPO::getCustomerName, customerName.trim())
                .ne(excludeId != null, CustomerPO::getId, excludeId);
        return customerDao.selectCount(wrapper) > 0;
    }

    @Override
    public boolean existsByPhone(String phone, String excludeId) {
        if (!notBlank(phone)) {
            return false;
        }
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerPO::getPhone, phone.trim())
                .ne(excludeId != null, CustomerPO::getId, excludeId);
        return customerDao.selectCount(wrapper) > 0;
    }

    @Override
    public long countPrivateByOwner(String ownerId) {
        if (!notBlank(ownerId)) {
            return 0;
        }
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerPO::getOwnerId, ownerId)
                .eq(CustomerPO::getPoolStatus, CustomerAggregate.POOL_PRIVATE);
        return customerDao.selectCount(wrapper);
    }

    @Override
    public List<CustomerAggregate> findRecyclable(LocalDateTime cutoff, int limit) {
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerPO::getPoolStatus, CustomerAggregate.POOL_PRIVATE)
                .and(w -> w.isNull(CustomerPO::getRecentFollowTime).lt(CustomerPO::getCreatedAt, cutoff)
                        .or(o -> o.isNotNull(CustomerPO::getRecentFollowTime).lt(CustomerPO::getRecentFollowTime, cutoff)))
                .orderByAsc(CustomerPO::getRecentFollowTime)
                .last("limit " + limit);
        return customerDao.selectList(wrapper).stream()
                .map(po -> CustomerAggregate.of(toEntity(po)))
                .toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private CustomerPO toPO(Customer c) {
        CustomerPO po = new CustomerPO();
        po.setId(c.getId());
        po.setCustomerName(c.getCustomerName());
        po.setIndustry(c.getIndustry());
        po.setLevel(c.getLevel());
        po.setSource(c.getSource());
        po.setStatus(c.getStatus());
        po.setPhone(c.getPhone());
        po.setAddress(c.getAddress());
        po.setWebsite(c.getWebsite());
        po.setRemark(c.getRemark());
        po.setPoolStatus(c.getPoolStatus());
        po.setOwnerId(c.getOwnerId());
        po.setOwnerName(c.getOwnerName());
        po.setDeptId(c.getDeptId());
        po.setClaimTime(c.getClaimTime());
        po.setToPoolTime(c.getToPoolTime());
        po.setToPoolReason(c.getToPoolReason());
        po.setRecentFollowTime(c.getRecentFollowTime());
        po.setNextTime(c.getNextTime());
        po.setCreatorId(c.getCreatorId());
        po.setCreatorName(c.getCreatorName());
        return po;
    }

    private Customer toEntity(CustomerPO po) {
        return Customer.builder()
                .id(po.getId())
                .customerName(po.getCustomerName())
                .industry(po.getIndustry())
                .level(po.getLevel())
                .source(po.getSource())
                .status(po.getStatus())
                .phone(po.getPhone())
                .address(po.getAddress())
                .website(po.getWebsite())
                .remark(po.getRemark())
                .poolStatus(po.getPoolStatus())
                .ownerId(po.getOwnerId())
                .ownerName(po.getOwnerName())
                .deptId(po.getDeptId())
                .claimTime(po.getClaimTime())
                .toPoolTime(po.getToPoolTime())
                .toPoolReason(po.getToPoolReason())
                .recentFollowTime(po.getRecentFollowTime())
                .nextTime(po.getNextTime())
                .creatorId(po.getCreatorId())
                .creatorName(po.getCreatorName())
                .lockVersion(po.getLockVersion())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
