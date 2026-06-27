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
import vip.mate.system.admin.domain.dict.model.entity.DictType;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

/**
 * Dict Type CRUD endpoints — the left pane of the frontend's
 * {@code views/admin/DictManager.vue} dual-pane layout.
 *
 * Path is {@code /api/v1/admin/dict/types} (plural) so the legacy
 * {@code /api/v1/admin/dict/type/{code}} read-by-code endpoint kept on
 * {@link DictController} continues to work without conflict.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/dict/types")
@RequiredArgsConstructor
@SaCheckLogin
public class DictTypeController {

    private final DictCommandService commandService;
    private final IDictQueryService queryService;

    @GetMapping
    @SaCheckPermission(Perms.DICT_LIST)
    public Result<PageResult<DictType>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.ok(queryService.pageTypes(pageNum, pageSize, keyword));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.DICT_LIST)
    public Result<DictType> getById(@PathVariable String id) {
        return Result.ok(queryService.findTypeById(id));
    }

    @PostMapping
    @SaCheckPermission(Perms.DICT_ADD)
    @OperationLog(module = "字典类型", type = "新增")
    public Result<String> create(@RequestBody DictTypeReq req) {
        DictType type = DictType.builder()
                .dictType(req.dictType())
                .dictName(req.dictName())
                .status(req.status() == null ? 1 : req.status())
                .remark(req.remark())
                .build();
        return Result.ok(commandService.createType(type));
    }

    @PutMapping("/{id}")
    @SaCheckPermission(Perms.DICT_EDIT)
    @OperationLog(module = "字典类型", type = "修改")
    public Result<Void> update(@PathVariable String id, @RequestBody DictTypeReq req) {
        DictType patch = DictType.builder()
                .dictName(req.dictName())
                .remark(req.remark())
                .status(req.status())
                .build();
        commandService.updateType(id, patch);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.DICT_DELETE)
    @OperationLog(module = "字典类型", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteType(id);
        return Result.ok();
    }

    /** Both create and update accept the same body; on update {@code dictType} is ignored. */
    record DictTypeReq(String dictType, String dictName, Integer status, String remark) {}
}
