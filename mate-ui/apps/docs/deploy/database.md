# 数据库迁移（Flyway）

MateCloud 用 **Flyway** 管理每个服务的数据库版本。服务启动时自动执行未应用的迁移，**无需手动建库建表**——只要 MySQL 可连、库已创建（`matecloud`），首次启动即完成初始化。

## 核心机制：每服务独立版本线

与大多数教程不同，MateCloud **不使用**默认的全局 `flyway_schema_history` 表，而是**按模块**各建一张历史表：

```
flyway_history_${mate.module.code}
```

例如 `mate-system` 的 `mate.module.code: system`（见其 `application.yml`），历史表即 `flyway_history_system`；`mate-notice` 为 `flyway_history_notice`。

这样每个服务拥有**完全独立的版本线**：

- 各服务的迁移都可以从 `V1.0.0` 重新编号，互不冲突（旧的共享表会让 notice 的 `V1.2.1` 和 system 的 `V1.2.1` 撞车）。
- 多个服务共用同一个物理库（`matecloud`）也不会互相干扰。
- 新增服务必须设置 `mate.module.code`，否则历史表退化为 `flyway_history_shared`（故意取个难看的名字，提醒你忘了配）。

相关默认配置在 `mate-base.jar` 的 `mate-defaults.yml`，无需在服务里重复声明：

```yaml
spring:
  flyway:
    enabled: true
    table: flyway_history_${mate.module.code:shared}
    baseline-on-migrate: true     # 新服务可在非空库上自举历史
    baseline-version: 0
    validate-on-migrate: true     # 严格校验 checksum，杜绝静默漂移
    out-of-order: false           # 严格按版本顺序执行
    repair-on-migrate: true       # 启动时自动清理「失败」的迁移记录
    locations: classpath:db/migration
```

## 迁移文件位置与命名

每个服务的迁移脚本放在：

```
src/main/resources/db/migration/V{version}__{description}.sql
```

- `V` — 固定大写前缀
- `version` — 点分版本号，**严格递增**（`1.0.0` → `1.0.1` → `1.1.0`）
- `__` — 双下划线
- `description` — 小写下划线描述

## mate-system 的初始化基线（3 个文件）

`mate-system` 的迁移集已收口为一个干净的初始基线，新项目以此为起点：

| 文件 | 作用 |
| --- | --- |
| `V1.0.0__init_schema.sql` | 建表：核心 RBAC/系统/租户 + 仓库内产品表（channel/material/model/sso），共 25 张 |
| `V1.0.1__init_seed.sql` | 最小种子：`admin/admin123`、内置角色、内置租户与套餐、系统参数、数据字典、完整菜单树及授权 |
| `V1.0.2__example_add_column.sql` | Flyway 迁移**示例与规范**范本（复制它来写新迁移）|

> 业务表的菜单如何出现在后台，见 [菜单配置](/guide/menu-config)。

## 建表约定

照搬基线里的写法即可（与 `V1.0.0__init_schema.sql` 一致）：

```sql
-- V1.1.0__add_order_table.sql
CREATE TABLE `mate_order` (
  `id`          varchar(64)  NOT NULL COMMENT '主键（雪花 ID，字符串存储避免前端精度丢失）',
  `tenant_id`   varchar(32)  NOT NULL DEFAULT '1' COMMENT '租户ID',
  `order_no`    varchar(64)  NOT NULL COMMENT '订单号',
  `status`      tinyint      NOT NULL DEFAULT 0 COMMENT '状态',
  `created_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`     tinyint      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';
```

要点：

- 表名 `mate_` 前缀；引擎 `InnoDB`，字符集 `utf8mb4`。
- 主键 `id` 用 `varchar(64)` 存雪花 ID（字符串，避免 JS Number 精度丢失）。
- **不建物理外键**——关系约束在应用层维护（便于分库与逻辑删除）。
- 统一审计列：`created_at` / `updated_at` / `deleted`（逻辑删除）；多租户表带 `tenant_id`。
- 数据 `INSERT` 用 `INSERT IGNORE` 或 `ON DUPLICATE KEY UPDATE` 保证幂等。

## CLI 辅助

```bash
# 查看某模块的表结构
java -jar mate-cli/target/mate-cli.jar db describe mate_system --service mate-system

# 手动触发某模块的 Flyway 迁移（一般无需，启动时自动执行）
java -jar mate-cli/target/mate-cli.jar db migrate --module mate-system
```

## 注意事项与排错

### 迁移文件一旦应用，不可再改

Flyway 用 checksum 校验已应用的迁移。**改动一个已落库的 `V*` 文件（哪怕只是注释或空白）会改变 checksum，导致下次启动校验失败：**

```
Migration checksum mismatch for migration version 1.0.3
-> Applied to database : 1463784236
-> Resolved locally    : -234453209
```

需要变更时一律**新增**一个更高版本的 `V*` 文件向前修复，**不要**编辑旧文件。

### 修复 checksum 漂移（repair）

如果确认本地文件是对的、库里记录的 checksum 旧了（例如文件被合法重构过），有两种修法：

1. **改回触发自动 repair**：默认 `spring.flyway.repair-on-migrate: true`，但它只清理「失败」的记录，对 checksum 漂移不一定生效。
2. **直接修历史表**（开发库最快，等价于 `flyway repair`）——注意表名是按模块的 `flyway_history_<module>`：

   ```sql
   -- 把记录的 checksum 改成本地文件的值（从报错的 "Resolved locally" 取）
   UPDATE flyway_history_system SET checksum = -234453209 WHERE version = '1.0.3';
   ```

### 重置为干净初始态（开发环境）

想让某服务从头重新初始化：删掉它的表 + 删掉它的历史表，再重启服务即可（Flyway 会重新跑全部迁移）。

```sql
-- 示例：重置 mate-system（会丢失该服务数据，仅限开发库）
DROP TABLE flyway_history_system;
-- DROP 掉 mate-system 拥有的业务表 ...
```

> 不要在迁移文件里用 `CREATE TABLE IF NOT EXISTS` 来「兼容已存在的表」——那样表结构演进会被静默跳过。正确做法是重置历史 + 重启。
