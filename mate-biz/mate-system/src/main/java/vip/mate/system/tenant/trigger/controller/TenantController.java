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
package vip.mate.system.tenant.trigger.controller;

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
import vip.mate.system.tenant.application.command.TenantCommand;
import vip.mate.system.tenant.application.command.TenantCommandService;
import vip.mate.system.tenant.application.command.TenantPackageCommand;
import vip.mate.system.tenant.application.query.TenantQueryService;
import vip.mate.system.tenant.domain.model.entity.Tenant;
import vip.mate.system.tenant.domain.model.entity.TenantPackage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantCommandService commandService;
    private final TenantQueryService queryService;

    @GetMapping
    public Result<PageResult<Tenant>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return Result.ok(queryService.pageQuery(keyword, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Tenant> getById(@PathVariable String id) {
        Tenant tenant = queryService.findById(id);
        return tenant == null ? Result.fail("Tenant not found") : Result.ok(tenant);
    }

    @GetMapping("/by-code/{code}")
    public Result<Tenant> getByCode(@PathVariable String code) {
        Tenant tenant = queryService.findByCode(code);
        return tenant == null ? Result.fail("Tenant not found") : Result.ok(tenant);
    }

    @GetMapping("/packages")
    public Result<List<TenantPackage>> packages() {
        return Result.ok(queryService.listPackages());
    }

    @PostMapping("/packages")
    public Result<String> createPackage(@RequestBody TenantPackageCommand cmd) {
        return Result.ok(commandService.createPackage(cmd));
    }

    @PutMapping("/packages/{id}")
    public Result<Void> updatePackage(@PathVariable String id, @RequestBody TenantPackageCommand cmd) {
        cmd.setId(id);
        commandService.updatePackage(cmd);
        return Result.ok();
    }

    @DeleteMapping("/packages/{id}")
    public Result<Void> deletePackage(@PathVariable String id) {
        commandService.deletePackage(id);
        return Result.ok();
    }

    @PostMapping
    public Result<String> create(@RequestBody TenantCommand cmd) {
        return Result.ok(commandService.create(cmd));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody TenantCommand cmd) {
        cmd.setId(id);
        commandService.update(cmd);
        return Result.ok();
    }

    @PostMapping("/{id}/suspend")
    public Result<Void> suspend(@PathVariable String id) {
        commandService.suspend(id);
        return Result.ok();
    }

    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable String id) {
        commandService.activate(id);
        return Result.ok();
    }

    @PostMapping("/{id}/renew")
    public Result<Void> renew(@PathVariable String id,
                              @RequestParam("expireAt") long expireAtMillis) {
        commandService.renew(id,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(expireAtMillis), ZoneId.systemDefault()));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
