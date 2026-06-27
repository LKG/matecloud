# RFC-002: mate-common (mate-base + mate-api)

- **Status**: Draft
- **Created**: 2026-04-11
- **Author**: MateCloud Team

## 背景

`mate-common` 是纯类型库模块，不包含任何 auto-configuration。分为两个子模块：

- **mate-base**: 基础类型定义，包含 BaseEntity、Result、异常体系、分页请求等所有模块共享的基础类
- **mate-api**: RPC 接口定义，包含 Dubbo 服务接口、Command/Response DTO、枚举常量等

## 设计方案

### Change 1: mate-base/pom.xml

Create `D:\codes\matecloud\mate-common\mate-base\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-common</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-base</artifactId>
    <packaging>jar</packaging>
    <name>mate-base</name>
    <description>Base types - entities, results, exceptions, requests</description>

    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Jackson Annotations (for JSON serialization hints) -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
        </dependency>

        <!-- MyBatis Plus Annotation (for @TableField, @TableLogic, @Version, etc.) -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-annotation</artifactId>
        </dependency>

        <!-- Jakarta Validation API -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>

</project>
```

### Change 2: mate-base Java Sources

#### BaseEntity.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\model\entity\BaseEntity.java`

```java
package vip.mate.base.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Base entity with common fields for all persistent entities.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Primary key (String type to support distributed ID strategies).
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * Record creation time, auto-filled on insert.
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdAt;

    /**
     * Record last update time, auto-filled on insert and update.
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedAt;

    /**
     * Logical delete flag: 0 = active, 1 = deleted.
     */
    @TableLogic
    private Integer deleted;

    /**
     * Optimistic lock version.
     */
    @Version
    private Integer lockVersion;
}
```

#### Result.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\result\Result.java`

```java
package vip.mate.base.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import vip.mate.base.exception.ErrorCode;
import vip.mate.base.response.ResponseCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified API response wrapper.
 *
 * @param <T> the type of response data
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Response code.
     */
    private String code;

    /**
     * Response message.
     */
    private String msg;

    /**
     * Whether the request was successful.
     */
    private Boolean success;

    /**
     * Response data payload.
     */
    private T data;

    private Result(String code, String msg, Boolean success, T data) {
        this.code = code;
        this.msg = msg;
        this.success = success;
        this.data = data;
    }

    // ==================== Static Factory Methods ====================

    /**
     * Success with data.
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), true, data);
    }

    /**
     * Success without data.
     */
    public static <T> Result<T> ok() {
        return new Result<>(ResponseCode.SUCCESS.getCode(), ResponseCode.SUCCESS.getMessage(), true, null);
    }

    /**
     * Success with custom message.
     */
    public static <T> Result<T> ok(T data, String msg) {
        return new Result<>(ResponseCode.SUCCESS.getCode(), msg, true, data);
    }

    /**
     * Failure with ResponseCode.
     */
    public static <T> Result<T> fail(ResponseCode responseCode) {
        return new Result<>(responseCode.getCode(), responseCode.getMessage(), false, null);
    }

    /**
     * Failure with custom message.
     */
    public static <T> Result<T> fail(String msg) {
        return new Result<>(ResponseCode.FAILURE.getCode(), msg, false, null);
    }

    /**
     * Failure with code and message.
     */
    public static <T> Result<T> fail(String code, String msg) {
        return new Result<>(code, msg, false, null);
    }

    /**
     * Failure from ErrorCode interface.
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), false, null);
    }

    /**
     * Build result conditionally.
     */
    public static <T> Result<T> condition(boolean flag) {
        return flag ? ok() : fail(ResponseCode.FAILURE);
    }
}
```

#### ResponseCode.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\response\ResponseCode.java`

```java
package vip.mate.base.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

/**
 * Standard response codes for the platform.
 */
@Getter
@AllArgsConstructor
public enum ResponseCode implements ErrorCode {

    SUCCESS("200", "Operation successful"),
    FAILURE("500", "Operation failed"),

    // Client errors (4xx)
    BAD_REQUEST("400", "Bad request"),
    UNAUTHORIZED("401", "Unauthorized"),
    FORBIDDEN("403", "Forbidden"),
    NOT_FOUND("404", "Resource not found"),
    METHOD_NOT_ALLOWED("405", "Method not allowed"),
    CONFLICT("409", "Resource conflict"),
    TOO_MANY_REQUESTS("429", "Too many requests"),

    // Server errors (5xx)
    INTERNAL_ERROR("500", "Internal server error"),
    SERVICE_UNAVAILABLE("503", "Service unavailable"),
    GATEWAY_TIMEOUT("504", "Gateway timeout"),

    // Business errors (Bxxxx)
    PARAM_VALID_ERROR("B0001", "Parameter validation failed"),
    DATA_NOT_FOUND("B0002", "Data not found"),
    DUPLICATE_DATA("B0003", "Duplicate data"),
    OPERATION_NOT_ALLOWED("B0004", "Operation not allowed"),

    // RPC errors (Rxxxx)
    RPC_CALL_FAILED("R0001", "RPC call failed"),
    RPC_TIMEOUT("R0002", "RPC call timeout"),
    RPC_SERVICE_NOT_FOUND("R0003", "RPC service not found");

    private final String code;
    private final String message;
}
```

#### ErrorCode.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\exception\ErrorCode.java`

```java
package vip.mate.base.exception;

/**
 * Error code contract. Implement this interface to define module-specific error codes.
 */
public interface ErrorCode {

    /**
     * Get the error code string.
     */
    String getCode();

    /**
     * Get the human-readable error message.
     */
    String getMessage();
}
```

#### BizException.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\exception\BizException.java`

```java
package vip.mate.base.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * Business logic exception. Thrown when a business rule is violated.
 * Should be caught by GlobalExceptionHandler and converted to a Result response.
 */
@Getter
public class BizException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Error code.
     */
    private final String code;

    /**
     * Error message.
     */
    private final String msg;

    public BizException(String msg) {
        super(msg);
        this.code = "500";
        this.msg = msg;
    }

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }

    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.msg = errorCode.getMessage();
    }

    public BizException(String code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.msg = msg;
    }
}
```

#### BasePageReq.java

Create `D:\codes\matecloud\mate-common\mate-base\src\main\java\vip\mate\base\request\BasePageReq.java`

```java
package vip.mate.base.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Base pagination request. Extend this class for paginated query requests.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class BasePageReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Page number, starting from 1.
     */
    @Min(value = 1, message = "Page number must be >= 1")
    private Integer pageNum = 1;

    /**
     * Page size.
     */
    @Min(value = 1, message = "Page size must be >= 1")
    @Max(value = 500, message = "Page size must be <= 500")
    private Integer pageSize = 10;
}
```

### Change 3: mate-api/pom.xml

Create `D:\codes\matecloud\mate-common\mate-api\pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-common</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-api</artifactId>
    <packaging>jar</packaging>
    <name>mate-api</name>
    <description>RPC API interfaces, commands, responses, and constants</description>

    <dependencies>
        <!-- mate-base (for Result, ErrorCode, etc.) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Jakarta Validation API (for Command validation annotations) -->
        <dependency>
            <groupId>jakarta.validation</groupId>
            <artifactId>jakarta.validation-api</artifactId>
        </dependency>
    </dependencies>

</project>
```

### Change 4: mate-api Java Sources

#### IRpcUserService.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\system\service\IRpcUserService.java`

```java
package vip.mate.api.system.service;

import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.Result;

/**
 * Dubbo RPC interface for user operations.
 * Implemented by mate-system, consumed by mate-auth and other services.
 */
public interface IRpcUserService {

    /**
     * Get user info by user ID.
     *
     * @param userId the user ID
     * @return user info wrapped in Result
     */
    Result<UserInfoResponse> getUserById(String userId);

    /**
     * Get user info by username.
     *
     * @param username the username
     * @return user info wrapped in Result
     */
    Result<UserInfoResponse> getUserByUsername(String username);

    /**
     * Get user info by mobile phone number.
     *
     * @param mobile the mobile phone number
     * @return user info wrapped in Result
     */
    Result<UserInfoResponse> getUserByMobile(String mobile);

    /**
     * Register a new user.
     *
     * @param command the registration command
     * @return user info of the newly registered user
     */
    Result<UserInfoResponse> registerUser(RegisterUserCommand command);
}
```

#### IRpcDictService.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\system\service\IRpcDictService.java`

```java
package vip.mate.api.system.service;

import vip.mate.base.result.Result;

import java.util.List;
import java.util.Map;

/**
 * Dubbo RPC interface for dictionary operations.
 * Implemented by mate-system, consumed by other services needing dict data.
 */
public interface IRpcDictService {

    /**
     * Get dictionary value by dict type and dict code.
     *
     * @param dictType the dictionary type
     * @param dictCode the dictionary code
     * @return the dictionary value
     */
    Result<String> getDictValue(String dictType, String dictCode);

    /**
     * Get all dictionary entries for a given type.
     * Returns a list of maps, each containing "code", "value", "label".
     *
     * @param dictType the dictionary type
     * @return list of dict entries
     */
    Result<List<Map<String, String>>> getDictListByType(String dictType);

    /**
     * Refresh dictionary cache for the given type.
     *
     * @param dictType the dictionary type to refresh, or null to refresh all
     * @return success or failure
     */
    Result<Boolean> refreshDictCache(String dictType);
}
```

#### RegisterUserCommand.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\system\command\RegisterUserCommand.java`

```java
package vip.mate.api.system.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

/**
 * Command object for user registration via RPC.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class RegisterUserCommand implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Username, 4-32 characters, alphanumeric and underscore only.
     */
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 32, message = "Username must be 4-32 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, digits, and underscores")
    private String username;

    /**
     * Password (plaintext, will be hashed by the service).
     */
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 64, message = "Password must be 6-64 characters")
    private String password;

    /**
     * Mobile phone number.
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "Invalid mobile phone number")
    private String mobile;

    /**
     * Email address.
     */
    private String email;

    /**
     * Real name.
     */
    private String realName;
}
```

#### UserInfoResponse.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\system\response\UserInfoResponse.java`

```java
package vip.mate.api.system.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.api.system.enums.UserStatus;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * User information response DTO returned by RPC calls.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public class UserInfoResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * User ID.
     */
    private String userId;

    /**
     * Username.
     */
    private String username;

    /**
     * Real name.
     */
    private String realName;

    /**
     * Mobile phone number.
     */
    private String mobile;

    /**
     * Email address.
     */
    private String email;

    /**
     * Avatar URL.
     */
    private String avatar;

    /**
     * User status.
     */
    private UserStatus status;

    /**
     * Role code list.
     */
    private List<String> roleCodes;

    /**
     * Permission code list.
     */
    private List<String> permissions;

    /**
     * Department ID.
     */
    private String deptId;

    /**
     * Department name.
     */
    private String deptName;

    /**
     * Account creation time.
     */
    private Date createdAt;
}
```

#### UserStatus.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\system\enums\UserStatus.java`

```java
package vip.mate.api.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * User account status.
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    ACTIVE(0, "Active"),
    DISABLED(1, "Disabled"),
    LOCKED(2, "Locked");

    private final Integer code;
    private final String desc;

    /**
     * Get UserStatus by code value.
     */
    public static UserStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown UserStatus code: " + code);
    }
}
```

#### RpcConstants.java

Create `D:\codes\matecloud\mate-common\mate-api\src\main\java\vip\mate\api\rpc\RpcConstants.java`

```java
package vip.mate.api.rpc;

/**
 * Shared RPC constants for Dubbo service declarations.
 * Use these constants in @DubboService and @DubboReference annotations
 * to ensure version and timeout consistency across all services.
 */
public final class RpcConstants {

    private RpcConstants() {
        // prevent instantiation
    }

    /**
     * Default RPC interface version. Update when making breaking changes.
     */
    public static final String VERSION = "1.0.0";

    /**
     * Default RPC call timeout in milliseconds.
     */
    public static final int DEFAULT_TIMEOUT = 5000;

    /**
     * Long-running RPC call timeout in milliseconds (e.g., batch operations).
     */
    public static final int LONG_TIMEOUT = 15000;

    /**
     * Default retry count for idempotent RPC calls.
     */
    public static final int DEFAULT_RETRIES = 2;

    /**
     * No retry (for non-idempotent write operations).
     */
    public static final int NO_RETRY = 0;

    /**
     * Dubbo group: system service.
     */
    public static final String GROUP_SYSTEM = "system";
}
```

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `mate-common/mate-base/pom.xml` | New | mate-base 模块 POM |
| `mate-common/mate-base/src/main/java/vip/mate/base/model/entity/BaseEntity.java` | New | 基础实体类 |
| `mate-common/mate-base/src/main/java/vip/mate/base/result/Result.java` | New | 统一响应包装 |
| `mate-common/mate-base/src/main/java/vip/mate/base/response/ResponseCode.java` | New | 响应码枚举 |
| `mate-common/mate-base/src/main/java/vip/mate/base/exception/ErrorCode.java` | New | 错误码接口 |
| `mate-common/mate-base/src/main/java/vip/mate/base/exception/BizException.java` | New | 业务异常类 |
| `mate-common/mate-base/src/main/java/vip/mate/base/request/BasePageReq.java` | New | 分页请求基类 |
| `mate-common/mate-api/pom.xml` | New | mate-api 模块 POM |
| `mate-common/mate-api/src/main/java/vip/mate/api/system/service/IRpcUserService.java` | New | 用户 RPC 接口 |
| `mate-common/mate-api/src/main/java/vip/mate/api/system/service/IRpcDictService.java` | New | 字典 RPC 接口 |
| `mate-common/mate-api/src/main/java/vip/mate/api/system/command/RegisterUserCommand.java` | New | 注册命令 DTO |
| `mate-common/mate-api/src/main/java/vip/mate/api/system/response/UserInfoResponse.java` | New | 用户信息响应 DTO |
| `mate-common/mate-api/src/main/java/vip/mate/api/system/enums/UserStatus.java` | New | 用户状态枚举 |
| `mate-common/mate-api/src/main/java/vip/mate/api/rpc/RpcConstants.java` | New | RPC 常量 |

## 验证方案

1. `cd mate-common && mvn compile` 编译通过
2. `mate-base` 无任何 Spring Boot / auto-configuration 依赖
3. `mate-api` 仅依赖 `mate-base` + validation API，无 Spring 运行时依赖
4. 所有类 Serializable，支持 Dubbo Hessian2 序列化
5. BaseEntity 的 `@SuperBuilder` 可被子类正确继承
