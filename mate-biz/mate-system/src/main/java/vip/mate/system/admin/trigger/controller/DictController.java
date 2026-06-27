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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.system.admin.application.command.DictCommandService;
import vip.mate.system.admin.application.query.IDictQueryService;
import vip.mate.system.admin.domain.dict.model.entity.DictData;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.Result;

import java.util.List;

/**
 * Legacy dict endpoints — kept for backward compat with existing RPC consumers
 * (e.g. {@code /dict/type/{code}} read-by-code is queried at app startup to
 * cache enums). New frontend code uses {@link DictTypeController} +
 * {@link DictDataController} instead.
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/admin/dict")
@RequiredArgsConstructor
@SaCheckLogin
public class DictController {

    private final DictCommandService dictCommandService;
    private final IDictQueryService dictQueryService;

    @PostMapping
    @SaCheckPermission(Perms.DICT_ADD)
    @OperationLog(module = "字典", type = "新增")
    public Result<String> create(@RequestBody DictData dictData) {
        return Result.ok(dictCommandService.create(dictData));
    }

    @PutMapping
    @SaCheckPermission(Perms.DICT_EDIT)
    @OperationLog(module = "字典", type = "修改")
    public Result<Void> update(@RequestBody DictData dictData) {
        dictCommandService.update(dictData);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission(Perms.DICT_DELETE)
    @OperationLog(module = "字典", type = "删除")
    public Result<Void> delete(@PathVariable String id) {
        dictCommandService.delete(id);
        return Result.ok();
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.DICT_LIST)
    public Result<DictData> get(@PathVariable String id) {
        return Result.ok(dictQueryService.findById(id));
    }

    /**
     * Read-by-type. Public read for any logged-in user (no permission code) —
     * dropdowns / status badges in many pages depend on this. Admin-side
     * editing is gated on {@link DictDataController}.
     */
    @GetMapping("/type")
    public Result<List<DictData>> listByType(@RequestParam String dictType) {
        return Result.ok(dictQueryService.findByType(dictType));
    }

    @GetMapping("/label")
    public Result<String> getLabel(@RequestParam String dictType, @RequestParam String dictValue) {
        return Result.ok(dictQueryService.findLabel(dictType, dictValue));
    }
}
