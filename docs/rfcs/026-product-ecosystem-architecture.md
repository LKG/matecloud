# RFC-026: Product Ecosystem Architecture — Platform + Apps Model

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team

> "We made the iTunes Music Store and the App Store, which completely changed
> the music industry and the app industry. You have to be willing to cannibalize
> yourself." — Steve Jobs

## 核心理念

MateCloud 不是一个系统，是一个**产品平台**。

```
┌──────────────────────────────────────────────────┐
│                MateCloud Platform                 │ ← 你拥有 (开源/免费)
│  (脚手架 + 通用服务 + Starter 生态 + AI 工具链)   │
└──────────────┬───────────────────────┬────────────┘
               │                       │
    ┌──────────┴───────┐    ┌─────────┴────────┐
    │  mate-apps/      │    │   第三方开发者     │
    │  (你的官方 App)   │    │   (社区/ISV)      │
    │                  │    │                   │
    │  mate-crm        │    │  xxx-logistics    │
    │  mate-mall       │    │  xxx-hospital     │
    │  mate-video      │    │  xxx-finance      │
    │  mate-edu        │    │  xxx-anything     │
    │  (独立售卖)      │    │  (独立售卖)       │
    └──────────────────┘    └───────────────────┘
```

**一句话定义**: MateCloud = 微服务的 App Store。平台免费，App 独立收费。

---

## Part 1: 代码仓库组织 — Multi-Repo, Not Mono-Repo

**关键决策**: 每个子系统是**独立 Git 仓库**，不是 matecloud 里的子目录。

```
GitHub / GitLab 组织结构:
├── matecloud/matecloud          # 平台内核 (当前仓库, 开源)
├── matecloud/mate-crm           # CRM 子系统 (独立仓库, 商业)
├── matecloud/mate-mall          # 商城子系统 (独立仓库, 商业)
├── matecloud/mate-video         # 短视频子系统 (独立仓库, 商业)
├── matecloud/mate-edu           # 教学子系统 (独立仓库, 商业)
├── matecloud/mate-admin-ui      # 管理后台前端 (独立仓库, 开源)
└── matecloud/mate-app-template  # 子系统脚手架模板 (开源)
```

**为什么不放在一个仓库里?**

1. **独立售卖** — 不同客户买不同 App，不能让买 CRM 的人看到商城源码
2. **独立版本** — CRM v2.1 和 Mall v1.3 各自迭代，互不阻塞
3. **独立团队** — 一个 agent/团队负责一个 App，权限隔离
4. **独立部署** — 客户可以只部署 Platform + CRM，不装其他
5. **独立授权** — 平台开源 Apache 2.0，App 商业授权

---

## Part 2: 子系统如何引用平台

每个子系统通过 **Maven 依赖** 引用 matecloud 平台，就像 App 引用 iOS SDK。

### 子系统 pom.xml 模板

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <!-- 不继承 matecloud parent! 独立 parent -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
    </parent>

    <groupId>vip.mate.app</groupId>
    <artifactId>mate-crm</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <properties>
        <matecloud.version>1.0.0</matecloud.version>
    </properties>

    <!-- 引入 MateCloud BOM -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>vip.mate</groupId>
                <artifactId>matecloud-bom</artifactId>
                <version>${matecloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <modules>
        <module>mate-crm-service</module>   <!-- 核心业务 -->
        <module>mate-crm-api</module>       <!-- RPC 契约 -->
    </modules>
</project>
```

### 子系统业务模块 pom.xml

```xml
<dependencies>
    <!-- Platform Starters: 按需引入 -->
    <dependency><groupId>vip.mate</groupId><artifactId>mate-base</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-api</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-ds-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-web-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-nacos-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-rpc-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-cache-starter</artifactId></dependency>
    <dependency><groupId>vip.mate</groupId><artifactId>mate-security-starter</artifactId></dependency>

    <!-- 引用 Platform 的 RPC 接口 -->
    <dependency><groupId>vip.mate</groupId><artifactId>mate-api</artifactId></dependency>
</dependencies>
```

**这意味着**: 平台需要发布到 **Maven 私服** (Nexus/Artifactory)，子系统通过 GAV 坐标拉取。

---

## Part 3: 新增 matecloud-bom 模块

在平台仓库新增一个 BOM 模块，子系统只需 import 一个 BOM 就拿到所有版本：

```
matecloud/
├── matecloud-bom/          ← 新增: Bill of Materials
│   └── pom.xml             # 只有 dependencyManagement, 无代码
├── mate-common/
├── mate-starters/
└── ...
```

```xml
<!-- matecloud-bom/pom.xml -->
<project>
    <groupId>vip.mate</groupId>
    <artifactId>matecloud-bom</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <!-- 所有 mate-common -->
            <dependency><groupId>vip.mate</groupId><artifactId>mate-base</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>vip.mate</groupId><artifactId>mate-api</artifactId><version>${project.version}</version></dependency>
            <!-- 所有 mate-starters -->
            <dependency><groupId>vip.mate</groupId><artifactId>mate-ds-starter</artifactId><version>${project.version}</version></dependency>
            <dependency><groupId>vip.mate</groupId><artifactId>mate-web-starter</artifactId><version>${project.version}</version></dependency>
            <!-- ... 全部 20 个 starter ... -->
        </dependencies>
    </dependencyManagement>
</project>
```

---

## Part 4: 子系统与平台的交互方式

### 4.1 RPC 调用平台服务

子系统调用平台的用户、权限、字典等能力：

```java
// mate-crm-service 中
@DubboReference(version = RpcConstants.DUBBO_VERSION)
private IRpcUserService rpcUserService;      // 来自 mate-api

@DubboReference(version = RpcConstants.DUBBO_VERSION)
private IRpcDictService rpcDictService;      // 来自 mate-api

@DubboReference(version = RpcConstants.DUBBO_VERSION)
private IRpcPermissionService rpcPermissionService;
```

### 4.2 子系统暴露自己的 RPC

CRM 的 API 契约放在 `mate-crm-api`，其他子系统可以引用：

```java
// mate-crm-api (独立 jar)
package vip.mate.app.crm.api;

public interface IRpcCustomerService {
    Result<CustomerDTO> getById(String customerId);
    Result<List<CustomerDTO>> listByOwner(String ownerId);
}
```

```java
// mate-mall-service 中引用 CRM
@DubboReference(version = "1.0.0")
private IRpcCustomerService customerService;  // 来自 mate-crm-api
```

### 4.3 事件驱动跨系统

```java
// CRM 发布事件
eventPublisher.publish("crm.customer.created", new CustomerCreatedEvent(customerId));

// Mall 监听事件
@DomainEventHandler(topic = "crm.customer.created")
public void onCustomerCreated(CustomerCreatedEvent event) {
    // 自动为新客户创建商城账号
}
```

### 4.4 Gateway 统一路由

所有子系统注册到同一个 Nacos，Gateway 统一路由：

```yaml
# Nacos: mate-gateway-dev.yml
spring:
  cloud:
    gateway:
      routes:
        # Platform 服务
        - id: mate-auth
          uri: lb://mate-auth
          predicates: [Path=/api/v1/auth/**]
        - id: mate-system
          uri: lb://mate-system
          predicates: [Path=/api/v1/users/**, /api/v1/system/**]
        - id: mate-admin
          uri: lb://mate-admin
          predicates: [Path=/api/v1/admin/**]

        # App 服务 (按需添加)
        - id: mate-crm
          uri: lb://mate-crm
          predicates: [Path=/api/v1/crm/**]
        - id: mate-mall
          uri: lb://mate-mall
          predicates: [Path=/api/v1/mall/**]
        - id: mate-video
          uri: lb://mate-video
          predicates: [Path=/api/v1/video/**]
        - id: mate-edu
          uri: lb://mate-edu
          predicates: [Path=/api/v1/edu/**]
```

---

## Part 5: 子系统脚手架模板 (mate-app-template)

一条命令创建新子系统：

```bash
# 用 mate-cli 创建
java -jar mate-cli.jar new app mate-crm --port 9060 --package vip.mate.app.crm

# 生成:
mate-crm/
├── pom.xml                           # 引用 matecloud-bom
├── mate-crm-api/                     # RPC 契约 jar
│   ├── pom.xml
│   └── src/.../crm/api/
│       ├── IRpcCustomerService.java
│       ├── command/
│       └── response/
├── mate-crm-service/                 # 业务服务
│   ├── pom.xml
│   ├── src/.../crm/
│   │   ├── MateApp.java              # @SpringBootApplication
│   │   ├── trigger/controller/
│   │   ├── application/command/
│   │   ├── application/query/
│   │   ├── domain/model/aggregate/
│   │   ├── domain/service/
│   │   ├── infrastructure/dao/
│   │   └── types/exception/
│   └── src/main/resources/
│       ├── bootstrap.yml
│       └── db/schema.sql
├── docs/
│   └── README.md
├── .gitignore
└── Dockerfile
```

---

## Part 6: 四个子系统的领域划分

### mate-crm (客户关系管理)

```
核心聚合根:
├── CustomerAggregate     # 客户 (线索→商机→客户 生命周期)
├── ContactAggregate      # 联系人
├── OpportunityAggregate  # 商机 (状态机: 初始→跟进→报价→谈判→成交/丢失)
├── ContractAggregate     # 合同
└── ActivityAggregate     # 跟进记录 (拜访/电话/邮件)

Port: 9060
API: /api/v1/crm/**
DB:  mate_crm_customer, mate_crm_contact, mate_crm_opportunity, mate_crm_contract, mate_crm_activity
```

### mate-mall (商城)

```
核心聚合根:
├── ProductAggregate      # 商品 (SPU + SKU)
├── CategoryAggregate     # 分类 (树形)
├── OrderAggregate        # 订单 (状态机: 创建→支付→发货→完成/取消/退款)
├── CartAggregate         # 购物车
├── PaymentAggregate      # 支付单
└── CouponAggregate       # 优惠券

Port: 9070
API: /api/v1/mall/**
DB:  mate_mall_product, mate_mall_sku, mate_mall_order, mate_mall_payment, mate_mall_coupon
```

### mate-video (短视频)

```
核心聚合根:
├── VideoAggregate        # 视频 (上传→转码→审核→发布)
├── ChannelAggregate      # 频道/创作者
├── InteractionAggregate  # 互动 (点赞/评论/收藏/分享)
├── FeedAggregate         # 信息流推荐
└── LiveAggregate         # 直播 (可选)

Port: 9080
API: /api/v1/video/**
DB:  mate_video_video, mate_video_channel, mate_video_interaction, mate_video_feed
Storage: MinIO (视频文件)
```

### mate-edu (教学)

```
核心聚合根:
├── CourseAggregate       # 课程 (章节→课时)
├── EnrollmentAggregate   # 报名/注册
├── LessonAggregate       # 课时 (视频/图文/直播)
├── ExamAggregate         # 考试 (试卷→题目→答题)
├── CertificateAggregate  # 证书
└── ProgressAggregate     # 学习进度

Port: 9090
API: /api/v1/edu/**
DB:  mate_edu_course, mate_edu_lesson, mate_edu_enrollment, mate_edu_exam, mate_edu_progress
```

### mate-oa (办公自动化)

```
核心聚合根:
├── ApprovalAggregate     # 审批流 (请假/报销/出差/采购 — 使用 mate-flow-starter)
├── AttendanceAggregate   # 考勤 (打卡/加班/请假统计)
├── MeetingAggregate      # 会议室预约
├── AnnouncementAggregate # 公告通知
├── DocumentAggregate     # 企业文档 (在线协作/版本管理)
└── ScheduleAggregate     # 日程管理

Port: 9100
API: /api/v1/oa/**
DB:  mate_oa_approval, mate_oa_attendance, mate_oa_meeting, mate_oa_announcement, mate_oa_document
特点: 重度依赖 mate-flow-starter (工作流)、mate-mq-starter (通知推送)
```

---

## Part 7: 商业模式

### 定价矩阵

```
                    开源版(免费)      基础版        专业版         企业版
                    ──────────     ────────     ──────────     ──────────
Platform            ✓              ✓            ✓              ✓
(matecloud)

Admin 后台          ✓              ✓            ✓              ✓
(mate-admin)

单个 App            ✗             1个           3个            全部
(CRM/Mall/OA/...)

多租户              ✗              ✗            ✓              ✓
(mate-tenant)

技术支持             社区            邮件          工单+电话       专属顾问

MateClaw AI         ✗              ✗            ✓              ✓
(AI 辅助开发)

价格/年              ¥0            ¥2,999       ¥9,999         ¥29,999
```

### 售卖方式

1. **平台免费 + App 付费** — 就像 iOS 免费但 App 收费
2. **按 App 单独购买** — CRM ¥1999/年, Mall ¥2999/年, 可组合
3. **订阅制** — 不卖永久授权，SaaS 模式年付
4. **增值服务** — 多租户能力、AI 辅助、定制开发

---

## Part 8: 版本兼容管理

### 平台语义化版本

```
matecloud 1.x.y
         │ │ └── patch: bug fix, 不破坏 API
         │ └──── minor: 新功能, 向后兼容
         └────── major: 破坏性变更, 子系统需适配
```

### 兼容矩阵

```
matecloud    mate-crm    mate-mall    mate-video    mate-edu
─────────    ────────    ─────────    ──────────    ────────
1.0.x        1.0.x       1.0.x        1.0.x         1.0.x
1.1.x        1.0.x ✓     1.1.x        1.0.x ✓       1.0.x ✓
2.0.x        2.0.x       2.0.x        2.0.x         1.x ✗
```

### mate-api 的版本策略

平台的 `mate-api` 只定义**平台级** RPC 接口 (User, Dict, Permission, Notice)。

子系统的 API 放在自己的 `mate-xxx-api` 包里，独立版本。跨子系统调用通过各自的 API jar。

---

## Part 9: 开发流程 (用 Claude Code / MateClaw)

### 创建新子系统

```bash
# Step 1: 用 CLI 创建
mate new app mate-crm --port 9060

# Step 2: 定义领域
mate new aggregate Customer --module mate-crm-service
mate new aggregate Opportunity --module mate-crm-service

# Step 3: 启动开发
cd mate-crm && mvn spring-boot:run -pl mate-crm-service

# Step 4: MateClaw AI 辅助
# "根据 CRM 客户管理需求，帮我完善 CustomerAggregate 的业务方法"
```

### 多系统联调

```bash
# docker-compose 一键拉起平台 + 所有 App
docker-compose -f docker-compose.platform.yml -f docker-compose.apps.yml up -d

# 或只起平台 + CRM
docker-compose -f docker-compose.platform.yml -f apps/crm/docker-compose.yml up -d
```

---

## Part 10: 关键设计决策总结

| 决策 | 选择 | 原因 |
|------|------|------|
| 仓库模式 | Multi-Repo | 独立授权、独立版本、独立团队 |
| 依赖方式 | Maven BOM + 私服 | 子系统像用 Spring Boot 一样用 MateCloud |
| 跨系统通信 | Dubbo RPC + RabbitMQ 事件 | 同步查询用 RPC，异步通知用事件 |
| 统一入口 | Gateway 动态路由 | 所有 App 共享一个 Gateway，按 path 路由 |
| 数据库 | 每个 App 独立库 | `mate_crm_*`, `mate_mall_*`, 物理隔离 |
| 表前缀 | `mate_{app}_` | 平台表: `mate_user`, CRM表: `mate_crm_customer` |
| 认证 | 共享 Sa-Token | 一次登录，所有 App 通用 (SSO) |
| 权限 | 平台 RBAC + App 扩展菜单 | 每个 App 注册自己的菜单到 mate_menu |
| AI 能力 | MateClaw 作为通用 AI 层 | 所有 App 通过 MateClaw API 获取 AI |

---

## 需要平台新增的能力

为支撑多 App 生态，平台需要新增:

| 能力 | 说明 | 优先级 |
|------|------|--------|
| **matecloud-bom** | 统一 BOM 模块 | P0 |
| **Maven 私服部署** | 发布 jar 到 Nexus | P0 |
| **mate-cli new app** | 子系统脚手架生成 | P0 |
| **App 菜单注册 API** | 子系统启动时自动注册菜单到 admin | P1 |
| **App 版本管理** | 平台记录已安装的 App 及版本 | P1 |
| **License 校验** | 商业授权验证 (RSA 签名) | P1 |
| **App Marketplace 页面** | 管理后台展示可用/已安装 App | P2 |
