# RFC-039: Gateway Routes & Request Transformation

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 8
- **Dependencies**: RFC-006, RFC-038

## 背景

当前 mate-gateway 只有 Sa-Token 认证过滤（`SaTokenGatewayConfig`），没有定义任何路由规则。请求经过认证后无处可去——没有 `spring.cloud.gateway.routes` 配置，没有服务转发，没有 Header 透传。

本 RFC 补全 Gateway 的核心职责：**路由 + 转发 + Header 透传 + CORS + 访问日志**。

## 设计方案

### Change 1: 路由配置

在 application.yml 中添加 Spring Cloud Gateway 路由定义，使用 Nacos 服务发现 (`lb://`)。

File: `mate-gateway/src/main/resources/application.yml`（追加内容）

```yaml
spring:
  cloud:
    gateway:
      routes:
        # Auth service
        - id: mate-auth
          uri: lb://mate-auth
          predicates:
            - Path=/api/v1/auth/**
          filters:
            - StripPrefix=0

        # System service (user management)
        - id: mate-system
          uri: lb://mate-system
          predicates:
            - Path=/api/v1/users/**,/api/v1/departments/**
          filters:
            - StripPrefix=0

        # Admin service (RBAC, Dict, Config, Logs)
        - id: mate-admin
          uri: lb://mate-admin
          predicates:
            - Path=/api/v1/admin/**
          filters:
            - StripPrefix=0

        # Notice service
        - id: mate-notice
          uri: lb://mate-notice
          predicates:
            - Path=/api/v1/notices/**
          filters:
            - StripPrefix=0

      # Global defaults
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:5173"
              - "http://localhost:3000"
            allowed-methods: "*"
            allowed-headers: "*"
            allow-credentials: true
            max-age: 3600
```

### Change 2: Header 透传过滤器

从 Sa-Token 会话中提取用户信息，注入到转发请求的 Header 中，下游服务通过 Header 获取当前用户，无需再次查询 Sa-Token。

File: `mate-gateway/src/main/java/vip/mate/gateway/filter/HeaderRelayFilter.java`

```java
package vip.mate.gateway.filter;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Relay user context from Sa-Token session to downstream services via HTTP headers.
 * <p>
 * Downstream services can read these headers directly without coupling to Sa-Token.
 */
@Slf4j
@Component
public class HeaderRelayFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_NAME = "X-User-Name";
    private static final String HEADER_TENANT_ID = "X-Tenant-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        // Only relay if user is logged in
        if (StpUtil.isLogin()) {
            String userId = StpUtil.getLoginIdAsString();
            requestBuilder.header(HEADER_USER_ID, userId);

            // Username stored in session by mate-auth LoginCommand
            Object username = StpUtil.getSession().get("username");
            if (username != null) {
                requestBuilder.header(HEADER_USER_NAME, username.toString());
            }

            log.debug("[Gateway] Relaying user context: userId={}", userId);
        }

        // Relay tenant ID from client header (if present)
        String tenantId = exchange.getRequest().getHeaders().getFirst(HEADER_TENANT_ID);
        if (tenantId != null) {
            requestBuilder.header(HEADER_TENANT_ID, tenantId);
        }

        return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
    }

    @Override
    public int getOrder() {
        // Run after Sa-Token authentication filter but before routing
        return -90;
    }
}
```

### Change 3: 访问日志过滤器

记录每个请求的方法、路径、状态码和耗时，用于运维排查。

File: `mate-gateway/src/main/java/vip/mate/gateway/filter/RequestLogFilter.java`

```java
package vip.mate.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Access log filter — logs method, path, status, and elapsed time.
 */
@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long elapsed = System.currentTimeMillis() - start;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value() : 0;
            log.info("[Gateway] {} {} {} {}ms", method, path, status, elapsed);
        }));
    }

    @Override
    public int getOrder() {
        // Run first to capture full request lifecycle
        return -100;
    }
}
```

### Change 4: 安全 Header 清理

防止外部请求伪造内部 Header（如 X-User-Id），在认证前清除这些 Header。

File: `mate-gateway/src/main/java/vip/mate/gateway/filter/SecurityHeaderFilter.java`

```java
package vip.mate.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Strip internal headers from incoming requests to prevent spoofing.
 * These headers are set exclusively by {@link HeaderRelayFilter}.
 */
@Component
public class SecurityHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleaned = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Name");
                })
                .build();
        return chain.filter(exchange.mutate().request(cleaned).build());
    }

    @Override
    public int getOrder() {
        // Run before everything else
        return -200;
    }
}
```

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-gateway/src/main/resources/application.yml` | Modify | 添加路由规则 + CORS + 全局过滤器 |
| `mate-gateway/src/main/java/.../filter/HeaderRelayFilter.java` | New | 用户上下文 Header 透传 |
| `mate-gateway/src/main/java/.../filter/RequestLogFilter.java` | New | 访问日志 |
| `mate-gateway/src/main/java/.../filter/SecurityHeaderFilter.java` | New | 防 Header 伪造 |

## 验证方案

1. `mvn clean compile -pl mate-gateway -am` — 编译通过
2. 启动 Nacos + mate-auth + mate-system + mate-gateway
3. `curl http://localhost:9010/api/v1/auth/captcha` — 应转发到 mate-auth 并返回验证码
4. `curl http://localhost:9010/api/v1/users/1` — 未登录应返回 401
5. 登录获取 token 后：`curl -H "Authorization: <token>" http://localhost:9010/api/v1/users/1` — 应转发到 mate-system
6. 检查 mate-system 日志中应出现 `X-User-Id` Header
7. 检查 gateway 日志中应出现访问日志 `[Gateway] GET /api/v1/users/1 200 45ms`
8. 伪造测试：`curl -H "X-User-Id: hacker" http://localhost:9010/api/v1/users/1` — 应被 SecurityHeaderFilter 清除

## 注意事项

- **StripPrefix=0**：不去除路径前缀，下游服务保持 `/api/v1/xxx` 格式
- **CORS 配置**：开发环境允许 localhost:5173（Vite 默认端口），生产环境需通过 Nacos 覆盖
- **Filter 执行顺序**：SecurityHeaderFilter(-200) → RequestLogFilter(-100) → HeaderRelayFilter(-90) → SaTokenFilter → 路由转发
- **Gateway 是 WebFlux**：所有 Filter 使用 Reactor API，不能用 Servlet API
- **服务名对应**：路由的 `lb://mate-auth` 需要和各服务在 Nacos 注册的 `spring.application.name` 一致
