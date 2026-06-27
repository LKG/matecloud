# 新建业务模块

## 使用 CLI（推荐）

```bash
java -jar mate-cli/target/mate-cli.jar new module mate-order --port 9060
```

此命令自动完成：

1. 创建 `mate-biz/mate-order/pom.xml`，引入核心 Starter
2. 生成 DDD 四层包结构
3. 创建 `application.yml`，配置 Nacos 导入
4. 生成 `MateOrderApplication.java`（`@SpringBootApplication` + `@EnableDubbo`）
5. 注册到 `mate-biz/pom.xml` 的 `<modules>` 列表

## 手动创建

### 1. 创建 Maven 模块

在 `mate-biz/` 下创建 `mate-order/pom.xml`：

```xml
<parent>
    <groupId>vip.mate</groupId>
    <artifactId>mate-biz</artifactId>
    <version>${revision}</version>
</parent>

<artifactId>mate-order</artifactId>

<dependencies>
    <!-- 核心 Starter -->
    <dependency><groupId>vip.mate</groupId><artifactId>mate-ds-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-web-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-cache-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-nacos-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-rpc-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-sa-token-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-monitor-starter</artifactId></dependency>
</dependencies>
```

### 2. 创建包结构

```
src/main/java/vip/mate/order/
├── trigger/
│   ├── controller/
│   ├── rpc/
│   └── event/
├── application/
│   ├── command/
│   ├── query/
│   └── convertor/
├── domain/
│   ├── model/
│   │   ├── aggregate/
│   │   ├── entity/
│   │   └── valobj/
│   ├── service/
│   └── adapter/
│       ├── repository/
│       └── port/
├── infrastructure/
│   ├── adapter/
│   │   ├── repository/
│   │   └── port/
│   ├── dao/
│   │   └── po/
│   └── config/
└── types/
    ├── exception/
    └── constant/
```

### 3. 创建启动类

```java
@SpringBootApplication
@EnableDubbo
public class MateOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(MateOrderApplication.class, args);
    }
}
```

### 4. 配置 application.yml

```yaml
spring:
  application:
    name: mate-order
  config:
    import:
      - classpath:mate-defaults.yml
      - optional:nacos:mate-infra-${spring.profiles.active:dev}.yml
      - optional:nacos:${spring.application.name}-${spring.profiles.active:dev}.yml

server:
  port: 9060

logging:
  level:
    vip.mate.order: debug
```

### 5. 注册模块

将 `<module>mate-order</module>` 添加到 `mate-biz/pom.xml` 的 `<modules>` 中。

### 6. 构建验证

```bash
mvn clean install -pl mate-biz/mate-order -am -DskipTests
```

新服务会自动继承 `mate-defaults.yml` + `mate-infra-dev.yml`，无需额外 Nacos 配置。
