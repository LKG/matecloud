# mate-ds-starter

数据源 Starter，封装 MyBatis Plus + Druid 连接池 + Flyway 数据库迁移。

## 提供的能力

- **MyBatis Plus** 自动配置（分页插件、乐观锁插件、审计字段自动填充）
- **Druid** 连接池（监控页面可选开启）
- **Flyway** 数据库版本迁移（启动时自动执行）
- **MyMetaObjectHandler** 自动填充 `createTime`、`updateTime`、`createBy`、`updateBy`

## 配置

数据源连接信息通常在 Nacos 的 `mate-infra-${profile}.yml` 中配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mate_system?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## Flyway 迁移

迁移脚本放在 `src/main/resources/db/migration/` 目录下：

```
db/migration/
├── V1__init_schema.sql
├── V2__add_user_status.sql
└── V3__add_dept_table.sql
```

命名规范：`V{version}__{description}.sql`

## 持久化对象（PO）

PO 类放在 `infrastructure/dao/po/` 包下，继承 `BaseEntity`：

```java
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mate_user")
public class UserPO extends BaseEntity {
    private String username;
    private String email;
    private Integer status;
}
```
