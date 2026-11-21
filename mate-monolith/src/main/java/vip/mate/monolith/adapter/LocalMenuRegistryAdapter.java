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
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.system.menu.IRpcMenuRegistry;
import vip.mate.api.system.menu.MenuNode;
import vip.mate.base.result.Result;
import vip.mate.system.admin.application.menu.MenuRegistryService;

import java.util.List;

/**
 * In-process implementation of {@link IRpcMenuRegistry} for monolith mode.
 * <p>
 * Dubbo is switched off in one-JVM wiring, so mate-menu-starter's
 * {@code MenuAutoRegistrar} finds no Dubbo reference; it resolves this bean
 * through {@code ObjectProvider} instead and delegates to mate-system's
 * {@link MenuRegistryService} — the exact same adopt-chain upsert the Dubbo
 * facade ({@code RpcMenuRegistryServiceImpl}) runs in microservice mode.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalMenuRegistryAdapter implements IRpcMenuRegistry {

    private final MenuRegistryService menuRegistryService;

    @Override
    public Result<Integer> register(String moduleCode, List<MenuNode> menus) {
        try {
            return Result.ok(menuRegistryService.register(moduleCode, menus));
        } catch (Exception e) {
            log.error("[menu] in-process registration failed for module {}", moduleCode, e);
            return Result.fail("menu registration failed: " + e.getMessage());
        }
    }
}
