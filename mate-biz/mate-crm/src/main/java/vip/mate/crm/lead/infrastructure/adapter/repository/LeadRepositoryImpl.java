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
package vip.mate.crm.lead.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.lead.domain.adapter.repository.ILeadRepository;
import vip.mate.crm.lead.domain.model.aggregate.LeadAggregate;
import vip.mate.crm.lead.domain.model.entity.Lead;
import vip.mate.crm.lead.domain.model.valobj.LeadQuery;
import vip.mate.crm.lead.infrastructure.dao.LeadDao;
import vip.mate.crm.lead.infrastructure.dao.po.LeadPO;
import vip.mate.crm.lead.types.enums.LeadStatus;

import java.util.List;

/** 线索仓储实现: PO<->聚合转换 + wrapper 数据权限过滤。 */
@Repository
@RequiredArgsConstructor
public class LeadRepositoryImpl implements ILeadRepository {

    private final LeadDao leadDao;

    @Override
    public void save(LeadAggregate aggregate) {
        leadDao.insert(toPO(aggregate.getLead()));
    }

    @Override
    public void update(LeadAggregate aggregate) {
        leadDao.updateById(toPO(aggregate.getLead()));
    }

    @Override
    public LeadAggregate findById(String id) {
        LeadPO po = leadDao.selectById(id);
        return po == null ? null : LeadAggregate.of(toEntity(po));
    }

    @Override
    public boolean existsFollowingByMobile(String mobile, String excludeId) {
        if (mobile == null || mobile.isBlank()) {
            return false;
        }
        LambdaQueryWrapper<LeadPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LeadPO::getMobile, mobile)
                .eq(LeadPO::getStatus, LeadStatus.FOLLOWING.name())
                .ne(excludeId != null, LeadPO::getId, excludeId);
        return leadDao.selectCount(wrapper) > 0;
    }

    @Override
    public void deleteById(String id) {
        leadDao.deleteById(id);
    }

    @Override
    public PageResult<LeadAggregate> page(LeadQuery query) {
        LambdaQueryWrapper<LeadPO> wrapper = new LambdaQueryWrapper<>();
        if (query.keyword() != null && !query.keyword().isBlank()) {
            String kw = query.keyword().trim();
            wrapper.and(w -> w.like(LeadPO::getLeadName, kw)
                    .or().like(LeadPO::getContactName, kw)
                    .or().like(LeadPO::getMobile, kw));
        }
        wrapper.eq(query.status() != null && !query.status().isBlank(), LeadPO::getStatus, query.status())
                .eq(query.source() != null && !query.source().isBlank(), LeadPO::getSource, query.source())
                .eq(query.ownerId() != null && !query.ownerId().isBlank(), LeadPO::getOwnerId, query.ownerId());
        // 数据权限: owner_id/dept_id 范围过滤
        (query.scope() == null ? CrmScope.all() : query.scope())
                .apply(wrapper, LeadPO::getOwnerId, LeadPO::getDeptId);
        wrapper.orderByDesc(LeadPO::getCreatedAt);

        Page<LeadPO> result = leadDao.selectPage(new Page<>(query.pageNum(), query.pageSize()), wrapper);
        List<LeadAggregate> list = result.getRecords().stream()
                .map(po -> LeadAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    private LeadPO toPO(Lead lead) {
        LeadPO po = new LeadPO();
        po.setId(lead.getId());
        po.setLeadName(lead.getLeadName());
        po.setContactName(lead.getContactName());
        po.setMobile(lead.getMobile());
        po.setTelephone(lead.getTelephone());
        po.setEmail(lead.getEmail());
        po.setIndustry(lead.getIndustry());
        po.setSource(lead.getSource());
        po.setStatus(lead.getStatus());
        po.setAddress(lead.getAddress());
        po.setRemark(lead.getRemark());
        po.setNextTime(lead.getNextTime());
        po.setConvertedCustomerId(lead.getConvertedCustomerId());
        po.setOwnerId(lead.getOwnerId());
        po.setOwnerName(lead.getOwnerName());
        po.setDeptId(lead.getDeptId());
        po.setCreatorId(lead.getCreatorId());
        po.setCreatorName(lead.getCreatorName());
        return po;
    }

    private Lead toEntity(LeadPO po) {
        return Lead.builder()
                .id(po.getId())
                .leadName(po.getLeadName())
                .contactName(po.getContactName())
                .mobile(po.getMobile())
                .telephone(po.getTelephone())
                .email(po.getEmail())
                .industry(po.getIndustry())
                .source(po.getSource())
                .status(po.getStatus())
                .address(po.getAddress())
                .remark(po.getRemark())
                .nextTime(po.getNextTime())
                .convertedCustomerId(po.getConvertedCustomerId())
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
