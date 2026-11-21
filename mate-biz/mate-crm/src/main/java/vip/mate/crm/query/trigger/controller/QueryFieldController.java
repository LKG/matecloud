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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.crm.query.application.QueryFieldService;
import vip.mate.crm.query.dto.QueryFieldMeta;

import java.util.List;

/**
 * 可查字段元数据 REST 入口(/api/v1/crm/query-fields)。
 *
 * <p>元数据非敏感(不含真实列名), 仅要求登录。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/query-fields")
@RequiredArgsConstructor
@SaCheckLogin
public class QueryFieldController {

    private final QueryFieldService queryFieldService;

    @GetMapping
    public Result<List<QueryFieldMeta>> list(@RequestParam String module) {
        return Result.ok(queryFieldService.list(module));
    }
}
