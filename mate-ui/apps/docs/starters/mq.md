# mate-mq-starter

消息队列 Starter，封装 RabbitMQ + 延迟队列 + 领域事件桥接。

## 提供的能力

- **RabbitMQ 自动配置** — Exchange / Queue / Binding 声明式配置
- **延迟队列** — `@DelayQueueListener` 注解，基于 RabbitMQ 延迟消息插件
- **领域事件桥接** — `DomainEventPublisher` 端口，将领域事件自动发布到 MQ

## 延迟队列

```java
// 发送延迟消息
@RequiredArgsConstructor
public class OrderCommandService {
    private final DelayQueuePublisher delayPublisher;

    public void createOrder(CreateOrderCommand cmd) {
        // ... 创建订单
        delayPublisher.send("order.timeout.check", orderId, 30, TimeUnit.MINUTES);
    }
}

// 监听延迟消息
@Component
public class OrderTimeoutHandler {
    @DelayQueueListener(queue = "order.timeout.check")
    public void onTimeout(Long orderId) {
        // 30 分钟后执行超时检查
    }
}
```

## 领域事件

在 Domain 层定义事件，通过 `DomainEventPublisher` 端口发布：

```java
// domain/event/UserCreatedEvent.java
public record UserCreatedEvent(Long userId, String username) {}

// application/command/UserCommandService.java
domainEventPublisher.publish(new UserCreatedEvent(user.getId(), user.getUsername()));
```

`mate-mq-starter` 自动将领域事件序列化并发布到 RabbitMQ，其他服务可以监听消费。
