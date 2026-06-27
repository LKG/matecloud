# RFC-016: Database Sharding

| Field       | Value                              |
|-------------|------------------------------------|
| **RFC**     | 016                                |
| **Title**   | Database Sharding                  |
| **Status**  | Draft                              |
| **Created** | 2026-04-11                         |

## Summary

This RFC defines **mate-sharding-starter**, a Spring Boot starter integrating Apache ShardingSphere JDBC for transparent table and database sharding. It provides a custom hash-based sharding algorithm supporting a 2-database x 4-table topology (8 shards total), fully transparent to MyBatis Plus at the application layer.

Key components:
- `MateShardingAlgorithm` -- custom sharding algorithm (hash mod 8, split across 2 DBs x 4 tables)
- YAML configuration template for ShardingSphere rules
- Transparent integration with MyBatis Plus (no code changes needed in repositories)

Reference: kaleido-ai/kaleido-common/kaleido-ds (CustomShardingAlgorithm)

---

## 1. pom.xml

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

    <artifactId>mate-sharding-starter</artifactId>
    <packaging>jar</packaging>
    <description>MateCloud Sharding Starter - ShardingSphere JDBC integration for database/table sharding</description>

    <dependencies>
        <!-- ShardingSphere JDBC -->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-jdbc</artifactId>
            <version>5.5.2</version>
        </dependency>

        <!-- ShardingSphere sharding algorithm SPI -->
        <dependency>
            <groupId>org.apache.shardingsphere</groupId>
            <artifactId>shardingsphere-sharding-api</artifactId>
            <version>5.5.2</version>
        </dependency>

        <!-- MyBatis Plus (provided by business module) -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- HikariCP connection pool -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

        <!-- MySQL driver -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- mate-base -->
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
    </dependencies>
</project>
```

---

## 2. Java Source Code

### 2.1 MateShardingAlgorithm

```java
package vip.mate.starter.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * Custom hash-based sharding algorithm for MateCloud.
 * <p>
 * Supports a topology of 2 databases x 4 tables = 8 total shards.
 * The sharding key value is hashed modulo 8 to determine the shard:
 * <ul>
 *   <li>Database index = (hash % 8) / 4  --> ds_0 or ds_1</li>
 *   <li>Table suffix   = (hash % 8) % 4  --> _0, _1, _2, _3</li>
 * </ul>
 * </p>
 *
 * <p>For range queries, all available target names are returned (full scan).</p>
 */
@Slf4j
public class MateShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final int TOTAL_DB_COUNT = 2;
    private static final int TABLES_PER_DB = 4;
    private static final int TOTAL_SHARDS = TOTAL_DB_COUNT * TABLES_PER_DB; // 8

    private Properties props = new Properties();

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Comparable<?>> shardingValue) {
        Comparable<?> value = shardingValue.getValue();

        // Calculate shard index from the sharding key value
        long shardingValueLong = toShardingLong(value);
        long shardIndex = Math.abs(shardingValueLong) % TOTAL_SHARDS;

        // Determine database and table
        int databaseIndex = (int) (shardIndex / TABLES_PER_DB);
        int tableSuffix = (int) (shardIndex % TABLES_PER_DB);

        String targetDb = "ds_" + databaseIndex;
        String targetTable = shardingValue.getLogicTableName() + "_" + tableSuffix;

        log.debug("[mate-sharding] Sharding key={}, value={}, shardIndex={}, target={}.{}",
                shardingValue.getColumnName(), value, shardIndex, targetDb, targetTable);

        // For database sharding: return matching database name
        for (String name : availableTargetNames) {
            if (name.equals(targetDb)) {
                return name;
            }
            if (name.equals(targetTable)) {
                return name;
            }
        }

        // Fallback: return constructed name
        return targetTable;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Comparable<?>> shardingValue) {
        // Range queries hit all shards
        log.debug("[mate-sharding] Range query on {}, returning all {} targets",
                shardingValue.getLogicTableName(), availableTargetNames.size());
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "MATE_HASH";
    }

    /**
     * Convert a sharding key value to a long for modulo calculation.
     */
    private long toShardingLong(Comparable<?> value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String strValue) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return Math.abs(strValue.hashCode());
            }
        }
        return Math.abs(value.hashCode());
    }
}
```

### 2.2 MateDbShardingAlgorithm (Database-level)

```java
package vip.mate.starter.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * Database-level sharding algorithm.
 * <p>
 * Routes to ds_0 or ds_1 based on (shardingKey % 8) / 4.
 * </p>
 */
@Slf4j
public class MateDbShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final int TOTAL_SHARDS = 8;
    private static final int TABLES_PER_DB = 4;

    private Properties props = new Properties();

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Comparable<?>> shardingValue) {
        long shardingValueLong = toShardingLong(shardingValue.getValue());
        long shardIndex = Math.abs(shardingValueLong) % TOTAL_SHARDS;
        int databaseIndex = (int) (shardIndex / TABLES_PER_DB);

        String targetDb = "ds_" + databaseIndex;

        for (String name : availableTargetNames) {
            if (name.endsWith(String.valueOf(databaseIndex))) {
                return name;
            }
        }
        return targetDb;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Comparable<?>> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "MATE_DB_HASH";
    }

    private long toShardingLong(Comparable<?> value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String strValue) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return Math.abs(strValue.hashCode());
            }
        }
        return Math.abs(value.hashCode());
    }
}
```

### 2.3 MateTableShardingAlgorithm (Table-level)

```java
package vip.mate.starter.sharding.algorithm;

import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * Table-level sharding algorithm.
 * <p>
 * Routes to table suffix _0 through _3 based on (shardingKey % 8) % 4.
 * </p>
 */
@Slf4j
public class MateTableShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    private static final int TOTAL_SHARDS = 8;
    private static final int TABLES_PER_DB = 4;

    private Properties props = new Properties();

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<Comparable<?>> shardingValue) {
        long shardingValueLong = toShardingLong(shardingValue.getValue());
        long shardIndex = Math.abs(shardingValueLong) % TOTAL_SHARDS;
        int tableSuffix = (int) (shardIndex % TABLES_PER_DB);

        String targetTable = shardingValue.getLogicTableName() + "_" + tableSuffix;

        for (String name : availableTargetNames) {
            if (name.endsWith("_" + tableSuffix)) {
                return name;
            }
        }
        return targetTable;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Comparable<?>> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
        this.props = props;
    }

    @Override
    public String getType() {
        return "MATE_TABLE_HASH";
    }

    private long toShardingLong(Comparable<?> value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String strValue) {
            try {
                return Long.parseLong(strValue);
            } catch (NumberFormatException e) {
                return Math.abs(strValue.hashCode());
            }
        }
        return Math.abs(value.hashCode());
    }
}
```

### 2.4 ShardingAutoConfiguration

```java
package vip.mate.starter.sharding.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for mate-sharding-starter.
 * <p>
 * Activates when ShardingSphere JDBC is on the classpath.
 * The actual sharding rules are driven by ShardingSphere's own YAML/properties
 * configuration. This starter provides the custom algorithm implementations
 * registered via ShardingSphere SPI.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.apache.shardingsphere.driver.ShardingSphereDriver")
@ComponentScan(basePackages = "vip.mate.starter.sharding")
public class ShardingAutoConfiguration {
    // ShardingSphere algorithms are discovered via SPI (META-INF/services)
    // No explicit bean registration needed for algorithms
}
```

### 2.5 SPI Registration

**`META-INF/services/org.apache.shardingsphere.sharding.spi.ShardingAlgorithm`**

```text
vip.mate.starter.sharding.algorithm.MateShardingAlgorithm
vip.mate.starter.sharding.algorithm.MateDbShardingAlgorithm
vip.mate.starter.sharding.algorithm.MateTableShardingAlgorithm
```

### 2.6 AutoConfiguration.imports

**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**

```text
vip.mate.starter.sharding.config.ShardingAutoConfiguration
```

---

## 3. ShardingSphere YAML Configuration Template

The following `sharding.yaml` should be placed in the business module's resources or referenced from `application.yml`:

```yaml
# sharding.yaml - ShardingSphere JDBC configuration
# Place in business module: src/main/resources/sharding.yaml

dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/mate_order_0?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    hikari:
      minimumIdle: 5
      maximumPoolSize: 20
      connectionTimeout: 30000
  ds_1:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://localhost:3306/mate_order_1?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD:root}
    hikari:
      minimumIdle: 5
      maximumPoolSize: 20
      connectionTimeout: 30000

rules:
  - !SHARDING
    tables:
      t_order:
        actualDataNodes: ds_${0..1}.t_order_${0..3}
        databaseStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: mate_db_hash
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: mate_table_hash
        keyGenerateStrategy:
          column: order_id
          keyGeneratorName: snowflake
      t_order_item:
        actualDataNodes: ds_${0..1}.t_order_item_${0..3}
        databaseStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: mate_db_hash
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: mate_table_hash

    shardingAlgorithms:
      mate_db_hash:
        type: MATE_DB_HASH
      mate_table_hash:
        type: MATE_TABLE_HASH
      mate_hash:
        type: MATE_HASH

    keyGenerators:
      snowflake:
        type: SNOWFLAKE

props:
  sql-show: true
```

---

## 4. application.yml Integration

```yaml
# In business module's application.yml
spring:
  datasource:
    driver-class-name: org.apache.shardingsphere.driver.ShardingSphereDriver
    url: jdbc:shardingsphere:classpath:sharding.yaml

# MyBatis Plus works transparently -- no special config needed
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
```

---

## 5. Usage Example

### 5.1 Entity (MyBatis Plus)

```java
package vip.mate.biz.order.infrastructure.dao.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class OrderPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long orderId;

    private Long userId;
    private BigDecimal amount;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### 5.2 Mapper (transparent sharding)

```java
package vip.mate.biz.order.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.biz.order.infrastructure.dao.po.OrderPO;

@Mapper
public interface OrderDao extends BaseMapper<OrderPO> {
    // All standard MyBatis Plus operations work transparently.
    // ShardingSphere intercepts SQL and routes to correct shard.
}
```

### 5.3 Service Layer (no sharding awareness needed)

```java
@Service
@RequiredArgsConstructor
public class OrderRepositoryImpl implements IOrderRepository {

    private final OrderDao orderDao;

    @Override
    public void save(OrderAggregate order) {
        OrderPO po = OrderConvertor.toPO(order);
        orderDao.insert(po);
        // ShardingSphere automatically routes based on order_id
    }

    @Override
    public OrderAggregate findById(Long orderId) {
        OrderPO po = orderDao.selectById(orderId);
        // ShardingSphere routes the SELECT to the correct shard
        return po != null ? OrderConvertor.toAggregate(po) : null;
    }
}
```

---

## 6. DDL for Sharded Tables

```sql
-- Execute on mate_order_0
CREATE TABLE t_order_0 (
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Repeat for t_order_1, t_order_2, t_order_3 on mate_order_0
-- Repeat for t_order_0, t_order_1, t_order_2, t_order_3 on mate_order_1
```

---

## 7. Module Structure

```
mate-starters/
  mate-sharding-starter/
    pom.xml
    src/main/java/
      vip/mate/starter/sharding/
        algorithm/
          MateShardingAlgorithm.java
          MateDbShardingAlgorithm.java
          MateTableShardingAlgorithm.java
        config/
          ShardingAutoConfiguration.java
    src/main/resources/
      META-INF/
        services/
          org.apache.shardingsphere.sharding.spi.ShardingAlgorithm
        spring/
          org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

---

## 8. Design Notes

1. **Topology: 2 DBs x 4 tables**: This is the default layout. The algorithm can be extended by making `TOTAL_DB_COUNT` and `TABLES_PER_DB` configurable via ShardingSphere properties.

2. **SPI registration**: ShardingSphere discovers sharding algorithms via Java SPI. The `getType()` method returns the algorithm name used in YAML config (`MATE_DB_HASH`, `MATE_TABLE_HASH`).

3. **Transparent to MyBatis Plus**: Application code writes standard MyBatis Plus queries. ShardingSphere JDBC driver intercepts and rewrites SQL at the JDBC layer.

4. **Range queries**: Range-based queries (BETWEEN, >, <) route to all shards. For high-performance range queries, consider adding a date-based sharding dimension.

5. **Key generation**: The config template uses ShardingSphere's built-in SNOWFLAKE key generator. For consistency with mate-distribute-starter, you may instead assign IDs in the domain layer via `SnowflakeUtil.newSnowflakeId()`.
