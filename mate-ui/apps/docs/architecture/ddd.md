# DDD 四层结构

每个业务模块在 `vip.mate.{service}/` 下严格分为四层：

## 层级结构

```
vip.mate.system/
├── trigger/              ← 入站适配器（接收外部请求）
│   ├── controller/       REST API
│   ├── rpc/              @DubboService RPC 提供者
│   ├── event/            @EventListener 事件监听
│   └── job/              @XxlJob 定时任务
│
├── application/          ← 应用编排层
│   ├── command/          写操作（@Transactional）
│   ├── query/            读操作（接口 + impl/）
│   └── convertor/        MapStruct 转换器
│
├── domain/               ← 领域核心（零框架依赖）
│   ├── model/
│   │   ├── aggregate/    聚合根
│   │   ├── entity/       实体
│   │   └── valobj/       值对象
│   ├── service/          领域服务
│   ├── adapter/
│   │   ├── repository/   仓储接口
│   │   └── port/         端口接口
│   └── event/            领域事件
│
├── infrastructure/       ← 出站适配器（实现细节）
│   ├── adapter/
│   │   ├── repository/   仓储实现
│   │   └── port/         端口实现
│   ├── dao/              MyBatis Plus Mapper
│   │   └── po/           持久化对象
│   └── config/           配置类
│
└── types/                ← 类型定义
    ├── exception/        ErrorCode 枚举
    └── constant/         常量
```

## 核心规则

### 1. Domain 层零框架依赖

Domain 层不使用任何 Spring、MyBatis 等框架注解。只包含纯 Java 代码，确保业务逻辑可测试、可迁移。

```java
// ✅ 正确：纯 Java 领域模型
public class User {
    private UserId id;
    private Username username;
    private UserStatus status;

    public void disable() {
        if (this.status == UserStatus.DISABLED) {
            throw new BizException(UserErrorCode.ALREADY_DISABLED);
        }
        this.status = UserStatus.DISABLED;
    }
}

// ❌ 错误：Domain 层出现框架注解
@Entity
@Table(name = "mate_user")
public class User { ... }
```

### 2. 仓储接口在 Domain，实现在 Infrastructure

```java
// domain/adapter/repository/UserRepository.java
public interface UserRepository {
    User findById(UserId id);
    void save(User user);
}

// infrastructure/adapter/repository/UserRepositoryImpl.java
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserMapper userMapper;
    private final UserConvertor convertor;

    @Override
    public User findById(UserId id) {
        UserPO po = userMapper.selectById(id.getValue());
        return convertor.toAggregate(po);
    }
}
```

### 3. 聚合根强制不变量

状态变更只通过聚合根的方法进行，不允许外部直接修改字段。

### 4. MapStruct 做所有转换

PO ↔ Entity ↔ DTO 之间的转换全部使用 MapStruct，禁止手动字段拷贝。

```java
@Mapper(componentModel = "spring")
public interface UserConvertor {
    User toAggregate(UserPO po);
    UserPO toPO(User aggregate);
    UserResponse toResponse(User user);
}
```

### 5. CQRS 读写分离

```java
// application/command/UserCommandService.java
@Service
@RequiredArgsConstructor
public class UserCommandService {
    @Transactional
    public void disableUser(DisableUserCommand cmd) { ... }
}

// application/query/UserQueryService.java（接口）
public interface UserQueryService {
    PageResult<UserResponse> listUsers(UserQuery query);
}

// application/query/impl/UserQueryServiceImpl.java
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService { ... }
```
