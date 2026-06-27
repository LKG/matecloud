# RFC-013: Business Infrastructure

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 4-5
- **Dependencies**: RFC-005 (mq/job starters), RFC-008 (mate-system)

## Overview

Business development repeatedly encounters common scenarios: workflow approval, rule engine, import/export, sequence generation. This RFC provides three new starters (`mate-flow-starter`, `mate-rule-starter`, `mate-excel-starter`) and adds `SequenceGenerator` + `RedisSequenceGenerator` to `mate-base` / `mate-cache-starter`.

---

## 1. Unified Sequence Generator (mate-base + mate-cache-starter)

### 1.1 Interface in mate-base

**File**: `mate-common/mate-base/src/main/java/vip/mate/base/sequence/SequenceGenerator.java`

```java
package vip.mate.base.sequence;

/**
 * Generates unique business sequence numbers.
 * Format: PREFIX + yyyyMMdd + zero-padded sequence (e.g. ORD202604120001).
 */
public interface SequenceGenerator {

    /**
     * Generate the next sequence number for the given prefix.
     *
     * @param prefix business prefix, e.g. "ORD", "USR"
     * @return formatted sequence string
     */
    String next(String prefix);

    /**
     * Generate the next sequence number with a custom zero-pad width.
     *
     * @param prefix   business prefix
     * @param padWidth zero-pad width (default 4)
     * @return formatted sequence string
     */
    String next(String prefix, int padWidth);
}
```

### 1.2 Redis Implementation in mate-cache-starter

**File**: `mate-starters/mate-cache-starter/src/main/java/vip/mate/starter/cache/sequence/RedisSequenceGenerator.java`

```java
package vip.mate.starter.cache.sequence;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import vip.mate.base.sequence.SequenceGenerator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis INCR-based sequence generator.
 * Key pattern: seq:{prefix}:{yyyyMMdd}, expires after 2 days.
 */
@Component
@RequiredArgsConstructor
public class RedisSequenceGenerator implements SequenceGenerator {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Duration KEY_TTL = Duration.ofDays(2);

    private final RedissonClient redissonClient;

    @Override
    public String next(String prefix) {
        return next(prefix, 4);
    }

    @Override
    public String next(String prefix, int padWidth) {
        String dateKey = LocalDate.now().format(DATE_FMT);
        String redisKey = "seq:" + prefix + ":" + dateKey;

        RAtomicLong counter = redissonClient.getAtomicLong(redisKey);
        long seq = counter.incrementAndGet();

        // Set TTL on first use each day
        if (seq == 1L) {
            counter.expire(KEY_TTL);
        }

        String format = "%0" + padWidth + "d";
        return prefix + dateKey + String.format(format, seq);
    }
}
```

---

## 2. Lightweight Workflow Engine (mate-flow-starter)

### 2.1 pom.xml

**File**: `mate-starters/mate-flow-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-flow-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-flow-starter</name>
    <description>Lightweight workflow engine starter - DDD state-machine based approval flow</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 2.2 Domain Models

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/NodeType.java`

```java
package vip.mate.starter.flow.model;

/**
 * Types of flow nodes.
 */
public enum NodeType {
    /** Start node - entry point of a flow */
    START,
    /** Approval node - requires assignee action */
    APPROVAL,
    /** CC (carbon copy) node - notify only, no action required */
    CC,
    /** Condition node - branches based on expression */
    CONDITION,
    /** End node - terminal node */
    END
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/ApproverType.java`

```java
package vip.mate.starter.flow.model;

/**
 * How to resolve the approver(s) for an approval node.
 */
public enum ApproverType {
    /** Fixed user IDs */
    FIXED_USER,
    /** Resolved by role key(s) */
    ROLE,
    /** Department leader of the initiator */
    DEPT_LEADER,
    /** Direct leader of the initiator */
    INITIATOR_LEADER
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/ApproverRule.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Rule that determines who should approve a given node.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApproverRule implements Serializable {

    /** How to resolve the approver */
    private ApproverType type;

    /** userId list (FIXED_USER) or roleKey list (ROLE) */
    private List<String> values;

    /** true = all must approve (countersign), false = any one is enough (or-sign) */
    private boolean multiApprove;

    // ---------- factory helpers ----------

    public static ApproverRule fixedUser(String... userIds) {
        return ApproverRule.builder()
                .type(ApproverType.FIXED_USER)
                .values(List.of(userIds))
                .multiApprove(false)
                .build();
    }

    public static ApproverRule role(String... roleKeys) {
        return ApproverRule.builder()
                .type(ApproverType.ROLE)
                .values(List.of(roleKeys))
                .multiApprove(false)
                .build();
    }

    public static ApproverRule deptLeader() {
        return ApproverRule.builder()
                .type(ApproverType.DEPT_LEADER)
                .values(List.of())
                .multiApprove(false)
                .build();
    }

    public static ApproverRule initiatorLeader() {
        return ApproverRule.builder()
                .type(ApproverType.INITIATOR_LEADER)
                .values(List.of())
                .multiApprove(false)
                .build();
    }
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowNode.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A single node in a flow definition graph.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowNode implements Serializable {

    private String nodeId;
    private NodeType type;
    private String name;
    private ApproverRule approverRule;

    // ---------- factory helpers ----------

    public static FlowNode start(String nodeId) {
        return FlowNode.builder()
                .nodeId(nodeId).type(NodeType.START).name("Start")
                .build();
    }

    public static FlowNode approval(String nodeId, String name, ApproverRule rule) {
        return FlowNode.builder()
                .nodeId(nodeId).type(NodeType.APPROVAL).name(name).approverRule(rule)
                .build();
    }

    public static FlowNode cc(String nodeId, String name) {
        return FlowNode.builder()
                .nodeId(nodeId).type(NodeType.CC).name(name)
                .build();
    }

    public static FlowNode end(String nodeId) {
        return FlowNode.builder()
                .nodeId(nodeId).type(NodeType.END).name("End")
                .build();
    }
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowEdge.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Directed edge between two flow nodes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowEdge implements Serializable {

    private String sourceNodeId;
    private String targetNodeId;
    /** Optional SpEL condition expression for CONDITION branches */
    private String conditionExpr;

    public static FlowEdge of(String source, String target) {
        return FlowEdge.builder()
                .sourceNodeId(source).targetNodeId(target)
                .build();
    }

    public static FlowEdge ofCondition(String source, String target, String expr) {
        return FlowEdge.builder()
                .sourceNodeId(source).targetNodeId(target).conditionExpr(expr)
                .build();
    }
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowDefinition.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Complete flow definition comprising nodes and edges.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinition implements Serializable {

    /** Unique key, e.g. "leave_approval" */
    private String flowKey;
    /** Human-readable name */
    private String name;
    /** Ordered list of nodes */
    private List<FlowNode> nodes;
    /** Directed edges between nodes */
    private List<FlowEdge> edges;

    /**
     * Find the next node(s) after the given source node.
     */
    public Optional<FlowNode> findNextNode(String sourceNodeId) {
        return edges.stream()
                .filter(e -> e.getSourceNodeId().equals(sourceNodeId))
                .findFirst()
                .flatMap(edge -> nodes.stream()
                        .filter(n -> n.getNodeId().equals(edge.getTargetNodeId()))
                        .findFirst());
    }

    /**
     * Find a node by its ID.
     */
    public Optional<FlowNode> findNode(String nodeId) {
        return nodes.stream()
                .filter(n -> n.getNodeId().equals(nodeId))
                .findFirst();
    }
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowStatus.java`

```java
package vip.mate.starter.flow.model;

/**
 * Runtime status of a flow instance.
 */
public enum FlowStatus {
    /** Flow is actively running, waiting for approval(s) */
    RUNNING,
    /** All approval nodes passed, flow completed successfully */
    APPROVED,
    /** An approver rejected the flow */
    REJECTED,
    /** The initiator cancelled the flow */
    CANCELLED
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowInstance.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * A running (or completed) instance of a flow definition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowInstance implements Serializable {

    private String instanceId;
    /** Reference to flow definition key */
    private String flowKey;
    /** Associated business entity ID */
    private String businessId;
    /** Business type discriminator, e.g. "leave", "expense" */
    private String businessType;
    /** User who initiated the flow */
    private String initiatorId;
    /** Current status */
    private FlowStatus status;
    /** ID of the node currently awaiting action */
    private String currentNodeId;
    /** JSON snapshot of the submitted form data */
    private String formData;
    private Date createTime;
    private Date updateTime;
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/TaskStatus.java`

```java
package vip.mate.starter.flow.model;

/**
 * Status of an individual approval task.
 */
public enum TaskStatus {
    /** Waiting for assignee to act */
    PENDING,
    /** Assignee approved */
    APPROVED,
    /** Assignee rejected */
    REJECTED,
    /** Task was transferred to another user */
    TRANSFERRED
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/model/FlowTask.java`

```java
package vip.mate.starter.flow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * An individual task assigned to an approver within a flow instance.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowTask implements Serializable {

    private String taskId;
    private String instanceId;
    private String nodeId;
    /** The user responsible for this task */
    private String assigneeId;
    private TaskStatus status;
    /** Approver's comment or reason */
    private String comment;
    private Date createTime;
    private Date completeTime;
}
```

### 2.3 Infrastructure - Persistent Objects

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/po/FlowInstancePO.java`

```java
package vip.mate.starter.flow.infrastructure.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.starter.ds.model.BasePO;

/**
 * Persistent object for mate_flow_instance table.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_flow_instance")
public class FlowInstancePO extends BasePO {

    private String flowKey;
    private String bizId;
    private String bizType;
    private String initiatorId;
    /** RUNNING / APPROVED / REJECTED / CANCELLED */
    private String status;
    private String currentNodeId;
    /** JSON text */
    private String formData;
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/po/FlowTaskPO.java`

```java
package vip.mate.starter.flow.infrastructure.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.starter.ds.model.BasePO;

import java.util.Date;

/**
 * Persistent object for mate_flow_task table.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_flow_task")
public class FlowTaskPO extends BasePO {

    private String instanceId;
    private String nodeId;
    private String assigneeId;
    /** PENDING / APPROVED / REJECTED / TRANSFERRED */
    private String status;
    private String comment;
    private Date completedAt;
}
```

### 2.4 Infrastructure - DAO (MyBatis Plus)

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/dao/FlowInstanceDao.java`

```java
package vip.mate.starter.flow.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.starter.flow.infrastructure.po.FlowInstancePO;

/**
 * MyBatis Plus mapper for mate_flow_instance.
 */
@Mapper
public interface FlowInstanceDao extends BaseMapper<FlowInstancePO> {
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/dao/FlowTaskDao.java`

```java
package vip.mate.starter.flow.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.starter.flow.infrastructure.po.FlowTaskPO;

/**
 * MyBatis Plus mapper for mate_flow_task.
 */
@Mapper
public interface FlowTaskDao extends BaseMapper<FlowTaskPO> {
}
```

### 2.5 Repository Interfaces

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/repository/FlowInstanceRepository.java`

```java
package vip.mate.starter.flow.repository;

import vip.mate.starter.flow.model.FlowInstance;

import java.util.Optional;

/**
 * Repository abstraction for flow instances.
 */
public interface FlowInstanceRepository {

    void save(FlowInstance instance);

    void update(FlowInstance instance);

    Optional<FlowInstance> findById(String instanceId);
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/repository/FlowTaskRepository.java`

```java
package vip.mate.starter.flow.repository;

import vip.mate.starter.flow.model.FlowTask;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for flow tasks.
 */
public interface FlowTaskRepository {

    void save(FlowTask task);

    void update(FlowTask task);

    Optional<FlowTask> findById(String taskId);

    List<FlowTask> findPendingByAssignee(String assigneeId);

    List<FlowTask> findByInstanceId(String instanceId);
}
```

### 2.6 Repository Implementations

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/repository/FlowInstanceRepositoryImpl.java`

```java
package vip.mate.starter.flow.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.starter.flow.infrastructure.dao.FlowInstanceDao;
import vip.mate.starter.flow.infrastructure.po.FlowInstancePO;
import vip.mate.starter.flow.model.FlowInstance;
import vip.mate.starter.flow.model.FlowStatus;
import vip.mate.starter.flow.repository.FlowInstanceRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlowInstanceRepositoryImpl implements FlowInstanceRepository {

    private final FlowInstanceDao flowInstanceDao;

    @Override
    public void save(FlowInstance instance) {
        FlowInstancePO po = toPO(instance);
        flowInstanceDao.insert(po);
    }

    @Override
    public void update(FlowInstance instance) {
        FlowInstancePO po = toPO(instance);
        flowInstanceDao.updateById(po);
    }

    @Override
    public Optional<FlowInstance> findById(String instanceId) {
        FlowInstancePO po = flowInstanceDao.selectById(instanceId);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    // ---------- converters ----------

    private FlowInstancePO toPO(FlowInstance d) {
        return FlowInstancePO.builder()
                .id(d.getInstanceId())
                .flowKey(d.getFlowKey())
                .bizId(d.getBusinessId())
                .bizType(d.getBusinessType())
                .initiatorId(d.getInitiatorId())
                .status(d.getStatus().name())
                .currentNodeId(d.getCurrentNodeId())
                .formData(d.getFormData())
                .build();
    }

    private FlowInstance toDomain(FlowInstancePO po) {
        return FlowInstance.builder()
                .instanceId(po.getId())
                .flowKey(po.getFlowKey())
                .businessId(po.getBizId())
                .businessType(po.getBizType())
                .initiatorId(po.getInitiatorId())
                .status(FlowStatus.valueOf(po.getStatus()))
                .currentNodeId(po.getCurrentNodeId())
                .formData(po.getFormData())
                .createTime(po.getCreatedAt())
                .updateTime(po.getUpdatedAt())
                .build();
    }
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/infrastructure/repository/FlowTaskRepositoryImpl.java`

```java
package vip.mate.starter.flow.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.starter.flow.infrastructure.dao.FlowTaskDao;
import vip.mate.starter.flow.infrastructure.po.FlowTaskPO;
import vip.mate.starter.flow.model.FlowTask;
import vip.mate.starter.flow.model.TaskStatus;
import vip.mate.starter.flow.repository.FlowTaskRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FlowTaskRepositoryImpl implements FlowTaskRepository {

    private final FlowTaskDao flowTaskDao;

    @Override
    public void save(FlowTask task) {
        flowTaskDao.insert(toPO(task));
    }

    @Override
    public void update(FlowTask task) {
        flowTaskDao.updateById(toPO(task));
    }

    @Override
    public Optional<FlowTask> findById(String taskId) {
        return Optional.ofNullable(flowTaskDao.selectById(taskId)).map(this::toDomain);
    }

    @Override
    public List<FlowTask> findPendingByAssignee(String assigneeId) {
        LambdaQueryWrapper<FlowTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTaskPO::getAssigneeId, assigneeId)
               .eq(FlowTaskPO::getStatus, TaskStatus.PENDING.name())
               .orderByDesc(FlowTaskPO::getCreatedAt);
        return flowTaskDao.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public List<FlowTask> findByInstanceId(String instanceId) {
        LambdaQueryWrapper<FlowTaskPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTaskPO::getInstanceId, instanceId)
               .orderByAsc(FlowTaskPO::getCreatedAt);
        return flowTaskDao.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    // ---------- converters ----------

    private FlowTaskPO toPO(FlowTask d) {
        return FlowTaskPO.builder()
                .id(d.getTaskId())
                .instanceId(d.getInstanceId())
                .nodeId(d.getNodeId())
                .assigneeId(d.getAssigneeId())
                .status(d.getStatus().name())
                .comment(d.getComment())
                .completedAt(d.getCompleteTime())
                .build();
    }

    private FlowTask toDomain(FlowTaskPO po) {
        return FlowTask.builder()
                .taskId(po.getId())
                .instanceId(po.getInstanceId())
                .nodeId(po.getNodeId())
                .assigneeId(po.getAssigneeId())
                .status(TaskStatus.valueOf(po.getStatus()))
                .comment(po.getComment())
                .createTime(po.getCreatedAt())
                .completeTime(po.getCompletedAt())
                .build();
    }
}
```

### 2.7 Flow Definition Service

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/service/FlowDefinitionService.java`

```java
package vip.mate.starter.flow.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import vip.mate.base.exception.BizException;
import vip.mate.starter.flow.model.FlowDefinition;

import java.util.Optional;

/**
 * Manages flow definitions. Definitions are stored in Redis for fast lookup.
 * In production, persist to DB as well for durability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDefinitionService {

    private static final String KEY_PREFIX = "flow:def:";

    private final RedissonClient redissonClient;

    /**
     * Deploy (or update) a flow definition.
     */
    public void deploy(FlowDefinition definition) {
        if (definition.getFlowKey() == null || definition.getFlowKey().isBlank()) {
            throw new BizException("FLOW_KEY_EMPTY", "flowKey must not be blank");
        }
        if (definition.getNodes() == null || definition.getNodes().isEmpty()) {
            throw new BizException("FLOW_NODES_EMPTY", "flow must have at least one node");
        }

        String json = JSON.toJSONString(definition);
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + definition.getFlowKey());
        bucket.set(json);
        log.info("Flow definition deployed: {}", definition.getFlowKey());
    }

    /**
     * Retrieve a flow definition by its key.
     */
    public Optional<FlowDefinition> getByKey(String flowKey) {
        RBucket<String> bucket = redissonClient.getBucket(KEY_PREFIX + flowKey);
        String json = bucket.get();
        if (json == null) {
            return Optional.empty();
        }
        return Optional.of(JSON.parseObject(json, FlowDefinition.class));
    }

    /**
     * Get definition or throw if not found.
     */
    public FlowDefinition requireByKey(String flowKey) {
        return getByKey(flowKey).orElseThrow(() ->
                new BizException("FLOW_DEF_NOT_FOUND", "Flow definition not found: " + flowKey));
    }
}
```

### 2.8 Flow Engine

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/engine/FlowEngine.java`

```java
package vip.mate.starter.flow.engine;

import vip.mate.starter.flow.model.FlowTask;

import java.util.List;
import java.util.Map;

/**
 * Core flow engine API for starting and interacting with workflow instances.
 */
public interface FlowEngine {

    /**
     * Start a new flow instance.
     *
     * @param flowKey      definition key
     * @param businessId   associated business entity ID
     * @param formData     key-value form data (will be serialized to JSON)
     * @return the new instance ID
     */
    String start(String flowKey, String businessId, Map<String, Object> formData);

    /**
     * Approve a pending task.
     *
     * @param taskId  the task to approve
     * @param comment approver's comment
     */
    void approve(String taskId, String comment);

    /**
     * Reject a pending task, which terminates the flow.
     *
     * @param taskId  the task to reject
     * @param comment rejection reason
     */
    void reject(String taskId, String comment);

    /**
     * Transfer a task to another user.
     *
     * @param taskId      the task to transfer
     * @param targetUserId the new assignee
     */
    void transfer(String taskId, String targetUserId);

    /**
     * Get all pending tasks for a given user.
     */
    List<FlowTask> myTodoList(String userId);

    /**
     * Get the full task timeline for a flow instance (audit trail).
     */
    List<FlowTask> getTimeline(String instanceId);
}
```

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/engine/FlowEngineImpl.java`

```java
package vip.mate.starter.flow.engine;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vip.mate.base.exception.BizException;
import vip.mate.base.sequence.SequenceGenerator;
import vip.mate.starter.flow.model.*;
import vip.mate.starter.flow.repository.FlowInstanceRepository;
import vip.mate.starter.flow.repository.FlowTaskRepository;
import vip.mate.starter.flow.service.FlowDefinitionService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlowEngineImpl implements FlowEngine {

    private final FlowDefinitionService flowDefinitionService;
    private final FlowInstanceRepository flowInstanceRepository;
    private final FlowTaskRepository flowTaskRepository;
    private final SequenceGenerator sequenceGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String start(String flowKey, String businessId, Map<String, Object> formData) {
        FlowDefinition definition = flowDefinitionService.requireByKey(flowKey);

        String instanceId = sequenceGenerator.next("FLW");

        // Find START node, then advance to the first APPROVAL node
        FlowNode startNode = definition.getNodes().stream()
                .filter(n -> n.getType() == NodeType.START)
                .findFirst()
                .orElseThrow(() -> new BizException("FLOW_NO_START", "No START node in flow: " + flowKey));

        FlowNode firstApproval = definition.findNextNode(startNode.getNodeId())
                .orElseThrow(() -> new BizException("FLOW_NO_NEXT", "No node after START in flow: " + flowKey));

        FlowInstance instance = FlowInstance.builder()
                .instanceId(instanceId)
                .flowKey(flowKey)
                .businessId(businessId)
                .businessType(flowKey)
                .initiatorId(resolveCurrentUserId())
                .status(FlowStatus.RUNNING)
                .currentNodeId(firstApproval.getNodeId())
                .formData(JSON.toJSONString(formData))
                .createTime(new Date())
                .build();
        flowInstanceRepository.save(instance);

        // Create the first task
        createTasksForNode(instanceId, firstApproval);

        log.info("Flow started: instanceId={}, flowKey={}, businessId={}", instanceId, flowKey, businessId);
        return instanceId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(String taskId, String comment) {
        FlowTask task = requirePendingTask(taskId);
        task.setStatus(TaskStatus.APPROVED);
        task.setComment(comment);
        task.setCompleteTime(new Date());
        flowTaskRepository.update(task);

        // Advance the flow to the next node
        FlowInstance instance = flowInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new BizException("FLOW_INSTANCE_NOT_FOUND", "Instance not found"));
        FlowDefinition definition = flowDefinitionService.requireByKey(instance.getFlowKey());

        Optional<FlowNode> nextOpt = definition.findNextNode(task.getNodeId());
        if (nextOpt.isEmpty() || nextOpt.get().getType() == NodeType.END) {
            // Flow completed
            instance.setStatus(FlowStatus.APPROVED);
            instance.setCurrentNodeId(nextOpt.map(FlowNode::getNodeId).orElse(null));
            instance.setUpdateTime(new Date());
            flowInstanceRepository.update(instance);
            log.info("Flow approved: instanceId={}", instance.getInstanceId());
        } else {
            FlowNode nextNode = nextOpt.get();
            instance.setCurrentNodeId(nextNode.getNodeId());
            instance.setUpdateTime(new Date());
            flowInstanceRepository.update(instance);
            createTasksForNode(instance.getInstanceId(), nextNode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(String taskId, String comment) {
        FlowTask task = requirePendingTask(taskId);
        task.setStatus(TaskStatus.REJECTED);
        task.setComment(comment);
        task.setCompleteTime(new Date());
        flowTaskRepository.update(task);

        FlowInstance instance = flowInstanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new BizException("FLOW_INSTANCE_NOT_FOUND", "Instance not found"));
        instance.setStatus(FlowStatus.REJECTED);
        instance.setUpdateTime(new Date());
        flowInstanceRepository.update(instance);

        log.info("Flow rejected: instanceId={}, taskId={}", instance.getInstanceId(), taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(String taskId, String targetUserId) {
        FlowTask task = requirePendingTask(taskId);
        task.setStatus(TaskStatus.TRANSFERRED);
        task.setComment("Transferred to " + targetUserId);
        task.setCompleteTime(new Date());
        flowTaskRepository.update(task);

        // Create a new PENDING task for the target user
        FlowTask newTask = FlowTask.builder()
                .taskId(sequenceGenerator.next("FTK"))
                .instanceId(task.getInstanceId())
                .nodeId(task.getNodeId())
                .assigneeId(targetUserId)
                .status(TaskStatus.PENDING)
                .createTime(new Date())
                .build();
        flowTaskRepository.save(newTask);

        log.info("Task transferred: taskId={} -> userId={}", taskId, targetUserId);
    }

    @Override
    public List<FlowTask> myTodoList(String userId) {
        return flowTaskRepository.findPendingByAssignee(userId);
    }

    @Override
    public List<FlowTask> getTimeline(String instanceId) {
        return flowTaskRepository.findByInstanceId(instanceId);
    }

    // ---------- private helpers ----------

    private FlowTask requirePendingTask(String taskId) {
        FlowTask task = flowTaskRepository.findById(taskId)
                .orElseThrow(() -> new BizException("FLOW_TASK_NOT_FOUND", "Task not found: " + taskId));
        if (task.getStatus() != TaskStatus.PENDING) {
            throw new BizException("FLOW_TASK_NOT_PENDING", "Task is not in PENDING status: " + taskId);
        }
        return task;
    }

    private void createTasksForNode(String instanceId, FlowNode node) {
        if (node.getType() != NodeType.APPROVAL) {
            return;
        }
        ApproverRule rule = node.getApproverRule();
        if (rule == null) {
            throw new BizException("FLOW_NO_RULE", "No approver rule for node: " + node.getNodeId());
        }

        List<String> assignees = resolveAssignees(rule);
        for (String assignee : assignees) {
            FlowTask task = FlowTask.builder()
                    .taskId(sequenceGenerator.next("FTK"))
                    .instanceId(instanceId)
                    .nodeId(node.getNodeId())
                    .assigneeId(assignee)
                    .status(TaskStatus.PENDING)
                    .createTime(new Date())
                    .build();
            flowTaskRepository.save(task);
        }
    }

    /**
     * Resolve assignee user IDs from an approver rule.
     * In a full implementation, ROLE and DEPT_LEADER would query the user service.
     * This version handles FIXED_USER directly and returns placeholder for others.
     */
    private List<String> resolveAssignees(ApproverRule rule) {
        return switch (rule.getType()) {
            case FIXED_USER -> rule.getValues();
            case ROLE -> {
                // TODO: integrate with user/role service to resolve role -> userIds
                log.warn("ROLE-based approver resolution is not yet implemented, using values as userIds");
                yield rule.getValues();
            }
            case DEPT_LEADER, INITIATOR_LEADER -> {
                // TODO: integrate with org service to resolve leader
                log.warn("{} approver resolution is not yet implemented", rule.getType());
                yield rule.getValues().isEmpty() ? List.of("SYSTEM") : rule.getValues();
            }
        };
    }

    private String resolveCurrentUserId() {
        // TODO: integrate with security context to get the current user
        return "SYSTEM";
    }
}
```

### 2.9 Auto-Configuration

**File**: `mate-starters/mate-flow-starter/src/main/java/vip/mate/starter/flow/config/FlowAutoConfiguration.java`

```java
package vip.mate.starter.flow.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the lightweight workflow engine.
 * Scans all flow-related beans and mappers.
 */
@AutoConfiguration
@ComponentScan("vip.mate.starter.flow")
@MapperScan("vip.mate.starter.flow.infrastructure.dao")
public class FlowAutoConfiguration {
}
```

### 2.10 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-flow-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.flow.config.FlowAutoConfiguration
```

### 2.11 SQL Schema

```sql
-- ============================================================
-- mate-flow-starter: Lightweight Workflow Tables
-- ============================================================

CREATE TABLE `mate_flow_definition` (
    `id`          VARCHAR(32)  NOT NULL COMMENT 'Primary key',
    `flow_key`    VARCHAR(64)  NOT NULL COMMENT 'Unique flow key, e.g. leave_approval',
    `name`        VARCHAR(128) NOT NULL COMMENT 'Flow name',
    `nodes_json`  TEXT         NOT NULL COMMENT 'JSON array of FlowNode',
    `edges_json`  TEXT         NOT NULL COMMENT 'JSON array of FlowEdge',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT 'Definition version',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / DISABLED',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `lock_version` INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_flow_key_version` (`flow_key`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flow definition';

CREATE TABLE `mate_flow_instance` (
    `id`              VARCHAR(32)  NOT NULL COMMENT 'Instance ID (sequence: FLW...)',
    `flow_key`        VARCHAR(64)  NOT NULL COMMENT 'Reference to flow definition',
    `biz_id`          VARCHAR(64)  NOT NULL COMMENT 'Associated business entity ID',
    `biz_type`        VARCHAR(32)  NOT NULL COMMENT 'Business type: leave / expense / purchase',
    `initiator_id`    VARCHAR(32)  NOT NULL COMMENT 'User who initiated the flow',
    `status`          VARCHAR(16)  NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING / APPROVED / REJECTED / CANCELLED',
    `current_node_id` VARCHAR(64)  NULL     COMMENT 'Current node awaiting action',
    `form_data`       TEXT         NULL     COMMENT 'JSON snapshot of submitted form',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `lock_version`    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_flow_key`    (`flow_key`),
    KEY `idx_biz`         (`biz_type`, `biz_id`),
    KEY `idx_initiator`   (`initiator_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flow instance';

CREATE TABLE `mate_flow_task` (
    `id`           VARCHAR(32)  NOT NULL COMMENT 'Task ID (sequence: FTK...)',
    `instance_id`  VARCHAR(32)  NOT NULL COMMENT 'FK -> mate_flow_instance.id',
    `node_id`      VARCHAR(64)  NOT NULL COMMENT 'Flow node ID',
    `assignee_id`  VARCHAR(32)  NOT NULL COMMENT 'Assigned approver user ID',
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED / TRANSFERRED',
    `comment`      VARCHAR(512) NULL     COMMENT 'Approver comment',
    `completed_at` DATETIME     NULL     COMMENT 'When the task was completed',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `lock_version` INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_instance`   (`instance_id`),
    KEY `idx_assignee`   (`assignee_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Flow approval task';
```

### 2.12 Usage Example

```java
// 1. Define and deploy a flow
FlowDefinition flow = FlowDefinition.builder()
    .flowKey("leave_approval")
    .name("Leave Approval")
    .nodes(List.of(
        FlowNode.start("start"),
        FlowNode.approval("leader", "Direct Leader",
            ApproverRule.fixedUser("user_leader_01")),
        FlowNode.approval("hr", "HR Review",
            ApproverRule.role("hr_manager")),
        FlowNode.end("end")
    ))
    .edges(List.of(
        FlowEdge.of("start", "leader"),
        FlowEdge.of("leader", "hr"),
        FlowEdge.of("hr", "end")
    ))
    .build();
flowDefinitionService.deploy(flow);

// 2. Start a flow instance
String instanceId = flowEngine.start("leave_approval", "leave_001",
    Map.of("days", 3, "reason", "Annual leave"));

// 3. Approve / reject / transfer
flowEngine.approve(taskId, "Approved, enjoy your vacation");
flowEngine.reject(taskId, "Days exceed your remaining balance");
flowEngine.transfer(taskId, "user_002");

// 4. Query my pending tasks
List<FlowTask> todos = flowEngine.myTodoList(userId);

// 5. Audit trail
List<FlowTask> timeline = flowEngine.getTimeline(instanceId);
```

---

## 3. Rule Engine (mate-rule-starter)

### 3.1 pom.xml

**File**: `mate-starters/mate-rule-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-rule-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-rule-starter</name>
    <description>Lightweight rule engine starter based on AviatorScript</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-ds-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.googlecode.aviator</groupId>
            <artifactId>aviator</artifactId>
            <version>5.4.3</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 3.2 Domain Model

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/model/RuleDefinition.java`

```java
package vip.mate.starter.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * A single rule within a rule group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition implements Serializable {

    /** Unique rule key, e.g. "vip_discount" */
    private String ruleKey;

    /** Rule group for categorization, e.g. "discount_rules" */
    private String ruleGroup;

    /** Human-readable name */
    private String name;

    /** Evaluation priority (lower = higher priority) */
    private int priority;

    /** Aviator condition expression, e.g. "order.amount > 1000 && user.level == 'VIP'" */
    private String condition;

    /** Aviator action expression, e.g. "discount = 0.8" */
    private String action;

    /** Whether this rule is active */
    private boolean enabled;
}
```

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/model/RuleResult.java`

```java
package vip.mate.starter.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Result of a rule engine evaluation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult implements Serializable {

    /** Whether any rule matched */
    private boolean matched;

    /** The key of the matched rule (null if no match) */
    private String matchedRuleKey;

    /** The facts map after action execution (may contain computed values) */
    private Map<String, Object> facts;

    public static RuleResult matched(String ruleKey, Map<String, Object> facts) {
        return RuleResult.builder()
                .matched(true)
                .matchedRuleKey(ruleKey)
                .facts(facts)
                .build();
    }

    public static RuleResult noMatch() {
        return RuleResult.builder()
                .matched(false)
                .build();
    }
}
```

### 3.3 Infrastructure

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/infrastructure/po/RuleDefinitionPO.java`

```java
package vip.mate.starter.rule.infrastructure.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import vip.mate.starter.ds.model.BasePO;

/**
 * Persistent object for mate_rule_definition table.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("mate_rule_definition")
public class RuleDefinitionPO extends BasePO {

    private String ruleKey;
    private String ruleGroup;
    private String name;
    private Integer priority;
    /** Aviator condition expression */
    private String conditionExpr;
    /** Aviator action expression */
    private String actionExpr;
    private Boolean enabled;
}
```

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/infrastructure/dao/RuleDao.java`

```java
package vip.mate.starter.rule.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import vip.mate.starter.rule.infrastructure.po.RuleDefinitionPO;

/**
 * MyBatis Plus mapper for mate_rule_definition.
 */
@Mapper
public interface RuleDao extends BaseMapper<RuleDefinitionPO> {
}
```

### 3.4 Repository

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/repository/RuleRepository.java`

```java
package vip.mate.starter.rule.repository;

import vip.mate.starter.rule.model.RuleDefinition;

import java.util.List;

/**
 * Repository for rule definitions.
 */
public interface RuleRepository {

    /**
     * Find all rules in a group, ordered by priority ascending.
     */
    List<RuleDefinition> findByGroup(String ruleGroup);

    void save(RuleDefinition rule);

    void update(RuleDefinition rule);
}
```

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/infrastructure/repository/RuleRepositoryImpl.java`

```java
package vip.mate.starter.rule.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import vip.mate.starter.rule.infrastructure.dao.RuleDao;
import vip.mate.starter.rule.infrastructure.po.RuleDefinitionPO;
import vip.mate.starter.rule.model.RuleDefinition;
import vip.mate.starter.rule.repository.RuleRepository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RuleRepositoryImpl implements RuleRepository {

    private final RuleDao ruleDao;

    @Override
    public List<RuleDefinition> findByGroup(String ruleGroup) {
        LambdaQueryWrapper<RuleDefinitionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleDefinitionPO::getRuleGroup, ruleGroup)
               .orderByAsc(RuleDefinitionPO::getPriority);
        return ruleDao.selectList(wrapper).stream().map(this::toDomain).toList();
    }

    @Override
    public void save(RuleDefinition rule) {
        ruleDao.insert(toPO(rule));
    }

    @Override
    public void update(RuleDefinition rule) {
        ruleDao.updateById(toPO(rule));
    }

    private RuleDefinitionPO toPO(RuleDefinition d) {
        return RuleDefinitionPO.builder()
                .ruleKey(d.getRuleKey())
                .ruleGroup(d.getRuleGroup())
                .name(d.getName())
                .priority(d.getPriority())
                .conditionExpr(d.getCondition())
                .actionExpr(d.getAction())
                .enabled(d.isEnabled())
                .build();
    }

    private RuleDefinition toDomain(RuleDefinitionPO po) {
        return RuleDefinition.builder()
                .ruleKey(po.getRuleKey())
                .ruleGroup(po.getRuleGroup())
                .name(po.getName())
                .priority(po.getPriority())
                .condition(po.getConditionExpr())
                .action(po.getActionExpr())
                .enabled(po.getEnabled())
                .build();
    }
}
```

### 3.5 Rule Engine

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/engine/RuleEngine.java`

```java
package vip.mate.starter.rule.engine;

import vip.mate.starter.rule.model.RuleResult;

import java.util.Map;

/**
 * Evaluates business rules against a set of facts.
 */
public interface RuleEngine {

    /**
     * Evaluate rules in the given group against the facts.
     * Returns on first match (ordered by priority).
     *
     * @param ruleGroup the rule group key
     * @param facts     input data map
     * @return evaluation result
     */
    RuleResult evaluate(String ruleGroup, Map<String, Object> facts);

    /**
     * Evaluate all matching rules (not just the first).
     *
     * @param ruleGroup the rule group key
     * @param facts     input data map
     * @return list of all matched results
     */
    java.util.List<RuleResult> evaluateAll(String ruleGroup, Map<String, Object> facts);
}
```

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/engine/RuleEngineImpl.java`

```java
package vip.mate.starter.rule.engine;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.starter.rule.model.RuleDefinition;
import vip.mate.starter.rule.model.RuleResult;
import vip.mate.starter.rule.repository.RuleRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuleEngineImpl implements RuleEngine {

    private final RuleRepository ruleRepository;

    /** Shared evaluator instance with caching enabled */
    private final AviatorEvaluatorInstance aviator = AviatorEvaluator.getInstance();

    @Override
    public RuleResult evaluate(String ruleGroup, Map<String, Object> facts) {
        List<RuleDefinition> rules = ruleRepository.findByGroup(ruleGroup);
        // Use a mutable copy so that action expressions can write to it
        Map<String, Object> env = new HashMap<>(facts);

        for (RuleDefinition rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            try {
                Object condResult = aviator.execute(rule.getCondition(), env);
                if (Boolean.TRUE.equals(condResult)) {
                    // Execute the action expression
                    aviator.execute(rule.getAction(), env);
                    log.debug("Rule matched: group={}, ruleKey={}", ruleGroup, rule.getRuleKey());
                    return RuleResult.matched(rule.getRuleKey(), env);
                }
            } catch (Exception e) {
                log.warn("Rule evaluation error: ruleKey={}, error={}", rule.getRuleKey(), e.getMessage());
            }
        }
        return RuleResult.noMatch();
    }

    @Override
    public List<RuleResult> evaluateAll(String ruleGroup, Map<String, Object> facts) {
        List<RuleDefinition> rules = ruleRepository.findByGroup(ruleGroup);
        Map<String, Object> env = new HashMap<>(facts);
        List<RuleResult> results = new ArrayList<>();

        for (RuleDefinition rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            try {
                Object condResult = aviator.execute(rule.getCondition(), env);
                if (Boolean.TRUE.equals(condResult)) {
                    aviator.execute(rule.getAction(), env);
                    results.add(RuleResult.matched(rule.getRuleKey(), new HashMap<>(env)));
                }
            } catch (Exception e) {
                log.warn("Rule evaluation error: ruleKey={}, error={}", rule.getRuleKey(), e.getMessage());
            }
        }
        return results;
    }
}
```

### 3.6 Auto-Configuration

**File**: `mate-starters/mate-rule-starter/src/main/java/vip/mate/starter/rule/config/RuleAutoConfiguration.java`

```java
package vip.mate.starter.rule.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for the lightweight rule engine.
 */
@AutoConfiguration
@ComponentScan("vip.mate.starter.rule")
@MapperScan("vip.mate.starter.rule.infrastructure.dao")
public class RuleAutoConfiguration {
}
```

### 3.7 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-rule-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.rule.config.RuleAutoConfiguration
```

### 3.8 SQL Schema

```sql
-- ============================================================
-- mate-rule-starter: Rule Engine Table
-- ============================================================

CREATE TABLE `mate_rule_definition` (
    `id`             VARCHAR(32)  NOT NULL COMMENT 'Primary key',
    `rule_key`       VARCHAR(64)  NOT NULL COMMENT 'Unique rule key',
    `rule_group`     VARCHAR(64)  NOT NULL COMMENT 'Rule group, e.g. discount_rules',
    `name`           VARCHAR(128) NOT NULL COMMENT 'Rule name',
    `priority`       INT          NOT NULL DEFAULT 0 COMMENT 'Priority (lower = higher)',
    `condition_expr` TEXT         NOT NULL COMMENT 'Aviator condition expression',
    `action_expr`    TEXT         NOT NULL COMMENT 'Aviator action expression',
    `enabled`        TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`        TINYINT      NOT NULL DEFAULT 0,
    `lock_version`   INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_key` (`rule_key`),
    KEY `idx_rule_group` (`rule_group`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Rule definition';
```

### 3.9 Usage Example

```java
// Insert rules (typically via admin UI or migration)
// rule_group = "discount_rules"
// rule 1: ruleKey="vip_discount", condition="order.amount > 1000 && user.level == 'VIP'", action="discount = 0.8"
// rule 2: ruleKey="new_user",     condition="user.registerDays < 30",                     action="discount = 0.9"

// Evaluate
Map<String, Object> facts = Map.of(
    "order", Map.of("amount", 1500, "category", "electronics"),
    "user", Map.of("level", "VIP", "registerDays", 365)
);
RuleResult result = ruleEngine.evaluate("discount_rules", facts);
// result.isMatched() == true
// result.getMatchedRuleKey() == "vip_discount"
// result.getFacts().get("discount") == 0.8
```

---

## 4. Excel Import/Export (mate-excel-starter)

### 4.1 pom.xml

**File**: `mate-starters/mate-excel-starter/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
        <relativePath>../pom.xml</relativePath>
    </parent>

    <artifactId>mate-excel-starter</artifactId>
    <packaging>jar</packaging>
    <name>mate-excel-starter</name>
    <description>Annotation-driven Excel import/export starter based on EasyExcel</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-web-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>4.0.3</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 4.2 Annotations

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/annotation/ExcelExport.java`

```java
package vip.mate.starter.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a controller method to export its return value as an XLSX file.
 * The method should return a {@code List<?>} of data objects annotated with EasyExcel's @ExcelProperty.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelExport {

    /** Download file name (without extension, .xlsx is appended automatically) */
    String fileName();

    /** The data class with @ExcelProperty annotations */
    Class<?> dataClass();

    /** Sheet name */
    String sheetName() default "Sheet1";
}
```

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/annotation/ExcelImport.java`

```java
package vip.mate.starter.excel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a controller method parameter to auto-parse an uploaded Excel file
 * into a {@code List<?>} of data objects.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelImport {

    /** The data class with @ExcelProperty annotations */
    Class<?> dataClass();

    /** Number of header rows to skip */
    int headRowNumber() default 1;
}
```

### 4.3 Import Result

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/model/ImportResult.java`

```java
package vip.mate.starter.excel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary of an Excel import operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult implements Serializable {

    private int total;
    private int success;
    private int fail;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
        this.fail++;
    }

    public void incrementSuccess() {
        this.success++;
    }
}
```

### 4.4 Excel Export - ResponseBodyAdvice

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/advice/ExcelResponseBodyAdvice.java`

```java
package vip.mate.starter.excel.advice;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import vip.mate.starter.excel.annotation.ExcelExport;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Intercepts controller responses annotated with @ExcelExport and writes
 * the return value as an XLSX stream to the HTTP response.
 */
@Slf4j
@ControllerAdvice
public class ExcelResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.hasMethodAnnotation(ExcelExport.class);
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        ExcelExport annotation = returnType.getMethodAnnotation(ExcelExport.class);
        if (annotation == null || !(body instanceof List<?> dataList)) {
            return body;
        }

        HttpServletResponse servletResponse =
                ((ServletServerHttpResponse) response).getServletResponse();

        String encodedFileName = URLEncoder.encode(annotation.fileName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        servletResponse.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        servletResponse.setCharacterEncoding("UTF-8");
        servletResponse.setHeader("Content-Disposition",
                "attachment;filename=" + encodedFileName + ".xlsx");
        servletResponse.setHeader("Access-Control-Expose-Headers", "Content-Disposition");

        try {
            EasyExcel.write(servletResponse.getOutputStream(), annotation.dataClass())
                    .sheet(annotation.sheetName())
                    .doWrite(dataList);
        } catch (IOException e) {
            log.error("Excel export failed: {}", e.getMessage(), e);
            throw new RuntimeException("Excel export failed", e);
        }

        // Return null to prevent further processing by message converters
        return null;
    }
}
```

### 4.5 Excel Import - Argument Resolver

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/resolver/ExcelImportResolver.java`

```java
package vip.mate.starter.excel.resolver;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;
import vip.mate.base.exception.BizException;
import vip.mate.starter.excel.annotation.ExcelImport;

import java.io.InputStream;
import java.util.List;

/**
 * Resolves controller method parameters annotated with @ExcelImport.
 * Reads the first uploaded file and parses it into a List of the declared data class.
 */
@Slf4j
public class ExcelImportResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(ExcelImport.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        ExcelImport annotation = parameter.getParameterAnnotation(ExcelImport.class);
        if (annotation == null) {
            throw new BizException("EXCEL_IMPORT_ERROR", "Missing @ExcelImport annotation");
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new BizException("EXCEL_IMPORT_ERROR", "Not an HTTP request");
        }

        MultipartFile file = resolveMultipartFile(request);
        if (file == null || file.isEmpty()) {
            throw new BizException("EXCEL_IMPORT_ERROR", "No file uploaded");
        }

        try (InputStream is = file.getInputStream()) {
            List<?> data = EasyExcel.read(is)
                    .head(annotation.dataClass())
                    .headRowNumber(annotation.headRowNumber())
                    .sheet()
                    .doReadSync();
            log.debug("Excel import: read {} rows from {}", data.size(), file.getOriginalFilename());
            return data;
        }
    }

    private MultipartFile resolveMultipartFile(HttpServletRequest request) {
        if (request instanceof MultipartRequest multipartRequest) {
            var fileMap = multipartRequest.getMultiFileMap();
            if (!fileMap.isEmpty()) {
                List<MultipartFile> files = fileMap.values().iterator().next();
                if (!files.isEmpty()) {
                    return files.getFirst();
                }
            }
        }
        return null;
    }
}
```

### 4.6 Auto-Configuration

**File**: `mate-starters/mate-excel-starter/src/main/java/vip/mate/starter/excel/config/ExcelAutoConfiguration.java`

```java
package vip.mate.starter.excel.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import vip.mate.starter.excel.advice.ExcelResponseBodyAdvice;
import vip.mate.starter.excel.resolver.ExcelImportResolver;

import java.util.List;

/**
 * Auto-configuration for annotation-driven Excel import/export.
 * Registers:
 * - ExcelResponseBodyAdvice: intercepts @ExcelExport annotated methods
 * - ExcelImportResolver: resolves @ExcelImport annotated parameters
 */
@AutoConfiguration
@ConditionalOnClass(name = "com.alibaba.excel.EasyExcel")
public class ExcelAutoConfiguration implements WebMvcConfigurer {

    @Bean
    @ConditionalOnMissingBean
    public ExcelResponseBodyAdvice excelResponseBodyAdvice() {
        return new ExcelResponseBodyAdvice();
    }

    @Bean
    @ConditionalOnMissingBean
    public ExcelImportResolver excelImportResolver() {
        return new ExcelImportResolver();
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(excelImportResolver());
    }
}
```

### 4.7 Spring Boot Auto-Configuration Registration

**File**: `mate-starters/mate-excel-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```
vip.mate.starter.excel.config.ExcelAutoConfiguration
```

### 4.8 Usage Example

```java
// --- Data class ---
@Data
public class UserExcelVO {
    @ExcelProperty("User ID")
    private String id;

    @ExcelProperty("Mobile")
    private String mobile;

    @ExcelProperty("Nickname")
    private String nickName;

    @ExcelProperty(value = "Status", converter = UserStatusConverter.class)
    private Integer status;

    @ExcelProperty("Created At")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}

// --- Export ---
@ExcelExport(fileName = "User List", dataClass = UserExcelVO.class)
@GetMapping("/export")
public List<UserExcelVO> exportUsers(UserPageQueryReq req) {
    return userQueryService.listForExport(req);
}

// --- Import ---
@PostMapping("/import")
public Result<ImportResult> importUsers(
        @ExcelImport(dataClass = UserExcelVO.class) List<UserExcelVO> dataList) {
    return Result.ok(userCommandService.batchImport(dataList));
}
```

---

## New Starter/Module Summary

| Name | Type | Core Capability |
|------|------|-----------------|
| mate-flow-starter | Starter | Lightweight workflow (define/start/approve/transfer/query) |
| mate-rule-starter | Starter | Aviator-based rule engine |
| mate-excel-starter | Starter | @ExcelExport/@ExcelImport annotation-driven import/export |
| SequenceGenerator | mate-base interface | Unified business sequence number generation |
| RedisSequenceGenerator | mate-cache-starter | Redis INCR implementation (PREFIX + yyyyMMdd + 0001) |

## Verification Plan

1. **Workflow**: Deploy leave flow -> Start instance -> Leader approve -> HR approve -> Status = APPROVED
2. **Rule Engine**: Insert discount rules -> Pass order facts -> Get discount = 0.8
3. **Excel Export**: GET /export -> Browser downloads .xlsx with data
4. **Excel Import**: POST /import with .xlsx -> Returns ImportResult with total/success/fail
5. **Sequence**: Call `sequenceGenerator.next("ORD")` -> Returns `ORD202604120001`, next call `ORD202604120002`
