# 规则 01 · DDD 架构铁律

> 详版见 `docs/conventions/coding-standards.md` 第 2–5 章与 `CLAUDE.md`。本文件只列**约束与可机检点**。

## 分层与依赖方向

```
trigger → application → domain ← infrastructure
                          ↑          ↑
                        types      types
```

每个业务模块包根 `vip.mate.{service}/`，严格四层：`trigger / application / domain / infrastructure / types`。

## 铁律

1. **领域纯净**：`domain/model/**`（aggregate / entity / valobj）**零框架依赖**——不出现
   `org.springframework`、`com.baomidou.mybatisplus`、`cn.dev33.satoken`、`jakarta.persistence`、
   MyBatis 等任何 import。框架注解只出现在 PO（`infrastructure/dao/po`）。
   - 例外（项目既有约定）：`domain/service/impl` 允许 `@Service`、`domain/adapter/port` 的端口
     可引用第三方 SDK 抽象类型——故校验**只盯 `domain/model/**`**。
   - ✅ 机检：`check-domain-purity`（阻断级）
2. **依赖反转**：domain 不依赖 infrastructure；仓储/端口**接口在 domain**、实现在 infrastructure。
3. **CQRS**：写走 `CommandService`（`@Transactional(rollbackFor=Exception.class)`），
   读走 `QueryService`（接口 + impl），二者绝不混在一个类。
4. **聚合根守不变量**：状态变更只经聚合方法，不对外暴露裸 setter。
5. **MapStruct 做所有对象转换**，禁止 `BeanUtils.copyProperties` / 手写逐字段拷贝。
   - ⚠️ 机检：`check-mapstruct`（告警级，mate-admin 有存量遗留待还）
6. **application 不直接碰 DAO**，必经仓储接口。

## 数据库

- 表 `mate_` 前缀；必备 `deleted` / `lock_version` / `created_at` / `updated_at`（关联表/审计表除外）。
- Flyway：`V{N}__{desc}.sql`，种子数据 `INSERT IGNORE` 幂等；**禁 `DROP TABLE` / `TRUNCATE TABLE`**。
  - ⚠️ 机检：`check-flyway`（告警级）
