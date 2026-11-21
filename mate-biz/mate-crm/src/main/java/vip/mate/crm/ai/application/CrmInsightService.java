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
package vip.mate.crm.ai.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.crm.business.application.query.BusinessQueryService;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.business.types.enums.BusinessStage;
import vip.mate.crm.customer.application.query.CustomerQueryService;
import vip.mate.crm.customer.domain.model.entity.Customer;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.crm.trade.application.query.ContractQueryService;
import vip.mate.crm.trade.domain.model.entity.Contract;
import vip.mate.crm.types.exception.CrmErrorCode;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户 360 智能洞察(对标 Wukong-AICRM): 基于 CRM 数据做确定性分析, 产出摘要 + 风险 + 下一步建议。
 *
 * <p>不依赖外部大模型即可用(单体/微服务两模式一致); 对话式分析走 {@code CrmCopilotTools} 的
 * {@code @Tool}(由 mate-ai ChatClient 编排调用)。
 *
 * @author mateaix
 */
@Service
@RequiredArgsConstructor
public class CrmInsightService {

    private static final int STALL_DAYS = 14;
    private static final int NO_FOLLOW_ALERT_DAYS = 30;

    private final CustomerQueryService customerQueryService;
    private final BusinessQueryService businessQueryService;
    private final ContractQueryService contractQueryService;

    public CustomerInsight analyze(String customerId) {
        Customer customer = customerQueryService.findById(customerId);
        if (customer == null) {
            throw new BizException(CrmErrorCode.RECORD_NOT_FOUND);
        }
        List<Business> businesses = businessQueryService.listByCustomer(customerId);
        List<Contract> contracts = contractQueryService.listByCustomer(customerId);
        List<FollowUp> followUps = customerQueryService.detail(customerId).followUps();

        BigDecimal totalMoney = sum(contracts, Contract::getMoney);
        BigDecimal received = sum(contracts, Contract::getReceivedMoney);
        BigDecimal receivable = totalMoney.subtract(received);

        List<Business> active = businesses.stream().filter(b -> isOngoing(b)).toList();
        List<String> stalled = active.stream()
                .filter(b -> stale(b, STALL_DAYS))
                .map(b -> b.getBusinessName() + "(" + b.getStage() + ")")
                .toList();

        LocalDateTime lastFollow = followUps.stream()
                .map(FollowUp::getCreatedAt).filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(customer.getRecentFollowTime());
        long daysSinceFollow = lastFollow == null
                ? Long.MAX_VALUE : Duration.between(lastFollow, LocalDateTime.now()).toDays();

        List<String> risks = buildRisks(customer, receivable, contracts, active, stalled, daysSinceFollow);
        List<String> suggestions = buildSuggestions(customer, active, stalled, daysSinceFollow, receivable);
        String summary = buildSummary(customer, businesses.size(), active.size(), totalMoney, received);

        return new CustomerInsight(customerId, customer.getCustomerName(), summary,
                businesses.size(), active.size(), stalled, contracts.size(),
                totalMoney, received, receivable, lastFollow,
                daysSinceFollow == Long.MAX_VALUE ? null : daysSinceFollow,
                risks, suggestions);
    }

    private List<String> buildRisks(Customer c, BigDecimal receivable, List<Contract> contracts,
                                    List<Business> active, List<String> stalled, long daysSinceFollow) {
        List<String> risks = new ArrayList<>();
        if (receivable.compareTo(BigDecimal.ZERO) > 0) {
            risks.add("存在未回款 " + receivable.toPlainString() + " 元");
        }
        contracts.stream()
                .filter(ct -> ct.getEndTime() != null && ct.getEndTime().isBefore(LocalDate.now().plusDays(30))
                        && ct.getEndTime().isAfter(LocalDate.now().minusDays(1)))
                .forEach(ct -> risks.add("合同[" + ct.getContractName() + "] 即将于 " + ct.getEndTime() + " 到期"));
        if (!stalled.isEmpty()) {
            risks.add(stalled.size() + " 个商机停滞超 " + STALL_DAYS + " 天: " + String.join("、", stalled));
        }
        if (daysSinceFollow == Long.MAX_VALUE) {
            risks.add("尚无跟进记录, 客户可能流失");
        } else if (daysSinceFollow >= NO_FOLLOW_ALERT_DAYS) {
            risks.add("已 " + daysSinceFollow + " 天未跟进, 接近公海回收阈值");
        }
        if ("DORMANT".equalsIgnoreCase(c.getStatus())) {
            risks.add("客户状态为沉睡");
        }
        return risks;
    }

    private List<String> buildSuggestions(Customer c, List<Business> active, List<String> stalled,
                                          long daysSinceFollow, BigDecimal receivable) {
        List<String> tips = new ArrayList<>();
        if (daysSinceFollow == Long.MAX_VALUE || daysSinceFollow >= NO_FOLLOW_ALERT_DAYS) {
            tips.add("尽快安排一次电话或上门回访, 更新最近跟进");
        }
        if (!stalled.isEmpty()) {
            tips.add("梳理停滞商机卡点, 推进到下一阶段或判定输单");
        }
        if (receivable.compareTo(BigDecimal.ZERO) > 0) {
            tips.add("催收未回款项, 核对回款计划到期情况");
        }
        if (active.isEmpty()) {
            tips.add("该客户暂无在途商机, 可挖掘增购/续约机会");
        } else {
            tips.add("有 " + active.size() + " 个在途商机, 明确下一步动作与预计成交时间");
        }
        return tips;
    }

    private String buildSummary(Customer c, int bizTotal, int bizActive, BigDecimal total, BigDecimal received) {
        StringBuilder sb = new StringBuilder();
        sb.append("客户「").append(c.getCustomerName()).append("」");
        if (c.getIndustry() != null) {
            sb.append("(行业 ").append(c.getIndustry()).append(", 等级 ").append(c.getLevel()).append(")");
        }
        sb.append(": 累计商机 ").append(bizTotal).append(" 个(在途 ").append(bizActive).append("), ");
        sb.append("签约 ").append(total.toPlainString()).append(" 元, 已回款 ").append(received.toPlainString()).append(" 元。");
        return sb.toString();
    }

    private static boolean isOngoing(Business b) {
        BusinessStage stage;
        try {
            stage = BusinessStage.of(b.getStage());
        } catch (RuntimeException e) {
            return false;
        }
        return !stage.isTerminal();
    }

    private static boolean stale(Business b, int days) {
        LocalDateTime at = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
        return at != null && at.isBefore(LocalDateTime.now().minusDays(days));
    }

    private static <T> BigDecimal sum(List<T> list, java.util.function.Function<T, BigDecimal> getter) {
        BigDecimal total = BigDecimal.ZERO;
        for (T t : list) {
            BigDecimal v = getter.apply(t);
            total = total.add(v == null ? BigDecimal.ZERO : v);
        }
        return total;
    }

    /** 客户 360 洞察读模型。 */
    public record CustomerInsight(String customerId, String customerName, String summary,
                                  int businessTotal, int businessActive, List<String> stalledBusinesses,
                                  int contractCount, BigDecimal totalMoney, BigDecimal receivedMoney,
                                  BigDecimal receivableMoney, LocalDateTime lastFollowTime,
                                  Long daysSinceFollow, List<String> risks, List<String> suggestions) {
    }
}
