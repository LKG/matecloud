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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import vip.mate.system.admin.application.command.OperationLogCommandService;
import vip.mate.system.admin.application.query.IOperationLogQueryService;
import vip.mate.system.admin.application.query.IOperationLogQueryService.OperationLogDetail;
import vip.mate.system.admin.application.query.IOperationLogQueryService.OperationLogVO;
import vip.mate.system.admin.trigger.annotation.OperationLog;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.Result;
import vip.mate.base.result.PageResult;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
@SaCheckLogin
public class OperationLogController {

    private final IOperationLogQueryService queryService;
    private final OperationLogCommandService commandService;

    @GetMapping
    @SaCheckPermission(Perms.LOG_LIST)
    public Result<PageResult<OperationLogVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(queryService.page(pageNum, pageSize, module, username,
                status, operationType, startTime, endTime));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.LOG_LIST)
    public Result<OperationLogDetail> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    /** Batch delete operation logs for cleanup. */
    @DeleteMapping
    @SaCheckPermission(Perms.LOG_DELETE)
    @OperationLog(module = "操作日志", type = "删除", description = "批量清理")
    public Result<Void> batchDelete(@RequestBody List<String> ids) {
        commandService.batchDelete(ids);
        return Result.ok();
    }
}
