# RFC-028: SaaS Multi-Tenant — Every App is SaaS-Ready

- **Status**: Draft — partial implementation landed 2026-06-03 (see status block)
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Dependencies**: RFC-012 (mate-tenant-starter), RFC-026 (App ecosystem), RFC-027 (AI infra)

> **Implementation status (2026-06-03).** The lifecycle + quota foundations are
> in; billing / app-marketplace / usage-dashboard are still design-only.
>
> | Area | Status | Where |
> |------|--------|-------|
> | 全生命周期：注册→开通→初始化 | ✅ done | `TenantProvisionService` (auto role/admin/menu + `TenantProvisionedEvent`) |
> | 到期处理：标记过期 / 宽限期判定 / 定时扫描 | ✅ done | `TenantLifecycleService` + hourly `TenantLifecycleJob`; `TenantAggregate.markExpiredIfDue/inGracePeriod` |
> | 冻结/到期拦截 (停止服务) | ✅ done (mate-system) | `TenantStatusInterceptor` → `TenantQuotaService.assertActiveTenant` (blocks SUSPENDED & past-grace EXPIRED) |
> | 套餐配额模型 (price/maxApps/aiEnabled/aiQuotaDaily) | ✅ done | migration `V1.3.2`, `TenantPackage` |
> | 用量配额：maxUsers / 功能开关 / 每日 AI 次数 | ✅ done | `TenantQuotaService.assertCanAddUser/assertFeature/recordAndCheckAiCall` |
> | 到期提醒 (7/3/1 天) | ⚠️ partial | `TenantLifecycleService.remindExpiring` logs only — no mate-notice channel yet |
> | 网关层全局配额闸门 (跨服务) | ❌ todo | needs an `IRpcTenantService` + reactive gateway filter; today enforcement is mate-system-local |
> | AI 配额接入 mate-ai/mate-aigc 调用链 | ❌ todo | `recordAndCheckAiCall` exists but isn't yet wrapped around the AI chokepoint |
> | 计费 / 支付 / App 市场 / 用量看板 | ❌ design-only | Part 3–5 below |
>
> Enable + verify: `docs/conventions/multi-tenant-guide.md`.

## 核心理念

多租户不是加一个 `tenant_id` 字段就完了。SaaS 是一套完整的商业基础设施:

```
租户注册 → 开通套餐 → 分配域名 → 数据隔离 → 用量计费 → 到期续费 → 冻结/删除
```

mate-tenant-starter (RFC-012) 解决了**数据隔离**。这个 RFC 解决**剩下的全部**。

---

## Part 1: SaaS 全生命周期

```
┌─ 租户注册 ─→ 套餐选择 ─→ 支付开通 ─→ 初始化 ─┐
│                                              │
│  ┌────────── 正常运营周期 ──────────┐         │
│  │ 使用 App → AI 分析 → 数据隔离   │         │
│  │ 成员管理 → 权限控制 → 用量统计   │←────────┘
│  └──────────────┬──────────────────┘
│                 │
│  ┌──── 到期处理 ────┐
│  │ 提醒续费 (7/3/1天)│
│  │ 宽限期 (7天只读)  │
│  │ 冻结 (停止服务)   │
│  │ 数据保留 (30天)   │
│  │ 彻底删除          │
│  └──────────────────┘
```

---

## Part 2: 平台级 SaaS 管理域 (mate-admin 扩展)

### 新增聚合根

```java
// ========== 租户 ==========
@Data @Builder
public class TenantAggregate {
    private String id;
    private String name;              // 企业名称
    private String domain;            // 二级域名: acme.matecloud.vip
    private String contactName;       // 联系人
    private String contactMobile;     // 联系电话
    private TenantStatus status;      // PENDING/ACTIVE/FROZEN/EXPIRED/DELETED
    private String packageId;         // 套餐ID
    private Date expireTime;          // 到期时间
    private Date createdAt;

    // -- 生命周期方法 --
    public void activate(String packageId, int months) {
        this.packageId = packageId;
        this.status = TenantStatus.ACTIVE;
        this.expireTime = DateUtil.offsetMonth(new Date(), months);
    }

    public void renew(int months) {
        this.expireTime = DateUtil.offsetMonth(
            expireTime.after(new Date()) ? expireTime : new Date(), months);
        this.status = TenantStatus.ACTIVE;
    }

    public void freeze() { this.status = TenantStatus.FROZEN; }
    public void expire() { this.status = TenantStatus.EXPIRED; }

    public boolean isExpired() { return new Date().after(expireTime); }
    public boolean inGracePeriod() {
        return isExpired() && DateUtil.betweenDay(expireTime, new Date(), true) <= 7;
    }
}

// ========== 套餐 ==========
@Data @Builder
public class TenantPackage {
    private String id;
    private String name;              // 基础版/专业版/企业版
    private Integer price;            // 月单价 (分)
    private Integer maxUsers;         // 最大用户数
    private Long maxStorage;          // 最大存储 (MB)
    private Integer maxApps;          // 可安装 App 数
    private List<String> appIds;      // 可用 App 列表
    private List<String> menuIds;     // 可用菜单
    private Boolean aiEnabled;        // 是否开放 AI
    private Integer aiQuotaPerDay;    // 每日 AI 调用次数
    private Boolean enabled;
}

// ========== 租户 App 安装 ==========
@Data @Builder
public class TenantApp {
    private String id;
    private String tenantId;
    private String appId;             // "crm" / "mall" / "oa"
    private String appVersion;
    private TenantAppStatus status;   // INSTALLED / DISABLED / UNINSTALLED
    private Date installedAt;
}

// ========== 用量统计 ==========
@Data @Builder
public class TenantUsage {
    private String tenantId;
    private String date;              // yyyy-MM-dd
    private Integer userCount;        // 当前用户数
    private Long storageUsed;         // 已用存储 (MB)
    private Integer apiCalls;         // API 调用次数
    private Integer aiCalls;          // AI 调用次数
}
```

### SQL

```sql
CREATE TABLE mate_tenant (
    id              VARCHAR(64) NOT NULL,
    name            VARCHAR(128) NOT NULL COMMENT '企业名称',
    domain          VARCHAR(64) COMMENT '二级域名',
    contact_name    VARCHAR(64) COMMENT '联系人',
    contact_mobile  VARCHAR(20) COMMENT '联系电话',
    status          TINYINT DEFAULT 0 COMMENT '0=待激活 1=正常 2=冻结 3=过期 4=删除',
    package_id      VARCHAR(64) COMMENT '套餐ID',
    expire_time     DATETIME COMMENT '到期时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain (domain)
) ENGINE=InnoDB COMMENT='租户';

CREATE TABLE mate_tenant_package (
    id              VARCHAR(64) NOT NULL,
    name            VARCHAR(64) NOT NULL COMMENT '套餐名',
    price           INT DEFAULT 0 COMMENT '月单价(分)',
    max_users       INT DEFAULT 10,
    max_storage     BIGINT DEFAULT 1024 COMMENT 'MB',
    max_apps        INT DEFAULT 1,
    app_ids         JSON COMMENT '可用App列表',
    menu_ids        JSON COMMENT '可用菜单列表',
    ai_enabled      TINYINT DEFAULT 0,
    ai_quota_daily  INT DEFAULT 100,
    enabled         TINYINT DEFAULT 1,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='租户套餐';

CREATE TABLE mate_tenant_app (
    id              VARCHAR(64) NOT NULL,
    tenant_id       VARCHAR(64) NOT NULL,
    app_id          VARCHAR(32) NOT NULL,
    app_version     VARCHAR(20),
    status          TINYINT DEFAULT 1 COMMENT '1=已安装 2=禁用 3=卸载',
    installed_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_app (tenant_id, app_id)
) ENGINE=InnoDB COMMENT='租户App安装记录';

CREATE TABLE mate_tenant_usage (
    id              BIGINT AUTO_INCREMENT,
    tenant_id       VARCHAR(64) NOT NULL,
    usage_date      DATE NOT NULL,
    user_count      INT DEFAULT 0,
    storage_used    BIGINT DEFAULT 0,
    api_calls       INT DEFAULT 0,
    ai_calls        INT DEFAULT 0,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_date (tenant_id, usage_date)
) ENGINE=InnoDB COMMENT='租户用量统计';

-- 种子数据
INSERT INTO mate_tenant_package (id, name, price, max_users, max_storage, max_apps, ai_enabled, ai_quota_daily) VALUES
('basic',        '基础版', 29900,  10,  1024,  1, 0, 0),
('professional', '专业版', 99900,  50,  10240, 3, 1, 500),
('enterprise',   '企业版', 299900, 999, 102400, 99, 1, 9999);
```

---

## Part 3: mate-tenant-starter 增强

### 3.1 租户配额拦截器

```java
/**
 * 在 Gateway 层拦截，检查租户配额。
 * 超出配额直接拒绝，不进入下游服务。
 */
@Component
public class TenantQuotaFilter implements GlobalFilter, Ordered {

    private final TenantCacheService tenantCache;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        if (tenantId == null) return chain.filter(exchange);

        TenantQuota quota = tenantCache.getQuota(tenantId);

        // 1. 租户状态检查
        if (quota.getStatus() == TenantStatus.FROZEN) {
            return writeError(exchange, 403, "租户已冻结，请联系管理员");
        }
        if (quota.getStatus() == TenantStatus.EXPIRED) {
            if (quota.isInGracePeriod()) {
                // 宽限期: 只允许 GET 请求 (只读)
                if (!exchange.getRequest().getMethod().equals(HttpMethod.GET)) {
                    return writeError(exchange, 403, "租户已过期，宽限期内仅可查看数据");
                }
            } else {
                return writeError(exchange, 403, "租户已过期，请续费");
            }
        }

        // 2. API 调用次数检查
        long todayCalls = tenantCache.incrementApiCalls(tenantId);
        if (todayCalls > quota.getMaxApiCallsPerDay()) {
            return writeError(exchange, 429, "今日 API 调用次数已达上限");
        }

        // 3. App 权限检查
        String path = exchange.getRequest().getPath().value();
        String appId = extractAppId(path); // /api/v1/crm/** → crm
        if (appId != null && !quota.getInstalledApps().contains(appId)) {
            return writeError(exchange, 403, "未开通该应用，请在管理后台安装");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() { return -90; } // 在认证之后, 路由之前
}
```

### 3.2 AI 调用配额

```java
/**
 * 增强 MateAI: 在调用 LLM 前检查租户 AI 配额。
 */
@Aspect
@Component
@ConditionalOnBean(MateAI.class)
public class TenantAIQuotaAspect {

    @Around("execution(* vip.mate.starter.ai.MateAI.*(..))")
    public Object checkAIQuota(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) return pjp.proceed(); // 非多租户模式

        TenantQuota quota = tenantCache.getQuota(tenantId);
        if (!quota.isAiEnabled()) {
            throw new BizException("TENB010", "当前套餐不支持 AI 功能，请升级");
        }

        long todayAICalls = tenantCache.incrementAICalls(tenantId);
        if (todayAICalls > quota.getAiQuotaDaily()) {
            throw new BizException("TENB011", "今日 AI 调用次数已达上限: " + quota.getAiQuotaDaily());
        }

        return pjp.proceed();
    }
}
```

### 3.3 租户初始化流程

```java
/**
 * 新租户开通时自动初始化。
 */
@Service
@RequiredArgsConstructor
public class TenantProvisionService {

    private final TenantRepository tenantRepo;
    private final IRpcUserService userService;

    @Transactional
    public void provision(TenantAggregate tenant, String adminMobile) {
        // 1. 保存租户
        tenantRepo.save(tenant);

        // 2. 创建租户管理员
        userService.register(RegisterUserCommand.builder()
            .mobile(adminMobile)
            .nickName(tenant.getName() + "-管理员")
            .tenantId(tenant.getId())
            .role("tenant_admin")
            .build());

        // 3. 初始化默认数据 (字典、角色、菜单)
        initDefaultData(tenant.getId());

        // 4. 安装默认 App
        TenantPackage pkg = packageRepo.findById(tenant.getPackageId());
        for (String appId : pkg.getAppIds()) {
            installApp(tenant.getId(), appId);
        }

        // 5. 发送开通通知
        eventPublisher.publish("tenant.provisioned",
            new TenantProvisionedEvent(tenant.getId(), adminMobile));
    }
}
```

---

## Part 4: 三种部署形态

### 形态 1: 共享 SaaS (默认)

所有租户共享一套服务，行级隔离。成本最低。

```
Gateway → Auth → System/Admin/CRM/Mall... (单实例，tenant_id 过滤)
                        ↓
                    MySQL (共享库, WHERE tenant_id = ?)
```

**配置**:
```yaml
mate:
  tenant:
    enabled: true
    isolation: ROW        # 行级隔离
    resolver: header
```

### 形态 2: 独立 Schema SaaS

每个租户独立 Schema，服务共享。安全性更高。

```
Gateway → Auth → System/Admin/CRM/Mall... (单实例，动态切换 Schema)
                        ↓
                    MySQL
                    ├── tenant_001 (schema)
                    ├── tenant_002 (schema)
                    └── tenant_003 (schema)
```

**配置**:
```yaml
mate:
  tenant:
    enabled: true
    isolation: SCHEMA
    resolver: domain      # acme.matecloud.vip → tenant_001
```

### 形态 3: 独立部署 (私有化)

大客户独享一套完整环境。最高安全等级。

```
客户专属 K8s Namespace:
├── Gateway
├── Auth + System + Admin
├── CRM + Mall (按购买)
├── MySQL (独占)
├── Redis (独占)
└── Nacos (独占或共享)
```

**通过 Helm Chart 一键部署**:
```bash
helm install acme-corp matecloud/platform \
  --set tenant.id=acme \
  --set tenant.domain=acme.matecloud.vip \
  --set apps={crm,mall,oa} \
  --namespace acme-corp
```

---

## Part 5: 运营后台页面 (mate-admin 扩展)

### 超级管理员看到的

```
🏢 租户管理
├── 租户列表 (搜索/筛选/状态)
├── 新建租户 (企业信息 + 选套餐 + 创建管理员)
├── 租户详情
│   ├── 基本信息
│   ├── 已安装 App
│   ├── 成员列表
│   ├── 用量统计 (API/AI/存储 图表)
│   └── 操作: 续费 / 冻结 / 升级套餐
├── 套餐管理 (基础版/专业版/企业版 CRUD)
└── 全局用量仪表盘

📱 App 市场
├── 可用 App 列表 (CRM/Mall/OA/Edu/Video)
├── 为租户安装/卸载 App
└── App 版本管理
```

### 租户管理员看到的

```
⚙️ 企业设置
├── 企业信息 (名称/Logo/域名)
├── 成员管理 (邀请/删除/角色)
├── 已安装应用
├── 我的套餐 (当前套餐/到期时间/升级)
└── 用量统计 (本企业)
```

---

## 关键设计决策

| 决策 | 选择 | 原因 |
|------|------|------|
| 默认隔离 | 行级 (tenant_id) | 95% 场景够用, 成本最低 |
| 配额检查位置 | Gateway 层 | 前置拦截, 不浪费下游资源 |
| AI 配额 | AOP 在 MateAI 上 | 透明, App 无感 |
| 租户缓存 | Redis + 本地缓存 | 每次请求都要读, 必须快 |
| 过期策略 | 提醒→宽限(只读)→冻结→保留→删除 | 给客户缓冲, 降低流失 |
| App 安装 | 数据库记录 + 菜单注册 | 动态控制, 不需要重启 |
