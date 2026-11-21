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
package vip.mate.crm.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vip.mate.crm.trade.domain.adapter.repository.IPaymentPlanRepository;
import vip.mate.crm.trade.domain.model.aggregate.PaymentPlanAggregate;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;

import java.time.LocalDate;
import java.util.List;

/**
 * 回款到期/逾期提醒任务: 扫描未结清且计划日期已过的回款期次, 置为逾期并提醒负责人。
 *
 * <p>通知通道: 演示环境以日志告警呈现; 生产可在此接入 mate-notice(经 {@code IRpcNoticeService}
 * 微服务 / 单体本地 bean)向合同负责人推送短信/邮件, 目标联系方式由 owner 解析后补全。
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmRepaymentRemindJob {

    private static final int BATCH_LIMIT = 500;

    private final IPaymentPlanRepository paymentPlanRepository;

    @Scheduled(cron = "${mate.crm.job.repayment-remind.cron:0 30 8 * * ?}")
    public void remind() {
        LocalDate today = LocalDate.now();
        List<PaymentPlanAggregate> due = paymentPlanRepository.findDuePlans(today, BATCH_LIMIT);
        if (due.isEmpty()) {
            log.info("[crm-job] repayment remind: no due plan (today={})", today);
            return;
        }
        int newlyOverdue = 0;
        for (PaymentPlanAggregate agg : due) {
            if (agg.markOverdueIfDue(today)) {
                paymentPlanRepository.updatePlan(agg);
                newlyOverdue++;
                PaymentPlan plan = agg.getPlan();
                java.math.BigDecimal pending = plan.getMoney()
                        .subtract(plan.getReceivedMoney() == null ? java.math.BigDecimal.ZERO : plan.getReceivedMoney());
                log.warn("[crm-job] 回款逾期提醒: 合同[{}] 第{}期 计划{} 待收{} 负责人{}",
                        plan.getContractNum(), plan.getTermNo(), plan.getCheckingDate(),
                        pending, plan.getOwnerName());
            }
        }
        log.info("[crm-job] repayment remind: {} due plan(s), {} newly marked overdue (today={})",
                due.size(), newlyOverdue, today);
    }
}
