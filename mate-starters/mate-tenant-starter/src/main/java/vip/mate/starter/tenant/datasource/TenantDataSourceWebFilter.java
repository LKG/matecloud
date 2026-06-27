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
package vip.mate.starter.tenant.datasource;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import vip.mate.starter.tenant.core.TenantContext;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.starter.tenant.web.TenantWebFilter;

import java.io.IOException;

/**
 * Servlet filter that routes the request to the tenant's datasource
 * (SCHEMA / DATASOURCE modes). Runs just after {@link TenantWebFilter}, so the
 * {@link TenantContext} is already populated.
 *
 * <p><b>Fail closed:</b> a normal (protected) request that arrives without a
 * tenant context is rejected with 400 — it must NOT silently fall back to the
 * master datasource (which holds tenant-management and other tenants' data).
 * The master datasource is reachable only via an {@code ignore-urls} path
 * (e.g. actuator/login) or programmatically through
 * {@link TenantDataSourceHelper#executeWithMaster}.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class TenantDataSourceWebFilter extends OncePerRequestFilter {

    private final TenantProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String dsName = resolveRoute(request.getRequestURI());
        if (dsName == null) {
            log.warn("Missing tenant context for datasource-routed request: {}",
                    request.getRequestURI());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing tenant context");
            return;
        }
        TenantDataSourceHelper.push(dsName);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantDataSourceHelper.poll();
        }
    }

    /**
     * Decide the datasource for a request URI. Returns the datasource name, or
     * {@code null} meaning "reject" (a protected path with no tenant context).
     * Package-private for unit testing.
     */
    String resolveRoute(String uri) {
        boolean ignored = isIgnored(uri);
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            // ignore-urls (actuator/login/...) may use master; protected paths fail closed.
            return ignored ? properties.getDefaultDsName() : null;
        }
        return TenantDataSourceHelper.resolveDsName(properties, tenantId);
    }

    private boolean isIgnored(String uri) {
        for (String pattern : properties.getIgnoreUrls()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }
}
