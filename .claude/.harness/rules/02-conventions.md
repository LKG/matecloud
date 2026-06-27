# 规则 02 · 编码约定

> 详版见 `CLAUDE.md` 的 Conventions 节、`docs/conventions/coding-standards.md` 第 14 章。

## 命名与结构

- 包根：`vip.mate.*`；starter：`vip.mate.starter.{name}`。
- API 前缀统一 `/api/v1/`，RESTful；返回值统一 `Result<T>`。
- 错误码 `{MODULE}{TYPE}{SEQ}`，如 `USRB001`
  （MODULE: SYS/USR/ORD/SEC/TEN/NTC/ADM；TYPE: A=参数 B=业务 C=RPC D=DB E=外部）。
- 类名后缀约定见 coding-standards.md §14.1（`Aggregate/PO/Convertor/Repository/Port/...`）。

## 技术选型红线

- **No Swagger** → 用 Smart-Doc 出 API 文档。
- **No JetCache** → Spring Cache + Caffeine + Redis。
- **Lombok** 收敛样板，**MapStruct** 做转换。
- Starter 自动装配：顶层类用 **`@AutoConfiguration`**（不是 `@Configuration`），
  注册进 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
  顶层类内的**嵌套** `@Configuration static class` 是允许的。
  - ⚠️ 机检：`check-autoconfig`（告警级）

## 无内联全限定名（No inline FQN）

每个类型都经 `import` 用简单名引用；**禁止**在方法签名/方法体里写
内联全限定名（如 `org.springframework.beans.factory.ObjectProvider<vip.mate.xxx.spi.XxxPort>`）
（`*AutoConfiguration` 类高发区）。

例外（保留 FQN）：
1. 字符串字面量条件，如 `@ConditionalOnClass(name="...")` / `@ConditionalOnBean(type="...")`；
2. 同简单名冲突，两个类无法同时 import（如两个都叫 `PrincipalResolver`）——FQN 其一。

## Import 布局（IntelliJ 默认）

新增 import 放进正确分组，**不要插在 `package` 行后面**（会留孤行+空行）。
1. 非 static、非 `java`/`javax`（`cn.* com.* jakarta.* lombok.* org.* vip.*`，按 FQN 升序）
2. `javax.*` 再 `java.*`
3. `import static ...`
组间一个空行。剥 FQN 成简单名后留意**同简单名冲突**（见上例外②），冲突就保留 FQN。
