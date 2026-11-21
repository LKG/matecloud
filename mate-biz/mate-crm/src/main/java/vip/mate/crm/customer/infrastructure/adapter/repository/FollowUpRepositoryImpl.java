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
import vip.mate.crm.customer.domain.adapter.repository.IFollowUpRepository;
import vip.mate.crm.customer.domain.model.aggregate.FollowUpAggregate;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.crm.customer.infrastructure.dao.FollowUpDao;
import vip.mate.crm.customer.infrastructure.dao.po.FollowUpPO;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;

import java.util.List;

/** 跟进记录仓储实现。 */
@Repository
@RequiredArgsConstructor
public class FollowUpRepositoryImpl implements IFollowUpRepository {

    private final FollowUpDao followUpDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(FollowUpAggregate aggregate) {
        followUpDao.insert(toPO(aggregate.getFollowUp()));
    }

    @Override
    public FollowUpAggregate findById(String id) {
        FollowUpPO po = followUpDao.selectById(id);
        return po == null ? null : FollowUpAggregate.of(toEntity(po));
    }

    @Override
    public void deleteById(String id) {
        followUpDao.deleteById(id);
    }

    @Override
    public PageResult<FollowUpAggregate> page(String bizType, String bizId, CrmScope scope,
                                              int pageNum, int pageSize) {
        LambdaQueryWrapper<FollowUpPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(bizType != null && !bizType.isBlank(), FollowUpPO::getBizType, bizType)
                .eq(bizId != null && !bizId.isBlank(), FollowUpPO::getBizId, bizId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, FollowUpPO::getUserId, FollowUpPO::getDeptId);
        wrapper.orderByDesc(FollowUpPO::getCreatedAt);

        Page<FollowUpPO> result = followUpDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<FollowUpAggregate> list = result.getRecords().stream()
                .map(po -> FollowUpAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<FollowUpAggregate> search(AdvancedQuery query, CrmScope scope) {
        Page<FollowUpPO> result = crmSearchSupport.search(followUpDao, "followup", query, scope, "user_id", "dept_id");
        List<FollowUpAggregate> list = result.getRecords().stream()
                .map(po -> FollowUpAggregate.of(toEntity(po)))
                .toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<FollowUpAggregate> listByBiz(String bizType, String bizId) {
        LambdaQueryWrapper<FollowUpPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowUpPO::getBizType, bizType).eq(FollowUpPO::getBizId, bizId)
                .orderByDesc(FollowUpPO::getCreatedAt);
        return followUpDao.selectList(wrapper).stream().map(po -> FollowUpAggregate.of(toEntity(po))).toList();
    }

    @Override
    public List<FollowUpAggregate> listByCustomerId(String customerId) {
        LambdaQueryWrapper<FollowUpPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FollowUpPO::getCustomerId, customerId).orderByDesc(FollowUpPO::getCreatedAt);
        return followUpDao.selectList(wrapper).stream().map(po -> FollowUpAggregate.of(toEntity(po))).toList();
    }

    private FollowUpPO toPO(FollowUp f) {
        FollowUpPO po = new FollowUpPO();
        po.setId(f.getId());
        po.setBizType(f.getBizType());
        po.setBizId(f.getBizId());
        po.setBizName(f.getBizName());
        po.setCustomerId(f.getCustomerId());
        po.setContent(f.getContent());
        po.setFollowType(f.getFollowType());
        po.setNextContent(f.getNextContent());
        po.setNextTime(f.getNextTime());
        po.setUserId(f.getUserId());
        po.setUserName(f.getUserName());
        po.setDeptId(f.getDeptId());
        return po;
    }

    private FollowUp toEntity(FollowUpPO po) {
        return FollowUp.builder()
                .id(po.getId())
                .bizType(po.getBizType())
                .bizId(po.getBizId())
                .bizName(po.getBizName())
                .customerId(po.getCustomerId())
                .content(po.getContent())
                .followType(po.getFollowType())
                .nextContent(po.getNextContent())
                .nextTime(po.getNextTime())
                .userId(po.getUserId())
                .userName(po.getUserName())
                .deptId(po.getDeptId())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
