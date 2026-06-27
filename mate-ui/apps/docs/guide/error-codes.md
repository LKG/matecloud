# 错误码

MateCloud 使用结构化错误码，格式为 `{MODULE}{TYPE}{SEQ}`。

## 格式说明

```
USRB001
│  │ │
│  │ └── 序号（001-999）
│  └──── 类型（A=参数 / B=业务 / C=RPC / D=数据库 / E=外部）
└─────── 模块（3 字母缩写）
```

## 模块编码

| 编码 | 模块 |
|------|------|
| SYS | 系统核心 |
| USR | 用户 |
| ORD | 订单 |
| SEC | 安全 |
| TEN | 租户 |
| NTC | 通知 |

## 类型编码

| 编码 | 含义 | 说明 |
|------|------|------|
| A | 参数异常 | 请求参数校验失败 |
| B | 业务异常 | 业务规则不满足 |
| C | RPC 异常 | 远程调用失败 |
| D | 数据库异常 | 数据操作失败 |
| E | 外部异常 | 第三方服务调用失败 |

## 定义方式

每个模块在 `types/exception/` 包下定义 `ErrorCode` 枚举：

```java
@Getter
@AllArgsConstructor
public enum UserErrorCode implements IErrorCode {
    USER_NOT_FOUND("USRB001", "用户不存在"),
    USER_DISABLED("USRB002", "用户已禁用"),
    DUPLICATE_USERNAME("USRA001", "用户名已存在");

    private final String code;
    private final String message;
}
```

## 使用方式

```java
throw new BizException(UserErrorCode.USER_NOT_FOUND);
```

框架的 `GlobalExceptionHandler`（来自 `mate-web-starter`）会自动将其转为标准 `Result` 响应。
