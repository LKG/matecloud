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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.system.admin.application.command.MenuCommandService;
import vip.mate.system.admin.application.query.IMenuQueryService;
import vip.mate.system.admin.domain.permission.model.entity.Menu;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.Result;

import java.util.List;

/**
 * Menu / route / button-permission tree management.
 *
 * <p>Path: {@code /api/v1/admin/menus}.
 * <p>Menu rows have three types: directory ({@code D}), routable menu ({@code M}),
 * and button-level permission ({@code F}). The frontend's left sidebar renders
 * D + M only — F nodes are filtered out and used by {@code v-permission}
 * directives instead.
 *
 * @author MateCloud
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
@SaCheckLogin
public class MenuController {

    private final MenuCommandService commandService;
    private final IMenuQueryService queryService;

    @PostMapping
    @SaCheckPermission(Perms.MENU_ADD)
    @OperationLog(module = "菜单", type = "新增")
    public Result<String> create(@Valid @RequestBody CreateMenuReq req) {
        return Result.ok(commandService.createMenu(
                req.parentId(), req.name(), req.nameEn(), req.shortName(),
                req.path(), req.component(), req.perms(), req.type(), req.icon(), req.sort()));
    }

    /**
     * Menu tree — read by both the admin UI (menu management page) AND every
     * authenticated user (sidebar / dynamic routes). Therefore guarded only
     * by {@code @SaCheckLogin} from the class level, NOT by a permission
     * code: locking it down would break every user's sidebar.
     */
    @GetMapping("/tree")
    public Result<List<Menu>> tree() {
        return Result.ok(queryService.findTree());
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.MENU_EDIT)
    @OperationLog(module = "菜单", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateMenuReq req) {
        commandService.updateMenu(id, req.name(), req.nameEn(), req.shortName(),
                req.path(), req.component(), req.perms(), req.icon(), req.sort());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.MENU_DELETE)
    @OperationLog(module = "菜单", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteMenu(id);
        return Result.ok();
    }

    record CreateMenuReq(String parentId, @NotBlank String name, String nameEn, String shortName,
                         String path, String component, String perms, @NotBlank String type,
                         String icon, Integer sort) {}
    record UpdateMenuReq(String name, String nameEn, String shortName, String path, String component,
                         String perms, String icon, Integer sort) {}
}
