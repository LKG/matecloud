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
package vip.mate.starter.tenant.web;

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
import vip.mate.starter.tenant.core.TenantId;
import vip.mate.starter.tenant.core.TenantProperties;
import vip.mate.starter.tenant.resolver.TenantResolver;

import java.io.IOException;

/**
 * Servlet filter that sets the TenantContext for each incoming request
 * using the configured TenantResolver.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class TenantWebFilter extends OncePerRequestFilter {

    private final TenantResolver tenantResolver;
    private final TenantProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        for (String pattern : properties.getIgnoreUrls()) {
            if (pathMatcher.match(pattern, uri)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        try {
            String tenantId = tenantResolver.resolve(request);
            if (tenantId != null && !tenantId.isBlank()) {
                if (!TenantId.isValid(tenantId)) {
                    log.warn("Rejected malformed tenant id for {}: {}", uri, tenantId);
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant id");
                    return;
                }
                TenantContext.setTenantId(tenantId);
                log.debug("Tenant context set: {}", tenantId);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
