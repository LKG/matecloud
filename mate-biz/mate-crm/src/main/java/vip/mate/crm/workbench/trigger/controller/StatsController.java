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
package vip.mate.crm.workbench.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.crm.workbench.application.query.StatsQueryService;

import java.util.List;

/**
 * 工作台看板统计 REST 入口(/api/v1/crm/stats): 总览 / 漏斗 / 成员排行 / 业绩趋势。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/stats")
@RequiredArgsConstructor
@SaCheckLogin
public class StatsController {

    private final StatsQueryService statsQueryService;

    @GetMapping("/overview")
    @SaCheckPermission("crm:dashboard:view")
    public Result<StatsQueryService.Dashboard> overview() {
        return Result.ok(statsQueryService.overview());
    }

    @GetMapping("/funnel")
    @SaCheckPermission("crm:dashboard:view")
    public Result<List<StatsQueryService.FunnelStage>> funnel() {
        return Result.ok(statsQueryService.funnel());
    }

    @GetMapping("/member-ranking")
    @SaCheckPermission("crm:dashboard:view")
    public Result<List<StatsQueryService.MemberStat>> memberRanking(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statsQueryService.memberRanking(limit));
    }

    @GetMapping("/trend")
    @SaCheckPermission("crm:dashboard:view")
    public Result<List<StatsQueryService.MonthTrend>> trend(@RequestParam(defaultValue = "6") int months) {
        return Result.ok(statsQueryService.trend(months));
    }
}
