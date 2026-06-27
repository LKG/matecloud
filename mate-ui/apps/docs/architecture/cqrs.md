# CQRS 模式

MateCloud 在应用层（application）严格区分读写操作。

## 写操作：CommandService

- 放在 `application/command/` 包下
- 使用 `@Transactional` 管理事务
- 接收 Command 对象，操作聚合根，通过仓储持久化
- 可以发布领域事件

```java
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public void createUser(CreateUserCommand cmd) {
        User user = User.create(cmd.getUsername(), cmd.getEmail());
        userRepository.save(user);
        eventPublisher.publish(new UserCreatedEvent(user.getId()));
    }

    @Transactional
    public void disableUser(DisableUserCommand cmd) {
        User user = userRepository.findById(cmd.getUserId());
        user.disable();
        userRepository.save(user);
    }
}
```

## 读操作：QueryService

- 放在 `application/query/` 包下
- 定义接口 + `impl/` 实现
- 不修改数据，不需要事务
- 可以直接查 DAO 层，不必经过聚合根

```java
// 接口
public interface UserQueryService {
    PageResult<UserResponse> listUsers(UserQuery query);
    UserDetailResponse getUser(Long id);
}

// 实现
@Service
@RequiredArgsConstructor
public class UserQueryServiceImpl implements UserQueryService {

    private final UserMapper userMapper;
    private final UserConvertor convertor;

    @Override
    public PageResult<UserResponse> listUsers(UserQuery query) {
        IPage<UserPO> page = userMapper.selectPage(query.toPage(), query.toWrapper());
        return PageResult.of(page, convertor::toResponse);
    }
}
```

## 为什么分离

| | CommandService | QueryService |
|---|---|---|
| 事务 | 需要 | 不需要 |
| 数据路径 | Controller → Command → 聚合根 → Repository | Controller → Query → DAO 直查 |
| 缓存 | 通常不缓存 | 可以积极缓存 |
| 扩展方向 | 保证一致性 | 优化查询性能 |

读写分离使得写操作保持领域完整性，读操作可以自由优化（直查视图、加缓存、异步索引），互不干扰。
