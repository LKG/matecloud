# RFC-045: 微服务 + 单体双模架构 — 一套代码，两种部署

- **Status**: Implemented (2026-06-28) — 见文末「实施记录」
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 10
- **Dependencies**: RFC-038, RFC-039

> ⚠️ 本文 Part 1–3 是最初设计草案，部分已过时（端口为 `8080` 非 `9000`；`mate-admin` 已并入
> `mate-system`；本地适配器为 4 个非 2 个）。**落地的真实方案与若干草案未预见的坑，见文末
> [实施记录](#实施记录-2026-06-28)。**

> "同一个代码库，`mate.rpc.mode=local` 启动一个 JAR 就是单体，`dubbo` 启动五个进程就是微服务。"

## 背景

MateCloud 当前是纯微服务架构——5 个独立进程、Nacos 服务发现、Dubbo RPC 通信。对于中小团队或开发阶段，部署 5 个进程 + Nacos + Redis 门槛过高。

**目标**：同一套代码，通过一个属性 `mate.rpc.mode=dubbo|local` 切换微服务和单体模式，零代码分支。

### 现状分析

| 维度 | 现状 | 单体可行性 |
|------|------|-----------|
| 服务间通信 | Dubbo RPC（仅 auth→system 2 个调用） | ✅ 替换为本地调用 |
| 数据库 | 所有服务共享同一个 MySQL | ✅ 零迁移 |
| 包名 | `vip.mate.auth/system/admin/notice` 完全隔离 | ✅ 无冲突 |
| 事件 | Spring ApplicationEventPublisher（进程内） | ✅ 直接可用 |
| Gateway | WebFlux（与 Servlet 不兼容） | ❌ 必须独立 |

### 架构决策

1. **单体 = auth + system + admin + notice 合并为一个 JAR**（Servlet）
2. **Gateway 始终独立**（WebFlux 与 Servlet 是 Spring Boot 硬约束，无法共存）
3. **切换靠属性不靠 Profile**：`mate.rpc.mode=dubbo|local`
4. **DDD 端口模式驱动**：利用现有 Port 接口，新增 Local 适配器

## 设计方案

### Part 1: 切换机制 — `mate.rpc.mode`

#### Change 1: mate-defaults.yml 新增属性

File: `mate-common/mate-base/src/main/resources/mate-defaults.yml`（追加）

```yaml
# ---- RPC Mode ----
# dubbo = microservice mode (Dubbo + Nacos, default)
# local = monolith mode (in-process calls, no Nacos/Dubbo needed)
mate:
  rpc:
    mode: ${MATE_RPC_MODE:dubbo}
```

#### Change 2: RpcAutoConfiguration 条件化

File: `mate-starters/mate-rpc-starter/src/main/java/vip/mate/starter/rpc/config/RpcAutoConfiguration.java`

```java
package vip.mate.starter.rpc.config;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Auto-configuration for Dubbo RPC.
 * Skipped entirely when mate.rpc.mode=local (monolith mode).
 */
@AutoConfiguration
@EnableDubbo
@ConditionalOnClass(name = "org.apache.dubbo.config.spring.context.annotation.EnableDubbo")
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class RpcAutoConfiguration {
}
```

#### Change 3: Dubbo 适配器条件化

File: `mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/user/DubboUserQueryAdapter.java`

在现有 `@Component` 前增加一行：

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboUserQueryAdapter implements UserQueryPort {
    // ... 现有代码不变
}
```

File: `mate-auth/src/main/java/vip/mate/auth/infrastructure/adapter/user/DubboUserRegistrationAdapter.java`

同样增加一行：

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "dubbo", matchIfMissing = true)
public class DubboUserRegistrationAdapter implements UserRegistrationPort {
    // ... 现有代码不变
}
```

#### Change 4: 移除各服务 Application 类的 @EnableDubbo

`@EnableDubbo` 已由 `RpcAutoConfiguration` 统一提供，各服务 Application 类上的是冗余的。移除后，`mate.rpc.mode=local` 时 `RpcAutoConfiguration` 不激活，Dubbo 就彻底不启动。

修改以下 4 个文件，删除 `@EnableDubbo` 注解：

- `mate-auth/src/.../MateAuthApplication.java`
- `mate-biz/mate-system/src/.../MateSystemApplication.java`
- `mate-admin/src/.../MateAdminApplication.java`
- `mate-biz/mate-notice/src/.../MateNoticeApplication.java`

示例（MateSystemApplication）：

```java
package vip.mate.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "vip.mate.system")
@EnableDiscoveryClient
@EnableAsync
public class MateSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(MateSystemApplication.class, args);
    }
}
```

> 微服务模式不受影响：`RpcAutoConfiguration` 的 `matchIfMissing = true` 保证默认行为不变。

---

### Part 2: mate-monolith 模块

#### Change 5: mate-monolith/pom.xml

File: `mate-monolith/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>matecloud</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-monolith</artifactId>
    <packaging>jar</packaging>
    <name>mate-monolith</name>
    <description>MateCloud Monolith - all services in one JAR</description>

    <dependencies>
        <!-- Four business services as libraries -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-auth</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-system</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-admin</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-notice</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Change 6: MateMonolithApplication

File: `mate-monolith/src/main/java/vip/mate/monolith/MateMonolithApplication.java`

```java
package vip.mate.monolith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Monolith entry point — all business services in one JVM.
 * <p>
 * Gateway remains separate (WebFlux ≠ Servlet).
 * Set {@code mate.rpc.mode=local} to use in-process calls instead of Dubbo RPC.
 */
@SpringBootApplication(scanBasePackages = {
        "vip.mate.auth",
        "vip.mate.system",
        "vip.mate.admin",
        "vip.mate.notice",
        "vip.mate.monolith"
})
@EnableAsync
public class MateMonolithApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateMonolithApplication.class, args);
    }
}
```

#### Change 7: LocalUserQueryAdapter — 本地用户查询

File: `mate-monolith/src/main/java/vip/mate/monolith/adapter/LocalUserQueryAdapter.java`

```java
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserQueryPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.auth.domain.model.valobj.Account;
import vip.mate.system.application.query.IUserQueryService;

/**
 * Monolith-mode implementation of {@link UserQueryPort}.
 * Calls mate-system's query service directly in-process — no Dubbo, no network.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalUserQueryAdapter implements UserQueryPort {

    private final IUserQueryService userQueryService;

    @Override
    public AuthUser findByAccount(Account account) {
        UserInfoResponse info = switch (account.kind()) {
            case USERNAME -> userQueryService.findByUsername(account.value());
            case MOBILE   -> userQueryService.findByMobileForAuth(account.value());
            case EMAIL    -> userQueryService.findByUsername(account.value());
        };
        return info != null ? AuthUser.from(info) : null;
    }

    @Override
    public AuthUser findById(String userId) {
        UserInfoResponse info = userQueryService.findById(userId);
        return info != null ? AuthUser.from(info) : null;
    }
}
```

> 对比 `DubboUserQueryAdapter`：去掉了 `Result<>` 包装/解包、Dubbo 异常处理、`safeCall` 包装。直接方法调用，零 overhead。

#### Change 8: LocalUserRegistrationAdapter — 本地用户注册

File: `mate-monolith/src/main/java/vip/mate/monolith/adapter/LocalUserRegistrationAdapter.java`

```java
package vip.mate.monolith.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.auth.domain.adapter.port.UserRegistrationPort;
import vip.mate.auth.domain.model.aggregate.AuthUser;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;

/**
 * Monolith-mode implementation of {@link UserRegistrationPort}.
 * Creates a user via mate-system's command service directly.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class LocalUserRegistrationAdapter implements UserRegistrationPort {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    @Override
    public AuthUser register(RegisterUserCommand command) {
        String userId = userCommandService.createUser(command);
        UserInfoResponse info = userQueryService.findById(userId);
        return AuthUser.from(info);
    }
}
```

#### Change 9: MonolithSecurityConfig — Sa-Token 认证拦截

单体模式没有 Gateway，需要在 Servlet 层做认证拦截（替代 Gateway 的 `SaReactorFilter`）。

File: `mate-monolith/src/main/java/vip/mate/monolith/config/MonolithSecurityConfig.java`

```java
package vip.mate.monolith.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token authentication for monolith mode.
 * Replaces the gateway's SaReactorFilter with a Servlet interceptor.
 * Reads the same path configuration as the gateway.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "mate.rpc.mode", havingValue = "local")
public class MonolithSecurityConfig implements WebMvcConfigurer {

    private final MonolithAuthProperties authProperties;

    public MonolithSecurityConfig(MonolithAuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // Public paths: no auth required
            SaRouter.match("/**")
                    .notMatch(authProperties.getPublicPaths().toArray(String[]::new))
                    .check(r -> StpUtil.checkLogin());

            // Admin paths: require admin role
            SaRouter.match(authProperties.getAdminPaths().toArray(String[]::new))
                    .check(r -> StpUtil.checkRole("admin"));

        })).addPathPatterns("/api/v1/**");

        log.info("[Monolith] Security interceptor registered. Public paths: {}",
                authProperties.getPublicPaths());
    }

    @Configuration
    @ConfigurationProperties(prefix = "mate.gateway.auth")
    public static class MonolithAuthProperties {
        private List<String> publicPaths = new ArrayList<>();
        private List<String> loginPaths = new ArrayList<>();
        private List<String> adminPaths = new ArrayList<>();

        public List<String> getPublicPaths() { return publicPaths; }
        public void setPublicPaths(List<String> publicPaths) { this.publicPaths = publicPaths; }
        public List<String> getLoginPaths() { return loginPaths; }
        public void setLoginPaths(List<String> loginPaths) { this.loginPaths = loginPaths; }
        public List<String> getAdminPaths() { return adminPaths; }
        public void setAdminPaths(List<String> adminPaths) { this.adminPaths = adminPaths; }
    }
}
```

#### Change 10: application.yml（单体配置）

File: `mate-monolith/src/main/resources/application.yml`

```yaml
# MateCloud Monolith Configuration
# All business services in one JVM, no Nacos/Dubbo required.

spring:
  application:
    name: mate-monolith
  config:
    import:
      - classpath:mate-defaults.yml
      # In monolith mode, datasource/redis config is inline (no Nacos)
  main:
    allow-bean-definition-overriding: true

  # Disable Nacos discovery and config
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false
    discovery:
      enabled: false

  # Datasource (override Nacos-sourced config)
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:matecloud}?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=GMT%2B8&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver

  # Redis
  data:
    redis:
      host: ${REDIS_HOST:127.0.0.1}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0

server:
  port: ${SERVER_PORT:9000}

# Monolith mode: in-process calls, no Dubbo
mate:
  rpc:
    mode: local

# Disable Dubbo entirely
dubbo:
  registry:
    address: N/A

# Reuse gateway auth path config for monolith security interceptor
mate.gateway.auth:
  public-paths:
    - /api/v1/auth/login
    - /api/v1/auth/sms/**
    - /api/v1/auth/captcha
    - /api/v1/auth/register
    - /actuator/**
  login-paths:
    - /api/v1/**
  admin-paths:
    - /api/v1/admin/admins/delete/**

logging:
  level:
    vip.mate: debug
```

---

### Part 3: Maven 双模构建

#### Change 11: Root pom.xml 新增模块和 Profile

File: `pom.xml`（root）

在 `<modules>` 中追加：

```xml
<module>mate-monolith</module>
```

新增 monolith profile：

```xml
<profiles>
    <profile>
        <id>monolith</id>
        <properties>
            <skip.spring-boot.repackage>true</skip.spring-boot.repackage>
        </properties>
    </profile>
</profiles>
```

#### Change 12: 各服务 pom.xml 支持 skip repackage

在 mate-auth、mate-admin、mate-biz/mate-system、mate-biz/mate-notice 的 `pom.xml` 中，修改 spring-boot-maven-plugin：

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <skip>${skip.spring-boot.repackage}</skip>
    </configuration>
</plugin>
```

在 root pom.xml 的 `<properties>` 中新增默认值：

```xml
<skip.spring-boot.repackage>false</skip.spring-boot.repackage>
```

这样：
- `mvn package`（默认）：每个服务独立打包为可执行 JAR
- `mvn package -Pmonolith -pl mate-monolith -am`：服务打包为普通 JAR，monolith 聚合为可执行 JAR

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-common/mate-base/src/.../mate-defaults.yml` | Modify | 新增 `mate.rpc.mode` 属性 |
| `mate-starters/mate-rpc-starter/.../RpcAutoConfiguration.java` | Modify | 加 `@ConditionalOnProperty` |
| `mate-auth/.../DubboUserQueryAdapter.java` | Modify | 加 `@ConditionalOnProperty` |
| `mate-auth/.../DubboUserRegistrationAdapter.java` | Modify | 加 `@ConditionalOnProperty` |
| `mate-auth/.../MateAuthApplication.java` | Modify | 删除 `@EnableDubbo` |
| `mate-biz/mate-system/.../MateSystemApplication.java` | Modify | 删除 `@EnableDubbo` |
| `mate-admin/.../MateAdminApplication.java` | Modify | 删除 `@EnableDubbo` |
| `mate-biz/mate-notice/.../MateNoticeApplication.java` | Modify | 删除 `@EnableDubbo` |
| `pom.xml` (root) | Modify | 新增 module + profile + property |
| `mate-auth/pom.xml` | Modify | spring-boot-plugin skip |
| `mate-admin/pom.xml` | Modify | spring-boot-plugin skip |
| `mate-biz/mate-system/pom.xml` | Modify | spring-boot-plugin skip |
| `mate-biz/mate-notice/pom.xml` | Modify | spring-boot-plugin skip |
| `mate-monolith/pom.xml` | New | 聚合依赖 |
| `mate-monolith/.../MateMonolithApplication.java` | New | 单体入口 |
| `mate-monolith/.../LocalUserQueryAdapter.java` | New | 本地用户查询 |
| `mate-monolith/.../LocalUserRegistrationAdapter.java` | New | 本地用户注册 |
| `mate-monolith/.../MonolithSecurityConfig.java` | New | Sa-Token 拦截器 |
| `mate-monolith/src/main/resources/application.yml` | New | 单体配置 |

## 验证方案

### 单体模式

1. `mvn clean compile -pl mate-monolith -am -Pmonolith` — 编译通过
2. `mvn clean package -pl mate-monolith -am -Pmonolith` — 打包成功
3. 启动：`java -jar mate-monolith/target/mate-monolith.jar`（不需要 Nacos）
4. `curl http://localhost:9000/api/v1/auth/captcha` — 返回验证码
5. 登录获取 token，访问 `/api/v1/admin/admins` — 正常返回
6. 创建用户 → 登录 → 一整套流程跑通
7. 检查日志无 Dubbo/Nacos 连接尝试

### 微服务模式（回归测试）

1. `mvn clean package`（不加 -Pmonolith）— 各服务独立打包
2. 启动 Nacos → 启动各服务 → Gateway 路由正常
3. 登录流程正常（auth 通过 Dubbo 调用 system）
4. 确认 `mate.rpc.mode` 默认值为 `dubbo`

## 注意事项

- **Gateway 不参与单体**：WebFlux 与 Servlet 是 Spring Boot 硬约束。单体模式下前端直连 `:9000`，或者 Gateway 独立部署做反向代理
- **`matchIfMissing = true`**：所有 `@ConditionalOnProperty` 都设了 `matchIfMissing = true`，确保不加配置时默认走微服务模式，完全向后兼容
- **Flyway 版本号**：单体模式下 4 个服务的迁移脚本在同一 classpath，需避免版本号冲突（admin: V1-V99, system: V100-V199, notice: V200-V299）
- **Bean 覆盖**：`allow-bean-definition-overriding: true` 应对可能的 bean 定义重复
- **只有 2 个 Local 适配器**：当前只有 auth→system 有 RPC 调用。如果未来新增跨服务 RPC，只需加对应的 Local 适配器
- **前端零改动**：API 路径完全一致，只改 base URL
- **这不是"微服务降级"**：单体模式是一种正式的部署形态，适合中小团队、开发环境、快速验证，不是临时方案

## 实施记录 (2026-06-28)

落地时发现草案漏掉了几个让单体「根本无法启动」的关键问题。最终方案如下，**全部不影响微服务模式**（共享 starter 的改动都用 `matchIfMissing=true` 保留 dubbo 默认行为，或仅在目标历史表为空时触发）。

### 1. 致命前提：业务模块必须能被当作「库」依赖（classifier）
`mate-auth/system/notice` 原本被 `spring-boot-maven-plugin` 打成可执行胖 JAR（类在 `BOOT-INF/classes/`），**无法作为 Maven 编译依赖** → 单体连编译都过不了。
- 方案：三个模块的 `spring-boot-maven-plugin` 加 `<classifier>exec</classifier>`。主 JAR 退回瘦库（单体可依赖），`*-exec.jar` 才是可运行胖包（微服务部署用）。
- 配套：根 `Dockerfile` 与三个服务 `Dockerfile` 改为优先取 `*-exec.jar`。

### 2. 配置优先级陷阱（`spring.config.import`）
被 `import` 进来的文件**优先级高于** importer 本身（这正是微服务里 Nacos `mate-infra` 能覆盖 `mate-defaults` 的机制）。所以单体写在 `application.yml` 顶层的覆盖项（`mate.rpc.mode=local`、关 Nacos）**全部被 `mate-defaults` 盖掉**，导致单体竟以 dubbo 模式启动、注册 Nacos、自调 Dubbo。
- 方案：把所有「覆盖 mate-defaults」的项放进**最后 import** 的 `mate-monolith/src/main/resources/mate-infra-local.yml`（它即单体版的「classpath mate-infra」），`application.yml` 只留入口与不冲突项。

### 3. 组合根：排除嵌套的 `@SpringBootApplication`
`scanBasePackages="vip.mate"` 会把三个服务的 `@SpringBootApplication` 当作 `@Configuration` 扫进来，重新激活它们的 `@EnableDiscoveryClient`/`@EnableAsync`。
- 方案：`MateMonolithApplication` 用显式 `@ComponentScan` + `excludeFilters` 排除这三个类（并保留 Boot 默认的两个 TypeExclude/AutoConfigurationExclude 过滤器）。

### 4. 单体专有的两处 Bean 冲突（多模块合一才暴露）
- **Mapper Bean 名冲突**：auth 与 system 各有一个 `LoginLogDao`，简单类名都叫 `loginLogDao` → 冲突。`DataSourceAutoConfiguration` 的 `@MapperScan` 改用 `FullyQualifiedAnnotationBeanNameGenerator`（按类型注入，对微服务透明）。
- **MyBatis TypeAlias 冲突**：两个 `LoginLogPO` 简单名相同 → 别名重复抛错。项目无任何 XML mapper，`setTypeAliasesPackage(...)` 是死配置 → 直接移除。

### 5. 消除重复：`RolePermissionResolverPort`
草案的「Local 适配器」会让 `LocalTokenIssuer` 与 `SaTokenIssuer` 90% 重复。改为抽出唯一随模式变化的「按用户查角色/权限」为端口：
- `SaTokenIssuer` 变为**两模式共用**的唯一 `TokenIssuerPort` 实现，只依赖该端口；
- `DubboRolePermissionResolver`（auth，dubbo）走 RPC + Redis 兜底；`LocalRolePermissionResolver`（monolith，local）直调 `IPermissionDomainService`。
- 本地适配器现共 4 个：`UserQuery` / `UserRegistration` / `NoticeDispatcher` / `RolePermissionResolver`。

### 6. 单体无需消息中间件（域事件走进程内）
真实域事件流本就是 Spring `ApplicationEventPublisher` + `@TransactionalEventListener`，RabbitMQ 那套（`DomainEventAutoConfiguration` 等）是零消费者的跨服务脚手架。
- 方案：`RabbitMqAutoConfiguration` / `DomainEventAutoConfiguration` 加 `@ConditionalOnProperty(mate.rpc.mode=dubbo, matchIfMissing=true)`；单体再 `spring.autoconfigure.exclude` 掉 Boot 的 `RabbitAutoConfiguration` → 不连 broker、`health: UP`。

### 7. 单体彻底关闭 Dubbo
`mate-defaults` 的 `dubbo.scan.base-packages` 会让 dubbo-spring-boot-autoconfigure 在无 `@EnableDubbo` 时仍扫描并导出 `@DubboService`。
- 方案：单体 `spring.autoconfigure.exclude` 掉 `DubboAutoConfiguration` / `DubboRelaxedBindingAutoConfiguration` / `DubboListenerAutoConfiguration`。

### 8. Flyway：一张历史表 + 「领养」既有 schema
单体所有迁移进单表 `flyway_history_monolith`（`mate.module.code=monolith`，各版本号全局唯一）。难点是**与微服务共用同一个库**时不能重复建表。
- 修复潜在 bug：`repairThenMigrate` 策略的 `@ConditionalOnClass(name="Flyway")` 用了非全限定名 → 条件永远 false、自愈逻辑从未生效。改为 `@ConditionalOnClass(Flyway.class)`。
- 扩展 `autoSeedPerServiceHistory`：除遗留单表外，还从**兄弟 `flyway_history_*` 表**把已应用记录播种进目标表（重排 `installed_rank`、跳过 baseline 伪记录、清理 `success=0` 残留）。于是单体首启会「领养」微服务已迁移的 schema → `No migration necessary`；全新库则照常跑全部迁移；二次启动幂等。

### 9. 构建 / 运行
- `mate-monolith/Dockerfile`（多阶段，`-Pmonolith` 构建）；`docker-compose.yml` 增加 `mate-monolith` 服务并置于 compose `profiles: [monolith]`（默认不随微服务栈启动）。
- `make monolith` / `run-monolith` / `monolith-up` / `monolith-down`。

### 部署注意
单体与微服务可共用同一个库（Flyway 自动领养），但**不要同时运行**两套写同一份数据。全新部署直接起单体即可；从微服务库切单体时，首启自动领养，无需手工 SQL。
