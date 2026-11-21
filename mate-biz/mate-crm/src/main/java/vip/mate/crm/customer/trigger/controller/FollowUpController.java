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
package vip.mate.crm.customer.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;
import vip.mate.crm.customer.application.command.FollowUpCommand;
import vip.mate.crm.customer.application.command.FollowUpCommandService;
import vip.mate.crm.customer.application.query.FollowUpQueryService;
import vip.mate.crm.customer.domain.model.entity.FollowUp;
import vip.mate.crm.query.dto.AdvancedQuery;

/**
 * 跟进记录 REST 入口(/api/v1/crm/followups)。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/followups")
@RequiredArgsConstructor
@SaCheckLogin
public class FollowUpController {

    private final FollowUpCommandService commandService;
    private final FollowUpQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:followup:list")
    public Result<PageResult<FollowUp>> page(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(bizType, bizId, pageNum, pageSize));
    }

    @PostMapping("/search")
    @SaCheckPermission("crm:followup:list")
    public Result<PageResult<FollowUp>> search(@RequestBody AdvancedQuery query) {
        return Result.ok(queryService.search(query));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:followup:list")
    public Result<FollowUp> getById(@PathVariable String id) {
        FollowUp followUp = queryService.findById(id);
        return followUp == null ? Result.fail("跟进记录不存在") : Result.ok(followUp);
    }

    @PostMapping
    @SaCheckPermission("crm:followup:add")
    public Result<String> save(@RequestBody FollowUpCommand cmd) {
        return Result.ok(commandService.save(cmd));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:followup:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
