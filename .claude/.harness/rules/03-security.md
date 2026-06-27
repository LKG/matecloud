# 规则 03 · 安全边界

> 详版见 `docs/conventions/coding-standards.md` 第 11 章。

## 鉴权

- 用 **Sa-Token**，不用 Spring Security。Controller 类标 `@SaCheckLogin`，方法标 `@SaCheckPermission(Perms.X)`。
- 权限用常量类 `Perms`（`{module}:{entity}:{action}`），不硬编码字符串。
- 密码 BCrypt 存储，永不明文；日志禁输出密码 / Token / BCrypt 哈希 / 敏感个人信息。

## 禁硬编码密钥

代码（非测试）里**禁止**出现真实密钥：`sk-…`、`AKIA…`(AWS)、`ghp_…`(GitHub)、
`-----BEGIN … PRIVATE KEY-----` 等。密钥走环境变量 / Nacos 占位符（`${...}`）。
测试桩里用 AWS 官方示例值（`AKIAIOSFODNN7EXAMPLE`）等假值是允许的。
- ✅ 机检：`check-secrets`（阻断级，已排除 `src/test`）

## 多租户边界

- 新增端点默认保守：写端点需要权限校验，读端点需要登录校验。
- 二级主体（userId/数据源/工作区）不可缺失即回退全局。
