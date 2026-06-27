# RFC-019: Dynamic Thread Pool

| Field       | Value                              |
|-------------|------------------------------------|
| **RFC**     | 019                                |
| **Title**   | Dynamic Thread Pool                |
| **Status**  | Draft                              |
| **Created** | 2026-04-11                         |

## Summary

This RFC defines **mate-dynamic-tp-starter**, a Spring Boot starter integrating DynamicTp (dynamic thread pool framework by Dromara) with Nacos-based configuration. Thread pool parameters (core size, max size, queue capacity, rejection policy) can be modified at runtime via Nacos config changes without restarting the application.

Key capabilities:
- Dynamic thread pool management via Nacos config
- Runtime parameter adjustment (no restart required)
- Built-in monitoring via Spring Boot Actuator
- Alarm notification support (configurable thresholds)
- `@EnableDynamicTp` auto-activation

Reference: kaleido-ai/kaleido-common/kaleido-dynamic-tp

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

    <artifactId>mate-dynamic-tp-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Dynamic Thread Pool Starter - Nacos-based dynamic thread pool management</description>

    <dependencies>
        <!-- DynamicTp Nacos Starter -->
        <dependency>
            <groupId>org.dromara.dynamictp</groupId>
            <artifactId>dynamic-tp-spring-boot-starter-nacos</artifactId>
            <version>1.2.2</version>
        </dependency>

        <!-- Spring Boot Actuator (for monitoring endpoints) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- mate-base -->
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

### 2.1 DynamicTpAutoConfiguration

```java
package vip.mate.starter.dynamictp.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.spring.annotation.EnableDynamicTp;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

/**
 * Auto-configuration for mate-dynamic-tp-starter.
 * <p>
 * Activates DynamicTp when the framework is on the classpath.
 * Thread pool definitions are managed via Nacos config (see Section 4).
 * </p>
 *
 * <p>
 * {@code @EnableDynamicTp} enables:
 * <ul>
 *   <li>Dynamic thread pool bean registration from Nacos config</li>
 *   <li>Runtime parameter refresh on Nacos config change</li>
 *   <li>Monitoring metrics collection</li>
 *   <li>Alarm notification (if configured)</li>
 * </ul>
 * </p>
 */
@Slf4j
@AutoConfiguration
@EnableDynamicTp
@ConditionalOnClass(name = "org.dromara.dynamictp.core.DtpRegistry")
public class DynamicTpAutoConfiguration {

    public DynamicTpAutoConfiguration() {
        log.info("[mate-dynamic-tp] DynamicTp auto-configuration activated");
    }
}
```

### 2.2 DynamicTpMonitorConfiguration

```java
package vip.mate.starter.dynamictp.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

import java.util.*;

/**
 * Actuator endpoint for DynamicTp thread pool metrics.
 * <p>
 * Exposes thread pool status at {@code /actuator/dynamic-tp}.
 * </p>
 */
@Slf4j
@AutoConfiguration
@ConditionalOnClass({DtpRegistry.class, Endpoint.class})
@Endpoint(id = "dynamic-tp")
public class DynamicTpMonitorConfiguration {

    @ReadOperation
    public Map<String, Object> threadPoolMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> pools = new ArrayList<>();

        try {
            Collection<DtpExecutor> executors = DtpRegistry.getAllExecutors();
            for (DtpExecutor executor : executors) {
                Map<String, Object> poolInfo = new LinkedHashMap<>();
                poolInfo.put("threadPoolName", executor.getThreadPoolName());
                poolInfo.put("corePoolSize", executor.getCorePoolSize());
                poolInfo.put("maximumPoolSize", executor.getMaximumPoolSize());
                poolInfo.put("activeCount", executor.getActiveCount());
                poolInfo.put("poolSize", executor.getPoolSize());
                poolInfo.put("queueSize", executor.getQueue().size());
                poolInfo.put("queueRemainingCapacity", executor.getQueue().remainingCapacity());
                poolInfo.put("completedTaskCount", executor.getCompletedTaskCount());
                poolInfo.put("taskCount", executor.getTaskCount());
                poolInfo.put("largestPoolSize", executor.getLargestPoolSize());
                pools.add(poolInfo);
            }
        } catch (Exception e) {
            log.warn("[mate-dynamic-tp] Failed to collect thread pool metrics", e);
            result.put("error", e.getMessage());
        }

        result.put("threadPools", pools);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
```

### 2.3 AutoConfiguration.imports

**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**

```text
vip.mate.starter.dynamictp.config.DynamicTpAutoConfiguration
vip.mate.starter.dynamictp.config.DynamicTpMonitorConfiguration
```

---

## 3. Module Structure

```
mate-starters/
  mate-dynamic-tp-starter/
    pom.xml
    src/main/java/
      vip/mate/starter/dynamictp/
        config/
          DynamicTpAutoConfiguration.java
          DynamicTpMonitorConfiguration.java
    src/main/resources/
      META-INF/spring/
        org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 4. Nacos Configuration

### 4.1 Thread Pool Definitions (Nacos dataId: `${spring.application.name}-dtp.yml`)

```yaml
# DynamicTp thread pool configuration
# Nacos dataId: mate-notice-dtp.yml (or mate-order-dtp.yml, etc.)
# Nacos group: DEFAULT_GROUP

spring:
  dynamic:
    tp:
      # Enable DynamicTp
      enabled: true
      # Enable banner
      enabledBanner: true
      # Collect interval (seconds)
      monitorInterval: 5

      executors:
        # Thread pool for notification retry
        - threadPoolName: noticeRetryExecutor
          executorType: common
          corePoolSize: 4
          maximumPoolSize: 16
          queueCapacity: 1000
          queueType: LinkedBlockingQueue
          rejectedHandlerType: CallerRunsPolicy
          keepAliveTime: 60
          allowCoreThreadTimeOut: false
          threadPoolAliasName: "Notice Retry Thread Pool"
          # Alarm thresholds
          notifyEnabled: true
          notifyItems:
            - type: capacity
              enabled: true
              threshold: 80   # alarm when queue usage > 80%
            - type: liveness
              enabled: true
              threshold: 80   # alarm when active/max > 80%
            - type: reject
              enabled: true
              threshold: 1    # alarm on any rejection

        # Thread pool for async business processing
        - threadPoolName: asyncBusinessExecutor
          executorType: common
          corePoolSize: 8
          maximumPoolSize: 32
          queueCapacity: 2000
          queueType: LinkedBlockingQueue
          rejectedHandlerType: CallerRunsPolicy
          keepAliveTime: 60
          allowCoreThreadTimeOut: false
          threadPoolAliasName: "Async Business Thread Pool"
          notifyEnabled: true
          notifyItems:
            - type: capacity
              enabled: true
              threshold: 80
            - type: liveness
              enabled: true
              threshold: 80

        # Thread pool for event publishing
        - threadPoolName: eventPublishExecutor
          executorType: common
          corePoolSize: 2
          maximumPoolSize: 8
          queueCapacity: 500
          queueType: LinkedBlockingQueue
          rejectedHandlerType: CallerRunsPolicy
          keepAliveTime: 60
          threadPoolAliasName: "Event Publish Thread Pool"
```

### 4.2 application.yml DynamicTp Config Reference

```yaml
# In business module's application.yml
spring:
  dynamic:
    tp:
      enabled: true
      # Nacos config for dynamic refresh
      nacos:
        dataId: ${spring.application.name}-dtp.yml
        group: DEFAULT_GROUP
      # Collect type: logging, micrometer, endpoint
      collectorTypes: micrometer,logging
      # Log interval (seconds)
      logPath: /tmp/logs/dynamic-tp
      monitorInterval: 5

# Actuator endpoints
management:
  endpoints:
    web:
      exposure:
        include: dynamic-tp,health,info,prometheus
```

---

## 5. Usage Examples

### 5.1 Inject and Use DtpExecutor

```java
package vip.mate.biz.order.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.dynamictp.core.executor.DtpExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventService {

    @Resource
    private DtpExecutor eventPublishExecutor;

    public void publishOrderCreatedEvent(String orderId) {
        CompletableFuture.runAsync(() -> {
            log.info("Publishing order created event: {}", orderId);
            // Send to MQ, notify downstream services, etc.
        }, eventPublishExecutor);
    }
}
```

### 5.2 Use ThreadPoolExecutor (standard JDK type)

```java
package vip.mate.biz.notice.trigger.job;

import jakarta.annotation.Resource;
import java.util.concurrent.ThreadPoolExecutor;

public class NoticeRetryJob {

    // DynamicTp registers beans as ThreadPoolExecutor type
    @Resource
    private ThreadPoolExecutor noticeRetryExecutor;

    public void process() {
        // Submit tasks to the dynamic thread pool
        noticeRetryExecutor.submit(() -> {
            // retry logic
        });
    }
}
```

### 5.3 Programmatic Access via DtpRegistry

```java
import org.dromara.dynamictp.core.DtpRegistry;
import org.dromara.dynamictp.core.executor.DtpExecutor;

public class ThreadPoolStatusService {

    public void logStatus() {
        DtpExecutor executor = (DtpExecutor) DtpRegistry.getExecutor("noticeRetryExecutor");
        log.info("Pool status: core={}, max={}, active={}, queueSize={}, completed={}",
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());
    }
}
```

### 5.4 Dynamic Adjustment (No Code Change)

To adjust thread pool parameters at runtime, simply update the Nacos config:

```yaml
# Change in Nacos: mate-notice-dtp.yml
# e.g., increase noticeRetryExecutor capacity during peak hours
executors:
  - threadPoolName: noticeRetryExecutor
    corePoolSize: 8        # was 4
    maximumPoolSize: 32     # was 16
    queueCapacity: 2000     # was 1000
```

DynamicTp detects the Nacos config change and adjusts the thread pool in real-time. No restart needed.

---

## 6. Monitoring

### 6.1 Actuator Endpoint

```
GET /actuator/dynamic-tp
```

Response:

```json
{
  "threadPools": [
    {
      "threadPoolName": "noticeRetryExecutor",
      "corePoolSize": 4,
      "maximumPoolSize": 16,
      "activeCount": 2,
      "poolSize": 4,
      "queueSize": 15,
      "queueRemainingCapacity": 985,
      "completedTaskCount": 1234,
      "taskCount": 1249,
      "largestPoolSize": 8
    },
    {
      "threadPoolName": "asyncBusinessExecutor",
      "corePoolSize": 8,
      "maximumPoolSize": 32,
      "activeCount": 5,
      "poolSize": 8,
      "queueSize": 3,
      "queueRemainingCapacity": 1997,
      "completedTaskCount": 5678,
      "taskCount": 5686,
      "largestPoolSize": 12
    }
  ],
  "timestamp": 1712835600000
}
```

### 6.2 Prometheus Metrics

When using `collectorTypes: micrometer`, DynamicTp exports metrics to Prometheus via Micrometer:

- `dynamic_tp_core_pool_size{tp_name="noticeRetryExecutor"}`
- `dynamic_tp_maximum_pool_size{tp_name="noticeRetryExecutor"}`
- `dynamic_tp_active_count{tp_name="noticeRetryExecutor"}`
- `dynamic_tp_queue_size{tp_name="noticeRetryExecutor"}`
- `dynamic_tp_completed_task_count{tp_name="noticeRetryExecutor"}`
- `dynamic_tp_reject_count{tp_name="noticeRetryExecutor"}`

These can be scraped by Prometheus and visualized in Grafana dashboards.

---

## 7. Design Notes

1. **Nacos-driven**: Thread pool definitions live in Nacos, not in application.yml. This enables runtime adjustment without redeployment. The `dynamic-tp-spring-boot-starter-nacos` dependency handles Nacos listener registration.

2. **Bean registration**: DynamicTp registers each configured thread pool as a Spring bean of type `DtpExecutor` (which extends `ThreadPoolExecutor`). You can inject them via `@Resource` using the `threadPoolName` as the bean name.

3. **Alarm support**: DynamicTp supports alarm notifications when queue capacity, liveness, or rejection thresholds are breached. Configure notify platforms (DingTalk, Lark, WeChat Work) in the Nacos config.

4. **Minimal starter code**: The kaleido reference pattern is followed -- the auto-configuration class simply adds `@EnableDynamicTp`. All complexity is in the framework; the starter is a thin wrapper ensuring it activates in the Spring Boot auto-configuration lifecycle.

5. **Actuator integration**: The custom endpoint (`/actuator/dynamic-tp`) provides a unified view of all managed thread pools. Combined with Prometheus metrics, this enables comprehensive observability.
