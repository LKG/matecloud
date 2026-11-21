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
import vip.mate.crm.trade.domain.adapter.repository.IPaymentPlanRepository;
import vip.mate.crm.trade.domain.model.aggregate.PaymentPlanAggregate;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;
import vip.mate.crm.trade.domain.model.entity.Repayment;
import vip.mate.crm.trade.infrastructure.dao.PaymentPlanDao;
import vip.mate.crm.trade.infrastructure.dao.RepaymentDao;
import vip.mate.crm.trade.infrastructure.dao.po.PaymentPlanPO;
import vip.mate.crm.trade.infrastructure.dao.po.RepaymentPO;
import vip.mate.crm.trade.types.enums.PlanStatus;

import java.time.LocalDate;
import java.util.List;

/** 回款计划 + 回款记录仓储实现。 */
@Repository
@RequiredArgsConstructor
public class PaymentPlanRepositoryImpl implements IPaymentPlanRepository {

    private final PaymentPlanDao paymentPlanDao;
    private final RepaymentDao repaymentDao;
    private final CrmSearchSupport crmSearchSupport;

    @Override
    public void savePlan(PaymentPlanAggregate aggregate) {
        paymentPlanDao.insert(toPlanPO(aggregate.getPlan()));
    }

    @Override
    public void updatePlan(PaymentPlanAggregate aggregate) {
        paymentPlanDao.updateById(toPlanPO(aggregate.getPlan()));
    }

    @Override
    public PaymentPlanAggregate findPlanById(String id) {
        PaymentPlanPO po = paymentPlanDao.selectById(id);
        if (po == null) {
            return null;
        }
        return PaymentPlanAggregate.of(toPlanEntity(po), listRepaymentsByPlan(id));
    }

    @Override
    public void deletePlan(String id) {
        paymentPlanDao.deleteById(id);
        LambdaQueryWrapper<RepaymentPO> del = new LambdaQueryWrapper<>();
        del.eq(RepaymentPO::getPlanId, id);
        repaymentDao.delete(del);
    }

    @Override
    public PageResult<PaymentPlan> pagePlans(String keyword, String contractId, Integer status,
                                             String ownerId, CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<PaymentPlanPO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(PaymentPlanPO::getContractNum, kw).or().like(PaymentPlanPO::getRemark, kw));
        }
        wrapper.eq(notBlank(contractId), PaymentPlanPO::getContractId, contractId)
                .eq(status != null, PaymentPlanPO::getStatus, status)
                .eq(notBlank(ownerId), PaymentPlanPO::getOwnerId, ownerId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, PaymentPlanPO::getOwnerId, PaymentPlanPO::getDeptId);
        wrapper.orderByAsc(PaymentPlanPO::getCheckingDate);

        Page<PaymentPlanPO> result = paymentPlanDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<PaymentPlan> list = result.getRecords().stream().map(this::toPlanEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<PaymentPlan> listByContract(String contractId) {
        LambdaQueryWrapper<PaymentPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentPlanPO::getContractId, contractId).orderByAsc(PaymentPlanPO::getTermNo);
        return paymentPlanDao.selectList(wrapper).stream().map(this::toPlanEntity).toList();
    }

    @Override
    public List<PaymentPlanAggregate> findDuePlans(LocalDate today, int limit) {
        LambdaQueryWrapper<PaymentPlanPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(PaymentPlanPO::getStatus, PlanStatus.SETTLED.code())
                .lt(PaymentPlanPO::getCheckingDate, today)
                .orderByAsc(PaymentPlanPO::getCheckingDate)
                .last("limit " + limit);
        return paymentPlanDao.selectList(wrapper).stream()
                .map(po -> PaymentPlanAggregate.of(toPlanEntity(po), null))
                .toList();
    }

    @Override
    public void insertRepayment(Repayment repayment) {
        repaymentDao.insert(toRepaymentPO(repayment));
    }

    @Override
    public Repayment findRepaymentById(String id) {
        RepaymentPO po = repaymentDao.selectById(id);
        return po == null ? null : toRepaymentEntity(po);
    }

    @Override
    public void deleteRepayment(String id) {
        repaymentDao.deleteById(id);
    }

    @Override
    public PageResult<Repayment> pageRepayments(String keyword, String contractId, String ownerId,
                                                CrmScope scope, int pageNum, int pageSize) {
        LambdaQueryWrapper<RepaymentPO> wrapper = new LambdaQueryWrapper<>();
        if (notBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(RepaymentPO::getNumber, kw).or().like(RepaymentPO::getContractNum, kw));
        }
        wrapper.eq(notBlank(contractId), RepaymentPO::getContractId, contractId)
                .eq(notBlank(ownerId), RepaymentPO::getOwnerId, ownerId);
        (scope == null ? CrmScope.all() : scope).apply(wrapper, RepaymentPO::getOwnerId, RepaymentPO::getDeptId);
        wrapper.orderByDesc(RepaymentPO::getReturnTime);

        Page<RepaymentPO> result = repaymentDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Repayment> list = result.getRecords().stream().map(this::toRepaymentEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public PageResult<Repayment> searchRepayments(AdvancedQuery query, CrmScope scope) {
        Page<RepaymentPO> result = crmSearchSupport.search(repaymentDao, "repayment", query, scope, "owner_id", "dept_id", "return_time");
        List<Repayment> list = result.getRecords().stream().map(this::toRepaymentEntity).toList();
        return PageResult.of(list, result.getTotal());
    }

    @Override
    public List<Repayment> listRepaymentsByPlan(String planId) {
        LambdaQueryWrapper<RepaymentPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RepaymentPO::getPlanId, planId).orderByDesc(RepaymentPO::getReturnTime);
        return repaymentDao.selectList(wrapper).stream().map(this::toRepaymentEntity).toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private PaymentPlanPO toPlanPO(PaymentPlan p) {
        PaymentPlanPO po = new PaymentPlanPO();
        po.setId(p.getId());
        po.setContractId(p.getContractId());
        po.setContractNum(p.getContractNum());
        po.setCustomerId(p.getCustomerId());
        po.setTermNo(p.getTermNo());
        po.setMoney(p.getMoney());
        po.setReceivedMoney(p.getReceivedMoney());
        po.setCheckingDate(p.getCheckingDate());
        po.setReturnType(p.getReturnType());
        po.setStatus(p.getStatus());
        po.setRemark(p.getRemark());
        po.setOwnerId(p.getOwnerId());
        po.setOwnerName(p.getOwnerName());
        po.setDeptId(p.getDeptId());
        po.setCreatorId(p.getCreatorId());
        po.setCreatorName(p.getCreatorName());
        return po;
    }

    private PaymentPlan toPlanEntity(PaymentPlanPO po) {
        return PaymentPlan.builder()
                .id(po.getId())
                .contractId(po.getContractId())
                .contractNum(po.getContractNum())
                .customerId(po.getCustomerId())
                .termNo(po.getTermNo())
                .money(po.getMoney())
                .receivedMoney(po.getReceivedMoney())
                .checkingDate(po.getCheckingDate())
                .returnType(po.getReturnType())
                .status(po.getStatus())
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

    private RepaymentPO toRepaymentPO(Repayment r) {
        RepaymentPO po = new RepaymentPO();
        po.setId(r.getId());
        po.setNumber(r.getNumber());
        po.setContractId(r.getContractId());
        po.setContractNum(r.getContractNum());
        po.setPlanId(r.getPlanId());
        po.setCustomerId(r.getCustomerId());
        po.setMoney(r.getMoney());
        po.setPaymentMethod(r.getPaymentMethod());
        po.setReturnTime(r.getReturnTime());
        po.setRemark(r.getRemark());
        po.setAttachment(r.getAttachment());
        po.setOwnerId(r.getOwnerId());
        po.setOwnerName(r.getOwnerName());
        po.setDeptId(r.getDeptId());
        po.setCreatorId(r.getCreatorId());
        po.setCreatorName(r.getCreatorName());
        return po;
    }

    private Repayment toRepaymentEntity(RepaymentPO po) {
        return Repayment.builder()
                .id(po.getId())
                .number(po.getNumber())
                .contractId(po.getContractId())
                .contractNum(po.getContractNum())
                .planId(po.getPlanId())
                .customerId(po.getCustomerId())
                .money(po.getMoney())
                .paymentMethod(po.getPaymentMethod())
                .returnTime(po.getReturnTime())
                .remark(po.getRemark())
                .attachment(po.getAttachment())
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
