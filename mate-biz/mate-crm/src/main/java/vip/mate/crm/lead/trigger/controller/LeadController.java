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
package vip.mate.crm.lead.trigger.controller;

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
import vip.mate.crm.lead.application.command.LeadCommand;
import vip.mate.crm.lead.application.command.LeadCommandService;
import vip.mate.crm.lead.application.query.ILeadQueryService;
import vip.mate.crm.lead.domain.model.entity.Lead;

import java.time.LocalDateTime;

/**
 * 线索 REST 入口(/api/v1/crm/leads)。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/leads")
@RequiredArgsConstructor
@SaCheckLogin
public class LeadController {

    private final LeadCommandService commandService;
    private final ILeadQueryService queryService;

    @GetMapping
    @SaCheckPermission("crm:lead:list")
    public Result<PageResult<Lead>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Boolean mineOnly,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.page(keyword, status, source, mineOnly, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("crm:lead:list")
    public Result<Lead> getById(@PathVariable String id) {
        Lead lead = queryService.findById(id);
        return lead == null ? Result.fail("线索不存在") : Result.ok(lead);
    }

    @PostMapping
    @SaCheckPermission("crm:lead:add")
    public Result<String> create(@RequestBody LeadCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("crm:lead:edit")
    public Result<Void> update(@PathVariable String id, @RequestBody LeadCommand cmd) {
        commandService.update(id, cmd);
        return Result.ok();
    }

    @PostMapping("/{id}/convert")
    @SaCheckPermission("crm:lead:convert")
    public Result<String> convert(@PathVariable String id) {
        return Result.ok(commandService.convert(id));
    }

    @PostMapping("/{id}/discard")
    @SaCheckPermission("crm:lead:edit")
    public Result<Void> discard(@PathVariable String id) {
        commandService.discard(id);
        return Result.ok();
    }

    @PutMapping("/{id}/owner")
    @SaCheckPermission("crm:lead:edit")
    public Result<Void> changeOwner(@PathVariable String id, @RequestParam String ownerId,
                                    @RequestParam(required = false) String ownerName,
                                    @RequestParam(required = false) String deptId) {
        commandService.changeOwner(id, ownerId, ownerName, deptId);
        return Result.ok();
    }

    @PutMapping("/{id}/follow")
    @SaCheckPermission("crm:lead:edit")
    public Result<Void> follow(@PathVariable String id,
                               @RequestParam(required = false) LocalDateTime nextTime) {
        commandService.touchFollow(id, nextTime);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("crm:lead:delete")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
