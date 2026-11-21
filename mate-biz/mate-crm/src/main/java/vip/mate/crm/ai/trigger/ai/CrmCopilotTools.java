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
package vip.mate.crm.ai.trigger.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import vip.mate.crm.ai.application.CrmInsightService;
import vip.mate.crm.business.application.query.BusinessQueryService;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.common.CurrentUserResolver;
import vip.mate.crm.workbench.application.command.TaskCommandService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM Copilot AI 工具(对标 Wukong-AICRM): 将 CRM 查询/摘要/建议/建待办能力以 Spring AI
 * {@code @Tool} 暴露, 由 {@code AiToolRegistry} 自动注册, 单体模式下在同一 JVM 工具表内,
 * 对话页 {@code POST /api/v1/ai/chat} 可直接被大模型编排调用。
 *
 * @author mateaix
 */
@Component
@RequiredArgsConstructor
public class CrmCopilotTools {

    private final CrmInsightService insightService;
    private final BusinessQueryService businessQueryService;
    private final TaskCommandService taskCommandService;
    private final CurrentUserResolver currentUserResolver;

    @Tool(description = "Summarize a CRM customer (360 view): overall health, deal/receivable status, "
            + "risks and recommended next actions. Returns a readable text digest.")
    public String summarizeCustomer(
            @ToolParam(description = "The customer ID to analyze") String customerId) {
        CrmInsightService.CustomerInsight ins = insightService.analyze(customerId);
        StringBuilder sb = new StringBuilder(ins.summary()).append('\n');
        if (!ins.risks().isEmpty()) {
            sb.append("风险: ").append(String.join("; ", ins.risks())).append('\n');
        }
        if (!ins.suggestions().isEmpty()) {
            sb.append("建议: ").append(String.join("; ", ins.suggestions()));
        }
        return sb.toString();
    }

    @Tool(description = "List ongoing sales opportunities (businesses) that have been stalled — "
            + "not updated for the given number of days — within the current user's data scope. "
            + "Useful for pipeline hygiene coaching.")
    public List<String> stalledBusinesses(
            @ToolParam(description = "Stall threshold in days, e.g. 14") int stallDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(1, stallDays));
        List<Business> ongoing = businessQueryService
                .page(null, null, null, 1, false, 1, 200).getList();
        return ongoing.stream()
                .filter(b -> {
                    LocalDateTime at = b.getUpdatedAt() != null ? b.getUpdatedAt() : b.getCreatedAt();
                    return at != null && at.isBefore(cutoff);
                })
                .map(b -> b.getBusinessName() + " [" + b.getStage() + "] 负责人:" 
                        + (b.getOwnerName() == null ? "-" : b.getOwnerName()))
                .toList();
    }

    @Tool(description = "Suggest concrete follow-up actions for a customer based on its recent "
            + "activity, receivables and opportunities.")
    public List<String> followUpSuggestions(
            @ToolParam(description = "The customer ID") String customerId) {
        return insightService.analyze(customerId).suggestions();
    }

    @Tool(description = "Create a follow-up todo task assigned to the current user for a customer. "
            + "Returns the new task ID.")
    public String createFollowUpTask(
            @ToolParam(description = "Task title, e.g. '回访客户A确认续约'") String title,
            @ToolParam(description = "Related customer ID") String customerId,
            @ToolParam(description = "Customer name (display)") String customerName,
            @ToolParam(description = "Due in how many days from now, e.g. 2") int dueInDays) {
        return taskCommandService.generate(title, "CUSTOMER", customerId, customerName,
                "AI 生成跟进任务", LocalDateTime.now().plusDays(Math.max(0, dueInDays)),
                "MEDIUM", currentUserResolver.require());
    }
}
