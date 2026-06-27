# RFC-017: Sentinel Circuit Breaker

| Field       | Value                              |
|-------------|------------------------------------|
| **RFC**     | 017                                |
| **Title**   | Sentinel Circuit Breaker           |
| **Status**  | Draft                              |
| **Created** | 2026-04-11                         |

## Summary

This RFC defines **mate-sentinel-starter**, a Spring Boot starter integrating Alibaba Sentinel for flow control, circuit breaking, and system adaptive protection. Rules are dynamically managed via Nacos, with built-in support for Dubbo service protection and a global exception handler for `BlockException`.

Key capabilities:
- Flow control (QPS/thread-based rate limiting)
- Circuit breaking (slow ratio, error ratio, error count)
- Dynamic rule push from Nacos (flow rules, degrade rules)
- Sentinel Dashboard connectivity
- Dubbo Sentinel filter integration
- GlobalExceptionHandler for `BlockException`

Reference: kaleido-ai/kaleido-common/kaleido-sentinel

---

## 1. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-sentinel-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Sentinel Starter - Flow control and circuit breaking with Nacos dynamic rules</description>

    <dependencies>
        <!-- Spring Cloud Alibaba Sentinel -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>

        <!-- Sentinel Nacos Datasource -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-sentinel-datasource</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-nacos</artifactId>
        </dependency>

        <!-- Sentinel Dubbo Adapter -->
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-apache-dubbo3-adapter</artifactId>
        </dependency>

        <!-- Actuator for health/metrics -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Spring Web (for exception handler) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- mate-base for Result type -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
    </dependencies>
</project>
```

---

## 2. Java Source Code

### 2.1 SentinelAutoConfiguration

```java
package vip.mate.starter.sentinel.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import vip.mate.starter.sentinel.handler.MateBlockExceptionHandler;

/**
 * Auto-configuration for mate-sentinel-starter.
 * <p>
 * Activates when Sentinel is on the classpath. Provides:
 * <ul>
 *   <li>Custom {@link BlockExceptionHandler} for unified error responses</li>
 *   <li>Sentinel rules are loaded from Nacos via Spring Cloud Alibaba Sentinel datasource config</li>
 * </ul>
 * </p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(BlockException.class)
public class SentinelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BlockExceptionHandler.class)
    public BlockExceptionHandler mateBlockExceptionHandler(ObjectMapper objectMapper) {
        log.info("[mate-sentinel] Registering MateBlockExceptionHandler");
        return new MateBlockExceptionHandler(objectMapper);
    }
}
```

### 2.2 MateBlockExceptionHandler

```java
package vip.mate.starter.sentinel.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified Sentinel BlockException handler for HTTP endpoints.
 * <p>
 * Returns structured JSON responses with appropriate HTTP status codes
 * based on the type of Sentinel block:
 * <ul>
 *   <li>FlowException      --> 429 Too Many Requests</li>
 *   <li>DegradeException    --> 503 Service Unavailable</li>
 *   <li>ParamFlowException  --> 429 Too Many Requests</li>
 *   <li>SystemBlockException--> 503 Service Unavailable</li>
 *   <li>AuthorityException  --> 403 Forbidden</li>
 * </ul>
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class MateBlockExceptionHandler implements BlockExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       BlockException ex) throws Exception {

        String requestUri = request.getRequestURI();
        int httpStatus;
        String code;
        String message;

        if (ex instanceof FlowException) {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS.value();
            code = "FLOW_LIMIT";
            message = "Request rate limit exceeded, please retry later";
            log.warn("[mate-sentinel] Flow limit triggered: uri={}, rule={}", requestUri, ex.getRule());
        } else if (ex instanceof DegradeException) {
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            code = "CIRCUIT_BREAK";
            message = "Service circuit breaker is open, please retry later";
            log.warn("[mate-sentinel] Circuit breaker triggered: uri={}, rule={}", requestUri, ex.getRule());
        } else if (ex instanceof ParamFlowException) {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS.value();
            code = "PARAM_FLOW_LIMIT";
            message = "Parameter-level rate limit exceeded";
            log.warn("[mate-sentinel] Param flow limit triggered: uri={}", requestUri);
        } else if (ex instanceof SystemBlockException) {
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            code = "SYSTEM_BLOCK";
            message = "System adaptive protection triggered";
            log.warn("[mate-sentinel] System block triggered: uri={}", requestUri);
        } else if (ex instanceof AuthorityException) {
            httpStatus = HttpStatus.FORBIDDEN.value();
            code = "AUTHORITY_BLOCK";
            message = "Request blocked by authority rule";
            log.warn("[mate-sentinel] Authority block triggered: uri={}", requestUri);
        } else {
            httpStatus = HttpStatus.TOO_MANY_REQUESTS.value();
            code = "BLOCKED";
            message = "Request blocked by Sentinel";
            log.warn("[mate-sentinel] Unknown block triggered: uri={}, type={}",
                    requestUri, ex.getClass().getSimpleName());
        }

        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", code);
        result.put("msg", message);
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
```

### 2.3 SentinelGlobalExceptionAdvice

```java
package vip.mate.starter.sentinel.handler;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler enhancement for Sentinel BlockException.
 * <p>
 * Catches BlockException thrown from @SentinelResource annotated methods
 * that don't have explicit blockHandler/fallback methods defined.
 * </p>
 */
@Slf4j
@Order(-1)
@RestControllerAdvice
@ConditionalOnClass(BlockException.class)
public class SentinelGlobalExceptionAdvice {

    @ExceptionHandler(BlockException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleBlockException(BlockException ex) {
        log.warn("[mate-sentinel] BlockException caught in global handler: rule={}, type={}",
                ex.getRule(), ex.getClass().getSimpleName());

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("timestamp", System.currentTimeMillis());

        if (ex instanceof FlowException) {
            result.put("code", "FLOW_LIMIT");
            result.put("msg", "Request rate limit exceeded");
        } else if (ex instanceof DegradeException) {
            result.put("code", "CIRCUIT_BREAK");
            result.put("msg", "Service circuit breaker is open");
        } else {
            result.put("code", "BLOCKED");
            result.put("msg", "Request blocked by Sentinel");
        }

        return result;
    }
}
```

### 2.4 SentinelDubboConfiguration

```java
package vip.mate.starter.sentinel.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Configuration for Sentinel Dubbo integration.
 * <p>
 * When sentinel-apache-dubbo3-adapter is on the classpath, Sentinel automatically
 * registers Dubbo filters for both provider and consumer sides. This configuration
 * class serves as a marker and logs the activation.
 * </p>
 * <p>
 * The Dubbo Sentinel filter provides:
 * <ul>
 *   <li>Provider-side: flow control and circuit breaking for incoming Dubbo RPC calls</li>
 *   <li>Consumer-side: circuit breaking for outgoing Dubbo RPC calls</li>
 * </ul>
 * </p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.csp.sentinel.adapter.dubbo3.SentinelDubboProviderFilter")
public class SentinelDubboConfiguration {

    public SentinelDubboConfiguration() {
        log.info("[mate-sentinel] Sentinel Dubbo3 adapter detected, provider/consumer filters active");
    }
}
```

### 2.5 AutoConfiguration.imports

**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**

```text
vip.mate.starter.sentinel.config.SentinelAutoConfiguration
vip.mate.starter.sentinel.config.SentinelDubboConfiguration
vip.mate.starter.sentinel.handler.SentinelGlobalExceptionAdvice
```

---

## 3. Nacos Rule Configuration

### 3.1 Flow Rules (Nacos dataId: `${spring.application.name}-flow-rules`)

```json
[
  {
    "resource": "/api/v1/order/create",
    "limitApp": "default",
    "grade": 1,
    "count": 100,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "/api/v1/user/info",
    "limitApp": "default",
    "grade": 1,
    "count": 500,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  },
  {
    "resource": "com.alibaba.dubbo.demo.DemoService:sayHello(java.lang.String)",
    "limitApp": "default",
    "grade": 1,
    "count": 200,
    "strategy": 0,
    "controlBehavior": 0,
    "clusterMode": false
  }
]
```

**Field reference:**
- `grade`: 0 = thread count, 1 = QPS
- `strategy`: 0 = direct, 1 = relate, 2 = chain
- `controlBehavior`: 0 = reject immediately, 1 = warm up, 2 = rate limiter queue

### 3.2 Degrade Rules (Nacos dataId: `${spring.application.name}-degrade-rules`)

```json
[
  {
    "resource": "/api/v1/order/create",
    "grade": 0,
    "count": 500,
    "timeWindow": 10,
    "minRequestAmount": 5,
    "statIntervalMs": 1000,
    "slowRatioThreshold": 0.5
  },
  {
    "resource": "/api/v1/payment/process",
    "grade": 1,
    "count": 0.5,
    "timeWindow": 30,
    "minRequestAmount": 10,
    "statIntervalMs": 1000
  },
  {
    "resource": "/api/v1/inventory/deduct",
    "grade": 2,
    "count": 5,
    "timeWindow": 20,
    "minRequestAmount": 5,
    "statIntervalMs": 1000
  }
]
```

**Field reference:**
- `grade`: 0 = slow request ratio, 1 = error ratio, 2 = error count
- `count`: threshold value (milliseconds for grade=0, ratio for grade=1, count for grade=2)
- `timeWindow`: circuit breaker recovery time in seconds
- `minRequestAmount`: minimum requests before circuit breaker activates

---

## 4. application.yml Configuration

```yaml
spring:
  cloud:
    sentinel:
      # Enable Sentinel
      enabled: true
      # Eager initialization (register at startup, not on first request)
      eager: true

      # Sentinel Dashboard connection
      transport:
        dashboard: localhost:8080
        port: 8719  # client-side port for dashboard communication

      # Nacos dynamic rule datasource
      datasource:
        # Flow control rules
        flow:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr:localhost:8848}
            namespace: ${spring.cloud.nacos.config.namespace:}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-flow-rules
            data-type: json
            rule-type: flow
        # Circuit breaker (degrade) rules
        degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr:localhost:8848}
            namespace: ${spring.cloud.nacos.config.namespace:}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-degrade-rules
            data-type: json
            rule-type: degrade
        # System adaptive protection rules (optional)
        system:
          nacos:
            server-addr: ${spring.cloud.nacos.server-addr:localhost:8848}
            namespace: ${spring.cloud.nacos.config.namespace:}
            group-id: SENTINEL_GROUP
            data-id: ${spring.application.name}-system-rules
            data-type: json
            rule-type: system

      # Dubbo integration (auto-activated when dubbo adapter is on classpath)
      filter:
        enabled: true

# Actuator endpoint for Sentinel metrics
management:
  endpoints:
    web:
      exposure:
        include: sentinel,health,info
```

---

## 5. Usage Examples

### 5.1 Annotation-based Protection

```java
package vip.mate.biz.order.trigger.http;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.base.result.Result;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;

    @PostMapping("/create")
    @SentinelResource(value = "orderCreate",
            blockHandler = "orderCreateBlockHandler",
            fallback = "orderCreateFallback")
    public Result<String> createOrder(@RequestBody CreateOrderCommand cmd) {
        return Result.success(orderCommandService.createOrder(cmd));
    }

    /**
     * Block handler: triggered when Sentinel flow/degrade rule activates
     */
    public Result<String> orderCreateBlockHandler(CreateOrderCommand cmd, BlockException ex) {
        return Result.error("FLOW_LIMIT", "Service is busy, please retry later");
    }

    /**
     * Fallback: triggered on any business exception
     */
    public Result<String> orderCreateFallback(CreateOrderCommand cmd, Throwable ex) {
        return Result.error("SERVICE_ERROR", "Order creation failed: " + ex.getMessage());
    }
}
```

### 5.2 Dubbo Service Protection (automatic)

```java
package vip.mate.biz.order.trigger.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.api.order.IRpcOrderService;
import vip.mate.base.result.Result;

/**
 * Sentinel Dubbo filter automatically protects this service.
 * Resource name format: interfaceName:methodName(paramTypes)
 * e.g., "vip.mate.api.order.IRpcOrderService:createOrder(CreateOrderCommand)"
 */
@DubboService(version = "1.0.0")
public class RpcOrderServiceImpl implements IRpcOrderService {

    @Override
    public Result<String> createOrder(CreateOrderCommand cmd) {
        // Sentinel flow control / circuit breaking applied automatically
        return Result.success("order-12345");
    }
}
```

### 5.3 Programmatic Sentinel (rare, for fine-grained control)

```java
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;

public class InventoryService {

    public void deductStock(Long productId, int quantity) {
        Entry entry = null;
        try {
            entry = SphU.entry("inventoryDeduct");
            // Business logic
            doDeductStock(productId, quantity);
        } catch (BlockException ex) {
            // Sentinel blocked
            throw new ServiceException("FLOW_LIMIT", "Inventory service is busy");
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }
}
```

---

## 6. Module Structure

```
mate-starters/
  mate-sentinel-starter/
    pom.xml
    src/main/java/
      vip/mate/starter/sentinel/
        config/
          SentinelAutoConfiguration.java
          SentinelDubboConfiguration.java
        handler/
          MateBlockExceptionHandler.java
          SentinelGlobalExceptionAdvice.java
    src/main/resources/
      META-INF/spring/
        org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 7. Design Notes

1. **Nacos as rule source**: Rules are pushed from Nacos, not persisted in Sentinel Dashboard. Dashboard is used for monitoring only. This ensures rules survive Dashboard restarts.

2. **Dubbo auto-integration**: The `sentinel-apache-dubbo3-adapter` automatically registers provider and consumer filters when on the classpath. No explicit filter configuration is needed in Dubbo properties.

3. **BlockExceptionHandler vs @ExceptionHandler**: The `MateBlockExceptionHandler` handles blocks at the Spring MVC filter level (before reaching controller). The `SentinelGlobalExceptionAdvice` catches `BlockException` thrown from `@SentinelResource` annotated methods.

4. **Eager initialization**: Setting `spring.cloud.sentinel.eager=true` ensures Sentinel registers resources at startup rather than lazily on first request, which provides protection from the very first request.

5. **Dashboard port**: The `transport.port` (default 8719) is the port the Sentinel client listens on for Dashboard communication. Each service instance on the same machine needs a unique port (Sentinel auto-increments if occupied).
