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
package vip.mate.crm.trade.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.crm.trade.application.command.PaymentPlanCommand;
import vip.mate.crm.trade.application.command.PaymentPlanCommandService;
import vip.mate.crm.trade.application.command.RepaymentCommand;
import vip.mate.crm.trade.application.command.RepaymentCommandService;
import vip.mate.crm.trade.application.query.PaymentPlanQueryService;
import vip.mate.crm.trade.domain.model.entity.PaymentPlan;
import vip.mate.crm.trade.domain.model.entity.Repayment;

import java.util.List;

/**
 * 回款 REST 入口(/api/v1/crm/repayments): 回款计划分期维护 + 回款登记闭环。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/repayments")
@RequiredArgsConstructor
@SaCheckLogin
public class AcceptanceController {

    private final PaymentPlanCommandService planCommandService;
    private final PaymentPlanQueryService planQueryService;
    private final RepaymentCommandService repaymentCommandService;

    // ---- 回款计划 ----

    @GetMapping("/plans")
    @SaCheckPermission("crm:repayment:list")
    public Result<PageResult<PaymentPlan>> pagePlans(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(planQueryService.pagePlans(keyword, contractId, status, mineOnly, pageNum, pageSize));
    }

    @GetMapping("/contracts/{contractId}/plans")
    @SaCheckPermission("crm:repayment:list")
    public Result<List<PaymentPlan>> listPlansByContract(@PathVariable String contractId) {
        return Result.ok(planQueryService.listPlansByContract(contractId));
    }

    @PostMapping("/plans")
    @SaCheckPermission("crm:repayment:plan")
    public Result<String> createPlan(@RequestBody PaymentPlanCommand cmd) {
        return Result.ok(planCommandService.create(cmd));
    }

    @PutMapping("/plans/{id}")
    @SaCheckPermission("crm:repayment:plan")
    public Result<Void> updatePlan(@PathVariable String id, @RequestBody PaymentPlanCommand cmd) {
        planCommandService.update(id, cmd);
        return Result.ok();
    }

    @DeleteMapping("/plans/{id}")
    @SaCheckPermission("crm:repayment:plan")
    public Result<Void> deletePlan(@PathVariable String id) {
        planCommandService.delete(id);
        return Result.ok();
    }

    // ---- 回款记录 ----

    @GetMapping
    @SaCheckPermission("crm:repayment:list")
    public Result<PageResult<Repayment>> pageRepayments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contractId,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(planQueryService.pageRepayments(keyword, contractId, mineOnly, pageNum, pageSize));
    }

    @GetMapping("/plans/{planId}/records")
    @SaCheckPermission("crm:repayment:list")
    public Result<List<Repayment>> listRepaymentsByPlan(@PathVariable String planId) {
        return Result.ok(planQueryService.listRepaymentsByPlan(planId));
    }

    @PostMapping
    @SaCheckPermission("crm:repayment:add")
    public Result<String> register(@RequestBody RepaymentCommand cmd) {
        return Result.ok(repaymentCommandService.register(cmd));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:repayment:delete")
    public Result<Void> deleteRepayment(@PathVariable String id) {
        repaymentCommandService.delete(id);
        return Result.ok();
    }
}
