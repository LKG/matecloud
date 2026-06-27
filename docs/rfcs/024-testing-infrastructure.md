# RFC-024: Testing Infrastructure (mate-test-starter)

| Field       | Value                                     |
|-------------|-------------------------------------------|
| **RFC**     | 024                                       |
| **Title**   | Testing Framework & Conventions           |
| **Status**  | Draft                                     |
| **Created** | 2026-04-11                                |
| **Module**  | mate-test-starter (new)                   |
| **Package** | vip.mate.starter.test                     |

---

## 1. Overview

Introduces `mate-test-starter`, a shared testing infrastructure module that provides:

1. **BaseIntegrationTest** -- Spring Boot test base with Testcontainers (MySQL, Redis, RabbitMQ)
2. **BaseDomainTest** -- Pure domain layer unit test base (no Spring context)
3. **MockRpcConfig** -- Auto-mock all Dubbo RPC references
4. **TestDataBuilder** -- Fluent builder for test fixtures
5. **Conventions** for unit tests, integration tests, and RPC contract tests

---

## 2. Module Structure

```
mate-starters/mate-test-starter/
  pom.xml
  src/main/java/vip/mate/starter/test/
    BaseIntegrationTest.java
    BaseDomainTest.java
    MockRpcConfig.java
    TestDataBuilder.java
    annotation/
      MateTest.java
      MateIntegrationTest.java
    container/
      MysqlContainerInitializer.java
      RedisContainerInitializer.java
      RabbitMqContainerInitializer.java
    util/
      TestRedisHelper.java
```

---

## 3. pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>mate-test-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Test Starter - Shared test infrastructure</description>

    <dependencies>
        <!-- Spring Boot Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- Testcontainers BOM -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers</artifactId>
            <version>1.20.4</version>
        </dependency>

        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.20.4</version>
        </dependency>

        <!-- Testcontainers: MySQL -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <version>1.20.4</version>
        </dependency>

        <!-- Testcontainers: Redis (via GenericContainer) -->
        <!-- No specific module needed; we use GenericContainer -->

        <!-- Testcontainers: RabbitMQ -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>rabbitmq</artifactId>
            <version>1.20.4</version>
        </dependency>

        <!-- Mockito -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
        </dependency>

        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
        </dependency>

        <!-- AssertJ (fluent assertions) -->
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>

        <!-- mate-base for common types -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- mate-api for RPC interfaces (to mock) -->
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-api</artifactId>
            <scope>compile</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- Dubbo (for mock reference config) -->
        <dependency>
            <groupId>org.apache.dubbo</groupId>
            <artifactId>dubbo-spring-boot-starter</artifactId>
            <version>${dubbo.version}</version>
            <optional>true</optional>
        </dependency>

        <!-- Spring Data Redis (optional, for TestRedisHelper) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

---

## 4. Source Code

### 4.1 @MateTest Annotation

```java
package vip.mate.starter.test.annotation;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for pure unit tests (domain / application layer).
 * <p>
 * No Spring context is loaded. Only Mockito is activated.
 * Test classes annotated with this will be tagged as "unit" for
 * Maven Surefire filtering.
 *
 * <pre>{@code
 * @MateTest
 * class UserDomainServiceTest {
 *     @Mock
 *     private UserRepository userRepository;
 *
 *     @InjectMocks
 *     private UserDomainServiceImpl userDomainService;
 *
 *     @Test
 *     void shouldCreateUser() { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public @interface MateTest {
}
```

### 4.2 @MateIntegrationTest Annotation

```java
package vip.mate.starter.test.annotation;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vip.mate.starter.test.MockRpcConfig;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for integration tests (trigger / infrastructure layer).
 * <p>
 * Loads full Spring context with "test" profile, imports MockRpcConfig
 * to mock all Dubbo RPC references, and tags as "integration" for
 * Maven Failsafe filtering.
 *
 * <pre>{@code
 * @MateIntegrationTest
 * class UserControllerIT extends BaseIntegrationTest {
 *
 *     @Autowired
 *     private MockMvc mockMvc;
 *
 *     @Test
 *     void shouldCreateUser() { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public @interface MateIntegrationTest {
}
```

### 4.3 MysqlContainerInitializer.java

```java
package vip.mate.starter.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ApplicationContextInitializer that starts a MySQL Testcontainer
 * and injects the JDBC connection properties into the Spring context.
 * <p>
 * The container is started once and shared across all test classes
 * in the same JVM via the static singleton pattern.
 */
public class MysqlContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final MySQLContainer<?> MYSQL;

    static {
        MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("matecloud_test")
                .withUsername("test")
                .withPassword("test")
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_unicode_ci",
                        "--lower_case_table_names=1"
                )
                .withReuse(true);
        MYSQL.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.datasource.url=" + MYSQL.getJdbcUrl(),
                "spring.datasource.username=" + MYSQL.getUsername(),
                "spring.datasource.password=" + MYSQL.getPassword(),
                "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
        ).applyTo(ctx.getEnvironment());
    }

    /**
     * Get the shared MySQL container instance (for direct JDBC access in tests).
     */
    public static MySQLContainer<?> getContainer() {
        return MYSQL;
    }
}
```

### 4.4 RedisContainerInitializer.java

```java
package vip.mate.starter.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ApplicationContextInitializer that starts a Redis Testcontainer
 * and injects the connection properties into the Spring context.
 */
public class RedisContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final GenericContainer<?> REDIS;

    static {
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withCommand("redis-server", "--appendonly", "yes")
                .withReuse(true);
        REDIS.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.data.redis.host=" + REDIS.getHost(),
                "spring.data.redis.port=" + REDIS.getMappedPort(6379),
                "spring.data.redis.password="
        ).applyTo(ctx.getEnvironment());
    }

    /**
     * Get the shared Redis container instance.
     */
    public static GenericContainer<?> getContainer() {
        return REDIS;
    }
}
```

### 4.5 RabbitMqContainerInitializer.java

```java
package vip.mate.starter.test.container;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * ApplicationContextInitializer that starts a RabbitMQ Testcontainer
 * and injects the connection properties into the Spring context.
 */
public class RabbitMqContainerInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final RabbitMQContainer RABBITMQ;

    static {
        RABBITMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
                .withReuse(true);
        RABBITMQ.start();
    }

    @Override
    public void initialize(ConfigurableApplicationContext ctx) {
        TestPropertyValues.of(
                "spring.rabbitmq.host=" + RABBITMQ.getHost(),
                "spring.rabbitmq.port=" + RABBITMQ.getAmqpPort(),
                "spring.rabbitmq.username=" + RABBITMQ.getAdminUsername(),
                "spring.rabbitmq.password=" + RABBITMQ.getAdminPassword()
        ).applyTo(ctx.getEnvironment());
    }

    /**
     * Get the shared RabbitMQ container instance.
     */
    public static RabbitMQContainer getContainer() {
        return RABBITMQ;
    }
}
```

### 4.6 BaseIntegrationTest.java

```java
package vip.mate.starter.test;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import vip.mate.starter.test.container.MysqlContainerInitializer;
import vip.mate.starter.test.container.RedisContainerInitializer;
import vip.mate.starter.test.container.RabbitMqContainerInitializer;

/**
 * Base class for integration tests that require the full Spring context
 * with Testcontainers for MySQL, Redis, and RabbitMQ.
 * <p>
 * Provides:
 * <ul>
 *   <li>Testcontainers: MySQL 8.0, Redis 7, RabbitMQ 3.13</li>
 *   <li>Spring Boot full context with "test" profile</li>
 *   <li>MockMvc for HTTP endpoint testing</li>
 *   <li>MockRpcConfig to mock all Dubbo RPC references</li>
 *   <li>Auto-configured datasource, Redis, and RabbitMQ connections</li>
 * </ul>
 *
 * <pre>{@code
 * class UserControllerIT extends BaseIntegrationTest {
 *
 *     @Test
 *     void shouldReturnUserById() throws Exception {
 *         mockMvc.perform(get("/api/v1/users/1"))
 *                .andExpect(status().isOk())
 *                .andExpect(jsonPath("$.data.userId").value(1));
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Convention:</strong> Integration test files must be named {@code *IT.java}
 * and placed in the {@code trigger/} or {@code infrastructure/} test packages.</p>
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(MockRpcConfig.class)
@ContextConfiguration(initializers = {
        MysqlContainerInitializer.class,
        RedisContainerInitializer.class,
        RabbitMqContainerInitializer.class
})
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;
}
```

### 4.7 BaseDomainTest.java

```java
package vip.mate.starter.test;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Base class for pure domain layer unit tests.
 * <p>
 * No Spring context is loaded. Uses Mockito for dependency mocking.
 * <p>
 * Provides:
 * <ul>
 *   <li>MockitoExtension for {@code @Mock} and {@code @InjectMocks}</li>
 *   <li>Tagged as "unit" for Maven Surefire filtering</li>
 * </ul>
 *
 * <pre>{@code
 * class UserDomainServiceTest extends BaseDomainTest {
 *
 *     @Mock
 *     private UserRepository userRepository;
 *
 *     @Mock
 *     private DomainEventPublisher eventPublisher;
 *
 *     @InjectMocks
 *     private UserDomainServiceImpl userDomainService;
 *
 *     @Test
 *     void shouldCreateUserWithDefaultRole() {
 *         // given
 *         when(userRepository.save(any())).thenReturn(testUser());
 *
 *         // when
 *         User user = userDomainService.createUser("13800138000", "Test");
 *
 *         // then
 *         assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
 *         verify(eventPublisher).publish(any(UserCreatedEvent.class));
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Convention:</strong> Unit test files must be named {@code *Test.java}
 * and placed in the {@code domain/} or {@code application/} test packages.</p>
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public abstract class BaseDomainTest {
}
```

### 4.8 MockRpcConfig.java

```java
package vip.mate.starter.test;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.api.service.IRpcUserService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test configuration that provides mock implementations for all Dubbo RPC references.
 * <p>
 * This replaces real RPC calls with Mockito mocks during integration tests,
 * eliminating the need for running dependent services.
 * <p>
 * Each mock is marked as {@code @Primary} to override any real Dubbo reference
 * that might be registered by the application context.
 *
 * <pre>{@code
 * // In your integration test, you can override mock behavior:
 * @Autowired
 * private IRpcUserService rpcUserService; // This is the mock
 *
 * @Test
 * void shouldHandleUserNotFound() {
 *     when(rpcUserService.findByMobile("unknown")).thenReturn(null);
 *     // ... test logic
 * }
 * }</pre>
 */
@TestConfiguration
public class MockRpcConfig {

    /**
     * Mock IRpcUserService with sensible defaults.
     * <p>
     * Default behavior:
     * - findByMobile() returns a test user with userId=1
     * - findById() returns a test user with the requested ID
     * - createByMobile() returns a new test user
     */
    @Bean
    @Primary
    public IRpcUserService mockRpcUserService() {
        IRpcUserService mock = Mockito.mock(IRpcUserService.class);

        // Default: return a test user for any mobile lookup
        when(mock.findByMobile(anyString())).thenAnswer(invocation -> {
            String mobile = invocation.getArgument(0);
            return buildTestUser(1L, mobile);
        });

        // Default: return a test user for any ID lookup
        when(mock.findById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            return buildTestUser(userId, "13800138000");
        });

        // Default: return a new user for createByMobile
        when(mock.createByMobile(anyString())).thenAnswer(invocation -> {
            String mobile = invocation.getArgument(0);
            return buildTestUser(100L, mobile);
        });

        return mock;
    }

    /**
     * Build a default test UserInfoDTO.
     */
    private UserInfoDTO buildTestUser(Long userId, String mobile) {
        UserInfoDTO user = new UserInfoDTO();
        user.setUserId(userId);
        user.setMobile(mobile);
        user.setNickName("Test User");
        user.setAvatar("https://example.com/avatar.png");
        user.setGender("UNKNOWN");
        user.setStatus("ACTIVE");
        user.setPassword("$2a$10$dummyhashfortest1234567890");
        user.setRoles(List.of("USER"));
        user.setPermissions(List.of("sys:user:list", "sys:user:view"));
        return user;
    }

    // ========================================================================
    // Add more mock RPC service beans here as new services are introduced.
    // Follow the same pattern:
    //   @Bean @Primary public ISomeRpcService mockSomeRpcService() { ... }
    // ========================================================================
}
```

### 4.9 TestDataBuilder.java

```java
package vip.mate.starter.test;

import vip.mate.api.dto.UserInfoDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating test data fixtures.
 * <p>
 * Provides pre-configured defaults that can be overridden as needed.
 * Every builder method returns {@code this} for chaining.
 *
 * <pre>{@code
 * UserInfoDTO admin = TestDataBuilder.user()
 *     .withUserId(1L)
 *     .withMobile("13800138001")
 *     .withRoles("ADMIN", "USER")
 *     .withPermissions("sys:user:list", "sys:user:create", "sys:user:delete")
 *     .build();
 *
 * UserInfoDTO frozenUser = TestDataBuilder.user()
 *     .withStatus("FROZEN")
 *     .build();
 * }</pre>
 */
public class TestDataBuilder {

    // ======================== User Builder ========================

    /**
     * Create a new UserInfoDTO builder with defaults.
     */
    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private Long userId = 1L;
        private String mobile = "13800138000";
        private String nickName = "Test User";
        private String avatar = "https://example.com/avatar.png";
        private String gender = "UNKNOWN";
        private String status = "ACTIVE";
        private String password = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        private List<String> roles = new ArrayList<>(List.of("USER"));
        private List<String> permissions = new ArrayList<>(List.of("sys:user:list"));

        public UserBuilder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        public UserBuilder withMobile(String mobile) {
            this.mobile = mobile;
            return this;
        }

        public UserBuilder withNickName(String nickName) {
            this.nickName = nickName;
            return this;
        }

        public UserBuilder withAvatar(String avatar) {
            this.avatar = avatar;
            return this;
        }

        public UserBuilder withGender(String gender) {
            this.gender = gender;
            return this;
        }

        public UserBuilder withStatus(String status) {
            this.status = status;
            return this;
        }

        public UserBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder withRoles(String... roles) {
            this.roles = List.of(roles);
            return this;
        }

        public UserBuilder withPermissions(String... permissions) {
            this.permissions = List.of(permissions);
            return this;
        }

        public UserBuilder asAdmin() {
            this.roles = List.of("ADMIN", "USER");
            this.permissions = List.of(
                    "sys:user:list", "sys:user:create", "sys:user:update", "sys:user:delete",
                    "sys:role:list", "sys:role:create", "sys:role:update", "sys:role:delete",
                    "sys:menu:list", "sys:menu:create", "sys:menu:update", "sys:menu:delete"
            );
            return this;
        }

        public UserBuilder asFrozen() {
            this.status = "FROZEN";
            return this;
        }

        public UserInfoDTO build() {
            UserInfoDTO user = new UserInfoDTO();
            user.setUserId(userId);
            user.setMobile(mobile);
            user.setNickName(nickName);
            user.setAvatar(avatar);
            user.setGender(gender);
            user.setStatus(status);
            user.setPassword(password);
            user.setRoles(roles);
            user.setPermissions(permissions);
            return user;
        }
    }
}
```

### 4.10 TestRedisHelper.java

```java
package vip.mate.starter.test.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Helper utility for Redis operations in integration tests.
 * <p>
 * Provides convenience methods for setup/teardown of Redis test data.
 *
 * <pre>{@code
 * @Autowired
 * private StringRedisTemplate redisTemplate;
 *
 * private TestRedisHelper redisHelper;
 *
 * @BeforeEach
 * void setUp() {
 *     redisHelper = new TestRedisHelper(redisTemplate);
 * }
 *
 * @AfterEach
 * void tearDown() {
 *     redisHelper.flushTestKeys();
 * }
 * }</pre>
 */
public class TestRedisHelper {

    private static final String TEST_KEY_PREFIX = "test:";

    private final StringRedisTemplate redisTemplate;

    public TestRedisHelper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Set a test key-value pair with a short TTL.
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(TEST_KEY_PREFIX + key, value, 5, TimeUnit.MINUTES);
    }

    /**
     * Set a key-value pair (without test prefix) for testing actual app behavior.
     */
    public void setRaw(String key, String value) {
        redisTemplate.opsForValue().set(key, value, 5, TimeUnit.MINUTES);
    }

    /**
     * Set a key-value pair with custom TTL.
     */
    public void setRaw(String key, String value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * Get a raw key value.
     */
    public String getRaw(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Check if a key exists.
     */
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Delete a specific key.
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * Flush all test keys (keys matching test:* pattern).
     */
    public void flushTestKeys() {
        Set<String> keys = redisTemplate.keys(TEST_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Flush keys matching a specific pattern.
     */
    public void flushPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

---

## 5. Test application-test.yml

Each service should include `src/test/resources/application-test.yml`:

```yaml
# Test profile configuration
spring:
  application:
    name: mate-system-test
  # Datasource, Redis, RabbitMQ are injected by Testcontainers initializers
  # Override any Nacos-dependent configs here
  cloud:
    nacos:
      discovery:
        enabled: false
      config:
        enabled: false

# Disable Dubbo discovery in tests
dubbo:
  registry:
    address: N/A
  protocol:
    name: dubbo
    port: -1
  consumer:
    check: false

# Flyway or schema init
spring.sql.init:
  mode: always
  schema-locations: classpath:db/schema.sql
  data-locations: classpath:db/test-data.sql

logging:
  level:
    vip.mate: debug
    org.testcontainers: info
```

---

## 6. Example Tests

### 6.1 UserDomainServiceTest (Unit Test)

```java
package vip.mate.system.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import vip.mate.starter.test.BaseDomainTest;
import vip.mate.starter.test.TestDataBuilder;
import vip.mate.system.domain.adapter.repository.UserRepository;
import vip.mate.system.domain.adapter.port.DomainEventPublisher;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.domain.service.impl.UserDomainServiceImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserDomainService.
 * <p>
 * Tests pure domain logic without Spring context or external dependencies.
 * <p>
 * Convention: *Test.java in domain/ package, runs with Maven Surefire.
 */
@DisplayName("UserDomainService Unit Tests")
class UserDomainServiceTest extends BaseDomainTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private UserDomainServiceImpl userDomainService;

    @Test
    @DisplayName("should create user with ACTIVE status and default role")
    void shouldCreateUserWithDefaults() {
        // given
        String mobile = "13800138000";
        String nickName = "New User";
        when(userRepository.findByMobile(mobile)).thenReturn(null);
        when(userRepository.save(any(UserAggregate.class))).thenAnswer(inv -> {
            UserAggregate agg = inv.getArgument(0);
            agg.getUser().setId(1L);
            return agg;
        });

        // when
        UserAggregate result = userDomainService.createUser(mobile, nickName);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUser().getMobile()).isEqualTo(mobile);
        assertThat(result.getUser().getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(any(UserAggregate.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("should reject creation when mobile already exists")
    void shouldRejectDuplicateMobile() {
        // given
        String mobile = "13800138000";
        when(userRepository.findByMobile(mobile)).thenReturn(new UserAggregate());

        // when & then
        assertThatThrownBy(() -> userDomainService.createUser(mobile, "Duplicate"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("should freeze user and publish event")
    void shouldFreezeUser() {
        // given
        UserAggregate userAgg = new UserAggregate();
        User user = new User();
        user.setId(1L);
        user.setMobile("13800138000");
        user.setStatus(UserStatus.ACTIVE);
        userAgg.setUser(user);
        when(userRepository.findById(1L)).thenReturn(userAgg);
        when(userRepository.save(any())).thenReturn(userAgg);

        // when
        userDomainService.freezeUser(1L);

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.FROZEN);
        verify(userRepository).save(userAgg);
        verify(eventPublisher).publish(any());
    }
}
```

### 6.2 UserRepositoryIT (Integration Test)

```java
package vip.mate.system.infrastructure.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vip.mate.starter.test.BaseIntegrationTest;
import vip.mate.starter.test.annotation.MateIntegrationTest;
import vip.mate.system.domain.model.aggregate.UserAggregate;
import vip.mate.system.domain.model.entity.User;
import vip.mate.system.domain.model.valobj.UserStatus;
import vip.mate.system.infrastructure.repository.impl.UserRepositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository (infrastructure layer).
 * <p>
 * Tests actual database operations against a MySQL Testcontainer.
 * <p>
 * Convention: *IT.java in infrastructure/ package, runs with Maven Failsafe.
 */
@MateIntegrationTest
@DisplayName("UserRepository Integration Tests")
class UserRepositoryIT extends BaseIntegrationTest {

    @Autowired
    private UserRepositoryImpl userRepository;

    @Test
    @DisplayName("should save and retrieve user by mobile")
    void shouldSaveAndFindByMobile() {
        // given
        UserAggregate userAgg = new UserAggregate();
        User user = new User();
        user.setMobile("13900139000");
        user.setNickName("Integration Test User");
        user.setStatus(UserStatus.ACTIVE);
        userAgg.setUser(user);

        // when
        UserAggregate saved = userRepository.save(userAgg);
        UserAggregate found = userRepository.findByMobile("13900139000");

        // then
        assertThat(saved.getUser().getId()).isNotNull();
        assertThat(found).isNotNull();
        assertThat(found.getUser().getMobile()).isEqualTo("13900139000");
        assertThat(found.getUser().getNickName()).isEqualTo("Integration Test User");
    }

    @Test
    @DisplayName("should return null for non-existent mobile")
    void shouldReturnNullForUnknownMobile() {
        // when
        UserAggregate result = userRepository.findByMobile("19999999999");

        // then
        assertThat(result).isNull();
    }
}
```

### 6.3 UserControllerIT (Integration Test)

```java
package vip.mate.system.trigger.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import vip.mate.starter.test.BaseIntegrationTest;
import vip.mate.starter.test.annotation.MateIntegrationTest;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController (trigger layer).
 * <p>
 * Tests HTTP endpoints against the full Spring context with Testcontainers.
 * RPC dependencies are mocked via MockRpcConfig.
 * <p>
 * Convention: *IT.java in trigger/ package, runs with Maven Failsafe.
 */
@MateIntegrationTest
@DisplayName("UserController Integration Tests")
class UserControllerIT extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/users should create a new user")
    void shouldCreateUser() throws Exception {
        Map<String, Object> request = Map.of(
                "mobile", "13700137000",
                "nickName", "Controller Test User"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").isNumber())
                .andExpect(jsonPath("$.data.mobile").value("13700137000"));
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} should return user by ID")
    void shouldGetUserById() throws Exception {
        // Assume test data was seeded via test-data.sql
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/users/999 should return 404 for non-existent user")
    void shouldReturn404ForMissingUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    @DisplayName("POST /api/v1/users with invalid mobile should return 400")
    void shouldRejectInvalidMobile() throws Exception {
        Map<String, Object> request = Map.of(
                "mobile", "123",  // invalid
                "nickName", "Bad Mobile"
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

### 6.4 IRpcUserServiceContractTest (RPC Contract Test)

```java
package vip.mate.system.trigger.rpc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vip.mate.api.dto.UserInfoDTO;
import vip.mate.api.service.IRpcUserService;
import vip.mate.starter.test.BaseIntegrationTest;
import vip.mate.starter.test.annotation.MateIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests for IRpcUserService Dubbo interface.
 * <p>
 * Verifies that the RPC implementation (RpcUserServiceImpl) fulfills
 * the interface contract defined in mate-api.
 * <p>
 * Convention: *ContractTest.java, verifies Dubbo interface contracts.
 * Runs against real infrastructure (Testcontainers) but tests the
 * actual service implementation, not the mock.
 */
@MateIntegrationTest
@DisplayName("IRpcUserService Contract Tests")
class IRpcUserServiceContractTest extends BaseIntegrationTest {

    @Autowired
    private IRpcUserService rpcUserService; // The actual impl, not mock

    @Test
    @DisplayName("findByMobile should return user with all required fields populated")
    void findByMobileShouldReturnCompleteUser() {
        // given - assume test data has user with mobile 13800138000
        String mobile = "13800138000";

        // when
        UserInfoDTO user = rpcUserService.findByMobile(mobile);

        // then - verify contract: all required fields are present
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isNotNull().isPositive();
        assertThat(user.getMobile()).isEqualTo(mobile);
        assertThat(user.getStatus()).isNotNull();
        assertThat(user.getRoles()).isNotNull().isNotEmpty();
        assertThat(user.getPermissions()).isNotNull();
        // Password should be present (caller decides whether to clear it)
        assertThat(user.getPassword()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("findByMobile should return null for non-existent mobile")
    void findByMobileShouldReturnNullForUnknown() {
        UserInfoDTO user = rpcUserService.findByMobile("19999999999");
        assertThat(user).isNull();
    }

    @Test
    @DisplayName("findById should return user with matching ID")
    void findByIdShouldReturnUser() {
        UserInfoDTO user = rpcUserService.findById(1L);

        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("createByMobile should return new user with default role")
    void createByMobileShouldReturnNewUser() {
        String newMobile = "15500155000";

        UserInfoDTO user = rpcUserService.createByMobile(newMobile);

        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isNotNull().isPositive();
        assertThat(user.getMobile()).isEqualTo(newMobile);
        assertThat(user.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getRoles()).contains("USER");
    }
}
```

---

## 7. Maven Configuration for Test Execution

### 7.1 Root pom.xml Plugin Configuration

```xml
<build>
    <pluginManagement>
        <plugins>
            <!-- Surefire: Unit tests (*Test.java) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <includes>
                        <include>**/*Test.java</include>
                    </includes>
                    <excludes>
                        <exclude>**/*IT.java</exclude>
                        <exclude>**/*ContractTest.java</exclude>
                    </excludes>
                    <groups>unit</groups>
                    <argLine>-Xms256m -Xmx512m</argLine>
                </configuration>
            </plugin>

            <!-- Failsafe: Integration tests (*IT.java, *ContractTest.java) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>3.5.2</version>
                <configuration>
                    <includes>
                        <include>**/*IT.java</include>
                        <include>**/*ContractTest.java</include>
                    </includes>
                    <groups>integration</groups>
                    <argLine>-Xms256m -Xmx1024m</argLine>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>integration-test</goal>
                            <goal>verify</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </pluginManagement>
</build>

<profiles>
    <!-- Profile to run integration tests (requires Docker for Testcontainers) -->
    <profile>
        <id>integration-test</id>
        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-failsafe-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </profile>
</profiles>
```

### 7.2 Per-Service Test Dependency

In each service `pom.xml`:

```xml
<dependencies>
    <!-- Test -->
    <dependency>
        <groupId>vip.mate</groupId>
        <artifactId>mate-test-starter</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Add to root `pom.xml` `<dependencyManagement>`:

```xml
<dependency>
    <groupId>vip.mate</groupId>
    <artifactId>mate-test-starter</artifactId>
    <version>${matecloud.version}</version>
    <scope>test</scope>
</dependency>
```

---

## 8. Test Execution Commands

```bash
# Run unit tests only (fast, no Docker needed)
mvn test

# Run unit tests for a specific module
mvn test -pl mate-biz/mate-system

# Run integration tests (requires Docker for Testcontainers)
mvn verify -Pintegration-test

# Run integration tests for a specific module
mvn verify -Pintegration-test -pl mate-biz/mate-system

# Run all tests (unit + integration)
mvn verify -Pintegration-test

# Run a specific test class
mvn test -pl mate-biz/mate-system -Dtest=UserDomainServiceTest

# Run with verbose output
mvn verify -Pintegration-test -Dsurefire.useFile=false
```

---

## 9. Test Conventions Summary

| Aspect              | Unit Test                    | Integration Test              | Contract Test                  |
|---------------------|------------------------------|-------------------------------|--------------------------------|
| **File naming**     | `*Test.java`                 | `*IT.java`                    | `*ContractTest.java`           |
| **Base class**      | `BaseDomainTest`             | `BaseIntegrationTest`         | `BaseIntegrationTest`          |
| **Location**        | `domain/`, `application/`    | `trigger/`, `infrastructure/` | `trigger/rpc/`                 |
| **Spring context**  | None                         | Full (with Testcontainers)    | Full (with Testcontainers)     |
| **Dependencies**    | Mocked (Mockito)             | Real + MockRpcConfig          | Real implementations           |
| **Database**        | None                         | MySQL Testcontainer           | MySQL Testcontainer            |
| **Redis**           | None                         | Redis Testcontainer           | Redis Testcontainer            |
| **RabbitMQ**        | None                         | RabbitMQ Testcontainer        | RabbitMQ Testcontainer         |
| **RPC**             | Mocked                       | Mocked (MockRpcConfig)        | Real implementation            |
| **Maven plugin**    | Surefire                     | Failsafe                      | Failsafe                       |
| **Tag**             | `unit`                       | `integration`                 | `integration`                  |
| **Docker required** | No                           | Yes                           | Yes                            |
| **Execution**       | `mvn test`                   | `mvn verify -Pintegration-test` | `mvn verify -Pintegration-test` |

---

## 10. CI/CD Integration

### GitHub Actions Example

```yaml
name: CI

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Run Unit Tests
        run: mvn test -B --no-transfer-progress

  integration-tests:
    runs-on: ubuntu-latest
    needs: unit-tests
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Run Integration Tests
        run: mvn verify -Pintegration-test -B --no-transfer-progress
```

---

## 11. Testing Checklist

- [ ] `mate-test-starter` compiles and can be included as test dependency
- [ ] `BaseDomainTest` subclass runs with Mockito, no Spring context
- [ ] `BaseIntegrationTest` subclass starts MySQL, Redis, RabbitMQ containers
- [ ] Testcontainers use reuse=true for faster repeat runs
- [ ] `MockRpcConfig` provides working mock for IRpcUserService
- [ ] `TestDataBuilder.user().build()` returns valid UserInfoDTO with defaults
- [ ] `TestDataBuilder.user().asAdmin().build()` returns admin user with full permissions
- [ ] `TestRedisHelper` can set/get/flush Redis test data
- [ ] `mvn test` runs only `*Test.java` files (unit tests)
- [ ] `mvn verify -Pintegration-test` runs `*IT.java` and `*ContractTest.java` files
- [ ] Tests use `@ActiveProfiles("test")` and `application-test.yml`
- [ ] Nacos/Dubbo discovery is disabled in test profile
- [ ] All example tests compile and demonstrate correct patterns
