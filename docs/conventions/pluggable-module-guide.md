# 可插拔微服务规范(Pluggable Module Guide)

> 本文是 MateCloud "新增/拆除一个业务微服务应当自洽、不改中心文件" 的统一规则。
> 适用于所有 `mate-biz/*` 业务服务与平台服务。**新增模块必须遵循本文**。
> 状态标记:✅ 已落地 · 🚧 约定待实现(roadmap)。

## 0. 设计原则

MateCloud 的**能力层**(starter 自动装配、SPI+注册表、mate-api 接口隔离)已是真插拔;
痛点在**供给层**——加一个服务曾需手工协调 4 处中心资源:Flyway 全局版本、网关静态路由、
mate-system 菜单表、根/子 pom。本规范的目标:

> **把供给从"编译期/人工协调"改为"运行期/约定自发现"。一个业务模块自带它的
> schema、菜单、路由、接口与能力声明,上线不碰任何中心文件。**

可插拔业务模块 = 自带这 5 样:
1. 独立的 Flyway 迁移 + 独立 history 表(§1)
2. 自描述的菜单清单 + 启动期自注册(§2)
3. 自声明的网关路由 + 公开路径(§3)
4. 仅经 `mate-api` 的 Dubbo 接口对外(§4)
5. 按需引入 starter 能力 + 用 SPI 注册表做内部扩展点(§5)

---

## 1. 数据迁移:每服务独立 Flyway history ✅

### 规则
- 所有服务共用同一物理库(便于跨服务读、无分布式事务),但**每个服务拥有自己的
  schema-history 表** `flyway_history_<module-code>`,版本线**完全独立**。
- 因此**不再有全局版本协调**:每个服务的迁移可以从 `V1` 重新编号,跨服务永不撞号。
- 一个服务的迁移**只 DDL 它自己拥有的表**。跨服务的数据(如菜单)一律走运行期注册(§2),
  **严禁**往别的服务的迁移目录里写 INSERT。

### 实现(已落地)
- `mate-common/mate-base/.../mate-defaults.yml` 的 Flyway 段:
  ```yaml
  spring:
    flyway:
      table: flyway_history_${mate.module.code:shared}  # 每服务一张表
      baseline-on-migrate: true     # 新服务在(非空)共享库上自举历史
      baseline-version: 0
      validate-on-migrate: true     # 安全已恢复:漂移/改脚本会被发现
      out-of-order: false           # 严格有序(独立表后不再需要乱序兜底)
      repair-on-migrate: true       # 启动时先 repair 再 migrate(由 ds-starter 的
                                    # FlywayMigrationStrategy 真实实现;Spring Boot 无此原生属性)
      locations: classpath:db/migration
  ```
- **每个服务**在自己的 `application.yml` 声明模块码:
  ```yaml
  mate:
    module:
      code: order       # → 历史表 flyway_history_order
  ```
  已设置:system / auth / ai / notice。`shared` 兜底名故意难看,提醒漏配。

### 版本号命名(约定)
- 每服务**自己的表内**版本唯一即可,无需跨服务避让。
- 仍建议保留可读前缀以便排错(现状:system=1.0–1.2,auth=1.3,ai=1.4,
  notice 历史用过 1.2.1)。**新服务从 `V1__init_schema.sql` 起步即可**。

### 新模块怎么做
1. 在 `src/main/resources/db/migration/` 放 `V1__<name>_schema.sql`(双方言:H2 MySQL 模式
   + MySQL;`CREATE TABLE IF NOT EXISTS`;加列用裸 `ALTER ... ADD COLUMN`)。
2. 在 `application.yml` 设 `mate.module.code: <name>`。
3. 完。该服务有了独立历史表,与任何人不冲突。

### 存量库升级:**自动迁移**(无需手动脚本)
ds-starter 的 `repairThenMigrate` 在 migrate 前会 `autoSeedPerServiceHistory`:**MySQL** 上若发现
旧的共享 `flyway_schema_history` 存在、而本服务的 `flyway_history_<code>` 为空,则按本服务**解析到的
脚本名**精确地把已应用行从旧表复制进新表 → 已应用迁移被识别、**绝不重跑**(否则空的新表会让
`baseline-on-migrate` 在 v0 重跑全部历史,在第一个非幂等 `ALTER ADD COLUMN` 处因"Duplicate column"
失败)。幂等:新表一旦有行即 no-op。因此**直接部署新包即可**:
```bash
docker-compose build mate-system mate-ai mate-notice mate-auth
docker-compose up -d mate-system mate-ai mate-notice mate-auth
docker-compose logs mate-system | grep -i "flyway"   # 应见 "per-service history transition: seeded N row(s)"
```
- 全新空库无旧表 → 自动跳过,Flyway 从零建各服务表。
- **手动兜底**(可选,等价但无需启动应用):`mysql -u <user> -p <db> < scripts/flyway-split-history.sql`。
  升级前先 `mysqldump ... flyway_schema_history > backup.sql` 留底更稳妥。
- 历史撞号 `V1.2.1`(notice/system):拆分脚本已处理——system 按区间拿到 1.2.1,
  notice 按精确文件名匹配(拿不到就重跑自己的幂等 `CREATE IF NOT EXISTS`),
  `repair-on-migrate` 对齐校验和,两边均干净启动。升级后请抽查 `mate_notice_*` 与
  渠道配置表是否都在(撞号期可能曾漏建其一)。
- 旧 `flyway_schema_history` 不删,确认无误后可 `RENAME ... TO ..._legacy` 归档。

### 单体模式(mate-monolith)注意
单体把所有模块的 `db/migration` 合并到**一个** classpath、一次 Flyway 运行(单张
`flyway_history_<monolith-code>`),此时**版本必须全局唯一**——历史 `V1.2.1` 的
notice/system 重叠在单体下会触发 "more than one migration with version" 解析错误。
若启用单体模式,需把 notice 的 `V1.2.1__notice_schema.sql` 重编号为不冲突版本。
微服务模式(默认)不受影响。

---

## 2. 菜单 / 权限:启动期自注册 ✅(机制就绪,存量逐步搬迁)

### 规则
每个业务服务**自带菜单清单** `menu-manifest.yml`,启动期经 Dubbo 幂等注册到 mate-system,
**用字符串 `code` 作稳定键**,数字 id 由 mate-system 内部分配。**不再往 mate-system 迁移
写菜单、不再人工分配菜单 id**(那曾违反 §1"只 DDL 自己的表",且 id 需全局避让)。

### 清单格式(扁平字段,与实现一致)
```yaml
# <service>/src/main/resources/menu-manifest.yml —— 服务自描述、自携带
menus:
  - code: order.management       # 稳定键(替代手工数字 id)
    name: 订单管理              # 中文名
    nameEn: Order Management
    path: /order                # 目录可带 path
    type: M                     # M=目录 C=菜单 F=按钮
    icon: ShoppingCart
    sort: 8
    children:
      - code: order.management.list
        parentCode: order.management
        name: 订单列表
        nameEn: Order List
        path: /order/list
        component: order/OrderList
        perms: order:list:view  # perms 即权限稳定键
        type: C
        icon: List
        sort: 1
```

### 机制(已实现)
- **契约**(`mate-api`):`vip.mate.api.system.menu.{MenuNode, IRpcMenuRegistry}`;
  `@CrossTenantRpc Result<Integer> register(String moduleCode, List<MenuNode> menus)`。
- **自注册**(`mate-starters/mate-menu-starter`,引入即生效):`@AutoConfiguration` +
  `@ConditionalOnProperty(mate.menu.auto-register, matchIfMissing=true)`;`MenuAutoRegistrar`
  监听 `ApplicationReadyEvent`,读 `classpath:menu-manifest.yml`(不存在则跳过),
  `@DubboReference(check=false)` 调 `register`;**失败仅 WARN + 限次重试,绝不让服务启动崩溃**。
  moduleCode 取自 `mate.module.code`(与 §1 同一标识)。
- **落库**(`mate-system` `RpcMenuRegistryServiceImpl` `@DubboService(group=system)`):
  `mate_menu` 加 `code` / `module_code` 两列(迁移 `V1.2.13__menu_code.sql`);先序展开树、
  按下述 **adopt 链**首命中即认领既有行(回填 code+module_code)或新建并绑超管角色 1;
  全程 `TenantHelper.withoutTenant`(mate_menu 为全局表)。

### adopt 匹配链(保证幂等、不产生重复)
对每个节点依次:① by `code`(精确,主键)→ ② path 非空 by `path` → ③ perms 非空 by `perms`
→ ④ 目录(无 path/perms)by `name`+`type`+同一 `parent_id`。命中则更新并回填 code;否则插入。
> 因此存量服务接入时,清单字段(path/perms/type)与既有迁移**保持一致**即可被既有行认领、
> 回填 code,**不会重复**;首次后 code 落库,之后全走 by-code 更新。

### 新模块怎么做
1. 写 `src/main/resources/menu-manifest.yml`(code 用 `<module>.xxx` 命名)。
2. 引入 `mate-menu-starter`(+ `mate-rpc-starter` 提供 Dubbo 运行时)。
3. 设 `mate.module.code`(§1 已设)。**不写任何 mate-system 菜单迁移、不分配数字 id。**

### 存量搬迁(P1.1 — **已回退**,存量菜单仍由 mate-system 迁移维护)
**结论(当前约定)**:
- **存量服务(system/ai/notice)的菜单继续由 mate-system 迁移维护**。
- **P1 自注册机制保留**(`mate-menu-starter` / `IRpcMenuRegistry` / `mate_menu.code` 列),
  **仅供新模块**使用:`mate new module` 生成的服务自带顶级菜单 manifest、零侵入注册。
- 彻底统一(让存量菜单也走 manifest)需要先把"三级分类 IA"模型化,列为 P1.2 待定,**非当前必须**。

---

## 3. 网关路由:发现元数据自建路由 ✅(public-paths 仍集中)

### 规则
服务**自声明**它的网关 path,网关按 Nacos 发现元数据**自动建路由**——加新服务**无需改 mate-gateway**。

```yaml
# <service>/src/main/resources/application.yml
spring:
  cloud:
    nacos:
      discovery:
        metadata:
          gateway-path: /api/v1/foo/**     # 逗号分隔可多条
```

### 机制(已实现)
- `mate-gateway` 的 `DiscoveryMetadataRouteLocator`(`RouteDefinitionLocator`)遍历 Nacos 注册服务,
  读各自实例的 `gateway-path` 元数据,为其构建 `dyn-<serviceId> → lb://<serviceId>`(`Path` 谓词)。
- `RouteRefreshScheduler`:`ApplicationReadyEvent` 触发一次 + 每 30s 兜底 `RefreshRoutesEvent`
  (Nacos 心跳为主触发),新服务上线 ≤30s 内可路由。
- **纯增量**:`mate-gateway/application.yml` 的**静态路由保留作兜底/回滚**(`dyn-` 前缀不与静态 id 冲突);
  发现异常逐服务隔离(warn 跳过),绝不影响网关启动与既有路由。

### public-paths 仍集中(有意为之)
公开路径是**安全边界**,集中在 `mate.gateway.auth.public-paths` 统一评审更稳妥(可放 Nacos
`mate-gateway-${profile}.yml` 外置,免重建 jar)。新服务若有公开端点(如 OAuth 回调),在该列表
加一条即可——这是少数仍需碰网关配置的项,且应当被集中审视。

---

## 4. 服务间耦合:只经 mate-api ✅

- 服务之间**只**通过 `mate-common/mate-api` 的 Dubbo 接口(`@DubboService` 实现留在本服务)
  或 MQ 领域事件通信。**严禁**业务服务编译期依赖另一个业务服务的内部类。
- 跨服务读"配置/能力"用读模型接口 + 可选注入(如 `ModelConfigStore` 放 mate-base,
  实现由 mate-system 提供,消费方 `ObjectProvider<ModelConfigStore>` 优雅降级)。

---

## 5. 内部扩展点:SPI + 注册表 ✅

新增"一类可扩展能力"时,照仓库既有范式:**接口集中 + `List<T>`/`Map` 注入 + 注册表按 key 解析**。

已有范例(照抄即可):
| 扩展点 | 接口 | 注册/发现 |
|---|---|---|
| 通知渠道 | `notice/.../channel/NoticeChannelAdapter` | `NoticeAdapterRegistry`(`List<Adapter>` 注入,按 `channel()`/mode 解析) |
| 登录方式 | `auth/.../login/LoginStrategy` | `LoginStrategyFactory`(`List<Strategy>` → 按枚举 key) |
| 模型配置读模型 | `base/.../model/ModelConfigStore` | `ObjectProvider` 可选注入,DB 实现在 mate-system |

要点:用**枚举/字符串 code 做 key**(非魔术散落的 if-else);新实现只需 `@Component` +
实现接口,**注册表自动发现**;提供"未命中回退"(如发布渠道回退 Mock)。

---

## 6. Starter 能力装配 ✅

- 用 Spring Boot 的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  注册 `@AutoConfiguration` 类;用 `@ConditionalOnClass` / `@ConditionalOnProperty`
  (`havingValue` + `matchIfMissing`)做开关;`@EnableConfigurationProperties` 绑 `mate.*` 配置。
- 业务服务"按需引 starter"即获得能力(DS/Web/Cache/Nacos/RPC/SaToken/Monitor 为常引 7 件套)。

---

## 7. 新增业务模块 Checklist ✅(一条命令)

```bash
java -jar mate-cli/target/mate-cli.jar new module mate-foo --port 90xx
```

`mate new module` 已**自动**完成 §1–§3 + pom 注册(P3):

- [x] DDD 4 层骨架 + `XxxApplication.java` + pom(含 `mate-menu-starter`)。
- [x] `application.yml`:`spring.config.import` mate-defaults + Nacos;**`mate.module.code: foo`**(§1);
      **`spring.cloud.nacos.discovery.metadata.gateway-path: /api/v1/foo/**`**(§3,网关零改动)。
- [x] `db/migration/V1__foo_schema.sql`:独立版本线从 V1 起的 demo 表(只建自己的表,§1)。
- [x] `menu-manifest.yml`:`foo.*` 菜单 demo,启动自注册(§2,**不写 mate-system 菜单迁移**)。
- [x] 自动把 `<module>mate-foo</module>` 写进 `mate-biz/pom.xml`(幂等)。

生成后只需:替换 demo 表/菜单/控制器为真实内容;对外接口定义在 `mate-api`(§4);
内部扩展点用 SPI+注册表(§5);**仅当有公开端点**时在网关 `public-paths` 加一条(安全边界,集中评审)。
**全程不碰 mate-system / mate-gateway。**

---

## 8. Roadmap

| 阶段 | 内容 | 状态 |
|---|---|---|
| **P0** | 每服务独立 Flyway history + 恢复 validate/有序 + 存量库拆分脚本 | ✅ |
| **P1** | 菜单 `menu-manifest.yml` + `IRpcMenuRegistry` 启动自注册机制(供**新模块**) | ✅ |
| **P1.1** | 存量菜单搬迁 manifest | ↩︎ 回退(与三级重构 IA 冲突,存量仍走迁移) |
| **P1.2** | 三级分类 IA 模型化后,存量菜单统一走 manifest | 🚧 待定 |
| **P2** | 网关按发现元数据 `gateway-path` 自建路由(新服务零网关改动) | ✅ |
| **P3** | `mate new module` 一条龙(pom 注册 + Flyway/菜单/路由 模板全接线) | ✅ |

> 全部完成。设计性保留项:① public-paths 仍集中(安全边界,有意为之);② 单体模式需全局
> 唯一版本(§1 末)。**可插拔闭环:新增服务 = 一条 `mate new module` 命令,不碰任何中心文件。**
