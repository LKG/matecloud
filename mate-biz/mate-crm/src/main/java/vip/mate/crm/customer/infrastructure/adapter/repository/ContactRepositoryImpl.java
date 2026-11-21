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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.customer.domain.adapter.repository.IContactRepository;
import vip.mate.crm.customer.domain.model.aggregate.ContactAggregate;
import vip.mate.crm.customer.domain.model.entity.Contact;
import vip.mate.crm.customer.infrastructure.dao.ContactDao;
import vip.mate.crm.customer.infrastructure.dao.po.ContactPO;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;

import java.util.List;

/** 联系人仓储实现。 */
@Repository
@RequiredArgsConstructor
public class ContactRepositoryImpl implements IContactRepository {

    private final ContactDao contactDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(ContactAggregate aggregate) {
        contactDao.insert(toPO(aggregate.getContact()));
    }

    @Override
    public void update(ContactAggregate aggregate) {
        contactDao.updateById(toPO(aggregate.getContact()));
    }

    @Override
    public ContactAggregate findById(String id) {
        ContactPO po = contactDao.selectById(id);
        return po == null ? null : ContactAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        contactDao.deleteById(id);
    }

    @Override
    public PageResult<ContactAggregate> page(String keyword, String customerId, CrmScope scope,
                                             int pageNum, int pageSize) {
        LambdaQueryWrapper<ContactPO> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(ContactPO::getContactName, kw)
                    .or().like(ContactPO::getMobile, kw));
        }
        wrapper.eq(customerId != null && !customerId.isBlank(), ContactPO::getCustomerId, customerId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, ContactPO::getOwnerId, ContactPO::getDeptId);
        wrapper.orderByDesc(ContactPO::getIsPrimary).orderByDesc(ContactPO::getCreatedAt);

        Page<ContactPO> result = contactDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ContactAggregate> list = result.getRecords().stream()
                .map(po -> ContactAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<ContactAggregate> search(AdvancedQuery query, CrmScope scope) {
        Page<ContactPO> result = crmSearchSupport.search(contactDao, "contact", query, scope, "owner_id", "dept_id");
        List<ContactAggregate> list = result.getRecords().stream()
                .map(po -> ContactAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<ContactAggregate> listByCustomerId(String customerId) {
        LambdaQueryWrapper<ContactPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContactPO::getCustomerId, customerId)
                .orderByDesc(ContactPO::getIsPrimary).orderByDesc(ContactPO::getCreatedAt);
        return contactDao.selectList(wrapper).stream()
                .map(po -> ContactAggregate.of(toEntity(po)))
                .toList();
    }

    @Override
    public void clearPrimary(String customerId) {
        LambdaUpdateWrapper<ContactPO> update = new LambdaUpdateWrapper<>();
        update.eq(ContactPO::getCustomerId, customerId)
                .eq(ContactPO::getIsPrimary, 1)
                .set(ContactPO::getIsPrimary, 0);
        contactDao.update(null, update);
    }

    private ContactPO toPO(Contact c) {
        ContactPO po = new ContactPO();
        po.setId(c.getId());
        po.setCustomerId(c.getCustomerId());
        po.setCustomerName(c.getCustomerName());
        po.setContactName(c.getContactName());
        po.setMobile(c.getMobile());
        po.setTelephone(c.getTelephone());
        po.setEmail(c.getEmail());
        po.setPosition(c.getPosition());
        po.setGender(c.getGender());
        po.setIsPrimary(c.getIsPrimary());
        po.setWechat(c.getWechat());
        po.setRemark(c.getRemark());
        po.setOwnerId(c.getOwnerId());
        po.setOwnerName(c.getOwnerName());
        po.setDeptId(c.getDeptId());
        po.setCreatorId(c.getCreatorId());
        po.setCreatorName(c.getCreatorName());
        return po;
    }

    private Contact toEntity(ContactPO po) {
        return Contact.builder()
                .id(po.getId())
                .customerId(po.getCustomerId())
                .customerName(po.getCustomerName())
                .contactName(po.getContactName())
                .mobile(po.getMobile())
                .telephone(po.getTelephone())
                .email(po.getEmail())
                .position(po.getPosition())
                .gender(po.getGender())
                .isPrimary(po.getIsPrimary())
                .wechat(po.getWechat())
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
