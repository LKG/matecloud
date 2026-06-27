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
package vip.mate.starter.tenant.resolver;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolves tenant ID from the Sa-Token session.
 *
 * @author mateaix
 */
@Slf4j
public class TokenTenantResolver implements TenantResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        try {
            Object tenantId = StpUtil.getSession().get("tenantId");
            return tenantId != null ? tenantId.toString() : null;
        } catch (Exception e) {
            log.debug("Failed to resolve tenant from token: {}", e.getMessage());
            return null;
        }
    }
}
