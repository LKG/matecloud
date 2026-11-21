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
package vip.mate.system.admin.trigger.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.StringUtils;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.menu.IRpcMenuRegistry;
import vip.mate.api.system.menu.MenuNode;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.menu.MenuRegistryService;

import java.util.List;

/**
 * Startup-time menu/permission self-registration endpoint (P1).
 *
 * <p>Thin Dubbo facade over {@link MenuRegistryService} — the adopt-chain
 * upsert logic lives in the application service so the monolith can reach the
 * exact same code path in-process (LocalMenuRegistryAdapter) without Dubbo.</p>
 *
 * @author mateaix
 */
@Slf4j
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM)
@RequiredArgsConstructor
public class RpcMenuRegistryServiceImpl implements IRpcMenuRegistry {

    private final MenuRegistryService menuRegistryService;

    @Override
    public Result<Integer> register(String moduleCode, List<MenuNode> menus) {
        if (!StringUtils.hasText(moduleCode)) {
            return Result.fail("moduleCode is required for menu registration");
        }
        try {
            return Result.ok(menuRegistryService.register(moduleCode, menus));
        } catch (Exception e) {
            log.error("[menu] module {} registration failed", moduleCode, e);
            return Result.fail("menu registration failed: " + e.getMessage());
        }
    }
}
