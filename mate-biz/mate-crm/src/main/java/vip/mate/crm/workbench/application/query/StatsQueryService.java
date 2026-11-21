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
package vip.mate.crm.workbench.application.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.crm.business.infrastructure.dao.BusinessDao;
import vip.mate.crm.business.infrastructure.dao.po.BusinessPO;
import vip.mate.crm.common.CrmScope;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.customer.infrastructure.dao.CustomerDao;
import vip.mate.crm.customer.infrastructure.dao.po.CustomerPO;
import vip.mate.crm.lead.infrastructure.dao.LeadDao;
import vip.mate.crm.lead.infrastructure.dao.po.LeadPO;
import vip.mate.crm.trade.infrastructure.dao.ContractDao;
import vip.mate.crm.trade.infrastructure.dao.PaymentPlanDao;
import vip.mate.crm.trade.infrastructure.dao.po.ContractPO;
import vip.mate.crm.trade.infrastructure.dao.po.PaymentPlanPO;
import vip.mate.crm.workbench.infrastructure.dao.TaskDao;
import vip.mate.crm.workbench.infrastructure.dao.po.TaskPO;
import vip.mate.crm.workbench.types.enums.TaskStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作台看板读模型: 绕过聚合, 直接用各上下文 DAO 做统计聚合。
 *
 * <p>所有统计在当前用户数据权限范围(CrmScope)内计算。演示规模下采用内存聚合,
 * 生产可下沉为专用统计视图/物化查询。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class StatsQueryService {

    private static final String STAGE_WIN = "WIN";
    private static final String STAGE_LOSE = "LOSE";
    private static final int PLAN_OVERDUE = 3;

    private final LeadDao leadDao;
    private final CustomerDao customerDao;
    private final BusinessDao businessDao;
    private final ContractDao contractDao;
    private final PaymentPlanDao paymentPlanDao;
    private final TaskDao taskDao;
    private final CurrentUserResolver currentUserResolver;

    /** 看板总览: 线索/客户/商机/合同/回款/待办 汇总 + 转化率。 */
    public Dashboard overview() {
        CrmScope scope = currentUserResolver.scope();

        long leadTotal = count(leadDao.selectList(scopeOn(new LambdaQueryWrapper<LeadPO>(),
                LeadPO::getOwnerId, LeadPO::getDeptId, scope)));
        long leadConverted = count(leadDao.selectList(scopeOn(new LambdaQueryWrapper<LeadPO>(),
                LeadPO::getOwnerId, LeadPO::getDeptId, scope).eq(LeadPO::getStatus, "CONVERTED")));

        long customerPrivate = customerDao.selectCount(privateScope(scope));
        long customerPublic = customerDao.selectCount(new LambdaQueryWrapper<CustomerPO>()
                .eq(CustomerPO::getPoolStatus, 1));

        List<BusinessPO> businesses = businessDao.selectList(scopeOn(new LambdaQueryWrapper<BusinessPO>(),
                BusinessPO::getOwnerId, BusinessPO::getDeptId, scope));
        long won = businesses.stream().filter(b -> STAGE_WIN.equalsIgnoreCase(b.getStage())).count();
        long lose = businesses.stream().filter(b -> STAGE_LOSE.equalsIgnoreCase(b.getStage())).count();

        List<ContractPO> signed = signedContracts(scope);
        BigDecimal contractMoney = sum(signed, ContractPO::getMoney);
        BigDecimal receivedMoney = sum(signed, ContractPO::getReceivedMoney);

        long overduePlans = paymentPlanDao.selectCount(new LambdaQueryWrapper<PaymentPlanPO>()
                .eq(PaymentPlanPO::getStatus, PLAN_OVERDUE));
        BigDecimal pendingPlanMoney = sum(paymentPlanDao.selectList(new LambdaQueryWrapper<PaymentPlanPO>()
                .ne(PaymentPlanPO::getStatus, 2)), po -> nvl(po.getMoney()).subtract(nvl(po.getReceivedMoney())));

        String myId = currentUserResolver.require().userId();
        long todayTasks = taskDao.selectCount(new LambdaQueryWrapper<TaskPO>()
                .eq(TaskPO::getOwnerId, myId).eq(TaskPO::getStatus, TaskStatus.OPEN.name()));

        return new Dashboard(leadTotal, leadConverted, rate(leadConverted, leadTotal),
                customerPrivate, customerPublic, businesses.size(), won, lose, rate(won, won + lose),
                signed.size(), contractMoney, receivedMoney, rate(receivedMoney, contractMoney),
                overduePlans, pendingPlanMoney, todayTasks);
    }

    /** 销售漏斗: 各阶段商机数与金额。 */
    public List<FunnelStage> funnel() {
        CrmScope scope = currentUserResolver.scope();
        List<BusinessPO> businesses = businessDao.selectList(scopeOn(new LambdaQueryWrapper<BusinessPO>(),
                BusinessPO::getOwnerId, BusinessPO::getDeptId, scope));
        String[] order = {"INITIAL", "DEMAND", "PLAN", "NEGOTIATION", "WIN", "LOSE"};
        List<FunnelStage> result = new ArrayList<>();
        for (String stage : order) {
            List<BusinessPO> in = businesses.stream().filter(b -> stage.equalsIgnoreCase(b.getStage())).toList();
            result.add(new FunnelStage(stage, in.size(), sum(in, BusinessPO::getMoney)));
        }
        return result;
    }

    /** 成员业绩排行(按签单金额降序)。 */
    public List<MemberStat> memberRanking(int limit) {
        List<ContractPO> signed = signedContracts(currentUserResolver.scope());
        Map<String, MemberAcc> acc = new LinkedHashMap<>();
        for (ContractPO c : signed) {
            String key = c.getOwnerId() == null ? "-" : c.getOwnerId();
            MemberAcc a = acc.computeIfAbsent(key,
                    k -> new MemberAcc(c.getOwnerId(), c.getOwnerName()));
            a.count++;
            a.money = a.money.add(nvl(c.getMoney()));
        }
        return acc.values().stream()
                .sorted((x, y) -> y.money.compareTo(x.money))
                .limit(limit)
                .map(m -> new MemberStat(m.ownerId, m.ownerName, m.count, m.money))
                .toList();
    }

    /** 近 N 月签约金额与回款趋势(按签约月分组)。 */
    public List<MonthTrend> trend(int months) {
        List<ContractPO> signed = signedContracts(currentUserResolver.scope());
        YearMonth from = YearMonth.now().minusMonths(months - 1L);
        List<MonthTrend> result = new ArrayList<>();
        for (int i = 0; i < months; i++) {
            YearMonth ym = from.plusMonths(i);
            String label = ym.toString();
            List<ContractPO> in = signed.stream()
                    .filter(c -> c.getSignTime() != null && YearMonth.from(c.getSignTime()).equals(ym)).toList();
            result.add(new MonthTrend(label, sum(in, ContractPO::getMoney), sum(in, ContractPO::getReceivedMoney)));
        }
        return result;
    }

    // ---- helpers ----

    private List<ContractPO> signedContracts(CrmScope scope) {
        LambdaQueryWrapper<ContractPO> wrapper = new LambdaQueryWrapper<ContractPO>()
                .in(ContractPO::getStatus, "APPROVED", "ARCHIVED");
        scope.apply(wrapper, ContractPO::getOwnerId, ContractPO::getDeptId);
        return contractDao.selectList(wrapper);
    }

    private LambdaQueryWrapper<CustomerPO> privateScope(CrmScope scope) {
        LambdaQueryWrapper<CustomerPO> wrapper = new LambdaQueryWrapper<CustomerPO>()
                .eq(CustomerPO::getPoolStatus, 0);
        scope.apply(wrapper, CustomerPO::getOwnerId, CustomerPO::getDeptId);
        return wrapper;
    }

    private static <T> LambdaQueryWrapper<T> scopeOn(LambdaQueryWrapper<T> wrapper,
                                                     com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> owner,
                                                     com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, ?> dept,
                                                     CrmScope scope) {
        scope.apply(wrapper, owner, dept);
        return wrapper;
    }

    private static <T> long count(List<T> list) {
        return list == null ? 0 : list.size();
    }

    private static <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        if (list == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (T t : list) {
            total = total.add(nvl(getter.apply(t)));
        }
        return total;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal rate(long part, long total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal rate(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return nvl(part).multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static final class MemberAcc {
        private final String ownerId;
        private final String ownerName;
        private int count;
        private BigDecimal money = BigDecimal.ZERO;

        private MemberAcc(String ownerId, String ownerName) {
            this.ownerId = ownerId;
            this.ownerName = ownerName;
        }
    }

    // ---- read models ----

    public record Dashboard(long leadTotal, long leadConverted, BigDecimal leadConvertRate,
                            long customerPrivate, long customerPublic,
                            long businessTotal, long businessWon, long businessLose, BigDecimal winRate,
                            long contractCount, BigDecimal contractMoney, BigDecimal receivedMoney,
                            BigDecimal receivedRate, long overduePlanCount, BigDecimal pendingPlanMoney,
                            long todayTaskCount) {
    }

    public record FunnelStage(String stage, int count, BigDecimal money) {
    }

    public record MemberStat(String ownerId, String ownerName, int contractCount, BigDecimal contractMoney) {
    }

    public record MonthTrend(String month, BigDecimal signMoney, BigDecimal receivedMoney) {
    }
}
