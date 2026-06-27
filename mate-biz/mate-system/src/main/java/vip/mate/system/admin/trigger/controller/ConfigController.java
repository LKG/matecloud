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
package vip.mate.system.admin.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.system.admin.application.command.ConfigCommandService;
import vip.mate.system.admin.application.query.IConfigQueryService;
import vip.mate.system.admin.application.query.IConfigQueryService.ConfigVO;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

/**
 * Runtime parameter / system config management. Holds key-value pairs that
 * are read by other modules at startup (and via {@code @Value("#{configValue('key')}")}
 * at runtime). Rows are flagged as built-in or user-editable; built-in rows
 * cannot be deleted via the UI.
 *
 * <p>Path: {@code /api/v1/admin/configs}.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
@SaCheckLogin
public class ConfigController {

    private final ConfigCommandService commandService;
    private final IConfigQueryService queryService;

    @PostMapping
    @SaCheckPermission(Perms.CONFIG_ADD)
    @OperationLog(module = "参数配置", type = "新增")
    public Result<String> create(@RequestBody CreateConfigReq req) {
        return Result.ok(commandService.createConfig(
                req.configKey(), req.configValue(), req.configName(), req.remark()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.CONFIG_LIST)
    public Result<ConfigVO> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    @GetMapping("/key/{configKey}")
    @SaCheckPermission(Perms.CONFIG_LIST)
    public Result<ConfigVO> getByKey(@PathVariable String configKey) {
        return Result.ok(queryService.findByKey(configKey));
    }

    /**
     * Config list endpoint.
     *
     * <p>Branches on the presence of {@code pageNum}:
     * <ul>
     *   <li>No params → returns the full {@code List<ConfigVO>} (used by RPC
     *       and bootstrap config caching).</li>
     *   <li>{@code pageNum} present → returns {@link PageResult} with optional
     *       {@code keyword} fuzzy match and {@code builtIn} filter.</li>
     * </ul>
     */
    @GetMapping
    @SaCheckPermission(Perms.CONFIG_LIST)
    public Result<?> list(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean builtIn) {
        if (pageNum == null) {
            return Result.ok(queryService.list());
        }
        int p = pageNum;
        int s = pageSize == null ? 10 : pageSize;
        return Result.ok(queryService.page(p, s, keyword, builtIn));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.CONFIG_EDIT)
    @OperationLog(module = "参数配置", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateConfigReq req) {
        commandService.updateConfig(id, req.configValue(), req.remark());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.CONFIG_DELETE)
    @OperationLog(module = "参数配置", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteConfig(id);
        return Result.ok();
    }

    record CreateConfigReq(String configKey, String configValue, String configName, String remark) {}
    record UpdateConfigReq(String configValue, String remark) {}
}
