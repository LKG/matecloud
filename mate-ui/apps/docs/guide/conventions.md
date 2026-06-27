# 编码规范

## 包命名

- **业务模块**：`vip.mate.{service}.*`
- **Starter**：`vip.mate.starter.{name}`

## 数据库

- **表名前缀**：`mate_`
- **主键**：雪花 ID（`Long`）
- **审计字段**：`create_time`、`update_time`、`create_by`、`update_by`（由 `MyMetaObjectHandler` 自动填充）

## API

- **路径前缀**：`/api/v1/`
- **响应格式**：统一 `Result<T>` 包装
- **文档工具**：Smart-Doc（不用 Swagger）

## 错误码

格式：`{MODULE}{TYPE}{SEQ}`

| 字段 | 说明 | 取值 |
|------|------|------|
| MODULE | 模块 | SYS / USR / ORD / SEC / TEN / NTC |
| TYPE | 类型 | A=参数 / B=业务 / C=RPC / D=数据库 / E=外部 |
| SEQ | 序号 | 001-999 |

示例：`USRB001` — 用户模块业务异常 001

## 对象转换

- 使用 **MapStruct**，禁止手动字段拷贝
- Convertor 放在 `application/convertor/` 包下

## 代码风格

- 使用 **Lombok**：`@Data`、`@SuperBuilder`、`@RequiredArgsConstructor`
- 不使用 JetCache，统一用 Spring Cache + Caffeine + Redis
- 不使用内联全限定名（FQN），所有类型通过 `import` 引入
- Domain 层**零框架依赖**——不使用 Spring / MyBatis 注解
