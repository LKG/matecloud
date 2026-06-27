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
import vip.mate.system.admin.application.command.DictCommandService;
import vip.mate.system.admin.application.query.IDictQueryService;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

/**
 * Dict Data CRUD endpoints — the right pane of the frontend's
 * {@code views/admin/DictManager.vue} dual-pane layout. Always scoped to a
 * single {@code dictType} for paginated browsing.
 *
 * <p>Coexists with legacy {@link DictController} ({@code /api/v1/admin/dict})
 * which still owns POST/PUT/DELETE on plain {@code DictData} bodies. Frontend
 * uses these (plural-path) endpoints exclusively going forward.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/dict/data")
@RequiredArgsConstructor
@SaCheckLogin
public class DictDataController {

    private final DictCommandService commandService;
    private final IDictQueryService queryService;

    @GetMapping
    @SaCheckPermission(Perms.DICT_LIST)
    public Result<PageResult<DictData>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam String dictType,
            @RequestParam(required = false) String keyword) {
        return Result.ok(queryService.pageData(dictType, pageNum, pageSize, keyword));
    }

    @PostMapping
    @SaCheckPermission(Perms.DICT_ADD)
    @OperationLog(module = "字典数据", type = "新增")
    public Result<String> create(@RequestBody DictData body) {
        if (body.getStatus() == null) body.setStatus(1);
        return Result.ok(commandService.create(body));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.DICT_EDIT)
    @OperationLog(module = "字典数据", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody DictData body) {
        body.setId(id);
        commandService.update(body);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.DICT_DELETE)
    @OperationLog(module = "字典数据", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.delete(id);
        return Result.ok();
    }
}
