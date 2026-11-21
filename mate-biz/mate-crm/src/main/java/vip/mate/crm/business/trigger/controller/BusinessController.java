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
package vip.mate.crm.business.trigger.controller;

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
import vip.mate.crm.business.application.command.BusinessCommand;
import vip.mate.crm.business.application.command.BusinessCommandService;
import vip.mate.crm.business.application.query.BusinessQueryService;
import vip.mate.crm.business.domain.model.entity.Business;
import vip.mate.crm.query.dto.AdvancedQuery;

/**
 * 商机 REST 入口(/api/v1/crm/businesses): 漏斗阶段推进 + 赢/输单。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/businesses")
@RequiredArgsConstructor
@SaCheckLogin
public class BusinessController {

    private final BusinessCommandService commandService;
    private final BusinessQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:business:list")
    public Result<PageResult<Business>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) Integer statusFlag,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, stage, customerId, statusFlag, mineOnly, pageNum, pageSize));
    }

    @PostMapping("/search")
    @SaCheckPermission("crm:business:list")
    public Result<PageResult<Business>> search(@RequestBody AdvancedQuery query) {
        return Result.ok(queryService.search(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:business:list")
    public Result<BusinessQueryService.BusinessDetail> detail(@PathVariable String id) {
        return Result.ok(queryService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("crm:business:add")
    public Result<String> create(@RequestBody BusinessCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("crm:business:edit")
    public Result<Void> update(@PathVariable String id, @RequestBody BusinessCommand cmd) {
        commandService.update(id, cmd);
        return Result.ok();
    }

    @PutMapping("/{id}/stage")
    @SaCheckPermission("crm:business:stage")
    public Result<Void> advance(@PathVariable String id,
                                @RequestParam(required = false) String stage) {
        if (stage == null || stage.isBlank()) {
            commandService.advance(id);
        } else {
            commandService.advanceTo(id, stage);
        }
        return Result.ok();
    }

    @PutMapping("/{id}/win")
    @SaCheckPermission("crm:business:stage")
    public Result<Void> win(@PathVariable String id, @RequestParam(required = false) String endRemark) {
        commandService.win(id, endRemark);
        return Result.ok();
    }

    @PutMapping("/{id}/lose")
    @SaCheckPermission("crm:business:stage")
    public Result<Void> lose(@PathVariable String id, @RequestParam(required = false) String endRemark) {
        commandService.lose(id, endRemark);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:business:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
