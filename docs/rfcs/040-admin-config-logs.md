# RFC-040: Admin Config & Logs Modules — 补全 RFC-009

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 8
- **Dependencies**: RFC-009

## 背景

RFC-009 设计了 mate-admin 的完整功能：RBAC（Admin/Role/Menu）、字典（Dict）、系统配置（Config）、操作日志（OperationLog）、登录日志（LoginLog）。

当前 RBAC 和 Dict 已有完整 DDD 实现。**Config、OperationLog、LoginLog 三个模块缺失**——没有 DAO、没有 PO、没有 Controller、没有领域模型。

本 RFC 补全这三个模块，遵循 mate-admin 现有的 DDD 分层模式。

## 设计方案

### Part 1: Config 系统配置模块

#### Change 1: Config 领域实体

File: `mate-admin/src/main/java/vip/mate/admin/domain/config/model/entity/Config.java`

```java
package vip.mate.admin.domain.config.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;

/**
 * System configuration key-value pair.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Config extends BaseEntity {

    /** Config key (unique), e.g. "sys.user.initPassword" */
    private String configKey;

    /** Config value, e.g. "123456" */
    private String configValue;

    /** Human-readable name */
    private String configName;

    /** Whether this is a built-in config (cannot be deleted) */
    private Boolean builtIn;

    /** Remark / description */
    private String remark;

    public static Config create(String configKey, String configValue, String configName, String remark) {
        return Config.builder()
                .configKey(configKey)
                .configValue(configValue)
                .configName(configName)
                .builtIn(false)
                .remark(remark)
                .build();
    }
}
```

#### Change 2: Config Repository 接口

File: `mate-admin/src/main/java/vip/mate/admin/domain/config/adapter/repository/ConfigRepository.java`

```java
package vip.mate.admin.domain.config.adapter.repository;

import vip.mate.admin.domain.config.model.entity.Config;
import java.util.List;

public interface ConfigRepository {
    void save(Config config);
    void update(Config config);
    void deleteById(String id);
    Config findById(String id);
    Config findByKey(String configKey);
    boolean existsByKey(String configKey);
    List<Config> list();
}
```

#### Change 3: Config PO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/po/ConfigPO.java`

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("mate_config")
public class ConfigPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String configKey;
    private String configValue;
    private String configName;
    private Boolean builtIn;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
    @TableLogic
    private Integer deleted;
}
```

#### Change 4: Config DAO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/ConfigDao.java`

```java
package vip.mate.admin.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import vip.mate.admin.infrastructure.dao.po.ConfigPO;

@Mapper
public interface ConfigDao extends BaseMapper<ConfigPO> {

    @Select("SELECT * FROM mate_config WHERE config_key = #{configKey} AND deleted = 0")
    ConfigPO selectByKey(String configKey);

    @Select("SELECT COUNT(*) > 0 FROM mate_config WHERE config_key = #{configKey} AND deleted = 0")
    boolean existsByKey(String configKey);
}
```

#### Change 5: Config Repository 实现

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/adapter/repository/ConfigRepositoryImpl.java`

```java
package vip.mate.admin.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.admin.domain.config.model.entity.Config;
import vip.mate.admin.infrastructure.dao.ConfigDao;
import vip.mate.admin.infrastructure.dao.po.ConfigPO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConfigRepositoryImpl implements ConfigRepository {

    private final ConfigDao configDao;

    @Override
    public void save(Config config) {
        configDao.insert(toPO(config));
    }

    @Override
    public void update(Config config) {
        configDao.updateById(toPO(config));
    }

    @Override
    public void deleteById(String id) {
        configDao.deleteById(id);
    }

    @Override
    public Config findById(String id) {
        ConfigPO po = configDao.selectById(id);
        return po != null ? toEntity(po) : null;
    }

    @Override
    public Config findByKey(String configKey) {
        ConfigPO po = configDao.selectByKey(configKey);
        return po != null ? toEntity(po) : null;
    }

    @Override
    public boolean existsByKey(String configKey) {
        return configDao.existsByKey(configKey);
    }

    @Override
    public List<Config> list() {
        return configDao.selectList(null).stream().map(this::toEntity).toList();
    }

    private ConfigPO toPO(Config c) {
        ConfigPO po = new ConfigPO();
        po.setId(c.getId());
        po.setConfigKey(c.getConfigKey());
        po.setConfigValue(c.getConfigValue());
        po.setConfigName(c.getConfigName());
        po.setBuiltIn(c.getBuiltIn());
        po.setRemark(c.getRemark());
        return po;
    }

    private Config toEntity(ConfigPO po) {
        return Config.builder()
                .id(po.getId())
                .configKey(po.getConfigKey())
                .configValue(po.getConfigValue())
                .configName(po.getConfigName())
                .builtIn(po.getBuiltIn())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
```

#### Change 6: Config CommandService

File: `mate-admin/src/main/java/vip/mate/admin/application/command/ConfigCommandService.java`

```java
package vip.mate.admin.application.command;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.admin.domain.config.model.entity.Config;
import vip.mate.admin.types.exception.AdminErrorCode;
import vip.mate.base.exception.BizException;

@Service
@RequiredArgsConstructor
public class ConfigCommandService {

    private final ConfigRepository configRepository;

    @Transactional
    public String createConfig(String configKey, String configValue, String configName, String remark) {
        if (configRepository.existsByKey(configKey)) {
            throw BizException.of(AdminErrorCode.DUPLICATE_CONFIG_KEY);
        }
        Config config = Config.create(configKey, configValue, configName, remark);
        configRepository.save(config);
        return config.getId();
    }

    @Transactional
    public void updateConfig(String id, String configValue, String remark) {
        Config config = findOrThrow(id);
        config.setConfigValue(configValue);
        config.setRemark(remark);
        configRepository.update(config);
    }

    @Transactional
    public void deleteConfig(String id) {
        Config config = findOrThrow(id);
        if (Boolean.TRUE.equals(config.getBuiltIn())) {
            throw BizException.of(AdminErrorCode.CANNOT_DELETE_BUILTIN_CONFIG);
        }
        configRepository.deleteById(id);
    }

    private Config findOrThrow(String id) {
        Config config = configRepository.findById(id);
        if (config == null) throw BizException.of(AdminErrorCode.CONFIG_NOT_EXIST);
        return config;
    }
}
```

#### Change 7: Config QueryService

File: `mate-admin/src/main/java/vip/mate/admin/application/query/IConfigQueryService.java`

```java
package vip.mate.admin.application.query;

import java.util.Date;
import java.util.List;

public interface IConfigQueryService {
    ConfigVO findById(String id);
    ConfigVO findByKey(String configKey);
    List<ConfigVO> list();

    record ConfigVO(String id, String configKey, String configValue,
                    String configName, Boolean builtIn, String remark, Date createdAt) {}
}
```

File: `mate-admin/src/main/java/vip/mate/admin/application/query/impl/ConfigQueryServiceImpl.java`

```java
package vip.mate.admin.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.application.query.IConfigQueryService;
import vip.mate.admin.domain.config.adapter.repository.ConfigRepository;
import vip.mate.admin.domain.config.model.entity.Config;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigQueryServiceImpl implements IConfigQueryService {

    private final ConfigRepository configRepository;

    @Override
    public ConfigVO findById(String id) {
        return toVO(configRepository.findById(id));
    }

    @Override
    public ConfigVO findByKey(String configKey) {
        return toVO(configRepository.findByKey(configKey));
    }

    @Override
    public List<ConfigVO> list() {
        return configRepository.list().stream().map(this::toVO).toList();
    }

    private ConfigVO toVO(Config c) {
        if (c == null) return null;
        return new ConfigVO(c.getId(), c.getConfigKey(), c.getConfigValue(),
                c.getConfigName(), c.getBuiltIn(), c.getRemark(), c.getCreatedAt());
    }
}
```

#### Change 8: Config Controller

File: `mate-admin/src/main/java/vip/mate/admin/trigger/controller/ConfigController.java`

```java
package vip.mate.admin.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.command.ConfigCommandService;
import vip.mate.admin.application.query.IConfigQueryService;
import vip.mate.admin.application.query.IConfigQueryService.ConfigVO;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigCommandService commandService;
    private final IConfigQueryService queryService;

    @PostMapping
    public Result<String> create(@RequestBody CreateConfigReq req) {
        return Result.ok(commandService.createConfig(
                req.configKey(), req.configValue(), req.configName(), req.remark()));
    }

    @GetMapping("/{id}")
    public Result<ConfigVO> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    @GetMapping("/key/{configKey}")
    public Result<ConfigVO> getByKey(@PathVariable String configKey) {
        return Result.ok(queryService.findByKey(configKey));
    }

    @GetMapping
    public Result<List<ConfigVO>> list() {
        return Result.ok(queryService.list());
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody UpdateConfigReq req) {
        commandService.updateConfig(id, req.configValue(), req.remark());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        commandService.deleteConfig(id);
        return Result.ok();
    }

    record CreateConfigReq(String configKey, String configValue, String configName, String remark) {}
    record UpdateConfigReq(String configValue, String remark) {}
}
```

---

### Part 2: OperationLog 操作日志模块

操作日志是只读的（只写入 + 查询，不修改不删除），不需要 Command 层。通过 AOP 注解自动记录。

#### Change 9: OperationLog PO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/po/OperationLogPO.java`

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("mate_operation_log")
public class OperationLogPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** Operator user ID */
    private String userId;
    /** Operator username */
    private String username;
    /** Operation module (e.g. "Admin", "Role", "Config") */
    private String module;
    /** Operation type (CREATE/UPDATE/DELETE/QUERY/EXPORT) */
    private String operationType;
    /** Request method (GET/POST/PUT/DELETE) */
    private String requestMethod;
    /** Request URL */
    private String requestUrl;
    /** Request params (JSON) */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private String requestParams;
    /** Response result (JSON, truncated to 2000 chars) */
    private String responseResult;
    /** Client IP */
    private String clientIp;
    /** Status (0=success, 1=failure) */
    private Integer status;
    /** Error message (if failed) */
    private String errorMsg;
    /** Execution time in ms */
    private Long duration;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
```

#### Change 10: OperationLog DAO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/OperationLogDao.java`

```java
package vip.mate.admin.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.OperationLogPO;

@Mapper
public interface OperationLogDao extends BaseMapper<OperationLogPO> {
}
```

#### Change 11: OperationLog QueryService

File: `mate-admin/src/main/java/vip/mate/admin/application/query/IOperationLogQueryService.java`

```java
package vip.mate.admin.application.query;

import java.util.Date;
import java.util.List;

public interface IOperationLogQueryService {
    List<OperationLogVO> pageQuery(int pageNum, int pageSize, String module, String username);

    record OperationLogVO(String id, String userId, String username, String module,
                          String operationType, String requestMethod, String requestUrl,
                          String clientIp, Integer status, String errorMsg,
                          Long duration, Date createdAt) {}
}
```

File: `mate-admin/src/main/java/vip/mate/admin/application/query/impl/OperationLogQueryServiceImpl.java`

```java
package vip.mate.admin.application.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.application.query.IOperationLogQueryService;
import vip.mate.admin.infrastructure.dao.OperationLogDao;
import vip.mate.admin.infrastructure.dao.po.OperationLogPO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogQueryServiceImpl implements IOperationLogQueryService {

    private final OperationLogDao operationLogDao;

    @Override
    public List<OperationLogVO> pageQuery(int pageNum, int pageSize, String module, String username) {
        LambdaQueryWrapper<OperationLogPO> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isBlank()) {
            wrapper.eq(OperationLogPO::getModule, module);
        }
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLogPO::getUsername, username);
        }
        wrapper.orderByDesc(OperationLogPO::getCreatedAt);

        Page<OperationLogPO> page = operationLogDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream().map(this::toVO).toList();
    }

    private OperationLogVO toVO(OperationLogPO po) {
        return new OperationLogVO(po.getId(), po.getUserId(), po.getUsername(),
                po.getModule(), po.getOperationType(), po.getRequestMethod(),
                po.getRequestUrl(), po.getClientIp(), po.getStatus(),
                po.getErrorMsg(), po.getDuration(), po.getCreatedAt());
    }
}
```

#### Change 12: OperationLog Controller

File: `mate-admin/src/main/java/vip/mate/admin/trigger/controller/OperationLogController.java`

```java
package vip.mate.admin.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.query.IOperationLogQueryService;
import vip.mate.admin.application.query.IOperationLogQueryService.OperationLogVO;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final IOperationLogQueryService queryService;

    @GetMapping
    public Result<List<OperationLogVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String username) {
        return Result.ok(queryService.pageQuery(pageNum, pageSize, module, username));
    }
}
```

#### Change 13: @OperationLog AOP 注解和切面

注解用于标注需要记录操作日志的 Controller 方法。

File: `mate-admin/src/main/java/vip/mate/admin/trigger/annotation/OperationLog.java`

```java
package vip.mate.admin.trigger.annotation;

import java.lang.annotation.*;

/**
 * Mark a controller method for automatic operation log recording.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** Module name, e.g. "Admin", "Role", "Config" */
    String module();
    /** Operation type, e.g. "CREATE", "UPDATE", "DELETE" */
    String type();
}
```

File: `mate-admin/src/main/java/vip/mate/admin/trigger/aspect/OperationLogAspect.java`

```java
package vip.mate.admin.trigger.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vip.mate.admin.infrastructure.dao.OperationLogDao;
import vip.mate.admin.infrastructure.dao.po.OperationLogPO;
import vip.mate.admin.trigger.annotation.OperationLog;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogDao operationLogDao;
    private final ObjectMapper objectMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogPO logPO = new OperationLogPO();
        logPO.setModule(opLog.module());
        logPO.setOperationType(opLog.type());

        // Extract request context
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            logPO.setRequestMethod(request.getMethod());
            logPO.setRequestUrl(request.getRequestURI());
            logPO.setClientIp(getClientIp(request));
            logPO.setUserId(request.getHeader("X-User-Id"));
            logPO.setUsername(request.getHeader("X-User-Name"));
        }

        // Capture request params
        try {
            String params = objectMapper.writeValueAsString(joinPoint.getArgs());
            logPO.setRequestParams(truncate(params, 2000));
        } catch (Exception e) {
            logPO.setRequestParams("[serialization error]");
        }

        Object result;
        try {
            result = joinPoint.proceed();
            logPO.setStatus(0);
            try {
                String resultStr = objectMapper.writeValueAsString(result);
                logPO.setResponseResult(truncate(resultStr, 2000));
            } catch (Exception ignored) {}
        } catch (Throwable ex) {
            logPO.setStatus(1);
            logPO.setErrorMsg(truncate(ex.getMessage(), 500));
            throw ex;
        } finally {
            logPO.setDuration(System.currentTimeMillis() - start);
            try {
                operationLogDao.insert(logPO);
            } catch (Exception e) {
                log.error("[OperationLog] Failed to save operation log", e);
            }
        }
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }

    private String truncate(String str, int maxLen) {
        return str != null && str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
```

---

### Part 3: LoginLog 登录日志模块

#### Change 14: LoginLog PO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/po/LoginLogPO.java`

```java
package vip.mate.admin.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("mate_login_log")
public class LoginLogPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    /** Login username */
    private String username;
    /** Client IP */
    private String clientIp;
    /** User agent */
    private String userAgent;
    /** Login type (PASSWORD/SMS/OAUTH) */
    private String loginType;
    /** Status (0=success, 1=failure) */
    private Integer status;
    /** Failure message */
    private String failMsg;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;
}
```

#### Change 15: LoginLog DAO

File: `mate-admin/src/main/java/vip/mate/admin/infrastructure/dao/LoginLogDao.java`

```java
package vip.mate.admin.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.admin.infrastructure.dao.po.LoginLogPO;

@Mapper
public interface LoginLogDao extends BaseMapper<LoginLogPO> {
}
```

#### Change 16: LoginLog QueryService

File: `mate-admin/src/main/java/vip/mate/admin/application/query/ILoginLogQueryService.java`

```java
package vip.mate.admin.application.query;

import java.util.Date;
import java.util.List;

public interface ILoginLogQueryService {
    List<LoginLogVO> pageQuery(int pageNum, int pageSize, String username, Integer status);

    record LoginLogVO(String id, String username, String clientIp, String userAgent,
                      String loginType, Integer status, String failMsg, Date createdAt) {}
}
```

File: `mate-admin/src/main/java/vip/mate/admin/application/query/impl/LoginLogQueryServiceImpl.java`

```java
package vip.mate.admin.application.query.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.admin.application.query.ILoginLogQueryService;
import vip.mate.admin.infrastructure.dao.LoginLogDao;
import vip.mate.admin.infrastructure.dao.po.LoginLogPO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginLogQueryServiceImpl implements ILoginLogQueryService {

    private final LoginLogDao loginLogDao;

    @Override
    public List<LoginLogVO> pageQuery(int pageNum, int pageSize, String username, Integer status) {
        LambdaQueryWrapper<LoginLogPO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(LoginLogPO::getUsername, username);
        }
        if (status != null) {
            wrapper.eq(LoginLogPO::getStatus, status);
        }
        wrapper.orderByDesc(LoginLogPO::getCreatedAt);

        Page<LoginLogPO> page = loginLogDao.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream().map(this::toVO).toList();
    }

    private LoginLogVO toVO(LoginLogPO po) {
        return new LoginLogVO(po.getId(), po.getUsername(), po.getClientIp(),
                po.getUserAgent(), po.getLoginType(), po.getStatus(),
                po.getFailMsg(), po.getCreatedAt());
    }
}
```

#### Change 17: LoginLog Controller

File: `mate-admin/src/main/java/vip/mate/admin/trigger/controller/LoginLogController.java`

```java
package vip.mate.admin.trigger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.admin.application.query.ILoginLogQueryService;
import vip.mate.admin.application.query.ILoginLogQueryService.LoginLogVO;
import vip.mate.base.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/login-logs")
@RequiredArgsConstructor
public class LoginLogController {

    private final ILoginLogQueryService queryService;

    @GetMapping
    public Result<List<LoginLogVO>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.ok(queryService.pageQuery(pageNum, pageSize, username, status));
    }
}
```

---

### Part 4: ErrorCode 扩展

#### Change 18: 新增 Config 和 Log 相关错误码

确认现有 `AdminErrorCode` 枚举中已包含以下错误码（无需新增）：

```java
// Config errors (already defined in AdminErrorCode.java)
DUPLICATE_CONFIG_KEY("SYSB010", "Config key already exists"),   // 已存在
CANNOT_DELETE_BUILTIN_CONFIG("SYSB011", "Cannot delete built-in config"),  // 已存在
CONFIG_NOT_EXIST("A007", "Config does not exist"),              // 已存在，使用 A007 编码
```

> 注意：`CONFIG_NOT_EXIST` 使用 `A007` 而非 `SYSB012`，与现有代码保持一致。

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `domain/config/model/entity/Config.java` | New | Config 领域实体 |
| `domain/config/adapter/repository/ConfigRepository.java` | New | Config 仓储接口 |
| `infrastructure/dao/po/ConfigPO.java` | New | Config 持久化对象 |
| `infrastructure/dao/ConfigDao.java` | New | Config MyBatis Mapper |
| `infrastructure/adapter/repository/ConfigRepositoryImpl.java` | New | Config 仓储实现 |
| `application/command/ConfigCommandService.java` | New | Config 写服务 |
| `application/query/IConfigQueryService.java` | New | Config 读接口 |
| `application/query/impl/ConfigQueryServiceImpl.java` | New | Config 读实现 |
| `trigger/controller/ConfigController.java` | New | Config REST 控制器 |
| `infrastructure/dao/po/OperationLogPO.java` | New | 操作日志 PO |
| `infrastructure/dao/OperationLogDao.java` | New | 操作日志 DAO |
| `application/query/IOperationLogQueryService.java` | New | 操作日志查询接口 |
| `application/query/impl/OperationLogQueryServiceImpl.java` | New | 操作日志查询实现 |
| `trigger/controller/OperationLogController.java` | New | 操作日志 Controller |
| `trigger/annotation/OperationLog.java` | New | 操作日志注解 |
| `trigger/aspect/OperationLogAspect.java` | New | 操作日志 AOP 切面 |
| `infrastructure/dao/po/LoginLogPO.java` | New | 登录日志 PO |
| `infrastructure/dao/LoginLogDao.java` | New | 登录日志 DAO |
| `application/query/ILoginLogQueryService.java` | New | 登录日志查询接口 |
| `application/query/impl/LoginLogQueryServiceImpl.java` | New | 登录日志查询实现 |
| `trigger/controller/LoginLogController.java` | New | 登录日志 Controller |
| `types/exception/AdminErrorCode.java` | Modify | 新增 Config 错误码 |

## 验证方案

1. `mvn clean compile -pl mate-admin -am` — 编译通过
2. 启动 mate-admin 服务
3. Config CRUD 测试：
   - `POST /api/v1/admin/configs` — 创建配置项
   - `GET /api/v1/admin/configs` — 获取列表
   - `GET /api/v1/admin/configs/key/sys.user.initPassword` — 按 key 查询
   - `PUT /api/v1/admin/configs/{id}` — 修改
   - `DELETE /api/v1/admin/configs/{id}` — 删除
4. OperationLog 测试：
   - 给 AdminController 的 create 方法加 `@OperationLog(module = "Admin", type = "CREATE")`
   - 调用创建 admin 接口
   - `GET /api/v1/admin/operation-logs` — 应能查到记录
5. LoginLog 测试：
   - 手动插入一条 mate_login_log 记录
   - `GET /api/v1/admin/login-logs` — 应能查到

## 注意事项

- **操作日志是异步友好的**：当前实现是同步写入，如果并发量大可以改用 `@Async` 或 MQ
- **登录日志写入方**：LoginLog 由 mate-auth 写入（通过 Dubbo RPC 调用 mate-admin 或直接写入同库），本 RFC 只负责查询端
- **@OperationLog 注解选择性使用**：不是所有接口都需要记录，只在关键写操作上标注
- **Config 缓存**：频繁读取的配置可用 `@Cacheable` 缓存，当前先不加，后续按需引入
- **所有路径都在 mate-admin 包内**：遵循 DDD 分层，新增文件在 `vip.mate.admin.*` 包下
