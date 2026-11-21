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
package vip.mate.crm.customer.trigger.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mate.base.result.Result;
import vip.mate.crm.customer.application.command.PoolConfigCommand;
import vip.mate.crm.customer.application.command.PoolConfigCommandService;
import vip.mate.crm.customer.domain.adapter.repository.IPoolConfigRepository;
import vip.mate.crm.customer.domain.model.entity.PoolConfig;

/**
 * 公海规则配置 REST 入口(/api/v1/crm/pool-config, 单行)。
 *
 * @author mateaix
 */
@RestController
@RequestMapping("/api/v1/crm/pool-config")
@RequiredArgsConstructor
@SaCheckLogin
public class PoolConfigController {

    private final IPoolConfigRepository poolConfigRepository;
    private final PoolConfigCommandService commandService;

    @GetMapping
    @SaCheckPermission("crm:pool:config")
    public Result<PoolConfig> get() {
        return Result.ok(poolConfigRepository.load());
    }

    @PutMapping
    @SaCheckPermission("crm:pool:config:edit")
    public Result<Void> save(@RequestBody PoolConfigCommand cmd) {
        commandService.save(cmd);
        return Result.ok();
    }
}
