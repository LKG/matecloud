# RFC-038: Starter Consolidation Execution — 29 to 13

- **Status**: Done
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 8
- **Dependencies**: RFC-029 (Accepted)

## 背景

RFC-029 已被接受，决定将 29 个 starter 精简为 13 个（Tier 1: 7 个核心 + Tier 2: 6 个业务），高级 starter 移到 `mate-starters-contrib/`。

本 RFC 是 RFC-029 的**执行方案**，包含每一步的具体操作、代码变更和验证步骤。

**5 个合并 + 2 个删除 + 8 个移动 = 15 个操作。**

## 设计方案

### Change 1: 创建 mate-starters-contrib 父 POM

File: `mate-starters-contrib/pom.xml`

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

    <artifactId>mate-starters-contrib</artifactId>
    <packaging>pom</packaging>
    <name>mate-starters-contrib</name>
    <description>Advanced starters - use when needed, not by default</description>

    <modules>
        <module>mate-seata-starter</module>
        <module>mate-sharding-starter</module>
        <module>mate-sentinel-starter</module>
        <module>mate-gray-starter</module>
        <module>mate-flow-starter</module>
        <module>mate-rule-starter</module>
        <module>mate-ai-starter</module>
        <module>mate-test-starter</module>
    </modules>

</project>
```

### Change 2: 合并 mate-lock-starter → mate-cache-starter

**操作步骤：**

1. 移动 5 个 Java 文件到 mate-cache-starter，保持原包名 `vip.mate.starter.lock`：

```
mate-starters/mate-lock-starter/src/main/java/vip/mate/starter/lock/
├── DistributedLock.java            → mate-cache-starter/src/main/java/vip/mate/starter/lock/
├── DistributedLockAspect.java      → mate-cache-starter/src/main/java/vip/mate/starter/lock/
├── DistributedLockAutoConfiguration.java → mate-cache-starter/src/main/java/vip/mate/starter/lock/
├── DistributedLockException.java   → mate-cache-starter/src/main/java/vip/mate/starter/lock/
└── DistributedLockService.java     → mate-cache-starter/src/main/java/vip/mate/starter/lock/
```

2. 合并 auto-imports — 在 mate-cache-starter 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中增加一行：

```text
vip.mate.starter.cache.CacheAutoConfiguration
vip.mate.starter.lock.DistributedLockAutoConfiguration
```

3. mate-lock-starter 的 pom.xml 依赖 `mate-cache-starter` + `spring-boot-starter-aop`。合并后 mate-cache-starter 已经有 aop 依赖，无需额外变更。

4. 删除 `mate-starters/mate-lock-starter/` 目录。

5. 全局替换引用：

```xml
<!-- Before -->
<dependency>
    <groupId>vip.mate</groupId>
    <artifactId>mate-lock-starter</artifactId>
</dependency>

<!-- After: 删除上面的依赖，mate-cache-starter 已经包含 -->
```

### Change 3: 合并 mate-distribute-starter → mate-cache-starter

**操作步骤：**

1. 移动 3 个 Java 文件，保持原包名 `vip.mate.starter.distribute`：

```
mate-starters/mate-distribute-starter/src/main/java/vip/mate/starter/distribute/
├── config/DistributeAutoConfiguration.java → mate-cache-starter/src/main/java/vip/mate/starter/distribute/config/
├── util/SnowflakeUtil.java                 → mate-cache-starter/src/main/java/vip/mate/starter/distribute/util/
└── worker/WorkerIdHolder.java              → mate-cache-starter/src/main/java/vip/mate/starter/distribute/worker/
```

2. 更新 auto-imports：

```text
vip.mate.starter.cache.CacheAutoConfiguration
vip.mate.starter.lock.DistributedLockAutoConfiguration
vip.mate.starter.distribute.config.DistributeAutoConfiguration
```

3. 合并 pom.xml 依赖 — mate-distribute-starter 额外依赖 hutool-all 和 mate-base，需加入 mate-cache-starter 的 pom.xml：

File: `mate-starters/mate-cache-starter/pom.xml` (合并后)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-cache-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-cache-starter</name>
    <description>MateCloud Cache Starter - Caffeine L1 + Redis L2 + Distributed Lock + Snowflake ID</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.redisson</groupId>
            <artifactId>redisson-spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.github.ben-manes.caffeine</groupId>
            <artifactId>caffeine</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

4. 删除 `mate-starters/mate-distribute-starter/` 目录。

### Change 4: 合并 mate-datascope-starter + mate-idempotent-starter → mate-security-starter

**操作步骤：**

1. 移动 datascope (7 files)，保持原包名：

```
mate-starters/mate-datascope-starter/src/main/java/vip/mate/starter/datascope/
├── DataScope.java
├── DataScopeContext.java
├── DataScopeHolder.java
├── DataPermission.java
├── DataScopeAspect.java
├── DataScopeInterceptor.java
└── DataScopeAutoConfiguration.java
→ mate-security-starter/src/main/java/vip/mate/starter/datascope/  (全部)
```

2. 移动 idempotent (6 files)，保持原包名：

```
mate-starters/mate-idempotent-starter/src/main/java/vip/mate/starter/idempotent/
├── annotation/Idempotent.java
├── annotation/IdempotentType.java
├── aspect/IdempotentAspect.java
├── config/IdempotentProperties.java
├── config/IdempotentAutoConfiguration.java
└── controller/IdempotentTokenController.java
→ mate-security-starter/src/main/java/vip/mate/starter/idempotent/  (全部)
```

3. 更新 mate-security-starter 的 auto-imports 添加两行：

```text
vip.mate.starter.security.SecurityAutoConfiguration
vip.mate.starter.datascope.DataScopeAutoConfiguration
vip.mate.starter.idempotent.config.IdempotentAutoConfiguration
```

4. 合并 pom.xml 依赖（datascope 依赖 mybatis-plus-extension，idempotent 依赖 redisson）。

5. 删除 `mate-starters/mate-datascope-starter/` 和 `mate-starters/mate-idempotent-starter/` 目录。

### Change 5: 合并 mate-trace-starter → mate-monitor-starter

**操作步骤：**

1. 移动 1 个文件：

```
mate-starters/mate-trace-starter/src/main/java/vip/mate/starter/trace/TraceAutoConfiguration.java
→ mate-monitor-starter/src/main/java/vip/mate/starter/trace/TraceAutoConfiguration.java
```

2. 合并 pom.xml 的 micrometer-tracing 依赖到 mate-monitor-starter。

3. 更新 auto-imports 添加一行：

```text
vip.mate.starter.monitor.MonitorAutoConfiguration
vip.mate.starter.trace.TraceAutoConfiguration
```

4. 删除 `mate-starters/mate-trace-starter/` 目录。

### Change 6: 合并 mate-devtools-starter → mate-web-starter

**操作步骤：**

1. 移动 2 个文件，给 DevtoolsAutoConfiguration 加 `@Profile("dev")`：

```
mate-starters/mate-devtools-starter/src/main/java/vip/mate/starter/devtools/
├── ApiTimingFilter.java
└── DevtoolsAutoConfiguration.java
→ mate-web-starter/src/main/java/vip/mate/starter/devtools/  (全部)
```

修改 DevtoolsAutoConfiguration 加上 dev profile 限定：

```java
package vip.mate.starter.devtools;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@AutoConfiguration
@Profile("dev")
public class DevtoolsAutoConfiguration {

    @Bean
    public ApiTimingFilter apiTimingFilter() {
        return new ApiTimingFilter();
    }
}
```

2. 更新 mate-web-starter 的 auto-imports：

```text
vip.mate.starter.web.config.WebAutoConfiguration
vip.mate.starter.devtools.DevtoolsAutoConfiguration
```

3. 删除 `mate-starters/mate-devtools-starter/` 目录。

### Change 7: 删除 mate-dynamic-tp-starter 和 mate-doc-starter

1. 删除 `mate-starters/mate-dynamic-tp-starter/` 目录。
2. 删除 `mate-starters/mate-doc-starter/` 目录。
3. Smart-Doc 配置保留在 root pom.xml 的 `<pluginManagement>` 中。
4. 动态线程池配置写到文档（README 或 docs/）示例：

```yaml
# application.yml — 如果需要自定义线程池
spring:
  task:
    execution:
      pool:
        core-size: 8
        max-size: 32
        queue-capacity: 100
```

### Change 8: 移动 8 个 starter 到 contrib/

将以下目录从 `mate-starters/` 移动到 `mate-starters-contrib/`：

```
mate-starters/mate-seata-starter/      → mate-starters-contrib/mate-seata-starter/
mate-starters/mate-sharding-starter/   → mate-starters-contrib/mate-sharding-starter/
mate-starters/mate-sentinel-starter/   → mate-starters-contrib/mate-sentinel-starter/
mate-starters/mate-gray-starter/       → mate-starters-contrib/mate-gray-starter/
mate-starters/mate-flow-starter/       → mate-starters-contrib/mate-flow-starter/
mate-starters/mate-rule-starter/       → mate-starters-contrib/mate-rule-starter/
mate-starters/mate-ai-starter/         → mate-starters-contrib/mate-ai-starter/
mate-starters/mate-test-starter/       → mate-starters-contrib/mate-test-starter/
```

每个移动后需要更新其 pom.xml 的 parent：

```xml
<parent>
    <groupId>vip.mate</groupId>
    <artifactId>mate-starters-contrib</artifactId>
    <version>1.0.0</version>
    <relativePath>../pom.xml</relativePath>
</parent>
```

### Change 9: 更新 mate-starters/pom.xml

合并后 mate-starters 只保留 13 个模块：

File: `mate-starters/pom.xml`

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

    <artifactId>mate-starters</artifactId>
    <packaging>pom</packaging>
    <name>mate-starters</name>
    <description>Auto-configuration starter modules (13 starters: 7 core + 6 business)</description>

    <modules>
        <!-- Tier 1: Core (7) -->
        <module>mate-ds-starter</module>
        <module>mate-web-starter</module>
        <module>mate-cache-starter</module>
        <module>mate-nacos-starter</module>
        <module>mate-rpc-starter</module>
        <module>mate-sa-token-starter</module>
        <module>mate-monitor-starter</module>
        <!-- Tier 2: Business (6) -->
        <module>mate-mq-starter</module>
        <module>mate-job-starter</module>
        <module>mate-security-starter</module>
        <module>mate-file-starter</module>
        <module>mate-excel-starter</module>
        <module>mate-tenant-starter</module>
    </modules>

</project>
```

### Change 10: 更新 root pom.xml

在 root pom.xml 的 `<modules>` 中增加 `mate-starters-contrib`：

```xml
<modules>
    <module>mate-common</module>
    <module>mate-starters</module>
    <module>mate-starters-contrib</module>
    <module>mate-gateway</module>
    <module>mate-auth</module>
    <module>mate-admin</module>
    <module>mate-biz</module>
    <module>mate-cli</module>
</modules>
```

同时在 `<dependencyManagement>` 中更新 artifactId 引用：
- 删除 `mate-lock-starter`、`mate-distribute-starter`、`mate-datascope-starter`、`mate-idempotent-starter`、`mate-trace-starter`、`mate-devtools-starter`、`mate-dynamic-tp-starter`、`mate-doc-starter`
- 确保 `mate-cache-starter` 描述更新

### Change 11: 全局搜索并替换已删除 starter 的引用

在所有 pom.xml 和 Java import 中搜索以下 artifactId 并替换：

| 旧引用 | 替换为 | 说明 |
|--------|--------|------|
| `mate-lock-starter` | `mate-cache-starter` | 锁已内置 |
| `mate-distribute-starter` | `mate-cache-starter` | ID 生成已内置 |
| `mate-datascope-starter` | `mate-security-starter` | 数据权限已内置 |
| `mate-idempotent-starter` | `mate-security-starter` | 幂等已内置 |
| `mate-trace-starter` | `mate-monitor-starter` | 追踪已内置 |
| `mate-devtools-starter` | `mate-web-starter` | 开发工具已内置 |
| `mate-dynamic-tp-starter` | (删除引用) | 改用 Spring 原生配置 |
| `mate-doc-starter` | (删除引用) | 改用 Maven 插件 |

Java import 无需变更，因为包名保持不变（`vip.mate.starter.lock.*` 等）。

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-starters-contrib/pom.xml` | New | contrib 父 POM |
| `mate-starters/mate-cache-starter/pom.xml` | Modify | 增加 lock + distribute 依赖 |
| `mate-starters/mate-cache-starter/src/.../lock/*.java` | Move | 5 个文件从 lock-starter |
| `mate-starters/mate-cache-starter/src/.../distribute/**/*.java` | Move | 3 个文件从 distribute-starter |
| `mate-starters/mate-cache-starter/.../AutoConfiguration.imports` | Modify | 增加 2 个 auto-config |
| `mate-starters/mate-security-starter/.../datascope/*.java` | Move | 7 个文件从 datascope-starter |
| `mate-starters/mate-security-starter/.../idempotent/**/*.java` | Move | 6 个文件从 idempotent-starter |
| `mate-starters/mate-security-starter/.../AutoConfiguration.imports` | Modify | 增加 2 个 auto-config |
| `mate-starters/mate-monitor-starter/.../trace/*.java` | Move | 1 个文件从 trace-starter |
| `mate-starters/mate-web-starter/.../devtools/*.java` | Move | 2 个文件从 devtools-starter |
| `mate-starters/pom.xml` | Modify | 29 modules → 13 modules |
| `pom.xml` (root) | Modify | 增加 mate-starters-contrib module |
| 8 个 contrib starter 的 `pom.xml` | Modify | 更新 parent 为 mate-starters-contrib |
| 所有引用已删 starter 的 `pom.xml` | Modify | 替换 artifactId |
| `mate-starters/mate-lock-starter/` | Delete | 已合并 |
| `mate-starters/mate-distribute-starter/` | Delete | 已合并 |
| `mate-starters/mate-datascope-starter/` | Delete | 已合并 |
| `mate-starters/mate-idempotent-starter/` | Delete | 已合并 |
| `mate-starters/mate-trace-starter/` | Delete | 已合并 |
| `mate-starters/mate-devtools-starter/` | Delete | 已合并 |
| `mate-starters/mate-dynamic-tp-starter/` | Delete | 不再需要 |
| `mate-starters/mate-doc-starter/` | Delete | Maven 插件替代 |

## 验证方案

1. `mvn clean compile` — 全量编译通过，无 missing artifact 错误
2. `ls mate-starters/` — 只有 13 个目录
3. `ls mate-starters-contrib/` — 有 8 个目录
4. `grep -r "mate-lock-starter" --include="pom.xml"` — 零结果
5. `grep -r "mate-distribute-starter" --include="pom.xml"` — 零结果
6. `grep -r "mate-datascope-starter" --include="pom.xml"` — 零结果
7. `grep -r "mate-idempotent-starter" --include="pom.xml"` — 零结果
8. 检查 Java import — `vip.mate.starter.lock.*` 等包名未变，import 应全部正常

## 注意事项

- **包名不变**：合并只是移动文件到新 starter，Java 包名保持原样（`vip.mate.starter.lock.*`），所有使用方的 import 不需要改
- **先合并后移动**：先执行 5 个合并操作，确保编译通过后，再执行 8 个 contrib 移动
- **auto-imports 文件格式**：每行一个全限定类名，无空行
- **依赖传递**：合并后 mate-cache-starter 变成一个"胖 starter"，下游只需引 mate-cache-starter 就自动获得锁和 ID 生成能力
- **contrib 不参与默认构建**：可以在 root pom.xml 中用 profile 控制 contrib 是否编译，避免拖慢日常开发
- **文档同步更新**：代码合并完成后，以下文档需要同步更新 starter 列表和目录结构描述：
  - `README.md` — 更新模块列表为 13 + contrib
  - `CLAUDE.md` — 更新 Module Structure 章节
  - `docs/rfcs/README.md` — 已在本次更新
