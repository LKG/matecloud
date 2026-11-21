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
package vip.mate.crm.aftersale.infrastructure.adapter.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.base.result.PageResult;
import vip.mate.crm.aftersale.domain.adapter.repository.IAfterSaleRepository;
import vip.mate.crm.aftersale.domain.model.aggregate.AfterSaleAggregate;
import vip.mate.crm.aftersale.domain.model.entity.AfterSale;
import vip.mate.crm.aftersale.domain.model.entity.AfterSaleLog;
import vip.mate.crm.aftersale.infrastructure.dao.AfterSaleDao;
import vip.mate.crm.aftersale.infrastructure.dao.AfterSaleLogDao;
import vip.mate.crm.aftersale.infrastructure.dao.po.AfterSaleLogPO;
import vip.mate.crm.aftersale.infrastructure.dao.po.AfterSalePO;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.query.dto.AdvancedQuery;
import vip.mate.crm.query.infrastructure.CrmSearchSupport;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 售后工单仓储实现: 主表 + 处理日志(日志仅追加)。 */
@Repository
@RequiredArgsConstructor
public class AfterSaleRepositoryImpl implements IAfterSaleRepository {

    private final AfterSaleDao afterSaleDao;
    private final AfterSaleLogDao afterSaleLogDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void save(AfterSaleAggregate aggregate) {
        afterSaleDao.insert(toPO(aggregate.getAfterSale()));
        saveLogs(aggregate.getLogs());
    }

    @Override
    public void update(AfterSaleAggregate aggregate) {
        afterSaleDao.updateById(toPO(aggregate.getAfterSale()));
        // 处理日志仅追加: @TableLogic 下删后重插会与软删残留行主键冲突, 且丢失审计轨迹
        Set<String> existing = loadLogs(aggregate.getId()).stream()
                .map(AfterSaleLog::getId).collect(Collectors.toSet());
        List<AfterSaleLog> fresh = aggregate.getLogs() == null ? List.of()
                : aggregate.getLogs().stream().filter(l -> !existing.contains(l.getId())).toList();
        saveLogs(fresh);
    }

    @Override
    public AfterSaleAggregate findById(String id) {
        AfterSalePO po = afterSaleDao.selectById(id);
        if (po == null) {
            return null;
        }
        return AfterSaleAggregate.of(toEntity(po), loadLogs(id));
    }

    @Override
    public void deleteById(String id) {
        afterSaleDao.deleteById(id);
        LambdaQueryWrapper<AfterSaleLogPO> del = new LambdaQueryWrapper<>();
        del.eq(AfterSaleLogPO::getAfterSaleId, id);
        afterSaleLogDao.delete(del);
    }

    @Override
    public PageResult<AfterSale> page(String keyword, String status, String type, String customerId,
                                      String ownerId, CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<AfterSalePO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(AfterSalePO::getTitle, kw)
                    .or().like(AfterSalePO::getTicketNo, kw)
                    .or().like(AfterSalePO::getCustomerName, kw));
        }
        wrapper.eq(notBlank(status), AfterSalePO::getStatus, status)
                .eq(notBlank(type), AfterSalePO::getType, type)
                .eq(notBlank(customerId), AfterSalePO::getCustomerId, customerId)
                .eq(notBlank(ownerId), AfterSalePO::getOwnerId, ownerId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, AfterSalePO::getOwnerId, AfterSalePO::getDeptId);
        wrapper.orderByDesc(AfterSalePO::getUpdatedAt);

        Page<AfterSalePO> result = afterSaleDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AfterSale> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<AfterSale> search(AdvancedQuery query, CrmScope scope) {
        Page<AfterSalePO> result = crmSearchSupport.search(afterSaleDao, "aftersale", query, scope, "owner_id", "dept_id", "updated_at");
        List<AfterSale> list = result.getRecords().stream().map(this::toEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    private void saveLogs(List<AfterSaleLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (AfterSaleLog log : logs) {
            afterSaleLogDao.insert(toLogPO(log));
        }
    }

    private List<AfterSaleLog> loadLogs(String afterSaleId) {
        LambdaQueryWrapper<AfterSaleLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AfterSaleLogPO::getAfterSaleId, afterSaleId).orderByAsc(AfterSaleLogPO::getCreatedAt);
        return afterSaleLogDao.selectList(wrapper).stream().map(this::toLogEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private AfterSalePO toPO(AfterSale a) {
        AfterSalePO po = new AfterSalePO();
        po.setId(a.getId());
        po.setTicketNo(a.getTicketNo());
        po.setTitle(a.getTitle());
        po.setCustomerId(a.getCustomerId());
        po.setCustomerName(a.getCustomerName());
        po.setContractId(a.getContractId());
        po.setType(a.getType());
        po.setPriority(a.getPriority());
        po.setStatus(a.getStatus());
        po.setDescription(a.getDescription());
        po.setHandlerId(a.getHandlerId());
        po.setHandlerName(a.getHandlerName());
        po.setResolveTime(a.getResolveTime());
        po.setCloseTime(a.getCloseTime());
        po.setRating(a.getRating());
        po.setRatingRemark(a.getRatingRemark());
        po.setOwnerId(a.getOwnerId());
        po.setOwnerName(a.getOwnerName());
        po.setDeptId(a.getDeptId());
        po.setCreatorId(a.getCreatorId());
        po.setCreatorName(a.getCreatorName());
        return po;
    }

    private AfterSale toEntity(AfterSalePO po) {
        return AfterSale.builder()
                .id(po.getId())
                .ticketNo(po.getTicketNo())
                .title(po.getTitle())
                .customerId(po.getCustomerId())
                .customerName(po.getCustomerName())
                .contractId(po.getContractId())
                .type(po.getType())
                .priority(po.getPriority())
                .status(po.getStatus())
                .description(po.getDescription())
                .handlerId(po.getHandlerId())
                .handlerName(po.getHandlerName())
                .resolveTime(po.getResolveTime())
                .closeTime(po.getCloseTime())
                .rating(po.getRating())
                .ratingRemark(po.getRatingRemark())
                .ownerId(po.getOwnerId())
                .ownerName(po.getOwnerName())
                .deptId(po.getDeptId())
                .creatorId(po.getCreatorId())
                .creatorName(po.getCreatorName())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

    private AfterSaleLogPO toLogPO(AfterSaleLog l) {
        AfterSaleLogPO po = new AfterSaleLogPO();
        po.setId(l.getId());
        po.setAfterSaleId(l.getAfterSaleId());
        po.setAction(l.getAction());
        po.setContent(l.getContent());
        po.setOperatorId(l.getOperatorId());
        po.setOperatorName(l.getOperatorName());
        return po;
    }

    private AfterSaleLog toLogEntity(AfterSaleLogPO po) {
        return AfterSaleLog.builder()
                .id(po.getId())
                .afterSaleId(po.getAfterSaleId())
                .action(po.getAction())
                .content(po.getContent())
                .operatorId(po.getOperatorId())
                .operatorName(po.getOperatorName())
                .createdAt(po.getCreatedAt())
                .build();
    }
}
