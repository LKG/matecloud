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
package vip.mate.crm.approval.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.crm.approval.application.command.ApprovalCommandService;
import vip.mate.crm.approval.application.query.ApprovalQueryService;
import vip.mate.crm.approval.domain.model.entity.Approval;

/**
 * 审批中心 REST 入口(/api/v1/crm/approvals): 我发起的 / 待我审批 / 明细 / 通过 / 驳回 / 撤回。
 *
 * <p>合同提交审批走本入口, 终态由应用层回调回写合同状态。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/approvals")
@RequiredArgsConstructor
@SaCheckLogin
public class ApprovalController {

    private final ApprovalCommandService commandService;
    private final ApprovalQueryService queryService;

    /** 我发起的。 */
    @GetMapping("/mine")
    @SaCheckPermission("crm:approval:list")
    public Result<PageResult<Approval>> pageMine(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.pageMine(keyword, status, bizType, pageNum, pageSize));
    }

    /** 待我审批(在途)。 */
    @GetMapping("/todo")
    @SaCheckPermission("crm:approval:list")
    public Result<PageResult<Approval>> pageTodo(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String bizType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.pageTodo(keyword, bizType, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:approval:list")
    public Result<ApprovalQueryService.ApprovalDetail> detail(@PathVariable String id) {
        return Result.ok(queryService.detail(id));
    }

    /** 合同提交审批: 建审批实例并回写合同 PENDING。 */
    @PostMapping("/contract/{contractId}")
    @SaCheckPermission("crm:contract:submit")
    public Result<String> submitContract(@PathVariable String contractId) {
        return Result.ok(commandService.submitContract(contractId));
    }

    @PutMapping("/{id}/approve")
    @SaCheckPermission("crm:approval:audit")
    public Result<Void> approve(@PathVariable String id, @RequestParam(required = false) String remark) {
        commandService.approve(id, remark);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    @SaCheckPermission("crm:approval:audit")
    public Result<Void> reject(@PathVariable String id, @RequestParam(required = false) String remark) {
        commandService.reject(id, remark);
        return Result.ok();
    }

    @PutMapping("/{id}/cancel")
    @SaCheckPermission("crm:approval:list")
    public Result<Void> cancel(@PathVariable String id, @RequestParam(required = false) String remark) {
        commandService.cancel(id, remark);
        return Result.ok();
    }
}
