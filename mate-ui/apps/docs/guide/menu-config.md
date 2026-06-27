# 菜单配置

后台左侧/顶部菜单由**后端数据驱动**：前端登录后拉取当前用户有权限的菜单，动态生成路由与导航。菜单数据存在 `mate_menu` 表，授权关系存在 `mate_role_menu` 表。

MateCloud 有**两种**往系统里加菜单的方式，按归属区分：

| 方式 | 适用场景 | 由谁维护 | 是否被自动对账清理 |
| --- | --- | --- | --- |
| **① 手工菜单（种子）** | mate-system 自身的系统/监控/AI 等内置菜单 | Flyway 种子迁移 | 否（`module_code` 为空，对账不动它）|
| **② 模块声明式菜单** | 业务模块/场景包随服务自带的菜单 | `menu-manifest.yml` + 启动自注册 | 是（按 `module_code` 全量对账）|

## 菜单字段与类型

`mate_menu` 关键字段（声明式清单 `MenuNode` 与之对应）：

| 字段 | 说明 |
| --- | --- |
| `code` | 稳定唯一标识（声明式菜单的对账主键）|
| `parent_id` / `parentCode` | 父节点（顶级为 `NULL`）|
| `name` / `name_en` | 中/英文名称 |
| `path` | 前端路由路径，如 `/system/users` |
| `component` | 前端组件路径，如 `system/UserList` |
| `perms` | 权限码，对应 Controller 的 `@SaCheckPermission`，如 `sys:user:list` |
| `type` | **M**=目录、**C**=菜单(页面)、**F**=按钮 |
| `icon` / `sort` | 图标 / 同级排序 |
| `module_code` | 归属模块（声明式菜单自动回填；手工菜单为空）|

`type` 三级结构：目录 `M` → 菜单 `C` → 按钮 `F`。按钮没有 `path`，只有 `perms`，用于前端按钮级权限控制。

## 方式①：手工菜单（种子迁移）

mate-system 自身的菜单写在 `V1.0.1__init_seed.sql` 里，用 `INSERT IGNORE` 幂等插入 `mate_menu` + `mate_role_menu`：

```sql
-- 目录
INSERT IGNORE INTO `mate_menu` (`id`,`parent_id`,`name`,`path`,`perms`,`type`,`icon`,`sort`) VALUES
('100', NULL,  '系统管理', '/system',        NULL,            'M', 'Settings', 1),
('101', '100', '用户管理', '/system/users',  'sys:user:list', 'C', 'Users',    1);

-- 授权给超级管理员角色（id=1）
INSERT IGNORE INTO `mate_role_menu` (`role_id`,`menu_id`) VALUES ('1','100'),('1','101');
```

新增内置菜单 = **追加一个新的 `V*` 迁移**（不要改已发布的种子文件）。这类菜单 `module_code` 为空，不会被模块对账误删。

## 方式②：模块声明式菜单（推荐给业务模块）

业务模块只需在 `src/main/resources/` 放一份 `menu-manifest.yml`，引入 `mate-menu-starter`，**启动时自动注册**到 mate-system，无需写 SQL、无需改 mate-system。

```yaml
# src/main/resources/menu-manifest.yml
menus:
  - code: order_root           # 稳定唯一 code
    name: 订单中心
    type: M
    icon: shopping-cart
    sort: 20
    children:
      - code: order_list
        parentCode: order_root
        name: 订单列表
        path: /order/list
        component: order/OrderList
        perms: order:list
        type: C
        sort: 1
        children:
          - code: order_export
            name: 导出
            perms: order:export
            type: F            # 按钮：只有 perms，无 path
            sort: 1
```

### 工作原理

1. 模块引入 `mate-menu-starter`，并设置 `mate.module.code`（如 `order`）。
2. 服务 `ApplicationReadyEvent` 时，`MenuAutoRegistrar` 扫描 classpath 上**所有** jar 的 `menu-manifest.yml`（`classpath*:`，每个 starter/场景包各带一份都会被聚合），扁平化后通过 Dubbo RPC 调 `mate-system` 的 `IRpcMenuRegistry.register(moduleCode, menus)`。
3. mate-system 按 `code` **幂等 upsert**，回填 `module_code`，并把每个菜单**自动授权给超级管理员角色**。
4. **声明式对账**：manifest 是该模块菜单的全量声明——归属本 `module_code` 但本次未声明的 `code` 会被**自动删除**（含角色绑定）。所以改名/重组/拔掉某模块后，旧菜单不会残留为孤儿。
5. 容错：mate-system 未就绪时只 WARN 不阻断本服务启动，重试 3 次；下次成功启动会重新对账。

> 因为②是全量对账，**不要**用方式①的种子去插一个属于某模块（带 `module_code`）的菜单——下次该模块自注册时会把没在 manifest 里声明的它删掉。两种方式各管各的：内置菜单用①，业务模块用②。

## 授权与可见性

- 注册/种子时菜单只默认授予**超级管理员**（`ROLE_ADMIN`, id=1）。其它角色需在「系统管理 → 角色管理」里勾选授权（写 `mate_role_menu`）。
- 前端按登录用户的角色取并集菜单渲染；`type=F` 的按钮权限码用于页面内按钮的显示/禁用。

## 常见问题

**Q：菜单加了但前端看不到？**
- 确认菜单已授权给当前用户的角色（`mate_role_menu`）。
- 前端菜单有缓存，重新登录或刷新。

**Q：拔掉一个模块后，它的菜单还在？**
- 声明式菜单（有 `module_code`）会在该模块下次启动对账时清掉；若模块已永久下线、不再启动，则需手工清理：先删 `mate_role_menu` 再删 `mate_menu`（按 `module_code` 过滤）。
