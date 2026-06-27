# MateCloud 编码规范

> 本规范基于 MateCloud 脚手架的实际代码模式提炼而成，适用于所有基于本脚手架的业务开发。
> MateCloud 作为开源脚手架，主攻技术方向与领域设计，规范的目标是确保团队在此基础上扩展时保持一致性。

---

## 目录

1. [项目结构规范](#1-项目结构规范)
2. [DDD 分层架构规范](#2-ddd-分层架构规范)
3. [领域层规范](#3-领域层规范)
4. [应用层规范](#4-应用层规范)
5. [基础设施层规范](#5-基础设施层规范)
6. [触发层规范](#6-触发层规范)
7. [类型层规范](#7-类型层规范)
8. [Starter 开发规范](#8-starter-开发规范)
9. [数据库规范](#9-数据库规范)
10. [RPC 与跨服务通信规范](#10-rpc-与跨服务通信规范)
11. [安全规范](#11-安全规范)
12. [配置管理规范](#12-配置管理规范)
13. [前端规范](#13-前端规范)
14. [命名规范](#14-命名规范)
15. [异常处理规范](#15-异常处理规范)
16. [测试规范](#16-测试规范)
17. [Git 与协作规范](#17-git-与协作规范)

---

## 1. 项目结构规范

### 1.1 模块划分原则

```
matecloud/
├── mate-common/              # 纯类型，ZERO auto-config
│   ├── mate-base/            # 基础类：BaseEntity, Result, BizException, ErrorCode
│   └── mate-api/             # 跨服务契约：Dubbo RPC 接口、共享 DTO、枚举
├── mate-starters/            # 即插即用 starter（7 核心 + 6 业务）
├── mate-starters-contrib/    # 进阶 starter（分片、Seata、灰度等）
├── mate-gateway/             # API 网关（WebFlux）
├── mate-auth/                # 认证服务
├── mate-admin/               # 后台管理（RBAC + 字典 + 配置 + 日志）
├── mate-biz/                 # 业务模块目录
│   ├── mate-system/          # DDD 标杆模块
│   └── mate-{name}/          # 新业务模块
├── mate-cli/                 # CLI 工具
└── mate-ui/                  # 前端 monorepo
```

### 1.2 新模块创建

**优先使用 CLI 创建模块：**

```bash
java -jar mate-cli/target/mate-cli.jar new module mate-{name} --port {port}
```

**手动创建时必须：**

1. 在 `mate-biz/` 下新建 `mate-{name}/pom.xml`，引入所需 starter
2. 创建 `application.yml`，通过 `spring.config.import` 引入 Nacos 配置
3. 创建启动类 `Mate{Name}Application.java`，标注 `@SpringBootApplication` + `@EnableDubbo`
4. 按 DDD 四层包结构组织代码
5. 在根 `pom.xml` 和 `mate-biz/pom.xml` 中添加 `<module>` 声明

### 1.3 端口分配

| 范围 | 用途 |
|------|------|
| 9010 | 网关 |
| 9020 | 认证 |
| 9030-9039 | 系统基础模块 |
| 9040-9049 | 后台管理模块 |
| 9050-9059 | 通知/消息模块 |
| 9060+ | 业务扩展模块 |

新模块端口递增分配，避免冲突。

---

## 2. DDD 分层架构规范

### 2.1 四层包结构

每个业务模块的包根为 `vip.mate.{service}/`，严格按以下分层组织：

```
vip.mate.{service}/
├── trigger/              # 入站适配器
│   ├── controller/       # REST 控制器
│   ├── rpc/              # @DubboService 实现
│   ├── event/            # @EventListener 事件监听
│   └── job/              # @XxlJob 定时任务
│
├── application/          # 编排层
│   ├── command/          # 写操作服务（@Transactional）
│   ├── query/            # 读操作（接口 + impl/）
│   └── convertor/        # MapStruct 转换器
│
├── domain/               # 核心领域逻辑
│   ├── model/
│   │   ├── aggregate/    # 聚合根
│   │   ├── entity/       # 领域实体
│   │   └── valobj/       # 值对象
│   ├── service/          # 领域服务（接口 + impl/）
│   ├── adapter/
│   │   ├── repository/   # 仓储接口
│   │   └── port/         # 出站端口接口
│   ├── event/            # 领域事件
│   └── constant/         # 领域常量/枚举
│
├── infrastructure/       # 出站适配器
│   ├── adapter/
│   │   ├── repository/   # 仓储实现
│   │   │   └── convertor/ # PO↔Entity MapStruct 转换
│   │   └── port/         # 出站端口实现
│   ├── dao/              # MyBatis Plus Mapper
│   │   ├── po/           # 持久化对象
│   │   └── seed/         # 种子数据（CommandLineRunner）
│   └── config/           # 模块级配置类
│
└── types/                # 模块级类型
    ├── exception/        # ErrorCode 枚举 + 自定义 Exception
    ├── constant/         # 模块常量
    ├── security/         # 权限常量（Perms）
    └── excel/            # Excel 导入导出行定义
```

### 2.2 层间依赖规则

```
trigger → application → domain ← infrastructure
                            ↑         ↑
                          types      types
```

**铁律：**

| 规则 | 说明 |
|------|------|
| domain 层零框架依赖 | 不允许 Spring、MyBatis、Sa-Token 等任何框架注解 |
| domain 不依赖 infrastructure | 通过 Repository/Port 接口反转依赖 |
| application 不直接操作 DAO | 必须通过 Repository 接口 |
| trigger 不包含业务逻辑 | 仅做参数校验、权限检查、委托调用 |
| infrastructure 不定义业务规则 | 仅负责技术实现 |

### 2.3 多子域模块

当一个服务包含多个子域时（如 mate-admin 包含 permission、dict、config），在 domain 层按子域组织：

```
domain/
├── permission/
│   ├── model/aggregate/
│   ├── model/entity/
│   ├── model/valobj/
│   ├── service/
│   └── adapter/
├── dict/
│   ├── model/entity/
│   └── adapter/
└── config/
    ├── model/entity/
    └── adapter/
```

---

## 3. 领域层规范

### 3.1 聚合根

聚合根封装实体及其关联的值集合，是事务一致性的边界：

```java
// 正确：聚合根包装实体 + 操作流
@Getter
public class UserAggregate {
    private final User user;
    private final List<UserOperateStream> operateStreams = new ArrayList<>();

    // 工厂方法创建聚合
    public static UserAggregate create(String username, String mobile, ...) {
        User user = User.create(username, mobile, ...);
        UserAggregate agg = new UserAggregate(user);
        agg.addOperateStream(UserOperateType.CREATE, "创建用户");
        return agg;
    }

    // 业务方法——状态变更只能通过聚合方法
    public void freeze() {
        user.checkActive();
        user.setStatus(UserStatus.FROZEN);
        addOperateStream(UserOperateType.FREEZE, "冻结用户");
    }

    // 操作流收集与清理
    public List<UserOperateStream> getAndClearOperateStreams() {
        List<UserOperateStream> copy = new ArrayList<>(operateStreams);
        operateStreams.clear();
        return copy;
    }
}
```

**规则：**
- 外部不直接修改实体字段，只通过聚合的业务方法
- 聚合方法内记录操作流（审计追踪）
- 工厂方法中完成初始化验证
- 使用 `getAndClearOperateStreams()` 在持久化后清理事件

### 3.2 领域实体

```java
// 正确：实体使用工厂方法，包含业务守卫
@Getter @Setter
public class User extends BaseEntity {
    private String username;
    private String mobile;
    private UserStatus status;
    // ... 其他字段

    // 工厂方法：生成 ID + 初始状态
    public static User create(String username, String mobile, ...) {
        User user = new User();
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setStatus(UserStatus.ACTIVE);
        user.setUsername(username);
        user.setMobile(mobile);
        return user;
    }

    // 业务守卫方法
    public void checkActive() {
        if (!UserStatus.ACTIVE.equals(this.status)) {
            throw UserException.of(UserErrorCode.USER_FROZEN);
        }
    }

    // 状态查询方法
    public boolean isActive() { return UserStatus.ACTIVE.equals(status); }
    public boolean isFrozen() { return UserStatus.FROZEN.equals(status); }
}
```

**规则：**
- 实体继承 `BaseEntity`，自动获得 `id`、`createdAt`、`updatedAt`、`deleted`、`lockVersion`
- ID 使用 UUID（去连字符），在工厂方法中生成
- 不使用任何框架注解（`@TableField`、`@Column` 等只出现在 PO 中）
- 状态变更通过语义化方法，不暴露 setter 给外部直接调用
- 业务守卫方法抛出领域异常

### 3.3 值对象

```java
// 正确：值对象用枚举实现，包含语义查询方法
@Getter
@AllArgsConstructor
public enum UserStatus {
    ACTIVE(0, "正常"),
    FROZEN(1, "冻结"),
    DELETED(2, "注销");

    private final int code;
    private final String description;

    public boolean isActive() { return this == ACTIVE; }
    public boolean canModify() { return this != DELETED; }

    // 从持久化值还原
    public static UserStatus fromCode(int code) {
        for (UserStatus s : values()) {
            if (s.code == code) return s;
        }
        throw new IllegalArgumentException("未知状态码: " + code);
    }
}
```

**规则：**
- 值对象优先用枚举实现
- 必须提供 `fromCode()` 工厂方法用于从数据库还原
- 提供领域语义的查询方法（`isActive()`、`canModify()`）
- 包含 code + description 双字段

### 3.4 领域服务

```java
// 接口定义在 domain/service/
public interface IUserDomainService {
    UserAggregate createUser(String username, String mobile, String rawPassword, ...);
    UserAggregate findByIdOrThrow(String userId);
    void changePassword(String userId, String oldPassword, String newPassword);
}

// 实现在 domain/service/impl/
@Service
public class UserDomainServiceImpl implements IUserDomainService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder; // 密码编码在服务层，不在实体中

    @Override
    public UserAggregate createUser(...) {
        // 1. 唯一性校验
        if (userRepository.existsByUsername(username)) {
            throw UserException.of(UserErrorCode.DUPLICATE_USERNAME);
        }
        // 2. 通过聚合工厂创建
        String encodedPwd = encoder.encode(rawPassword);
        return UserAggregate.create(username, mobile, encodedPwd, ...);
    }
}
```

**规则：**
- 领域服务处理跨实体/跨聚合的业务逻辑
- 密码编码等技术操作在领域服务中执行，不在实体中
- 唯一性校验在领域服务中完成
- 返回聚合根而非实体

### 3.5 仓储接口

```java
// 定义在 domain/adapter/repository/
public interface UserRepository {
    void save(UserAggregate aggregate);
    void update(UserAggregate aggregate);
    Optional<UserAggregate> findById(String id);
    Optional<UserAggregate> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByMobile(String mobile);
    PageResult<User> pageQuery(String keyword, int offset, int limit);
}
```

**规则：**
- 仓储操作聚合根，不操作裸实体
- 查询方法返回 `Optional`，不返回 null
- 分页查询返回 `PageResult<Entity>`
- 不暴露底层 SQL 细节（如 `LambdaQueryWrapper`）

### 3.6 出站端口

```java
// 领域事件发布端口
public interface DomainEventPublisher {
    void publish(BaseDomainEvent event);
    void publishAsync(BaseDomainEvent event);
}

// 认证模块的端口示例
public interface TokenIssuerPort {
    LoginResult issue(AuthUser user);
    void revokeCurrentSession();
    String currentUserId();
}

public interface LoginAttemptPort {
    void ensureNotLocked(String account);
    void recordFailure(String account);
    void recordSuccess(String account);
}
```

**规则：**
- 所有对外部系统的依赖（Redis、MQ、第三方 API）都通过端口接口抽象
- 端口接口定义在 `domain/adapter/port/`，实现在 `infrastructure/adapter/port/`
- 端口方法签名使用领域语言，不泄露技术细节

### 3.7 领域事件

```java
// 基础事件
@Getter
public abstract class BaseDomainEvent {
    private final String eventId = UUID.randomUUID().toString();
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private final String aggregateId;
    private final String aggregateType;

    public abstract String getEventType();
    public abstract String getDescription();
}

// 具体事件
public class UserCreatedEvent extends BaseDomainEvent {
    private final String username;
    private final String mobile;

    @Override
    public String getEventType() { return "USER_CREATED"; }
}
```

**规则：**
- 事件继承 `BaseDomainEvent`，自动携带 `eventId`、`occurredAt`、`aggregateId`
- 事件名称用 `{AGGREGATE}_{PAST_TENSE_VERB}` 格式（如 `USER_CREATED`）
- 事件是不可变的，字段用 `final`
- 通过 `DomainEventPublisher` 端口发布

---

## 4. 应用层规范

### 4.1 CQRS 分离

**写操作 → CommandService：**

```java
@Service
@RequiredArgsConstructor
public class UserCommandService {
    private final IUserDomainService userDomainService;
    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public void createUser(CreateUserRequest req) {
        // 1. 通过领域服务创建聚合
        UserAggregate agg = userDomainService.createUser(
            req.getUsername(), req.getMobile(), req.getPassword(), ...);
        // 2. 持久化
        userRepository.save(agg);
        // 3. 发布领域事件
        eventPublisher.publishAsync(new UserCreatedEvent(agg.getUser().getId(), ...));
    }
}
```

**读操作 → QueryService（接口 + 实现）：**

```java
// 接口
public interface IUserQueryService {
    PageResult<UserResponse> page(String keyword, int pageNum, int pageSize);
    UserResponse findById(String id);
    // Auth 专用——包含密码字段
    UserResponse findByUsernameForAuth(String username);
}

// 实现
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements IUserQueryService {
    private final UserRepository userRepository;
    private final UserConvertor convertor;

    @Override
    public PageResult<UserResponse> page(String keyword, int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        PageResult<User> page = userRepository.pageQuery(keyword, offset, pageSize);
        return PageResult.of(convertor.toResponseList(page.getList()), page.getTotal());
    }
}
```

**规则：**
- Command 和 Query 绝不混在同一个类中
- CommandService 标注 `@Transactional(rollbackFor = Exception.class)`
- QueryService 使用 接口 + impl 分离，便于 mock 和替换
- 查询不走领域服务，直接通过仓储读取

### 4.2 MapStruct 转换器

```java
@Mapper(componentModel = "spring")
public interface UserConvertor {
    // 默认映射——忽略敏感字段
    @Mapping(target = "password", ignore = true)
    UserResponse toResponse(User user);

    // Auth 场景——携带密码
    UserResponse toResponseWithPassword(User user);

    // 值对象映射
    @Named("statusToCode")
    default Integer statusToCode(UserStatus status) {
        return status == null ? null : status.getCode();
    }

    @Named("codeToStatus")
    default UserStatus codeToStatus(Integer code) {
        return code == null ? null : UserStatus.fromCode(code);
    }
}
```

**规则：**
- 所有对象转换必须使用 MapStruct，禁止手动逐字段拷贝
- 使用 `componentModel = "spring"` 支持依赖注入
- 敏感字段（password）默认 `ignore = true`
- 值对象（枚举↔code）转换使用 `@Named` 方法
- 转换器定义在 `application/convertor/`（应用层）和 `infrastructure/adapter/repository/convertor/`（基础设施层）

### 4.3 批量操作模式

```java
// 批量操作使用 REQUIRES_NEW 隔离每行事务
@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
public void importSingleRow(UserImportRow row) {
    // 单行导入逻辑...
}

// 批量冻结
@Transactional(rollbackFor = Exception.class)
public void batchFreeze(List<String> ids) {
    ids.forEach(id -> {
        UserAggregate agg = userDomainService.findByIdOrThrow(id);
        agg.freeze();
        userRepository.update(agg);
    });
}
```

**规则：**
- 批量导入使用 `REQUIRES_NEW` 传播级别，单行失败不影响其他行
- 批量操作（冻结、解冻、删除）在同一事务内完成
- Excel 导出硬限 50,000 行，防止 OOM
- 密码变更后必须通过 Sa-Token 踢出用户会话

### 4.4 请求/响应 DTO

```java
// 使用 record 定义查询 VO（Java 16+）
public record AdminVO(
    String id,
    String username,
    String nickName,
    String avatar,
    Integer status,
    List<String> roleIds,
    LocalDateTime createdAt
) {}

// 或使用 Lombok @Data 的传统 DTO
@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名格式不正确")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64 位")
    private String password;
}
```

**规则：**
- 查询返回的 VO 优先使用 `record`，不可变且简洁
- 写操作的 Command/Request 使用 `@Data`，配合 Jakarta Validation 注解
- RPC 传输对象必须实现 `Serializable`
- 密码字段在 Response 中默认不输出

---

## 5. 基础设施层规范

### 5.1 仓储实现

```java
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserDao userDao;
    private final UserOperateStreamDao streamDao;
    private final UserInfraConvertor convertor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(UserAggregate aggregate) {
        // 1. 转换并保存主实体
        UserPO po = convertor.toPO(aggregate.getUser());
        userDao.insert(po);
        // 2. 保存操作流
        List<UserOperateStream> streams = aggregate.getAndClearOperateStreams();
        streams.forEach(s -> streamDao.insert(convertor.toStreamPO(s)));
    }

    @Override
    public Optional<UserAggregate> findById(String id) {
        UserPO po = userDao.selectById(id);
        if (po == null) return Optional.empty();
        User user = convertor.toEntity(po);
        return Optional.of(new UserAggregate(user));
    }
}
```

**规则：**
- 仓储实现标注 `@Repository`
- 聚合保存时同时持久化操作流/关联实体
- PO↔Entity 转换通过 MapStruct 完成
- `findById` 返回 `Optional`，不返回 null
- 多对多关系（如 admin_role）使用"先删后插"策略

### 5.2 持久化对象（PO）

```java
@Data
@TableName("mate_user")
public class UserPO extends BasePO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String username;
    private String mobile;
    private String password;
    private Integer status;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer lockVersion;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

**规则：**
- PO 类名以 `PO` 结尾，放在 `infrastructure/dao/po/` 包下
- 表名使用 `mate_` 前缀
- ID 使用 `IdType.ASSIGN_ID`（雪花算法）
- 必须包含 `deleted`（逻辑删除）、`lockVersion`（乐观锁）、`createdAt`、`updatedAt`
- JSON 类型字段使用 `JacksonTypeHandler`
- 审计日志表（LoginLog、OperationLog）不使用逻辑删除

### 5.3 MyBatis DAO

```java
@Mapper
public interface UserDao extends BaseMapper<UserPO> {
    // 自定义查询——始终过滤已删除记录
    @Select("SELECT * FROM mate_user WHERE mobile = #{mobile} AND deleted = 0")
    UserPO selectByMobile(@Param("mobile") String mobile);

    // 存在性检查
    @Select("SELECT COUNT(*) FROM mate_user WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    // 分页查询——offset/limit 模式
    @Select("""
        SELECT * FROM mate_user WHERE deleted = 0
        AND (#{keyword} IS NULL OR username LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<UserPO> pageQuery(@Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("limit") int limit);
}
```

**规则：**
- DAO 接口继承 `BaseMapper<PO>`
- 自定义 SQL 使用 `@Select`/`@Delete` 注解
- 所有查询必须过滤 `deleted = 0`
- 分页使用 `offset/limit` 参数
- 复杂查询可使用 `LambdaQueryWrapper`（在 Repository 实现中）
- Mapper 扫描路径：`vip.mate.**.infrastructure.dao` 和 `vip.mate.**.dao`

### 5.4 种子数据

```java
@Component
@Order(100)
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {
    private final UserCommandService userCommandService;

    @Override
    public void run(String... args) {
        String username = env("MATE_SEED_ADMIN_USERNAME", "admin");
        String password = env("MATE_SEED_ADMIN_PASSWORD", "admin123");
        // 幂等：已存在则跳过
        if (userRepository.existsByUsername(username)) return;
        userCommandService.createUser(...);
    }
}
```

**规则：**
- 种子数据实现 `CommandLineRunner`，指定 `@Order`
- 必须幂等——已存在则跳过
- 默认值可通过环境变量覆盖
- 放在 `infrastructure/seed/` 或 `infrastructure/dao/seed/` 包下

---

## 6. 触发层规范

### 6.1 REST 控制器

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SaCheckLogin
public class UserController {
    private final UserCommandService commandService;
    private final IUserQueryService queryService;

    // 创建
    @PostMapping
    @SaCheckPermission(Perms.USER_ADD)
    public Result<Void> create(@RequestBody @Valid CreateUserRequest req) {
        commandService.createUser(req);
        return Result.ok();
    }

    // 分页查询
    @GetMapping
    @SaCheckPermission(Perms.USER_LIST)
    public Result<PageResult<UserResponse>> page(
            @RequestParam(required = false) String keyword,
            @Valid BasePageReq pageReq) {
        return Result.ok(queryService.page(keyword, pageReq.getPageNum(), pageReq.getPageSize()));
    }

    // 详情
    @GetMapping("/{id}")
    @SaCheckPermission(Perms.USER_LIST)
    public Result<UserResponse> getById(@PathVariable String id) {
        return Result.ok(queryService.findById(id));
    }

    // 批量操作
    @PostMapping("/batch-freeze")
    @SaCheckPermission(Perms.USER_EDIT)
    public Result<Void> batchFreeze(@RequestBody @NotEmpty List<String> ids) {
        commandService.batchFreeze(ids);
        return Result.ok();
    }
}
```

**规则：**
- 类级别标注 `@SaCheckLogin`，方法级别标注 `@SaCheckPermission`
- API 路径统一前缀 `/api/v1/`
- 返回值统一包装为 `Result<T>`
- 分页使用 `BasePageReq`（默认 pageNum=1, pageSize=10, 最大 500）
- 控制器不包含业务逻辑，仅做参数校验和委托
- 写操作用 `POST`/`PUT`/`DELETE`，读操作用 `GET`
- 批量操作路径：`/batch-{action}`（如 `/batch-freeze`、`/batch-delete`）

### 6.2 路径设计规则

```
POST   /api/v1/{resource}              # 创建
GET    /api/v1/{resource}              # 分页列表
GET    /api/v1/{resource}/{id}         # 详情
PUT    /api/v1/{resource}/{id}         # 更新
DELETE /api/v1/{resource}/{id}         # 删除（逻辑删除）
PUT    /api/v1/{resource}/{id}/freeze  # 状态变更
PUT    /api/v1/{resource}/password     # 自助操作
POST   /api/v1/{resource}/batch-{act}  # 批量操作
GET    /api/v1/{resource}/export       # 导出
POST   /api/v1/{resource}/import       # 导入
GET    /api/v1/{resource}/import/template  # 导入模板
```

### 6.3 RPC 服务实现

```java
@DubboService(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM,
              timeout = RpcConstants.DEFAULT_TIMEOUT)
public class RpcUserServiceImpl implements IRpcUserService {
    private final IUserQueryService queryService;

    @Override
    public Result<UserInfoResponse> getUserByUsername(String username) {
        UserResponse user = queryService.findByUsernameForAuth(username);
        return user != null ? Result.ok(toRpcResponse(user)) : Result.fail("用户不存在");
    }
}
```

**规则：**
- 使用 `@DubboService` 注解，指定 `version`、`group`、`timeout`
- 返回值统一用 `Result<T>` 包装
- 超时时间使用 `RpcConstants` 常量
- RPC 实现类放在 `trigger/rpc/` 包下

### 6.4 Excel 导入导出

```java
// 导出行定义——放在 types/excel/
@Data
public class UserExportRow {
    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("状态")
    private String status;  // 注意：导出用中文标签，不用 code

    @ExcelProperty("创建时间")
    private LocalDateTime createdAt;
}

// 控制器中的导出
@GetMapping("/export")
@SaCheckPermission(Perms.USER_LIST)
public void export(@RequestParam(required = false) String keyword,
                   HttpServletResponse response) throws IOException {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition",
        "attachment;filename=" + URLEncoder.encode("用户列表.xlsx", StandardCharsets.UTF_8));
    List<UserExportRow> rows = queryService.exportRows(keyword);
    EasyExcel.write(response.getOutputStream(), UserExportRow.class)
             .sheet("用户").doWrite(rows);
}
```

**规则：**
- 导出行定义放在 `types/excel/` 包下
- 列名使用中文
- 状态字段导出为中文标签而非 code
- 导出上限 50,000 行
- 导入使用 `REQUIRES_NEW` 事务隔离

---

## 7. 类型层规范

### 7.1 错误码

```java
@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {
    PARAM_NULL       ("USRA001", "请求参数为空"),
    DUPLICATE_MOBILE ("USRB001", "手机号已存在"),
    USER_NOT_FOUND   ("USRB002", "用户不存在"),
    USER_FROZEN      ("USRB003", "用户已冻结"),
    OLD_PASSWORD_WRONG("USRB004", "原密码错误"),
    PASSWORD_TOO_SHORT("USRA002", "密码不能少于6位");

    private final String code;
    private final String message;
}
```

**错误码编码规则：`{MODULE}{TYPE}{SEQ}`**

| 段 | 值 | 说明 |
|----|-----|------|
| MODULE | SYS/USR/ORD/SEC/TEN/NTC/ADM | 模块缩写（3字母） |
| TYPE | A | 参数校验错误 |
| TYPE | B | 业务逻辑错误 |
| TYPE | C | RPC 调用错误 |
| TYPE | D | 数据库错误 |
| TYPE | E | 外部服务错误 |
| SEQ | 001-999 | 序号 |

### 7.2 模块异常

```java
public class UserException extends BizException {
    private UserException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
    }

    public static UserException of(ErrorCode errorCode) {
        return new UserException(errorCode);
    }
}
```

**规则：**
- 每个模块一个自定义 Exception 类，继承 `BizException`
- 使用静态工厂 `of(ErrorCode)` 创建实例
- 不暴露公共构造函数

### 7.3 权限常量

```java
public final class Perms {
    private Perms() {}

    // 按实体分组
    public static final String USER_LIST   = "system:user:list";
    public static final String USER_ADD    = "system:user:add";
    public static final String USER_EDIT   = "system:user:edit";
    public static final String USER_DELETE = "system:user:delete";
    public static final String USER_RESET  = "system:user:reset";
}
```

**命名格式：`{module}:{entity}:{action}`**

---

## 8. Starter 开发规范

### 8.1 目录结构

```
mate-starters/mate-{name}-starter/
├── src/main/java/vip/mate/starter/{name}/
│   ├── config/
│   │   └── {Name}AutoConfiguration.java
│   ├── annotation/          # 自定义注解（如 @DistributedLock、@RateLimit）
│   ├── aspect/              # AOP 切面
│   └── ...
└── src/main/resources/
    └── META-INF/spring/
        └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 8.2 Auto-Configuration

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({GlobalExceptionHandler.class, JacksonConfiguration.class})
public class WebAutoConfiguration {
    // 按条件装配 Bean
}
```

**规则：**
- 使用 Spring Boot 3.x 的 `@AutoConfiguration`（不用 `@Configuration`）
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 使用 `@ConditionalOnClass`、`@ConditionalOnBean`、`@ConditionalOnProperty` 控制装配
- Starter 不引入业务依赖，只提供技术能力
- 配置属性使用 `@ConfigurationProperties`

### 8.3 AOP 切面模式

```java
// 注解定义
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    String key();               // SpEL 表达式
    long waitTime() default 3;  // 等待时间（秒）
    long leaseTime() default 10; // 持锁时间（秒）
}

// 切面实现
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {
    private final DistributedLockService lockService;

    @Around("@annotation(lock)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock lock) throws Throwable {
        String key = parseSpEL(lock.key(), pjp);
        return lockService.executeWithLock(key, lock.waitTime(), lock.leaseTime(),
            () -> pjp.proceed());
    }
}
```

**规则：**
- 功能型注解（限流、幂等、审计等）用 AOP 实现
- Key 支持 SpEL 表达式解析
- 切面 `@Around` 方法必须处理异常
- 每个功能独立一个 AutoConfiguration 类

---

## 9. 数据库规范

### 9.1 表设计

```sql
CREATE TABLE mate_user (
    id          VARCHAR(32)  NOT NULL COMMENT '主键（雪花/UUID）',
    username    VARCHAR(64)  NOT NULL COMMENT '用户名',
    mobile      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-正常 1-冻结 2-注销',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-正常 1-已删除',
    lock_version INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

**规则：**

| 规则 | 说明 |
|------|------|
| 表前缀 | `mate_` |
| 主键 | `id VARCHAR(32)`，使用雪花算法或 UUID（去连字符） |
| 必有字段 | `deleted`、`lock_version`、`created_at`、`updated_at` |
| 命名 | 蛇形命名（`snake_case`），MyBatis Plus 自动映射驼峰 |
| 字符集 | `utf8mb4` |
| 引擎 | `InnoDB` |
| 注释 | 每列必须有 COMMENT |
| 关联表 | 无需 `deleted`/`lock_version`（如 `mate_admin_role`） |
| 审计表 | 无需 `deleted`（如 `mate_login_log`、`mate_operation_log`） |

### 9.2 Flyway 迁移

```
src/main/resources/db/migration/
├── V1__admin_schema.sql         # 建表
├── V2__admin_seed_data.sql      # 种子数据（INSERT IGNORE）
├── V3__dict_type_remark.sql     # 结构变更
└── V4__demo_data.sql            # 演示数据
```

**规则：**
- 文件名格式：`V{N}__{description}.sql`（双下划线）
- 种子数据使用 `INSERT IGNORE` 保证幂等
- 结构变更和数据变更分开文件
- 不使用 `DROP TABLE` / `TRUNCATE`
- 版本号递增，不跳号

### 9.3 MyBatis Plus 全局配置

已在 `mate-ds-starter` 中统一配置：

| 配置 | 值 | 说明 |
|------|-----|------|
| `idType` | `ASSIGN_ID` | 雪花算法 ID |
| `logicDeleteField` | `deleted` | 逻辑删除字段 |
| `logicDeleteValue` | `1` | 已删除值 |
| `logicNotDeleteValue` | `0` | 未删除值 |
| `mapUnderscoreToCamelCase` | `true` | 下划线转驼峰 |
| 分页插件 | `maxLimit = 500` | 单页最大行数 |
| 乐观锁插件 | 自动启用 | 基于 `@Version` 字段 |
| 防全表操作插件 | 自动启用 | 阻止无 WHERE 的 UPDATE/DELETE |

---

## 10. RPC 与跨服务通信规范

### 10.1 RPC 接口定义

```java
// 定义在 mate-common/mate-api 模块
public interface IRpcUserService {
    Result<UserInfoResponse> getUserById(String userId);
    Result<UserInfoResponse> getUserByUsername(String username);
    Result<Void> registerUser(RegisterUserCommand command);
}
```

**规则：**
- 接口定义在 `mate-api` 模块，以 `IRpc{Entity}Service` 命名
- 返回值统一用 `Result<T>` 包装
- 参数/返回对象必须实现 `Serializable`
- 放在 `vip.mate.api.{module}.service` 包下

### 10.2 RPC 常量

```java
public final class RpcConstants {
    public static final String VERSION = "1.0.0";
    public static final int DEFAULT_TIMEOUT = 5000;   // 5 秒
    public static final int LONG_TIMEOUT = 15000;     // 15 秒
    public static final int DEFAULT_RETRIES = 2;
    public static final int NO_RETRY = 0;
    public static final String GROUP_SYSTEM = "system";
    public static final String GROUP_ADMIN = "admin";
}
```

### 10.3 RPC 消费方

```java
@DubboReference(version = RpcConstants.VERSION, group = RpcConstants.GROUP_SYSTEM,
                timeout = RpcConstants.DEFAULT_TIMEOUT, retries = RpcConstants.DEFAULT_RETRIES)
private IRpcUserService rpcUserService;
```

**规则：**
- 消费方必须指定 `timeout` 和 `retries`
- 写操作 `retries = NO_RETRY`（防止重复写入）
- 调用结果必须检查 `Result.isSuccess()`

---

## 11. 安全规范

### 11.1 认证

- 使用 Sa-Token，不使用 Spring Security
- Token 通过 `Authorization` 头传递（Bearer 前缀）
- 会话超时 24 小时，活跃超时 30 分钟
- 密码变更后立即踢出所有会话

### 11.2 授权

```java
// 类级别：要求登录
@SaCheckLogin

// 方法级别：要求特定权限
@SaCheckPermission("system:user:add")

// 使用权限常量（推荐）
@SaCheckPermission(Perms.USER_ADD)
```

### 11.3 密码安全

- 密码使用 BCrypt 加密存储，永不明文
- 密码最短 6 位
- 密码字段在 Response 中默认不输出（MapStruct `ignore = true`）
- Auth 专用接口可返回密码哈希（仅内部 RPC 使用）
- 密码重置后通过 `StpUtil.kickout(userId)` 踢出会话

### 11.4 防护能力（通过 mate-security-starter）

| 能力 | 注解 | 说明 |
|------|------|------|
| 接口签名 | `@ApiSign` | HMAC 签名验证（注解已定义，拦截器待实现） |
| 限流 | `@RateLimit` | IP/USER/GLOBAL 三种粒度 |
| 审计日志 | `@AuditLog` | 自动记录操作日志 |
| 数据权限 | `@DataPermission` | 行级数据过滤 |
| 幂等控制 | `@Idempotent` | PARAM/TOKEN/HEADER 三种模式 |
| 防重复提交 | `@RepeatSubmit` | MD5(method+args+URI) 去重 |
| 数据脱敏 | `@Desensitize` | 手机号/邮箱/身份证等 8 种类型 |

### 11.5 暴力破解防护

- 连续失败 5 次锁定账号 30 分钟
- 使用 Redis 计数器，自动过期
- 登录成功重置计数器

### 11.6 日志安全

- 日志中禁止输出密码、Token、敏感个人信息
- BCrypt 哈希值也不应出现在日志中
- 使用 `@Desensitize` 注解脱敏敏感字段

---

## 12. 配置管理规范

### 12.1 五层配置优先级

```
1. 环境变量 / JVM -D 参数                    ← 最高优先级
2. Nacos: ${app-name}-${profile}.yml         ← 服务级覆盖
3. Nacos: mate-infra-${profile}.yml          ← 共享基础设施
4. Classpath: mate-defaults.yml               ← 框架常量（打包在 mate-base.jar）
5. 服务 application.yml                       ← 入口点（~15 行）
```

### 12.2 服务 application.yml 模板

```yaml
server:
  port: 9030
spring:
  application:
    name: mate-system
  config:
    import:
      - classpath:mate-defaults.yml
      - optional:nacos:mate-infra-${spring.profiles.active}.yml
      - optional:nacos:${spring.application.name}-${spring.profiles.active}.yml
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
```

**规则：**
- 服务 `application.yml` 仅包含端口、应用名、配置导入
- 框架级配置放 `mate-defaults.yml`
- 基础设施配置（MySQL/Redis/MQ）放 Nacos `mate-infra-{profile}.yml`
- 服务特有配置放 Nacos `{app-name}-{profile}.yml`
- Nacos 配置使用 `optional:` 前缀，允许不存在

### 12.3 功能开关

```yaml
mate:
  feature:
    ai:
      enabled: false
    idempotent:
      enabled: false
    tenant:
      enabled: false
    devtools:
      enabled: false
```

通过 `@ConditionalOnProperty` 控制 Starter 是否激活。

---

## 13. 前端规范

### 13.1 项目结构

```
mate-ui/
├── apps/admin/                    # 管理后台应用
│   ├── src/
│   │   ├── router/                # 路由（静态 + 动态）
│   │   ├── stores/                # 应用级 Pinia Store
│   │   ├── views/                 # 页面组件（按模块分目录）
│   │   ├── layouts/               # 布局组件
│   │   ├── i18n/                  # 国际化（zh-CN / en-US）
│   │   └── directives/            # 自定义指令
│   └── vite.config.ts
└── packages/
    ├── core/                      # 核心：API 客户端、类型、Store、Composable
    ├── hooks/                     # 通用 Vue Composable
    ├── ui/                        # 共享 UI 组件库
    └── utils/                     # 工具函数
```

### 13.2 API 客户端

```typescript
// packages/core/src/api/client.ts
const client = axios.create({
  baseURL: '/api/v1',
  timeout: 15_000,
})

// 请求拦截：自动注入 Token + 租户 ID
client.interceptors.request.use(config => {
  const token = localStorage.getItem('mate_token')
  if (token) config.headers[tokenName] = token
  const tenantId = localStorage.getItem('mate_tenant_id')
  if (tenantId) config.headers['X-Tenant-Id'] = tenantId
  return config
})

// 响应拦截：解包 Result<T>
client.interceptors.response.use(resp => {
  const body = resp.data as Result<unknown>
  if (!body.success) throw new BizError(body.code, body.msg)
  return body.data  // 直接返回 data，消费方无需再解包
})
```

**规则：**
- 所有 API 调用通过共享 client 实例
- 响应拦截器自动解包 `Result<T>`，业务代码直接拿到 `data`
- 401 错误触发 `mate:unauthorized` 事件，由路由层处理
- 502/503/504 时 fallback 到 mock 数据（开发模式）

### 13.3 API 模块

```typescript
// packages/core/src/api/modules/user.ts
export const userApi = {
  list:    (params?: PageQuery) => client.get<PageResult<User>>('/users', { params }),
  getById: (id: string) => client.get<User>(`/users/${id}`),
  create:  (data: CreateUserReq) => client.post<void>('/users', data),
  update:  (id: string, data: UpdateUserReq) => client.put<void>(`/users/${id}`, data),
  freeze:  (id: string) => client.put<void>(`/users/${id}/freeze`),
  batchFreeze:  (ids: string[]) => client.post<void>('/users/batch-freeze', ids),
  batchDelete:  (ids: string[]) => client.post<void>('/users/batch-delete', ids),
  exportExcel:  (params?: any) => /* 使用 raw axios，保留 Content-Disposition */,
  importExcel:  (file: File) => /* multipart/form-data，120s 超时 */,
}
```

**规则：**
- 每个后端资源一个 API 模块文件
- 方法命名与 REST 动词对应
- 类型参数使用泛型声明返回类型
- 导出使用独立 axios 实例（需要保留响应头）
- 导入超时设为 120 秒

### 13.4 类型定义

```typescript
// 与后端 Result<T> 对齐
export interface Result<T = unknown> {
  code: string
  msg: string
  success: boolean
  data: T
}

// 与后端 PageResult<T> 对齐
export interface PageResult<T> {
  list: T[]
  total: number
}
```

**规则：**
- 前后端类型严格对齐
- 使用 `interface` 而非 `type` 定义 DTO
- 使用 `BizError` 类包装业务错误（含 `code` + `msg`）

### 13.5 状态管理（Pinia）

```typescript
// packages/core/src/stores/auth.ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('mate_token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const menuTree = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])

  // 权限检查——支持通配符
  function hasPermission(perm: string): boolean {
    return permissions.value.includes('*') || permissions.value.includes(perm)
  }

  // 登出——优雅降级（网络失败仍清理本地状态）
  async function logout() {
    try { await authApi.logout() } catch { /* ignore */ }
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('mate_token')
  }
})
```

**规则：**
- 使用 Composition API 风格（`setup` 函数）
- Token 持久化到 `localStorage`
- 登出时先调 API 再清本地（优雅降级）
- 权限检查支持 `*` 通配符

### 13.6 页面组件模式

```vue
<template>
  <MatePageCard title="用户管理">
    <!-- 搜索栏 -->
    <MateSearchBar v-model="keyword" @search="loadData" @reset="handleReset">
      <template #trailing>
        <el-button @click="handleCreate" v-permission="'system:user:add'">新增</el-button>
      </template>
    </MateSearchBar>

    <!-- 批量操作栏 -->
    <MateBatchBar :selected="selectedIds" @clear="clearSelection">
      <el-button @click="batchFreeze">批量冻结</el-button>
    </MateBatchBar>

    <!-- 数据表格 -->
    <MateTable :data="tableData" @selection-change="onSelectionChange">
      <el-table-column type="selection" />
      <el-table-column prop="username" label="用户名">
        <template #default="{ row }">
          <MateEntityCell :name="row.username" :subtitle="row.mobile" />
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <MateBadge :status="row.status" />
        </template>
      </el-table-column>
    </MateTable>

    <!-- 分页 -->
    <MatePagination v-model:page="pageNum" v-model:size="pageSize"
                    :total="total" @change="loadData" />

    <!-- 新增/编辑弹窗（合并） -->
    <MateDialog v-model="dialogVisible" :title="isEdit ? '编辑' : '新增'" @confirm="handleSubmit">
      <MateForm ref="formRef" :schema="formSchema" :model="formData" />
    </MateDialog>
  </MatePageCard>
</template>
```

**规则：**
- 使用 `MatePageCard` 包裹页面
- 搜索、批量操作、表格、分页使用 Mate 组件库
- 新增和编辑共用一个弹窗（通过 `isEdit` 区分）
- 权限指令 `v-permission` 控制按钮可见性
- 状态列使用 `MateBadge` 统一展示
- 用户名列使用 `MateEntityCell`（头像+名称+副标题）

### 13.7 Composable Hooks

```typescript
// useBatch：批量操作
const { run: batchFreeze, running } = useBatch()
await batchFreeze(userApi.batchFreeze, selectedIds)

// useExport：Excel 导出
const { download, exporting } = useExport()
await download('/users/export', { keyword })

// useImport：Excel 导入
const { upload, importing } = useImport()
const result = await upload('/users/import', file)
```

**规则：**
- 通用逻辑抽取为 Composable Hook
- 暴露 `running`/`exporting`/`importing` 状态用于 UI 联动
- 批量操作返回 `{ successCount, failCount, failures }` 供错误展示

### 13.8 路由

```typescript
// 静态路由：登录、注册、403、404
const staticRoutes = [
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  { path: '/403', component: ForbiddenView },
  { path: '/:pathMatch(.*)*', component: NotFoundView },
]

// 动态路由：从后端菜单树生成
function setupDynamicRoutes(menuTree: MenuItem[]) {
  const routes = buildRoutesFromMenu(menuTree)
  routes.forEach(r => router.addRoute('default', r))
}
```

**规则：**
- 公开路由（登录/注册）不做权限检查
- 已登录用户访问登录页重定向到首页
- 路由 `meta.perms` 控制页面权限
- 动态路由仅注册 MENU 类型菜单项，DIRECTORY 类型仅作为分组
- 组件映射通过 `VIEW_MAP` 维护

---

## 14. 命名规范

### 14.1 Java 命名

| 类别 | 规则 | 示例 |
|------|------|------|
| 包名 | `vip.mate.{module}.{layer}` | `vip.mate.system.domain.model.entity` |
| 类名 | 大驼峰 | `UserAggregate`, `UserCommandService` |
| 方法名 | 小驼峰 | `findByIdOrThrow`, `createUser` |
| 常量 | 全大写下划线 | `DEFAULT_TIMEOUT`, `GROUP_SYSTEM` |
| 变量 | 小驼峰 | `userRepository`, `pageSize` |

**类名后缀约定：**

| 后缀 | 位置 | 说明 |
|------|------|------|
| `Aggregate` | domain/model/aggregate | 聚合根 |
| `PO` | infrastructure/dao/po | 持久化对象 |
| `VO` | application/query | 视图对象（推荐用 record） |
| `Command` | application/command | 写操作命令 |
| `Request` | trigger/controller | 请求参数 |
| `Response` | trigger/controller 或 mate-api | 响应 DTO |
| `Convertor` | application/convertor 或 infra/convertor | MapStruct 转换器 |
| `Repository` | domain/adapter/repository | 仓储接口 |
| `RepositoryImpl` | infrastructure/adapter/repository | 仓储实现 |
| `Port` | domain/adapter/port | 出站端口 |
| `Dao` | infrastructure/dao | MyBatis Mapper |
| `Controller` | trigger/controller | REST 控制器 |
| `ErrorCode` | types/exception | 错误码枚举 |
| `Exception` | types/exception | 模块异常 |
| `Perms` | types/security | 权限常量 |
| `ExportRow` | types/excel | 导出行定义 |
| `ImportRow` | types/excel | 导入行定义 |
| `Seeder` | infrastructure/seed | 种子数据 |

### 14.2 数据库命名

| 类别 | 规则 | 示例 |
|------|------|------|
| 表名 | `mate_{entity}` 蛇形 | `mate_user`, `mate_admin_role` |
| 列名 | 蛇形 | `created_at`, `lock_version` |
| 索引 | `uk_{column}` / `idx_{column}` | `uk_username`, `idx_status` |
| 唯一索引 | `uk_` 前缀 | `uk_mobile` |
| 普通索引 | `idx_` 前缀 | `idx_created_at` |

### 14.3 前端命名

| 类别 | 规则 | 示例 |
|------|------|------|
| 组件文件 | 大驼峰 | `AdminList.vue`, `MateDialog.vue` |
| API 模块 | 小驼峰 | `user.ts`, `auth.ts` |
| Composable | `use` 前缀 | `useBatch.ts`, `useExport.ts` |
| Store | 小驼峰 | `auth.ts`, `system.ts` |
| 类型文件 | 小驼峰 | `result.ts`, `user.ts` |
| CSS 变量 | `--mc-` 前缀 | `--mc-primary`, `--mc-bg-card` |
| localStorage | `mate_` 前缀 | `mate_token`, `mate_dark` |

---

## 15. 异常处理规范

### 15.1 后端异常处理

**异常层级：**

```
Exception
└── RuntimeException
    └── BizException (code + msg)
        ├── UserException
        ├── AdminException
        └── ...
```

**全局异常处理（由 mate-web-starter 提供）：**

| 异常类型 | HTTP 状态码 | 处理方式 |
|---------|------------|---------|
| `BizException` | 200 | 返回 `Result.fail(code, msg)` |
| `MethodArgumentNotValidException` | 200 | 提取首个校验失败消息 |
| `BindException` | 200 | 提取首个校验失败消息 |
| `ConstraintViolationException` | 200 | 提取首个校验失败消息 |
| `MissingServletRequestParameterException` | 200 | 返回参数缺失提示 |
| `HttpRequestMethodNotSupportedException` | 405 | 方法不允许 |
| `NoResourceFoundException` | 404 | 资源不存在 |
| `Exception` (兜底) | 500 | 记录日志，返回通用错误 |

**规则：**
- 业务异常用 `BizException.of(ErrorCode)` 抛出
- 不在 Controller 中 try-catch 业务异常，交由全局处理
- 参数校验用 Jakarta Validation 注解（`@NotBlank`、`@Size`、`@Pattern`）
- RPC 调用失败包装为 `Result.fail()`，不抛异常

### 15.2 前端异常处理

```typescript
// BizError 类
export class BizError extends Error {
  code: string
  msg: string

  is(code: string): boolean {
    return this.code === code
  }
}

// 使用
try {
  await userApi.create(formData)
  ElMessage.success('创建成功')
} catch (e) {
  if (e instanceof BizError) {
    ElMessage.error(e.msg)  // 业务错误：展示后端消息
  }
  // 网络错误已在拦截器中处理
}
```

---

## 16. 测试规范

### 16.1 测试分层

```
src/test/java/
├── domain/           # 领域层单元测试（纯 Java，无 Spring Context）
├── application/      # 应用层测试（可 Mock 仓储）
├── infrastructure/   # 集成测试（Testcontainers）
└── trigger/          # API 测试（MockMvc）
```

### 16.2 测试命名

```java
// 方法名：被测方法_场景_预期结果
@Test
void createUser_duplicateUsername_throwsBizException() { ... }

@Test
void freeze_activeUser_statusChangedToFrozen() { ... }

@Test
void changePassword_wrongOldPassword_throwsException() { ... }
```

### 16.3 测试执行

```bash
mvn test                               # 单元测试
mvn verify -Pintegration-test          # 集成测试（需要 Docker）
```

---

## 17. Git 与协作规范

### 17.1 提交信息格式

```
{type}({scope}): {description}

# 示例
feat(system): 新增用户批量导入功能
fix(auth): 修复 SMS 验证码重复发送问题
refactor(admin): 重构角色权限查询逻辑
docs(conventions): 添加编码规范文档
chore(deps): 升级 Spring Boot 至 4.0.5
```

**type 类型：**

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复 Bug |
| `refactor` | 重构（不改变功能） |
| `docs` | 文档变更 |
| `chore` | 构建/工具变更 |
| `test` | 测试相关 |
| `style` | 代码格式（不影响逻辑） |
| `perf` | 性能优化 |

### 17.2 分支策略

```
main                    # 主分支
├── feature/{name}      # 功能开发
├── fix/{issue-id}      # Bug 修复
└── release/{version}   # 发布分支
```

---

## 附录 A：技术栈速查

| 层 | 技术 | 版本 |
|----|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 4.0.5 |
| 微服务 | Spring Cloud | 2025.1.1 |
| RPC | Dubbo | 3.3.6 |
| 注册中心 | Nacos | — |
| ORM | MyBatis Plus | 3.5.16 |
| 认证 | Sa-Token | 1.45.0 |
| 缓存 | Redisson | 4.5.0 |
| 序列化 | FastJSON2 | 2.0.53 |
| Excel | EasyExcel | 4.0.3 |
| 对象映射 | MapStruct | — |
| 工具库 | Lombok, HuTool, Guava | — |
| 前端框架 | Vue 3 | 3.5.32 |
| 构建 | Vite | 8.0.8 |
| UI 库 | Element Plus | 2.13.7 |
| 状态管理 | Pinia | 3.0.4 |
| HTTP | Axios | 1.7.0 |
| CSS | UnoCSS | 66.6.8 |
| 包管理 | pnpm | 10.11.0 |
| 编排 | Turbo | — |

## 附录 B：Claude Code 协作指南

使用 Claude Code 开发 MateCloud 时的要点：

1. **新建模块**优先使用 `mate-cli new module` 命令
2. **代码生成**使用 `mate-cli gen code --table {table} --module {module}`
3. **遵循 DDD 四层结构**，不要跨层直接调用
4. **领域层零框架依赖**是铁律，不可妥协
5. **MapStruct 做所有对象转换**，不用 `BeanUtils.copyProperties`（mate-admin 中的遗留用法应逐步替换）
6. **错误码遵循编码规则**：`{MODULE}{TYPE}{SEQ}`
7. **API 路径统一 `/api/v1/`**，RESTful 风格
8. **前端 API 调用通过共享 client**，不直接使用 axios
9. **权限用常量类 `Perms`**，不硬编码字符串
10. **配置不重复**——利用五层配置体系，每层只放该层该管的内容

---

## 附录 C：审计日志双机制说明

> 核查 RFC-011 + RFC-040 后发现项目中存在两套审计日志机制，此处澄清其定位差异。

### C.1 @AuditLog（mate-security-starter 提供）

```java
@AuditLog(module = "用户管理", type = "创建")
```

- **位置**: `vip.mate.starter.security.audit`
- **机制**: AOP 切面 → 发布 `AuditLogEvent` Spring 事件
- **持久化**: 当前无监听器消费此事件（待补全）
- **适用场景**: 跨模块通用审计（当所有服务都需要审计时）

### C.2 @OperationLog（mate-admin 模块自定义）

```java
@OperationLog(module = "角色管理", type = "创建")
```

- **位置**: `vip.mate.admin.trigger.annotation`
- **机制**: AOP 切面 → 直接 `operationLogDao.insert()`（同步持久化）
- **持久化**: 写入 `mate_operation_log` 表
- **适用场景**: mate-admin 内部操作审计

**选型建议**:
- 新业务模块如需审计，优先使用 `@AuditLog`（starter 级别，跨服务复用）
- mate-admin 内部继续使用 `@OperationLog`（已稳定运行）
- 后续需为 `@AuditLog` 补全持久化监听器

### C.3 LoginLog 跨服务数据流

```
mate-auth (写入) ──→ mate_login_log 表 ──→ mate-admin (只读查询)
```

- 登录日志由 mate-auth 的 `LoginLogPersistenceListener` 通过事件驱动写入
- mate-admin 的 `LoginLogController` 提供只读分页查询
- 跨库部署时注意两个服务需访问同一数据库

---

## 附录 D：Contrib Starter 使用指南

8 个 Contrib Starter 位于 `mate-starters-contrib/`，按需引入：

| Starter | 一句话说明 | 启用方式 |
|---------|----------|---------|
| `mate-ai-starter` | Spring AI 2.0 封装，多模型 + 工具注册 + 流式对话 | 引入依赖 + 配置 provider |
| `mate-sharding-starter` | ShardingSphere 分库分表，2库x4表 | 引入依赖 + 配置 sharding rules |
| `mate-sentinel-starter` | Sentinel 限流熔断 + Nacos 规则推送 | 引入依赖 + Nacos 规则配置 |
| `mate-seata-starter` | Seata 分布式事务 | 引入依赖 + Seata Server |
| `mate-gray-starter` | 灰度发布路由 | 引入依赖 + 网关灰度规则 |
| `mate-flow-starter` | 轻量工作流引擎 | 引入依赖 |
| `mate-rule-starter` | Aviator 规则引擎 | 引入依赖 |
| `mate-test-starter` | 测试基础设施 (Testcontainers) | testImplementation 引入 |

---

## 附录 E：延迟队列使用规范

### E.1 发送延迟消息

```java
@Autowired
private DelayQueueService delayQueueService;

// 发送 30 秒后执行的延迟消息
delayQueueService.offer("order:timeout:check", orderId, 30, TimeUnit.SECONDS);
```

### E.2 消费延迟消息

```java
@DelayQueueListener(topic = "order:timeout:check")
public void handleOrderTimeout(String orderId) {
    // 处理订单超时逻辑
}
```

**规则：**
- Topic 命名: `{module}:{action}:{detail}`（如 `order:timeout:check`）
- 消息体建议用 JSON 字符串，消费端自行反序列化
- `@DelayQueueListener` 方法由 `DelayQueueListenerRegistrar` 自动注册

### E.3 领域事件发布

```java
// 通过 EventPublisher 发布到 RabbitMQ
@Autowired
private EventPublisher eventPublisher;

eventPublisher.publish("user.created", new EventMessage<>(userId, eventData));

// 或通过 BaseEvent 子类
public class OrderCreatedEvent extends BaseEvent<OrderDTO> {
    @Override
    public String topic() { return "order.created"; }
}
eventPublisher.publish(new OrderCreatedEvent(orderDTO));
```

---

## 附录 F：AI 工具集成规范

### F.1 将方法暴露为 AI 可调用工具

```java
@Service
public class OrderQueryTool {

    @Tool("查询订单详情")
    public OrderDTO getOrder(
            @ToolParam("订单ID") String orderId) {
        return orderQueryService.findById(orderId);
    }
}
```

- 标注 `@Tool` 的方法会被 `AiToolRegistry` 自动发现并注册
- 可通过 REST 端点调用: `GET /api/v1/ai/tools` (列出) / `POST /api/v1/ai/tools/{name}` (执行)
- 可通过流式对话调用: `POST /api/v1/ai/chat/stream`
- 可通过 CLI 调用: `mate ai chat "查询订单 12345 的详情"`

### F.2 配置

```yaml
mate:
  feature:
    ai:
      enabled: true     # 启用 AI Starter
spring:
  ai:
    anthropic:           # 或 openai / dashscope / ollama
      api-key: ${AI_API_KEY}
```
