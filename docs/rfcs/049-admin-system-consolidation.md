# RFC-049: mate-admin 合并入 mate-system

> Status: **Accepted**
> Author: MateCloud Team
> Date: 2026-04-20

## 背景

MateCloud 当前将用户管理（mate-system）和后台管理（mate-admin）拆分为两个独立微服务。经过实际运行验证，发现以下问题：

1. **登录需要两次 RPC**：mate-auth → mate-system（用户认证）+ mate-admin（权限查询），增加了 5-10ms 延迟和一个故障点
2. **两张用户表**：`mate_user`（mate-system）和 `mate_admin`（mate-admin）通过 username 关联，设计复杂且容易数据不一致
3. **始终同时部署**：docker-compose 和 K8s 中两个服务从不单独部署，拆分带来的弹性扩缩价值为零
4. **开源脚手架定位**：作为脚手架，简洁优于展示微服务拆分——降低理解和部署门槛更重要
5. **LMS 商业版验证**：matecloud-lms 使用合并模式（mate-lms-system 包含所有 RBAC），运行稳定

## 决策

**mate-admin 合并入 mate-system**，合并后的模块名保持 `mate-system`（端口 9030）。

## 量化对比

| 维度 | 拆分（当前） | 合并（目标） |
|------|------------|------------|
| 微服务数 | 5 (gateway + auth + system + admin + notice) | 4 (gateway + auth + system + notice) |
| 登录 RPC 调用 | 2 次（串行） | 1 次 |
| 数据库表 | mate-system: 2 表, mate-admin: 12 表（分属两个迁移集） | mate-system: 14 表（统一迁移集） |
| REST 控制器 | system: 1, admin: 10 | system: 11 |
| Dubbo 服务 | system: 1 (IRpcUserService), admin: 2 (IRpcPermissionService + IRpcDictService) | system: 3（全部） |
| DDD 聚合 | system: 1, admin: 2 | system: 3 |
| 启动端口 | 9030 + 9040 | 9030 |

## 合并范围

### 移入 mate-system 的内容

从 `mate-admin/` 移入 `mate-biz/mate-system/`：

```
mate-admin/
├── domain/permission/     → system/domain/permission/     (Admin/Role/Menu 聚合)
├── domain/dict/           → system/domain/dict/           (DictType/DictData)
├── domain/config/         → system/domain/config/         (Config)
├── application/command/   → system/application/command/   (5 个 CommandService)
├── application/query/     → system/application/query/     (8 个 QueryService)
├── infrastructure/        → system/infrastructure/        (DAO/PO/Repository)
├── trigger/controller/    → system/trigger/controller/    (10 个 Controller)
├── trigger/rpc/           → system/trigger/rpc/           (2 个 DubboService)
├── types/                 → system/types/                 (ErrorCode/Perms/Excel)
└── db/migration/V1.0.*    → system/db/migration/V1.0.*   (12 张表的建表+种子)
```

### 删除

- `mate-admin/` 模块目录
- 根 `pom.xml` 中的 `<module>mate-admin</module>`
- 网关路由中 `mate-admin` 的独立路由（合并到 `mate-system`）
- `mate-monolith` 中对 `mate-admin` 的依赖

### 用户表合并

`mate_admin` 和 `mate_user` 合并为单一 `mate_user` 表：

```sql
-- mate_user 新增字段（从 mate_admin 合并）
ALTER TABLE mate_user ADD COLUMN nick_name VARCHAR(64) DEFAULT NULL;
-- mate_admin_role → mate_user_role (关联 mate_user.id)
-- mate_admin 表废弃
```

角色/菜单/权限直接关联 `mate_user`，不再需要两张用户表 + username 关联。

### RPC 接口变化

```java
// 之前: 两个服务
IRpcUserService       (group=system) → mate-system
IRpcPermissionService (group=admin)  → mate-admin

// 之后: 统一到一个服务
IRpcUserService       (group=system) → mate-system  // 保持不变
IRpcPermissionService (group=system) → mate-system  // group 从 admin 改为 system
IRpcDictService       (group=system) → mate-system  // 同上
```

mate-auth 的 `SaTokenIssuer` 只需改 group 即可，接口签名不变。

## 合并步骤

### Phase 1: 代码迁移（~2 天）

1. 将 mate-admin 的 domain/application/infrastructure/trigger/types 包移入 mate-system
2. 合并 Flyway 迁移文件（V1.0.x admin 迁移纳入 mate-system）
3. 更新 mate-system 的 pom.xml 依赖
4. 删除 mate-admin 模块

### Phase 2: 接口适配（~1 天）

1. `IRpcPermissionService` 和 `IRpcDictService` 的 group 从 `admin` 改为 `system`
2. mate-auth 的 `@DubboReference(group=GROUP_ADMIN)` 改为 `GROUP_SYSTEM`
3. 网关路由合并：`/api/v1/admin/**` 路由到 `mate-system`
4. 前端 API baseURL 无需改动（网关透明转发）

### Phase 3: 用户表合并（~1 天）

1. `mate_admin` 数据迁移到 `mate_user`
2. `mate_admin_role` 改为 `mate_user_role`
3. 权限查询从 `findByAdminId` 改为 `findByUserId`
4. 废弃 `mate_admin` 表

### Phase 4: 清理（~0.5 天）

1. 删除 `RpcConstants.GROUP_ADMIN`
2. 更新 docker-compose.yml（移除 mate-admin 服务）
3. 更新 K8s 部署清单
4. 更新编码规范和文档

## 不合并的内容

- **mate-notice**：通知服务有独立的业务域和技术栈（多渠道推送），保持独立
- **mate-auth**：认证是网关前置依赖，架构上必须独立
- **mate-gateway**：WebFlux 技术栈，不可能合并

## 风险与缓解

| 风险 | 缓解措施 |
|------|---------|
| mate-system 变大（11 Controller） | DDD 分包已按子域隔离（user/permission/dict/config），代码不混乱 |
| 单点故障影响更大 | 合并后反而减少了一个 RPC 故障点 |
| 迁移期间不可用 | Flyway + INSERT IGNORE 保证幂等，可一次性迁移 |

## 时间线

- **Phase 1-4**: 约 4.5 天
- **前置条件**: 当前所有启动问题修复完毕
- **产出**: mate-admin 完全删除，mate-system 承载全部后台管理功能

## 参考

- RFC-008: mate-system DDD Example
- RFC-009: Admin Backend Features
- matecloud-lms: mate-lms-system（合并模式的成功验证）
