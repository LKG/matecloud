# RFC-041: Database Migration with Flyway

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 8
- **Dependencies**: RFC-001

## 背景

当前每个服务有一个裸 `schema.sql` 文件（`mate-admin`、`mate-system`、`mate-notice`），没有版本管理。存在三个问题：

1. **无版本控制**：无法知道数据库是否已经执行过某个 DDL 变更
2. **无种子数据**：新部署需要手动插入默认管理员、角色、菜单等
3. **无统一初始化**：开发者 clone 后需要手动逐个执行 SQL

本 RFC 引入 Flyway 解决这三个问题。

## 设计方案

### Change 1: 在 mate-ds-starter 中集成 Flyway

File: `mate-starters/mate-ds-starter/pom.xml`（增加依赖）

```xml
<!-- 在 <dependencies> 中添加 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

Flyway 通过 Spring Boot 自动配置生效，无需额外 Java 代码。在 `mate-defaults.yml` 或各服务配置中添加默认配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    out-of-order: false
```

### Change 2: mate-admin 迁移脚本

将现有 `schema.sql` 转化为 Flyway 迁移脚本。

File: `mate-admin/src/main/resources/db/migration/V1__admin_schema.sql`

```sql
-- ========================================================
-- mate-admin schema (RBAC + Dict + Config + Logs)
-- V1: Initial schema
-- ========================================================

CREATE TABLE IF NOT EXISTS `mate_admin` (
    `id`          VARCHAR(64) NOT NULL,
    `username`    VARCHAR(64) NOT NULL,
    `password`    VARCHAR(128) NOT NULL,
    `nick_name`   VARCHAR(64) DEFAULT NULL,
    `avatar`      VARCHAR(512) DEFAULT NULL,
    `status`      TINYINT NOT NULL DEFAULT 0,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    `lock_version` INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_role` (
    `id`          VARCHAR(64) NOT NULL,
    `role_key`    VARCHAR(64) NOT NULL,
    `role_name`   VARCHAR(64) NOT NULL,
    `sort`        INT DEFAULT 0,
    `status`      TINYINT NOT NULL DEFAULT 0,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_menu` (
    `id`          VARCHAR(64) NOT NULL,
    `parent_id`   VARCHAR(64) DEFAULT NULL,
    `name`        VARCHAR(64) NOT NULL,
    `path`        VARCHAR(256) DEFAULT NULL,
    `component`   VARCHAR(256) DEFAULT NULL,
    `perms`       VARCHAR(128) DEFAULT NULL,
    `type`        VARCHAR(2) NOT NULL COMMENT 'M=dir, C=menu, F=button',
    `icon`        VARCHAR(64) DEFAULT NULL,
    `sort`        INT DEFAULT 0,
    `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_admin_role` (
    `admin_id` VARCHAR(64) NOT NULL,
    `role_id`  VARCHAR(64) NOT NULL,
    PRIMARY KEY (`admin_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_role_menu` (
    `role_id` VARCHAR(64) NOT NULL,
    `menu_id` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_dict_type` (
    `id`         VARCHAR(64) NOT NULL,
    `dict_type`  VARCHAR(64) NOT NULL,
    `dict_name`  VARCHAR(64) NOT NULL,
    `status`     TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_dict_data` (
    `id`         VARCHAR(64) NOT NULL,
    `dict_type`  VARCHAR(64) NOT NULL,
    `dict_label` VARCHAR(128) NOT NULL,
    `dict_value` VARCHAR(128) NOT NULL,
    `sort`       INT DEFAULT 0,
    `remark`     VARCHAR(512) DEFAULT NULL,
    `status`     TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_dict_type` (`dict_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_config` (
    `id`           VARCHAR(64) NOT NULL,
    `config_key`   VARCHAR(128) NOT NULL,
    `config_value` TEXT,
    `config_name`  VARCHAR(128) DEFAULT NULL,
    `built_in`     TINYINT NOT NULL DEFAULT 0,
    `remark`       VARCHAR(512) DEFAULT NULL,
    `created_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_operation_log` (
    `id`              VARCHAR(64) NOT NULL,
    `user_id`         VARCHAR(64) DEFAULT NULL,
    `username`        VARCHAR(64) DEFAULT NULL,
    `module`          VARCHAR(64) DEFAULT NULL,
    `operation_type`  VARCHAR(32) DEFAULT NULL,
    `request_method`  VARCHAR(16) DEFAULT NULL,
    `request_url`     VARCHAR(512) DEFAULT NULL,
    `request_params`  TEXT,
    `response_result` TEXT,
    `client_ip`       VARCHAR(64) DEFAULT NULL,
    `status`          TINYINT NOT NULL DEFAULT 0,
    `error_msg`       VARCHAR(512) DEFAULT NULL,
    `duration`        BIGINT DEFAULT NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `mate_login_log` (
    `id`         VARCHAR(64) NOT NULL,
    `username`   VARCHAR(64) DEFAULT NULL,
    `client_ip`  VARCHAR(64) DEFAULT NULL,
    `user_agent` VARCHAR(512) DEFAULT NULL,
    `login_type` VARCHAR(32) DEFAULT NULL,
    `status`     TINYINT NOT NULL DEFAULT 0,
    `fail_msg`   VARCHAR(256) DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_username` (`username`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

File: `mate-admin/src/main/resources/db/migration/V2__admin_seed_data.sql`

```sql
-- ========================================================
-- mate-admin seed data
-- V2: Default admin, roles, menus, dict, config
-- ========================================================

-- Default admin (password: admin123, BCrypt encoded)
INSERT INTO `mate_admin` (`id`, `username`, `password`, `nick_name`, `status`) VALUES
('1', 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Super Admin', 0);

-- Default roles
INSERT INTO `mate_role` (`id`, `role_key`, `role_name`, `sort`, `status`) VALUES
('1', 'ROLE_ADMIN', 'Administrator', 1, 0),
('2', 'ROLE_USER', 'Normal User', 2, 0);

-- Assign admin role to default admin
INSERT INTO `mate_admin_role` (`admin_id`, `role_id`) VALUES ('1', '1');

-- Default menus (top-level directories)
INSERT INTO `mate_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `icon`, `sort`) VALUES
('100', NULL, 'System Management', '/system', NULL, 'M', 'Setting', 1),
('101', '100', 'User Management', '/system/user', 'system/UserList', 'C', 'User', 1),
('102', '100', 'Role Management', '/system/role', 'system/RoleList', 'C', 'UserFilled', 2),
('103', '100', 'Menu Management', '/system/menu', 'system/MenuTree', 'C', 'Menu', 3),
('104', '100', 'Dict Management', '/system/dict', 'system/DictManager', 'C', 'Collection', 4),
('105', '100', 'Config Management', '/system/config', 'system/ConfigList', 'C', 'Tools', 5),
('200', NULL, 'Monitor', '/monitor', NULL, 'M', 'Monitor', 2),
('201', '200', 'Operation Logs', '/monitor/operation-log', 'monitor/OperationLog', 'C', 'Document', 1),
('202', '200', 'Login Logs', '/monitor/login-log', 'monitor/LoginLog', 'C', 'Key', 2);

-- Assign all menus to admin role
INSERT INTO `mate_role_menu` (`role_id`, `menu_id`) VALUES
('1', '100'), ('1', '101'), ('1', '102'), ('1', '103'), ('1', '104'), ('1', '105'),
('1', '200'), ('1', '201'), ('1', '202');

-- Default dict types
INSERT INTO `mate_dict_type` (`id`, `dict_type`, `dict_name`) VALUES
('1', 'sys_user_status', 'User Status'),
('2', 'sys_common_status', 'Common Status'),
('3', 'sys_notice_type', 'Notice Type');

INSERT INTO `mate_dict_data` (`id`, `dict_type`, `dict_label`, `dict_value`, `sort`) VALUES
('1', 'sys_user_status', 'Active', '0', 1),
('2', 'sys_user_status', 'Frozen', '1', 2),
('3', 'sys_user_status', 'Deleted', '2', 3),
('4', 'sys_common_status', 'Enabled', '0', 1),
('5', 'sys_common_status', 'Disabled', '1', 2),
('6', 'sys_notice_type', 'SMS', 'SMS', 1),
('7', 'sys_notice_type', 'Email', 'EMAIL', 2),
('8', 'sys_notice_type', 'WeChat', 'WECHAT', 3);

-- Default config
INSERT INTO `mate_config` (`id`, `config_key`, `config_value`, `config_name`, `built_in`) VALUES
('1', 'sys.user.initPassword', '123456', 'Default password for new users', 1),
('2', 'sys.account.captchaEnabled', 'true', 'Enable login captcha', 1),
('3', 'sys.account.registerEnabled', 'true', 'Enable user registration', 1);
```

### Change 3: mate-system 迁移脚本

File: `mate-biz/mate-system/src/main/resources/db/migration/V1__system_schema.sql`

```sql
-- ========================================================
-- mate-system schema (User)
-- V1: Initial schema
-- ========================================================

CREATE TABLE IF NOT EXISTS `mate_user` (
    `id`              VARCHAR(64)  NOT NULL COMMENT 'Primary key',
    `username`        VARCHAR(64)  NOT NULL COMMENT 'Username',
    `password`        VARCHAR(128) NOT NULL COMMENT 'BCrypt hashed password',
    `mobile`          VARCHAR(20)  DEFAULT NULL COMMENT 'Mobile number',
    `email`           VARCHAR(128) DEFAULT NULL COMMENT 'Email',
    `real_name`       VARCHAR(64)  DEFAULT NULL COMMENT 'Real name',
    `avatar`          VARCHAR(512) DEFAULT NULL COMMENT 'Avatar URL',
    `gender`          TINYINT      DEFAULT 0 COMMENT '0=unknown, 1=male, 2=female',
    `status`          TINYINT      DEFAULT 0 COMMENT '0=active, 1=frozen, 2=deleted',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT 'Last login time',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `lock_version`    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

CREATE TABLE IF NOT EXISTS `mate_user_operate_stream` (
    `id`              VARCHAR(64)  NOT NULL COMMENT 'Primary key',
    `user_id`         VARCHAR(64)  NOT NULL COMMENT 'User ID',
    `operate_type`    VARCHAR(32)  NOT NULL COMMENT 'Operation type',
    `operate_detail`  VARCHAR(512) DEFAULT NULL COMMENT 'Operation detail',
    `operate_time`    DATETIME     NOT NULL COMMENT 'Operation time',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User operation stream';
```

### Change 4: mate-notice 迁移脚本

File: `mate-biz/mate-notice/src/main/resources/db/migration/V1__notice_schema.sql`

```sql
-- ========================================================
-- mate-notice schema
-- V1: Initial schema
-- ========================================================

CREATE TABLE IF NOT EXISTS `mate_notice` (
    `id`              VARCHAR(64)  NOT NULL,
    `channel`         VARCHAR(32)  NOT NULL COMMENT 'SMS/EMAIL/WECHAT',
    `business_type`   VARCHAR(64)  NOT NULL COMMENT 'Business type',
    `recipient`       VARCHAR(256) NOT NULL COMMENT 'Recipient address (phone/email/openid)',
    `title`           VARCHAR(256) DEFAULT NULL,
    `content`         TEXT NOT NULL,
    `status`          VARCHAR(32)  NOT NULL COMMENT 'PENDING/SENT/FAILED',
    `retry_count`     INT NOT NULL DEFAULT 0,
    `max_retries`     INT NOT NULL DEFAULT 3,
    `fail_reason`     VARCHAR(512) DEFAULT NULL,
    `sent_at`         DATETIME DEFAULT NULL,
    `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_channel` (`channel`),
    KEY `idx_status` (`status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Notice records';
```

### Change 5: 统一初始化脚本

一个开发者 clone 项目后，执行这一个脚本就能创建所有数据库。

File: `deploy/db/init-all.sql`

```sql
-- ========================================================
-- MateCloud Database Initialization
-- Run this script as MySQL root user to create all databases
-- ========================================================

CREATE DATABASE IF NOT EXISTS `mate_admin` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mate_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS `mate_notice` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- Grant permissions (adjust user/password as needed)
-- CREATE USER IF NOT EXISTS 'mate'@'%' IDENTIFIED BY 'mate123';
-- GRANT ALL PRIVILEGES ON `mate_admin`.* TO 'mate'@'%';
-- GRANT ALL PRIVILEGES ON `mate_system`.* TO 'mate'@'%';
-- GRANT ALL PRIVILEGES ON `mate_notice`.* TO 'mate'@'%';
-- FLUSH PRIVILEGES;

-- Note: Table creation and seed data are handled by Flyway on first startup.
-- Just create the databases, then start the services.
```

### Change 6: 删除旧 schema.sql 文件

将旧文件移除，避免与 Flyway 迁移混淆：

```
删除: mate-admin/src/main/resources/db/schema.sql
删除: mate-biz/mate-system/src/main/resources/db/schema.sql
删除: mate-biz/mate-notice/src/main/resources/db/schema.sql
```

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-starters/mate-ds-starter/pom.xml` | Modify | 增加 Flyway 依赖 |
| `mate-admin/src/main/resources/db/migration/V1__admin_schema.sql` | New | Admin 建表 |
| `mate-admin/src/main/resources/db/migration/V2__admin_seed_data.sql` | New | Admin 种子数据 |
| `mate-biz/mate-system/src/main/resources/db/migration/V1__system_schema.sql` | New | System 建表 |
| `mate-biz/mate-notice/src/main/resources/db/migration/V1__notice_schema.sql` | New | Notice 建表 |
| `deploy/db/init-all.sql` | New | 统一数据库创建 |
| `mate-admin/src/main/resources/db/schema.sql` | Delete | 旧 schema |
| `mate-biz/mate-system/src/main/resources/db/schema.sql` | Delete | 旧 schema |
| `mate-biz/mate-notice/src/main/resources/db/schema.sql` | Delete | 旧 schema |

## 验证方案

1. `mvn clean compile -pl mate-starters/mate-ds-starter` — 编译通过
2. 创建空数据库：`mysql -uroot -e "source deploy/db/init-all.sql"`
3. 启动 mate-admin：`cd mate-admin && mvn spring-boot:run`
4. 检查 Flyway 日志输出：`Successfully applied 2 migration(s)`
5. 检查 `flyway_schema_history` 表存在且有 V1、V2 记录
6. 检查种子数据：`SELECT * FROM mate_admin WHERE username='admin'` — 应有一条记录
7. 再次启动 mate-admin — Flyway 应跳过已执行的迁移

## 注意事项

- **baseline-on-migrate: true**：允许在已有数据的库上首次启用 Flyway，自动创建 baseline
- **每个服务独立数据库**：mate-admin、mate-system、mate-notice 各自有自己的数据库和迁移目录
- **迁移脚本命名规则**：`V{版本}__描述.sql`，双下划线分隔，版本号递增
- **迁移脚本不可修改**：一旦执行过的迁移脚本不能修改，只能新增新版本
- **BCrypt 密码**：种子数据中 admin 密码是 `admin123` 的 BCrypt 编码，只用于开发环境
- **操作日志和登录日志表**：与 RFC-040 中的 PO 字段对齐
- **菜单种子数据中的 `path` 与 `component` 字段**：前端动态路由使用 `path`（如 `/system/config`）作为 VIEW_MAP 匹配 key，`component` 字段（如 `system/ConfigList`）仅做记录，不参与路由匹配
