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
package vip.mate.gateway.config;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Selects the correct auth strategy per request path.
 *
 * @author mateaix
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicStrategyFactory {

    private final AuthStrategyConfig authStrategyConfig;

    public void applyStrategies(SaReactorFilter filter) {
        filter.addExclude(authStrategyConfig.getPublicPaths().toArray(new String[0]));

        filter.setAuth(obj -> {
            SaRouter.match(authStrategyConfig.getAdminPaths())
                    .check(r -> {
                        StpUtil.checkLogin();
                        StpUtil.checkRole("ROLE_ADMIN");
                    });

            SaRouter.match(authStrategyConfig.getLoginPaths())
                    .check(r -> StpUtil.checkLogin());
        });

        log.info("[Gateway] Auth strategies applied - public: {}, login: {}, admin: {}",
                authStrategyConfig.getPublicPaths().size(),
                authStrategyConfig.getLoginPaths().size(),
                authStrategyConfig.getAdminPaths().size());
    }
}
