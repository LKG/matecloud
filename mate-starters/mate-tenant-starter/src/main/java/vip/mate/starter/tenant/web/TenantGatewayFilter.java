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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.starter.tenant.core.TenantId;
import vip.mate.starter.tenant.core.TenantProperties;

/**
 * Spring Cloud Gateway GlobalFilter that resolves tenant ID and propagates
 * it downstream as a request header.
 *
 * @author mateaix
 */
@Slf4j
@RequiredArgsConstructor
public class TenantGatewayFilter implements GlobalFilter, Ordered {

    private final TenantProperties properties;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        for (String pattern : properties.getIgnoreUrls()) {
            if (path.startsWith(pattern)) {
                return chain.filter(exchange);
            }
        }

        String tenantId = resolveTenantId(request);
        if (tenantId == null || tenantId.isBlank()) {
            log.warn("Missing tenant ID for request: {}", path);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.setComplete();
        }
        if (!TenantId.isValid(tenantId)) {
            log.warn("Rejected malformed tenant id for {}: {}", path, tenantId);
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.setComplete();
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(properties.getHeaderName(), tenantId)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private String resolveTenantId(ServerHttpRequest request) {
        // In domain mode the subdomain is the authoritative source. Derive it
        // ONLY from the Host and never read a client-supplied header here —
        // otherwise a caller could override their subdomain's tenant by simply
        // sending X-Tenant-Id (a tenant-selection bypass).
        if ("domain".equals(properties.getResolver())) {
            String host = request.getHeaders().getFirst("Host");
            if (host != null) {
                String[] parts = host.split("\\.");
                if (parts.length >= 3) {
                    return parts[0];
                }
            }
            return null;
        }
        // header / token mode: the tenant header is expected to come from a
        // trusted upstream. At the gateway, SecurityHeaderFilter strips any
        // client-supplied X-Tenant-Id and HeaderRelayFilter re-injects it from
        // the authenticated Sa-Token session before this filter runs.
        String tenantId = request.getHeaders().getFirst(properties.getHeaderName());
        if (tenantId != null && !tenantId.isBlank()) {
            return tenantId;
        }
        return null;
    }
}
