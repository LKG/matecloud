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
import vip.mate.system.admin.application.query.ILoginLogQueryService;
import vip.mate.system.admin.application.query.ILoginLogQueryService.LoginLogVO;
import vip.mate.system.admin.types.security.Perms;
import vip.mate.base.result.PageResult;
import vip.mate.base.result.Result;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/login-logs")
@RequiredArgsConstructor
@SaCheckLogin
public class LoginLogController {

    private final ILoginLogQueryService queryService;

    @GetMapping
    @SaCheckPermission(Perms.LOG_LIST)
    public Result<PageResult<LoginLogVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(queryService.page(pageNum, pageSize, username, status, loginType, startTime, endTime));
    }

    @GetMapping("/{id}")
    @SaCheckPermission(Perms.LOG_LIST)
    public Result<LoginLogVO> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }
}
