# RFC-008: mate-system DDD Example Business Module

- **Status**: Draft
- **Created**: 2026-04-11
- **Author**: MateCloud Team
- **Wave**: 3 (parallel with RFC-006, RFC-007)
- **Dependencies**: RFC-002 (mate-common), RFC-003 (core starters), RFC-004 (infra starters), RFC-005 (capability starters)

## 背景

mate-system 是 matecloud 脚手架的 **DDD 示范模块**，包含用户管理子域，展示完整的四层六边形架构（trigger -> application -> domain -> infrastructure）、CQRS、聚合根、领域事件等模式。后续业务模块照此模式复制即可。

## 设计方案

### Module Info

- **Port**: 9030
- **Package**: `vip.mate.system`
- **ArtifactId**: `mate-system`
- **Parent**: `mate-biz`

### Package Structure

```
vip.mate.system/
├── MateSystemApplication.java
├── trigger/
│   ├── controller/
│   │   └── UserController.java
│   └── rpc/
│       └── RpcUserServiceImpl.java
├── application/
│   ├── command/
│   │   └── UserCommandService.java
│   ├── query/
│   │   ├── IUserQueryService.java
│   │   └── impl/
│   │       └── UserQueryServiceImpl.java
│   └── convertor/
│       └── UserConvertor.java
├── domain/
│   ├── model/
│   │   ├── aggregate/
│   │   │   └── UserAggregate.java
│   │   ├── entity/
│   │   │   ├── User.java
│   │   │   └── UserOperateStream.java
│   │   └── valobj/
│   │       └── UserStatus.java
│   ├── service/
│   │   ├── IUserDomainService.java
│   │   └── impl/
│   │       └── UserDomainServiceImpl.java
│   ├── adapter/
│   │   ├── repository/
│   │   │   └── UserRepository.java
│   │   └── port/
│   │       └── DomainEventPublisher.java
│   ├── event/
│   │   ├── BaseDomainEvent.java
│   │   └── UserCreatedEvent.java
│   └── constant/
│       └── UserOperateType.java
├── infrastructure/
│   ├── adapter/
│   │   ├── repository/
│   │   │   ├── UserRepositoryImpl.java
│   │   │   └── convertor/
│   │   │       └── UserInfraConvertor.java
│   │   └── port/
│   │       └── DomainEventPublisherImpl.java
│   ├── dao/
│   │   ├── UserDao.java
│   │   ├── UserOperateStreamDao.java
│   │   └── po/
│   │       ├── UserPO.java
│   │       └── UserOperateStreamPO.java
│   └── config/
└── types/
    ├── exception/
    │   ├── UserErrorCode.java
    │   └── UserException.java
    └── constant/
```

---

### Change 1: pom.xml

File: `mate-biz/mate-system/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-biz</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-system</artifactId>
    <packaging>jar</packaging>
    <name>mate-system</name>
    <description>MateCloud System Service - User management DDD example</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
    </properties>

    <dependencies>
        <!-- mate-common -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api</artifactId>
        </dependency>

        <!-- mate-starters -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-nacos-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-rpc-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-sa-token-starter</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>cn.dev33</groupId>
                    <artifactId>sa-token-reactor-spring-boot3-starter</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-mq-starter</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### Change 2: Application Entry Point

File: `src/main/java/vip/mate/system/MateSystemApplication.java`

```java
package vip.mate.system;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "vip.mate.system")
@EnableDubbo
public class MateSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(MateSystemApplication.class, args);
    }
}
```

---

### Change 3: bootstrap.yml

File: `src/main/resources/bootstrap.yml`

```yaml
spring:
  application:
    name: mate-system
  profiles:
    active: dev
  config:
    import:
      - classpath:nacos.yml
      - nacos:${spring.application.name}-${spring.profiles.active}.yml
      - nacos:mate-ds.yml
      - nacos:mate-rpc.yml
      - nacos:mate-cache.yml
      - nacos:mate-mq.yml
      - nacos:mate-monitor.yml
server:
  port: 9030
```

---

### Change 4: Domain Layer - Aggregate Root

File: `domain/model/aggregate/UserAggregate.java`

```java
package vip.mate.system.domain.model.aggregate;

import lombok.Builder;
import lombok.Data;
import vip.mate.system.domain.constant.UserOperateType;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class UserAggregate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private User user;

    @Builder.Default
    private List<UserOperateStream> operateStreams = new ArrayList<>();

    public static UserAggregate create(User user) {
        if (user == null) {
            throw UserException.of(UserErrorCode.REQUEST_PARAM_NULL);
        }
        return UserAggregate.builder().user(user).build();
    }

    public static UserAggregate create(String mobile, String nickName) {
        User user = User.create(mobile, nickName);
        UserAggregate aggregate = create(user);
        aggregate.addOperateStream(UserOperateType.CREATE,
                String.format("User created, mobile: %s", mobile));
        return aggregate;
    }

    public String getId() {
        return user.getId();
    }

    public void changeNickName(String newNickName) {
        String oldNickName = user.getNickName();
        user.changeNickName(newNickName);
        addOperateStream(UserOperateType.CHANGE_NICKNAME,
                String.format("Nickname: %s -> %s", oldNickName, newNickName));
    }

    public void freeze() {
        user.freeze();
        addOperateStream(UserOperateType.FREEZE, "User frozen");
    }

    public void unfreeze() {
        user.unfreeze();
        addOperateStream(UserOperateType.UNFREEZE, "User unfrozen");
    }

    public void delete() {
        user.delete();
        addOperateStream(UserOperateType.DELETE, "User deleted");
    }

    private void addOperateStream(UserOperateType type, String detail) {
        operateStreams.add(UserOperateStream.create(user.getId(), type, detail));
    }

    public List<UserOperateStream> getAndClearOperateStreams() {
        List<UserOperateStream> streams = new ArrayList<>(operateStreams);
        operateStreams.clear();
        return streams;
    }
}
```

---

### Change 5: Domain Layer - Entity

File: `domain/model/entity/User.java`

```java
package vip.mate.system.domain.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

import java.util.Date;
import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private String mobile;
    private String nickName;
    private String avatar;
    private Integer gender;
    private UserStatus status;
    private Date lastLoginTime;

    public static User create(String mobile, String nickName) {
        return User.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .mobile(mobile)
                .nickName(nickName)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public void changeNickName(String newNickName) {
        checkActive();
        this.nickName = newNickName;
    }

    public void freeze() {
        checkActive();
        this.status = UserStatus.FROZEN;
    }

    public void unfreeze() {
        if (!status.isFrozen()) {
            throw UserException.of(UserErrorCode.USER_STATUS_INVALID);
        }
        this.status = UserStatus.ACTIVE;
    }

    public void delete() {
        if (status.isDeleted()) {
            throw UserException.of(UserErrorCode.USER_IS_DELETED);
        }
        this.status = UserStatus.DELETED;
    }

    public void updateLastLoginTime() {
        this.lastLoginTime = new Date();
    }

    public boolean isActive() { return status == UserStatus.ACTIVE; }
    public boolean isFrozen() { return status == UserStatus.FROZEN; }
    public boolean isDeleted() { return status == UserStatus.DELETED; }

    private void checkActive() {
        if (isFrozen()) throw UserException.of(UserErrorCode.USER_IS_FROZEN);
        if (isDeleted()) throw UserException.of(UserErrorCode.USER_IS_DELETED);
    }
}
```

File: `domain/model/entity/UserOperateStream.java`

```java
package vip.mate.system.domain.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import vip.mate.base.model.entity.BaseEntity;
import vip.mate.system.domain.constant.UserOperateType;

import java.util.Date;
import java.util.UUID;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserOperateStream extends BaseEntity {

    private String userId;
    private UserOperateType operateType;
    private String operateDetail;
    private Date operateTime;

    public static UserOperateStream create(String userId, UserOperateType type, String detail) {
        return UserOperateStream.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .userId(userId)
                .operateType(type)
                .operateDetail(detail)
                .operateTime(new Date())
                .build();
    }
}
```

---

### Change 6: Domain Layer - Value Object

File: `domain/model/valobj/UserStatus.java`

```java
package vip.mate.system.domain.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    ACTIVE(0, "Active"),
    FROZEN(1, "Frozen"),
    DELETED(2, "Deleted");

    private final int code;
    private final String desc;

    public boolean isActive() { return this == ACTIVE; }
    public boolean isFrozen() { return this == FROZEN; }
    public boolean isDeleted() { return this == DELETED; }
    public boolean canModify() { return this == ACTIVE; }
}
```

---

### Change 7: Domain Layer - Constants

File: `domain/constant/UserOperateType.java`

```java
package vip.mate.system.domain.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserOperateType {

    CREATE("CREATE", "Create user"),
    LOGIN("LOGIN", "User login"),
    CHANGE_NICKNAME("CHANGE_NICKNAME", "Change nickname"),
    FREEZE("FREEZE", "Freeze user"),
    UNFREEZE("UNFREEZE", "Unfreeze user"),
    DELETE("DELETE", "Delete user"),
    UPDATE_AVATAR("UPDATE_AVATAR", "Update avatar");

    private final String code;
    private final String description;
}
```

---

### Change 8: Domain Layer - Repository Interface

File: `domain/adapter/repository/UserRepository.java`

```java
package vip.mate.system.domain.adapter.repository;

import vip.mate.system.domain.model.aggregate.UserAggregate;

import java.util.List;

public interface UserRepository {

    void save(UserAggregate userAggregate);

    void update(UserAggregate userAggregate);

    UserAggregate findById(String id);

    UserAggregate findByMobile(String mobile);

    boolean existsByMobile(String mobile);

    List<UserAggregate> pageQuery(int pageNum, int pageSize);
}
```

---

### Change 9: Domain Layer - Events

File: `domain/event/BaseDomainEvent.java`

```java
package vip.mate.system.domain.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class BaseDomainEvent {

    private final String eventId;
    private final LocalDateTime occurredAt;
    private final String aggregateId;
    private final String aggregateType;
    private final int version;

    protected BaseDomainEvent(String aggregateId, String aggregateType) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.version = 1;
    }

    public abstract String getEventType();
    public abstract String getDescription();
}
```

File: `domain/event/UserCreatedEvent.java`

```java
package vip.mate.system.domain.event;

import lombok.Getter;

@Getter
public class UserCreatedEvent extends BaseDomainEvent {

    private final String mobile;
    private final String nickName;

    public UserCreatedEvent(String aggregateId, String mobile, String nickName) {
        super(aggregateId, "UserAggregate");
        this.mobile = mobile;
        this.nickName = nickName;
    }

    @Override
    public String getEventType() { return "USER_CREATED"; }

    @Override
    public String getDescription() {
        return String.format("User created: %s (%s)", nickName, mobile);
    }
}
```

---

### Change 10: Domain Layer - Port Interface

File: `domain/adapter/port/DomainEventPublisher.java`

```java
package vip.mate.system.domain.adapter.port;

import vip.mate.system.domain.event.BaseDomainEvent;

public interface DomainEventPublisher {

    void publish(BaseDomainEvent event);

    void publishAsync(BaseDomainEvent event);
}
```

---

### Change 11: Domain Layer - Domain Service

File: `domain/service/IUserDomainService.java`

```java
package vip.mate.system.domain.service;

import vip.mate.system.domain.model.aggregate.UserAggregate;

public interface IUserDomainService {

    UserAggregate createUser(String mobile, String nickName);

    UserAggregate findByIdOrThrow(String userId);

    UserAggregate changeNickName(String userId, String nickName);

    UserAggregate freezeUser(String userId);

    UserAggregate unfreezeUser(String userId);

    UserAggregate deleteUser(String userId);
}
```

File: `domain/service/impl/UserDomainServiceImpl.java`

```java
package vip.mate.system.domain.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.service.IUserDomainService;
import vip.mate.system.types.exception.UserErrorCode;
import vip.mate.system.types.exception.UserException;

@Service
@RequiredArgsConstructor
public class UserDomainServiceImpl implements IUserDomainService {

    private final UserRepository userRepository;

    @Override
    public UserAggregate createUser(String mobile, String nickName) {
        if (userRepository.existsByMobile(mobile)) {
            throw UserException.of(UserErrorCode.DUPLICATE_MOBILE);
        }
        if (nickName == null || nickName.isBlank()) {
            nickName = "user_" + mobile.substring(mobile.length() - 4);
        }
        return UserAggregate.create(mobile, nickName);
    }

    @Override
    public UserAggregate findByIdOrThrow(String userId) {
        UserAggregate aggregate = userRepository.findById(userId);
        if (aggregate == null) {
            throw UserException.of(UserErrorCode.USER_NOT_EXIST);
        }
        return aggregate;
    }

    @Override
    public UserAggregate changeNickName(String userId, String nickName) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.changeNickName(nickName);
        return aggregate;
    }

    @Override
    public UserAggregate freezeUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.freeze();
        return aggregate;
    }

    @Override
    public UserAggregate unfreezeUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.unfreeze();
        return aggregate;
    }

    @Override
    public UserAggregate deleteUser(String userId) {
        UserAggregate aggregate = findByIdOrThrow(userId);
        aggregate.delete();
        return aggregate;
    }
}
```

---

### Change 12: Application Layer - Command Service

File: `application/command/UserCommandService.java`

```java
package vip.mate.system.application.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.event.UserCreatedEvent;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.service.IUserDomainService;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final IUserDomainService userDomainService;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public String createUser(RegisterUserCommand command) {
        UserAggregate aggregate = userDomainService.createUser(
                command.getMobile(), command.getNickName());
        userRepository.save(aggregate);

        eventPublisher.publishAsync(new UserCreatedEvent(
                aggregate.getId(),
                command.getMobile(),
                command.getNickName()));

        log.info("User created, id={}, mobile={}", aggregate.getId(), command.getMobile());
        return aggregate.getId();
    }

    @Transactional
    public void changeNickName(String userId, String nickName) {
        UserAggregate aggregate = userDomainService.changeNickName(userId, nickName);
        userRepository.update(aggregate);
    }

    @Transactional
    public void freezeUser(String userId) {
        UserAggregate aggregate = userDomainService.freezeUser(userId);
        userRepository.update(aggregate);
    }

    @Transactional
    public void unfreezeUser(String userId) {
        UserAggregate aggregate = userDomainService.unfreezeUser(userId);
        userRepository.update(aggregate);
    }

    @Transactional
    public void deleteUser(String userId) {
        UserAggregate aggregate = userDomainService.deleteUser(userId);
        userRepository.update(aggregate);
    }
}
```

---

### Change 13: Application Layer - Query Service

File: `application/query/IUserQueryService.java`

```java
package vip.mate.system.application.query;

import vip.mate.api.system.response.UserInfoResponse;

import java.util.List;

public interface IUserQueryService {

    UserInfoResponse findById(String userId);

    List<UserInfoResponse> pageQuery(int pageNum, int pageSize);
}
```

File: `application/query/impl/UserQueryServiceImpl.java`

```java
package vip.mate.system.application.query.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.system.application.convertor.UserConvertor;
import vip.mate.system.application.query.IUserQueryService;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements IUserQueryService {

    private final UserRepository userRepository;
    private final UserConvertor userConvertor;

    @Override
    public UserInfoResponse findById(String userId) {
        UserAggregate aggregate = userRepository.findById(userId);
        if (aggregate == null) return null;
        return userConvertor.toResponse(aggregate);
    }

    @Override
    public List<UserInfoResponse> pageQuery(int pageNum, int pageSize) {
        return userRepository.pageQuery(pageNum, pageSize).stream()
                .map(userConvertor::toResponse)
                .toList();
    }
}
```

---

### Change 14: Application Layer - Convertor

File: `application/convertor/UserConvertor.java`

```java
package vip.mate.system.application.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.system.domain.model.aggregate.UserAggregate;

@Mapper(componentModel = "spring")
public interface UserConvertor {

    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.mobile", target = "mobile")
    @Mapping(source = "user.nickName", target = "nickName")
    @Mapping(source = "user.avatar", target = "avatar")
    @Mapping(source = "user.gender", target = "gender")
    @Mapping(source = "user.status", target = "status")
    UserInfoResponse toResponse(UserAggregate aggregate);
}
```

---

### Change 15: Trigger Layer - Controller

File: `trigger/controller/UserController.java`

```java
package vip.mate.system.trigger.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.base.result.Result;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    @PostMapping
    public Result<String> createUser(@Valid @RequestBody RegisterUserCommand command) {
        String userId = userCommandService.createUser(command);
        return Result.success(userId);
    }

    @GetMapping("/{id}")
    public Result<UserInfoResponse> getUser(@PathVariable String id) {
        return Result.success(userQueryService.findById(id));
    }

    @GetMapping
    public Result<List<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userQueryService.pageQuery(pageNum, pageSize));
    }

    @PutMapping("/{id}/nickname")
    public Result<Void> changeNickName(@PathVariable String id, @RequestParam String nickName) {
        userCommandService.changeNickName(id, nickName);
        return Result.success();
    }

    @PutMapping("/{id}/freeze")
    public Result<Void> freeze(@PathVariable String id) {
        userCommandService.freezeUser(id);
        return Result.success();
    }

    @PutMapping("/{id}/unfreeze")
    public Result<Void> unfreeze(@PathVariable String id) {
        userCommandService.unfreezeUser(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        userCommandService.deleteUser(id);
        return Result.success();
    }
}
```

---

### Change 16: Trigger Layer - RPC

File: `trigger/rpc/RpcUserServiceImpl.java`

```java
package vip.mate.system.trigger.rpc;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import vip.mate.api.rpc.RpcConstants;
import vip.mate.api.system.command.RegisterUserCommand;
import vip.mate.api.system.response.UserInfoResponse;
import vip.mate.api.system.service.IRpcUserService;
import vip.mate.base.result.Result;
import vip.mate.system.application.command.UserCommandService;
import vip.mate.system.application.query.IUserQueryService;

@DubboService(version = RpcConstants.DUBBO_VERSION)
@RequiredArgsConstructor
public class RpcUserServiceImpl implements IRpcUserService {

    private final UserCommandService userCommandService;
    private final IUserQueryService userQueryService;

    @Override
    public Result<UserInfoResponse> getById(String userId) {
        return Result.success(userQueryService.findById(userId));
    }

    @Override
    public Result<String> register(RegisterUserCommand command) {
        return Result.success(userCommandService.createUser(command));
    }
}
```

---

### Change 17: Infrastructure Layer - Repository Implementation

File: `infrastructure/adapter/repository/UserRepositoryImpl.java`

```java
package vip.mate.system.infrastructure.adapter.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.infrastructure.adapter.repository.convertor.UserInfraConvertor;
import vip.mate.system.infrastructure.dao.UserDao;
import vip.mate.system.infrastructure.dao.UserOperateStreamDao;
import vip.mate.system.infrastructure.dao.po.UserOperateStreamPO;
import vip.mate.system.infrastructure.dao.po.UserPO;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserDao userDao;
    private final UserOperateStreamDao userOperateStreamDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(UserAggregate aggregate) {
        UserPO po = UserInfraConvertor.INSTANCE.toPO(aggregate.getUser());
        userDao.insert(po);
        batchSaveStreams(aggregate.getAndClearOperateStreams());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserAggregate aggregate) {
        UserPO po = UserInfraConvertor.INSTANCE.toPO(aggregate.getUser());
        userDao.updateById(po);
        batchSaveStreams(aggregate.getAndClearOperateStreams());
    }

    @Override
    public UserAggregate findById(String id) {
        UserPO po = userDao.selectById(id);
        if (po == null) return null;
        User user = UserInfraConvertor.INSTANCE.toEntity(po);
        return UserAggregate.create(user);
    }

    @Override
    public UserAggregate findByMobile(String mobile) {
        UserPO po = userDao.selectByMobile(mobile);
        if (po == null) return null;
        User user = UserInfraConvertor.INSTANCE.toEntity(po);
        return UserAggregate.create(user);
    }

    @Override
    public boolean existsByMobile(String mobile) {
        return userDao.existsByMobile(mobile);
    }

    @Override
    public List<UserAggregate> pageQuery(int pageNum, int pageSize) {
        return userDao.pageQuery(pageNum, pageSize).stream()
                .map(po -> UserAggregate.create(UserInfraConvertor.INSTANCE.toEntity(po)))
                .toList();
    }

    private void batchSaveStreams(List<UserOperateStream> streams) {
        if (streams == null || streams.isEmpty()) return;
        for (UserOperateStream stream : streams) {
            UserOperateStreamPO po = UserInfraConvertor.INSTANCE.toStreamPO(stream);
            userOperateStreamDao.insert(po);
        }
    }
}
```

---

### Change 18: Infrastructure Layer - Convertor

File: `infrastructure/adapter/repository/convertor/UserInfraConvertor.java`

```java
package vip.mate.system.infrastructure.adapter.repository.convertor;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.entity.UserOperateStream;
import vip.mate.system.infrastructure.dao.po.UserOperateStreamPO;
import vip.mate.system.infrastructure.dao.po.UserPO;

@Mapper
public interface UserInfraConvertor {

    UserInfraConvertor INSTANCE = Mappers.getMapper(UserInfraConvertor.class);

    UserPO toPO(User user);

    User toEntity(UserPO po);

    UserOperateStreamPO toStreamPO(UserOperateStream stream);
}
```

---

### Change 19: Infrastructure Layer - DAO

File: `infrastructure/dao/UserDao.java`

```java
package vip.mate.system.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import vip.mate.system.infrastructure.dao.po.UserPO;

import java.util.List;

@Mapper
public interface UserDao extends BaseMapper<UserPO> {

    @Select("SELECT * FROM mate_user WHERE mobile = #{mobile} AND deleted = 0")
    UserPO selectByMobile(@Param("mobile") String mobile);

    @Select("SELECT COUNT(1) > 0 FROM mate_user WHERE mobile = #{mobile} AND deleted = 0")
    boolean existsByMobile(@Param("mobile") String mobile);

    @Select("SELECT * FROM mate_user WHERE deleted = 0 ORDER BY created_at DESC LIMIT #{offset}, #{pageSize}")
    List<UserPO> pageQuery(@Param("offset") int offset, @Param("pageSize") int pageSize);
}
```

File: `infrastructure/dao/UserOperateStreamDao.java`

```java
package vip.mate.system.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.system.infrastructure.dao.po.UserOperateStreamPO;

@Mapper
public interface UserOperateStreamDao extends BaseMapper<UserOperateStreamPO> {
}
```

---

### Change 20: Infrastructure Layer - PO

File: `infrastructure/dao/po/UserPO.java`

```java
package vip.mate.system.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("mate_user")
public class UserPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("mobile")
    private String mobile;

    @TableField("nick_name")
    private String nickName;

    @TableField("avatar")
    private String avatar;

    @TableField("gender")
    private Integer gender;

    @TableField("status")
    private Integer status;

    @TableField("last_login_time")
    private Date lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer lockVersion;
}
```

File: `infrastructure/dao/po/UserOperateStreamPO.java`

```java
package vip.mate.system.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("mate_user_operate_stream")
public class UserOperateStreamPO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("operate_type")
    private String operateType;

    @TableField("operate_detail")
    private String operateDetail;

    @TableField("operate_time")
    private Date operateTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    @TableLogic
    private Integer deleted;
}
```

---

### Change 21: Infrastructure Layer - Port Implementation

File: `infrastructure/adapter/port/DomainEventPublisherImpl.java`

```java
package vip.mate.system.infrastructure.adapter.port;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.domain.event.BaseDomainEvent;

@Component
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(BaseDomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @Override
    @Async
    public void publishAsync(BaseDomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
```

---

### Change 22: Types Layer - Exceptions

File: `types/exception/UserErrorCode.java`

```java
package vip.mate.system.types.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vip.mate.base.exception.ErrorCode;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    REQUEST_PARAM_NULL("U001", "Request parameter is null"),
    DUPLICATE_MOBILE("U002", "Mobile number already exists"),
    USER_NOT_EXIST("U003", "User does not exist"),
    USER_IS_FROZEN("U004", "User is frozen"),
    USER_IS_DELETED("U005", "User is deleted"),
    USER_STATUS_INVALID("U006", "Invalid user status");

    private final String code;
    private final String message;
}
```

File: `types/exception/UserException.java`

```java
package vip.mate.system.types.exception;

import vip.mate.base.exception.BizException;
import vip.mate.base.exception.ErrorCode;

public class UserException extends BizException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public static UserException of(ErrorCode errorCode) {
        return new UserException(errorCode);
    }
}
```

---

### Change 23: SQL Schema

File: `src/main/resources/db/schema.sql`

```sql
CREATE TABLE IF NOT EXISTS `mate_user` (
    `id`              VARCHAR(64)  NOT NULL COMMENT 'Primary key',
    `mobile`          VARCHAR(20)  NOT NULL COMMENT 'Mobile number',
    `nick_name`       VARCHAR(64)  DEFAULT NULL COMMENT 'Nickname',
    `avatar`          VARCHAR(512) DEFAULT NULL COMMENT 'Avatar URL',
    `gender`          TINYINT      DEFAULT 0 COMMENT '0=unknown, 1=male, 2=female',
    `status`          TINYINT      DEFAULT 0 COMMENT '0=active, 1=frozen, 2=deleted',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT 'Last login time',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `lock_version`    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mobile` (`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';

CREATE TABLE IF NOT EXISTS `mate_user_operate_stream` (
    `id`              VARCHAR(64)  NOT NULL COMMENT 'Primary key',
    `user_id`         VARCHAR(64)  NOT NULL COMMENT 'User ID',
    `operate_type`    VARCHAR(32)  NOT NULL COMMENT 'Operation type',
    `operate_detail`  VARCHAR(512) DEFAULT NULL COMMENT 'Operation detail',
    `operate_time`    DATETIME     NOT NULL COMMENT 'Operation time',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User operation stream';
```

---

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| mate-biz/mate-system/pom.xml | New | Module POM |
| MateSystemApplication.java | New | Application entry |
| bootstrap.yml | New | Nacos config |
| UserAggregate.java | New | Aggregate root |
| User.java | New | Domain entity |
| UserOperateStream.java | New | Domain entity |
| UserStatus.java | New | Value object |
| UserOperateType.java | New | Domain constant |
| UserRepository.java | New | Domain repository interface |
| BaseDomainEvent.java | New | Event base class |
| UserCreatedEvent.java | New | Domain event |
| DomainEventPublisher.java | New | Port interface |
| IUserDomainService.java | New | Domain service interface |
| UserDomainServiceImpl.java | New | Domain service impl |
| UserCommandService.java | New | CQRS command service |
| IUserQueryService.java | New | CQRS query interface |
| UserQueryServiceImpl.java | New | CQRS query impl |
| UserConvertor.java | New | MapStruct convertor |
| UserController.java | New | REST controller |
| RpcUserServiceImpl.java | New | Dubbo RPC provider |
| UserRepositoryImpl.java | New | Repository impl |
| UserInfraConvertor.java | New | Infra MapStruct convertor |
| DomainEventPublisherImpl.java | New | Event publisher impl |
| UserDao.java | New | MyBatis Plus DAO |
| UserOperateStreamDao.java | New | MyBatis Plus DAO |
| UserPO.java | New | Persistent object |
| UserOperateStreamPO.java | New | Persistent object |
| UserErrorCode.java | New | Error codes |
| UserException.java | New | Business exception |
| schema.sql | New | DDL |

## 验证方案

1. `mvn clean compile` - 编译通过
2. Start mate-system with Nacos running - verify Nacos registration
3. `GET /api/v1/users` - returns empty list
4. `POST /api/v1/users` with `{"mobile":"13800138000","nickName":"test"}` - creates user
5. `GET /api/v1/users/{id}` - returns user info
6. `PUT /api/v1/users/{id}/freeze` - freezes user
7. Verify Dubbo RPC service registered in Nacos
