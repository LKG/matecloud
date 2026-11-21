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
package vip.mate.crm.query.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.crm.query.application.SavedFilterService;
import vip.mate.crm.query.dto.SavedFilterRequest;
import vip.mate.crm.query.dto.SavedFilterVO;

import java.util.List;

/**
 * 个人常用筛选 REST 入口(/api/v1/crm/saved-filters), 按登录用户隔离。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/saved-filters")
@RequiredArgsConstructor
@SaCheckLogin
public class SavedFilterController {

    private final SavedFilterService savedFilterService;

    @GetMapping
    public Result<List<SavedFilterVO>> list(@RequestParam(required = false) String module) {
        return Result.ok(savedFilterService.list(module));
    }

    @PostMapping
    public Result<String> save(@RequestBody SavedFilterRequest request) {
        return Result.ok(savedFilterService.save(request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        savedFilterService.delete(id);
        return Result.ok();
    }
}
