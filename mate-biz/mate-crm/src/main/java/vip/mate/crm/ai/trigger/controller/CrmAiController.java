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
package vip.mate.crm.ai.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.crm.ai.application.CrmInsightService;

/**
 * CRM AI 洞察 REST 入口(/api/v1/crm/ai): 客户 360 智能摘要专用端点。
 *
 * <p>对话式查询由 {@link vip.mate.crm.ai.trigger.ai.CrmCopilotTools} 的 @Tool 经
 * mate-ai {@code /api/v1/ai/chat} 承载; 此处提供前端卡片直连的结构化洞察。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/ai")
@RequiredArgsConstructor
@SaCheckLogin
public class CrmAiController {

    private final CrmInsightService insightService;

    @GetMapping("/insight/{customerId}")
    @SaCheckPermission("crm:customer:list")
    public Result<CrmInsightService.CustomerInsight> insight(@PathVariable String customerId) {
        return Result.ok(insightService.analyze(customerId));
    }
}
