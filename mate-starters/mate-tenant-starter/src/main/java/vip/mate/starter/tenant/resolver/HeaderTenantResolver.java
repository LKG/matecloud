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

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import vip.mate.starter.tenant.core.TenantProperties;

/**
 * Resolves tenant ID from the HTTP header (default: X-Tenant-Id).
 *
 * @author mateaix
 */
@RequiredArgsConstructor
public class HeaderTenantResolver implements TenantResolver {

    private final TenantProperties properties;

    @Override
    public String resolve(HttpServletRequest request) {
        return request.getHeader(properties.getHeaderName());
    }
}
