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
package vip.mate.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import vip.mate.base.constant.AuthHeaders;

@Component
public class SecurityHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // The gateway is the trust boundary: derive the client IP from the actual
        // edge connection and publish it as X-Real-IP, dropping any client-supplied
        // value. Backend services trust X-Real-IP (not the spoofable
        // X-Forwarded-For) for rate-limiting and audit. Behind an external L7 load
        // balancer, configure real-client-IP trust at that edge.
        String realIp = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : null;
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(h -> {
                    // 认证上下文头一律剥离 (防伪造) — 与 HeaderRelayFilter 出口注入共用同一份清单,
                    // 新增头进 AuthHeaders.ALL 即自动纳入剥离
                    AuthHeaders.ALL.forEach(h::remove);
                    // 网关内部签名头同样剥离: 外部不得自带 X-Gateway-Sign/Ts, 否则可尝试重放伪造
                    AuthHeaders.INTERNAL.forEach(h::remove);
                    h.remove("X-Real-IP");
                    if (realIp != null && !realIp.isBlank()) {
                        h.set("X-Real-IP", realIp);
                    }
                })
                .build();
        return chain.filter(exchange.mutate().request(cleaned).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
