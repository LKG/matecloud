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
import vip.mate.crm.trade.application.command.ContractCommand;
import vip.mate.crm.trade.application.command.ContractCommandService;
import vip.mate.crm.trade.application.query.ContractQueryService;
import vip.mate.crm.trade.domain.model.entity.Contract;

/**
 * 合同 REST 入口(/api/v1/crm/contracts): 草稿/赢单转合同/提交审批/归档。
 *
 * <p>审批通过/驳回回写由审批中心(/api/v1/crm/approvals)驱动, 此处不直接暴露。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/contracts")
@RequiredArgsConstructor
@SaCheckLogin
public class ContractController {

    private final ContractCommandService commandService;
    private final ContractQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:contract:list")
    public Result<PageResult<Contract>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, status, customerId, mineOnly, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:contract:list")
    public Result<ContractQueryService.ContractDetail> detail(@PathVariable String id) {
        return Result.ok(queryService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("crm:contract:add")
    public Result<String> create(@RequestBody ContractCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    /** 赢单商机转合同草稿。 */
    @PostMapping("/from-business/{businessId}")
    @SaCheckPermission("crm:contract:add")
    public Result<String> fromBusiness(@PathVariable String businessId) {
        return Result.ok(commandService.createFromBusiness(businessId));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("crm:contract:edit")
    public Result<Void> update(@PathVariable String id, @RequestBody ContractCommand cmd) {
        commandService.update(id, cmd);
        return Result.ok();
    }

    @PutMapping("/{id}/submit")
    @SaCheckPermission("crm:contract:submit")
    public Result<Void> submit(@PathVariable String id) {
        commandService.submit(id);
        return Result.ok();
    }

    @PutMapping("/{id}/archive")
    @SaCheckPermission("crm:contract:archive")
    public Result<Void> archive(@PathVariable String id) {
        commandService.archive(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:contract:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
