# RFC-050: 占位菜单功能补全计划

> Status: **Draft**
> Date: 2026-04-21

## 现状

菜单种子数据共 19 项（3 目录 + 16 菜单），其中 **8 项已完整实现**、**8 项为占位符**（前端页面存在但后端无 API）。

### 已完整实现（可正常使用）

| ID | 菜单 | 后端 | 前端 | Mock |
|----|------|------|------|------|
| 101 | 用户管理 | UserController (14 endpoints) | UserList.vue | ✓ |
| 102 | 管理员 | AdminController (12 endpoints) | AdminList.vue | ✓ |
| 103 | 角色管理 | RoleController (6 endpoints) | RoleList.vue | ✓ |
| 104 | 菜单管理 | MenuController (4 endpoints) | MenuTree.vue | ✓ |
| 105 | 字典管理 | DictTypeController + DictDataController (8 endpoints) | DictManager.vue | ✓ |
| 106 | 参数设置 | ConfigController (6 endpoints) | ConfigList.vue | ✓ |
| 201 | 操作日志 | OperationLogController (2 endpoints) | OperationLog.vue | ✓ |
| 202 | 登录日志 | LoginLogController (2 endpoints) | LoginLog.vue | ✓ |

### 占位符（仅前端页面，无后端 API）

| ID | 菜单 | 缺失 | 优先级 |
|----|------|------|--------|
| 203 | 在线用户 | 后端 + API + Mock | P0 |
| 204 | 服务状态 | 后端 + API + Mock | P1 |
| 205 | 缓存监控 | 后端 + API + Mock | P1 |
| 206 | 接口文档 | 后端 + API + Mock | P2 |
| 301 | 代码生成 | 后端 + API + Mock | P2 |
| 302 | 文件管理 | 后端 + API + Mock | P1 |
| 303 | 定时任务 | 后端 + API + Mock | P2 |
| 304 | 任务日志 | 后端 + API + Mock | P2 |

---

## G1: 在线用户（P0，~1 天）

**ID**: 203 | **路径**: `/monitor/online`

### 后端

`OnlineUserController.java`（放在 `system/admin/trigger/controller/`）：

```
GET  /api/v1/admin/online-users           → 在线用户列表（分页）
POST /api/v1/admin/online-users/{id}/kick → 强踢下线
```

**实现方式**：通过 Sa-Token 的 `StpUtil.searchTokenValue()` 扫描 Redis 中的活跃会话，获取 tokenValue → 反查 session 获取 userId、username、loginTime、clientIp。

### 前端

完善 `monitor/OnlineUsers.vue`：
- 表格：用户名、登录IP、登录时间、Token
- 操作：强踢下线按钮
- API：`onlineApi.list()` + `onlineApi.kick(id)`

### Mock

添加 10 条在线用户 mock 数据。

---

## G2: 文件管理（P1，~2 天）

**ID**: 302 | **路径**: `/tools/storage`

### 后端

`StorageController.java`（放在 `system/admin/trigger/controller/`）：

```
GET    /api/v1/admin/storage         → 文件列表（分页，按 bucket 过滤）
POST   /api/v1/admin/storage/upload  → 上传文件（multipart）
DELETE /api/v1/admin/storage/{key}   → 删除文件
GET    /api/v1/admin/storage/{key}/url → 获取预签名下载链接
```

**实现方式**：调用 `mate-file-starter` 的 `MinioTemplate` 封装 REST API。

### 前端

完善 `tools/Storage.vue`：
- 文件列表表格：文件名、大小、类型、上传时间
- 上传按钮（拖拽上传）
- 预览/下载/删除操作
- API：`storageApi` 模块

---

## G3: 服务状态 + 缓存监控（P1，~1.5 天）

**ID**: 204 + 205 | **路径**: `/monitor/server/status` + `/monitor/server/cache`

### 后端

扩展现有 `MonitorController`：

```
GET /api/v1/admin/monitor/server    → 服务器信息（JVM、OS、CPU、内存、磁盘）
GET /api/v1/admin/monitor/cache     → Redis 缓存信息（key 数量、内存占用、命中率）
```

**服务器信息**：通过 `Runtime`、`ManagementFactory`、`OperatingSystemMXBean` 采集。

**缓存信息**：通过 `RedissonClient.getKeys().count()` + `INFO memory` 命令获取。

### 前端

完善两个 PlaceholderView：
- `ServerStatus.vue`：CPU/内存/磁盘仪表盘 + JVM 信息卡片
- `ServerCache.vue`：Redis 统计 + 常用 Key 列表 + 清理操作

---

## G4: 接口文档（P2，~0.5 天）

**ID**: 206 | **路径**: `/monitor/server/docs`

### 方案

不新增后端——直接在前端嵌入 Smart-Doc 生成的 HTML：
- `ApiDocs.vue` 改为 iframe 加载 `/doc.html`
- 或前端展示 OpenAPI/Smart-Doc JSON

**前置**：完成 RFC-022（Smart-Doc 配置 + Javadoc 补全）。

---

## G5: 代码生成（P2，~2 天）

**ID**: 301 | **路径**: `/tools/codegen`

### 后端

`CodeGenController.java`：

```
GET  /api/v1/admin/codegen/tables          → 数据库表列表
GET  /api/v1/admin/codegen/tables/{name}   → 表结构详情（列、类型、注释）
POST /api/v1/admin/codegen/preview          → 预览生成代码
POST /api/v1/admin/codegen/download         → 下载生成的 ZIP
```

**实现方式**：复用 `mate-cli` 的 `GenCommand` 逻辑，包装为 REST API。

### 前端

完善 `tools/CodeGen.vue`：
- 左侧：数据库表列表（可选择）
- 右侧：代码预览（PO/DAO/Entity/Repository/Controller）
- 操作：下载 ZIP

---

## G6: 定时任务（P2，~1 天）

**ID**: 303 + 304 | **路径**: `/tools/tasks/jobs` + `/tools/tasks/logs`

### 方案选择

**方案 A**：嵌入 XXL-Job Admin UI（iframe）
- 最快，0 后端代码
- `JobList.vue` iframe 指向 `xxl.job.admin-addresses`

**方案 B**：封装 XXL-Job REST API
- `JobController.java` 代理 XXL-Job Admin API
- 前端自定义 UI

**推荐方案 A**（脚手架阶段，够用即可）。

---

## 优先级排序与时间线

```
P0 (必须): G1 在线用户              ~1d
P1 (应做): G2 文件管理 + G3 监控     ~3.5d
P2 (可选): G4 文档 + G5 代码生成 + G6 任务  ~3.5d
                                    ────────
                                    总计 ~8d
```

## 执行建议

1. **G1 在线用户**——立即执行，用户登录后这是第一个会点开的监控页面
2. **G2 + G3**——可并行，一个做文件管理一个做监控面板
3. **G4/G5/G6**——按需求优先级排，脚手架阶段 iframe 方案足够

## 非菜单功能补全

| 功能 | 现状 | 建议 |
|------|------|------|
| Dashboard 真数据 | MonitorController 已实现 | 前端 Dashboard.vue 需对接真实 API |
| Profile 密码修改 | UserController 有接口 | 前端 Profile.vue 需对接 |
| 通知中心 | mate-notice 模块存在 | 菜单暂不添加，后续 RFC |
