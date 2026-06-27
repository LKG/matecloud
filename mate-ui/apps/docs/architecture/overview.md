# 总体架构

## 架构全景

<svg viewBox="0 0 900 612" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="MateCloud 架构全景" style="width:100%;height:auto;font-family:var(--vp-font-family-base)">
  <defs>
    <marker id="arch-ah" markerWidth="9" markerHeight="9" refX="6" refY="3" orient="auto" markerUnits="userSpaceOnUse">
      <path d="M0,0 L6,3 L0,6 Z" fill="var(--vp-c-brand-1)"/>
    </marker>
  </defs>

  <!-- 层背景 -->
  <rect class="arch-band" x="16" y="30"  width="744" height="66"  rx="10"/>
  <rect class="arch-band" x="16" y="126" width="744" height="62"  rx="10"/>
  <rect class="arch-band" x="16" y="218" width="744" height="158" rx="10"/>
  <rect class="arch-band" x="16" y="398" width="744" height="84"  rx="10"/>
  <rect class="arch-band" x="16" y="504" width="744" height="74"  rx="10"/>

  <!-- 连接箭头 -->
  <path class="arch-flow" marker-end="url(#arch-ah)" d="M390,96 L390,124"/>
  <text class="arch-lbl" x="398" y="114">HTTPS · REST  /api/v1</text>
  <path class="arch-flow" marker-end="url(#arch-ah)" d="M390,188 L390,216"/>
  <text class="arch-lbl" x="398" y="206">负载均衡 · Nacos 服务发现</text>
  <path class="arch-flow" marker-end="url(#arch-ah)" d="M390,376 L390,398"/>
  <text class="arch-lbl" x="398" y="391">编译期引入 · 运行于各服务进程内</text>
  <path class="arch-flow" marker-end="url(#arch-ah)" d="M390,482 L390,504"/>
  <text class="arch-lbl" x="398" y="497">持久化 · 缓存 · 消息 · 存储 · 注册配置</text>

  <!-- ===== 接入层 ===== -->
  <text class="arch-cap" x="20" y="24">接入层  CLIENT</text>
  <g>
    <rect class="arch-box" x="24"  y="38" width="170" height="50" rx="8"/>
    <text class="arch-t1" x="109" y="60" text-anchor="middle">Vue 3 Admin · :3000</text>
    <text class="arch-t2" x="109" y="76" text-anchor="middle">Element Plus · Vite</text>
    <rect class="arch-box" x="211" y="38" width="170" height="50" rx="8"/>
    <text class="arch-t1" x="296" y="60" text-anchor="middle">移动端</text>
    <text class="arch-t2" x="296" y="76" text-anchor="middle">H5 · 小程序</text>
    <rect class="arch-box" x="398" y="38" width="170" height="50" rx="8"/>
    <text class="arch-t1" x="483" y="60" text-anchor="middle">桌面端</text>
    <text class="arch-t2" x="483" y="76" text-anchor="middle">Electron</text>
    <rect class="arch-box" x="585" y="38" width="170" height="50" rx="8"/>
    <text class="arch-t1" x="670" y="60" text-anchor="middle">三方系统</text>
    <text class="arch-t2" x="670" y="76" text-anchor="middle">OpenAPI · Webhook</text>
  </g>

  <!-- ===== 网关层 ===== -->
  <text class="arch-cap" x="20" y="120">网关层  GATEWAY</text>
  <rect class="arch-box" x="24" y="130" width="731" height="54" rx="8"/>
  <rect x="24" y="130" width="4" height="54" rx="2" fill="#155aef"/>
  <text class="arch-t1" x="40" y="152">mate-gateway · :9010 <tspan class="arch-t2">Spring WebFlux (Reactor)</tspan></text>
  <text class="arch-t2" x="40" y="172">Sa-Token 统一鉴权 · 灰度路由 · 限流熔断 · Trace 链路透传 · CORS · 安全响应头</text>

  <!-- ===== 业务服务层 ===== -->
  <text class="arch-cap" x="20" y="212">业务服务层  SERVICES · DDD</text>
  <g>
    <rect class="arch-box" x="24"  y="226" width="170" height="54" rx="8"/>
    <rect x="24" y="226" width="4" height="54" rx="2" fill="#36b0e2"/>
    <text class="arch-t1" x="38" y="247">mate-auth · :9020</text>
    <text class="arch-t2" x="38" y="266">认证 · 登录 · Token</text>
    <rect class="arch-box" x="211" y="226" width="170" height="54" rx="8"/>
    <rect x="211" y="226" width="4" height="54" rx="2" fill="#155aef"/>
    <text class="arch-t1" x="225" y="247">mate-system · :9030</text>
    <text class="arch-t2" x="225" y="266">RBAC · 字典 · 配置 · AI</text>
    <rect class="arch-box" x="398" y="226" width="170" height="54" rx="8"/>
    <rect x="398" y="226" width="4" height="54" rx="2" fill="#f7a400"/>
    <text class="arch-t1" x="412" y="247">mate-notice · :9050</text>
    <text class="arch-t2" x="412" y="266">短信 · 邮件 · 站内信</text>
    <rect class="arch-box" x="585" y="226" width="170" height="54" rx="8" stroke-dasharray="4 3"/>
    <text class="arch-t1" x="670" y="247" text-anchor="middle">业务服务（可扩展）</text>
    <text class="arch-t2" x="670" y="266" text-anchor="middle">mate new module</text>
  </g>
  <!-- Dubbo RPC 总线 -->
  <path class="arch-flow" marker-start="url(#arch-ah)" marker-end="url(#arch-ah)" d="M44,294 L735,294"/>
  <rect x="300" y="285" width="180" height="17" rx="8" fill="var(--vp-c-bg-elv)"/>
  <text class="arch-brand" x="390" y="297" text-anchor="middle">Dubbo RPC（同步服务间调用）</text>
  <!-- DDD 四层 -->
  <rect class="arch-box" x="24" y="310" width="731" height="58" rx="8"/>
  <text class="arch-t2" x="38" y="328">服务内部统一 <tspan class="arch-brand">DDD 四层</tspan>：</text>
  <g>
    <rect class="arch-box" x="38"  y="334" width="166" height="26" rx="6"/>
    <text class="arch-t2" x="121" y="351" text-anchor="middle">trigger 触发层</text>
    <text class="arch-flow" x="208" y="351" font-size="12">›</text>
    <rect class="arch-box" x="220" y="334" width="166" height="26" rx="6"/>
    <text class="arch-t2" x="303" y="351" text-anchor="middle">application 应用层</text>
    <text class="arch-flow" x="390" y="351" font-size="12">›</text>
    <rect class="arch-box" x="402" y="334" width="170" height="26" rx="6"/>
    <text class="arch-t2" x="487" y="351" text-anchor="middle">domain 领域层 · 零框架</text>
    <text class="arch-flow" x="576" y="351" font-size="12">›</text>
    <rect class="arch-box" x="588" y="334" width="167" height="26" rx="6"/>
    <text class="arch-t2" x="671" y="351" text-anchor="middle">infrastructure 基础设施</text>
  </g>

  <!-- ===== 能力层 ===== -->
  <text class="arch-cap" x="20" y="392">能力层  STARTERS · 即插即用</text>
  <g>
    <rect class="arch-box" x="24"  y="406" width="232" height="68" rx="8"/>
    <text class="arch-t1" x="38" y="424">核心 · 默认引入</text>
    <text class="arch-t2" x="38" y="442">web · ds · cache · nacos</text>
    <text class="arch-t2" x="38" y="458">rpc · sa-token · monitor</text>
    <rect class="arch-box" x="274" y="406" width="232" height="68" rx="8"/>
    <text class="arch-t1" x="288" y="424">业务 · 按需引入</text>
    <text class="arch-t2" x="288" y="442">mq · job · security · file</text>
    <text class="arch-t2" x="288" y="458">excel · tenant · sms · sso · menu</text>
    <rect class="arch-box" x="524" y="406" width="231" height="68" rx="8"/>
    <text class="arch-t1" x="538" y="424">高级 · contrib</text>
    <text class="arch-t2" x="538" y="442">ai · model · gray · flow · rule</text>
    <text class="arch-t2" x="538" y="458">seata · sentinel · sharding · test</text>
  </g>

  <!-- ===== 基础设施层 ===== -->
  <text class="arch-cap" x="20" y="498">基础设施层  INFRASTRUCTURE</text>
  <g>
    <rect class="arch-box" x="24"  y="512" width="136" height="58" rx="8"/>
    <text class="arch-t1" x="92"  y="536" text-anchor="middle">MySQL 8.0</text>
    <text class="arch-t2" x="92"  y="552" text-anchor="middle">JDBC · Flyway</text>
    <rect class="arch-box" x="173" y="512" width="136" height="58" rx="8"/>
    <text class="arch-t1" x="241" y="536" text-anchor="middle">Redis 7</text>
    <text class="arch-t2" x="241" y="552" text-anchor="middle">Redisson · 缓存/锁</text>
    <rect class="arch-box" x="322" y="512" width="136" height="58" rx="8"/>
    <text class="arch-t1" x="390" y="536" text-anchor="middle">RabbitMQ 3.13</text>
    <text class="arch-t2" x="390" y="552" text-anchor="middle">领域事件 · AMQP</text>
    <rect class="arch-box" x="471" y="512" width="136" height="58" rx="8"/>
    <text class="arch-t1" x="539" y="536" text-anchor="middle">Nacos v3.2</text>
    <text class="arch-t2" x="539" y="552" text-anchor="middle">注册 · 配置中心</text>
    <rect class="arch-box" x="620" y="512" width="135" height="58" rx="8"/>
    <text class="arch-t1" x="687" y="536" text-anchor="middle">MinIO</text>
    <text class="arch-t2" x="687" y="552" text-anchor="middle">对象存储 · S3</text>
  </g>

  <!-- ===== 可观测性 右栏（横切） ===== -->
  <rect class="arch-box" x="772" y="226" width="112" height="344" rx="8" stroke-dasharray="4 3"/>
  <rect x="772" y="226" width="112" height="4" rx="2" fill="#f05674"/>
  <text class="arch-cap" x="828" y="252" text-anchor="middle" fill="var(--vp-c-text-2)">可观测性</text>
  <g transform="translate(828,400) rotate(-90)">
    <text class="arch-t2" x="0" y="-6" text-anchor="middle">Actuator · Prometheus</text>
    <text class="arch-t2" x="0" y="12" text-anchor="middle">Micrometer Tracing · 链路</text>
  </g>
</svg>

## 核心设计原则

### 1. 最小公共

`mate-common` 只包含纯类型（DTO、枚举、异常），零自动配置。不会因为引入公共包而带入不需要的依赖。

### 2. Starter = 即插即用

每个 Starter 封装一个完整的横切关注点。业务模块通过 Maven 依赖引入 Starter，自动配置即刻生效，无需手动写 `@Bean`。

### 3. DDD 四层架构

所有业务模块遵循统一的 [DDD 四层结构](/architecture/ddd)。Domain 层零框架依赖，确保业务逻辑的纯粹性。

### 4. CQRS 读写分离

写操作通过 `CommandService`（带 `@Transactional`），读操作通过 `QueryService`（接口 + 实现），严格分离。

### 5. 五层配置

[配置体系](/guide/configuration) 确保服务启动文件只需 ~15 行，环境差异通过 Nacos 动态管理。

## 服务通信

- **同步调用**：网关 → 服务走 HTTP，服务间走 Dubbo RPC
- **异步通信**：RabbitMQ 消息队列 + 领域事件
- **服务发现**：Nacos
- **配置中心**：Nacos
