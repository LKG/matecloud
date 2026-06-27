---
name: mate-new-module
description: 在 MateCloud 新建业务模块 (mate-{name}) 时使用。约束 DDD 四层结构、命名后缀、错误码、端口分配、领域纯净，避免手搓出不合规范的模块。当用户说"新建模块""加一个 mate-xxx 服务""scaffold a module"时触发。
---

# 新建 MateCloud 业务模块

目标：产出一个**符合 DDD 四层 + 项目约定**的新模块，而不是随手摆几个包。
完整规则见 `.claude/.harness/rules/01-architecture.md` 与 `02-conventions.md`。

## 第一步：优先用 CLI 脚手架（别手搓）

```bash
java -jar mate-cli/target/mate-cli.jar new module mate-{name} --port {port}
```

端口按段分配（见 coding-standards §1.3）：9030-9039 系统 / 9040-9049 后台 / 9050-9059 通知 / **9060+ 业务扩展**。新模块端口递增，别撞已用端口。

CLI 不可用时才手动建，且必须补齐：
1. `mate-biz/mate-{name}/pom.xml` 引入所需 starter（core7 必备）。
2. `application.yml`（~15 行）：端口 + 应用名 + `spring.config.import` 引 Nacos。
3. `Mate{Name}Application.java`：`@SpringBootApplication` + `@EnableDubbo`。
4. DDD 四层包：`trigger / application / domain / infrastructure / types`。
5. 在**根 `pom.xml`** 和 **`mate-biz/pom.xml`** 各加一行 `<module>`。

## 第二步：四层落位（包根 `vip.mate.{name}`）

- `trigger/` controller(`/api/v1/...` + `@SaCheckLogin`/`@SaCheckPermission`) · rpc · event · job
- `application/` command(写,`@Transactional`) · query(读,接口+impl) · convertor(MapStruct)
- `domain/` model{aggregate,entity,valobj} · service · adapter{repository,port}(**接口**) · event
- `infrastructure/` adapter 实现 · dao + dao/po(PO) · seed · config
- `types/` exception(ErrorCode 枚举) · constant · security(Perms)

## 约束清单（提交前逐条自查）

- [ ] **领域纯净**：`domain/model/**` 零框架 import（框架注解只在 PO）。
- [ ] **CQRS**：Command 与 Query 不混在一个类；Command 标 `@Transactional(rollbackFor=Exception.class)`。
- [ ] **仓储接口在 domain，实现在 infrastructure**；application 不直接碰 DAO。
- [ ] **所有对象转换用 MapStruct**，不用 `BeanUtils.copyProperties`。
- [ ] **错误码** `{MODULE}{TYPE}{SEQ}`（如 `ORDB001`；A=参数 B=业务 C=RPC D=DB E=外部）。
- [ ] **表** `mate_` 前缀，必备 `deleted/lock_version/created_at/updated_at`。
- [ ] **API** 统一 `/api/v1/`，返回 `Result<T>`；权限用 `Perms` 常量不硬编码。
- [ ] **无内联 FQN**，import 按 IntelliJ 默认分组（见 rules/02）。

## 第三步：建完即自检

```bash
bash .claude/.harness/checks/run-all.sh
```
阻断级必须全绿。代码生成可用 `mate-cli gen code --table {table} --module mate-{name} --service {svc}`。
