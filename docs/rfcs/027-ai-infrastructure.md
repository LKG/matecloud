# RFC-027: AI as Infrastructure — Every App is AI-Native

- **Status**: Draft
- **Created**: 2026-04-12
- **Author**: MateCloud Team

> "The computer is a bicycle for the mind." — Steve Jobs, 1990
>
> AI 不是自行车了。AI 是引擎。每辆车都有引擎，不需要单独买。

## 核心理念

**AI 不是子系统，AI 是基础设施。** 就像电不是一个产品——电是所有产品都需要的东西。

```
传统架构 (错误):                    MateCloud 架构 (正确):
┌─────────┐                        ┌─────────────────────────┐
│ mate-crm│──→ 调AI服务?           │      mate-ai-starter    │ ← 每个 App 自带
│ mate-oa │──→ 调AI服务?           │   (Spring AI Alibaba)   │
│ mate-edu│──→ 调AI服务?           └────────────┬────────────┘
└─────┬───┘                                     │
      │     ┌──────────┐            ┌───────────┴───────────┐
      └────→│ AI 服务   │            │ CRM  OA  Edu  Mall  Video│
            │ (单点瓶颈)│            │ 每个都内置 AI 能力      │
            └──────────┘            └───────────────────────┘
```

**不要做 AI 中台。** 做 AI Starter。

---

## Part 1: mate-ai-starter (新增)

### 一句话定义

引入 `mate-ai-starter`，你的服务就有了对话、摘要、分类、提取、生成的能力。

### pom.xml

```xml
<project>
    <parent>
        <groupId>vip.mate</groupId>
        <artifactId>mate-starters</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>mate-ai-starter</artifactId>
    <name>mate-ai-starter</name>
    <description>AI infrastructure: ChatModel + Embedding + structured output</description>

    <dependencies>
        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-base</artifactId>
        </dependency>

        <!-- Spring AI core -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-openai</artifactId>
        </dependency>

        <!-- Alibaba DashScope (通义千问) -->
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter</artifactId>
        </dependency>

        <!-- Structured Output (JSON Schema) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-model-observation</artifactId>
        </dependency>

        <!-- Vector Store (for RAG) -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-starter-vector-store-redis</artifactId>
        </dependency>

        <dependency>
            <groupId>vip.mate</groupId>
            <artifactId>mate-cache-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 核心抽象: MateAI

```java
package vip.mate.starter.ai;

import org.springframework.ai.chat.client.ChatClient;

/**
 * AI 能力门面 — 一个注入搞定所有 AI 操作。
 * 就像 RedissonService 封装了 Redis，MateAI 封装了 LLM。
 */
public class MateAI {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    // ==================== 对话 ====================

    /** 单轮对话 */
    public String chat(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }

    /** 带系统提示词的对话 */
    public String chat(String systemPrompt, String userMessage) {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userMessage)
            .call().content();
    }

    /** 流式对话 */
    public Flux<String> stream(String prompt) {
        return chatClient.prompt(prompt).stream().content();
    }

    // ==================== 结构化输出 ====================

    /** 让 AI 返回指定 Java 类型 (JSON Schema 约束) */
    public <T> T extract(String prompt, Class<T> type) {
        return chatClient.prompt(prompt)
            .call()
            .entity(type);
    }

    /** 让 AI 返回列表 */
    public <T> List<T> extractList(String prompt, Class<T> elementType) {
        return chatClient.prompt(prompt)
            .call()
            .entity(new ParameterizedTypeReference<List<T>>() {});
    }

    // ==================== 文本处理 ====================

    /** 摘要 */
    public String summarize(String text) {
        return chat("你是一个摘要专家。请用3-5句话总结以下内容，保留关键信息。", text);
    }

    /** 分类 */
    public <E extends Enum<E>> E classify(String text, Class<E> categories) {
        return extract(
            "将以下文本分类到最合适的类别: " + text,
            categories
        );
    }

    /** 情感分析 */
    public Sentiment analyzeSentiment(String text) {
        return extract(
            "分析以下文本的情感倾向: " + text,
            Sentiment.class
        );
    }

    /** 实体提取 */
    public <T> T extractEntities(String text, Class<T> entityType) {
        return extract(
            "从以下文本中提取结构化信息: " + text,
            entityType
        );
    }

    // ==================== 向量 / RAG ====================

    /** 文本转向量 */
    public float[] embed(String text) {
        return embeddingModel.embed(text);
    }

    /** 批量转向量 */
    public List<float[]> embedBatch(List<String> texts) {
        return embeddingModel.embed(texts);
    }
}
```

### 自动配置

```java
package vip.mate.starter.ai.config;

@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@EnableConfigurationProperties(MateAIProperties.class)
public class MateAIAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MateAI mateAI(ChatClient.Builder chatClientBuilder,
                          @Autowired(required = false) EmbeddingModel embeddingModel) {
        ChatClient chatClient = chatClientBuilder
            .defaultSystem("你是 MateCloud 平台的 AI 助手。简洁、准确、专业。")
            .build();
        return new MateAI(chatClient, embeddingModel);
    }
}
```

```java
@ConfigurationProperties(prefix = "mate.ai")
public class MateAIProperties {
    /** 默认模型 */
    private String model = "qwen-plus";
    /** 默认温度 */
    private double temperature = 0.7;
    /** 是否启用 RAG */
    private boolean ragEnabled = false;
    /** 向量存储类型 */
    private String vectorStore = "redis";
}
```

### 配置 (application.yml)

```yaml
mate:
  ai:
    model: qwen-plus
    temperature: 0.7
    rag-enabled: false

spring:
  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY}
    # 或 OpenAI 兼容
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
```

---

## Part 2: 每个子系统如何用 AI

### CRM: 智能客户洞察

```java
// mate-crm-service
@Service
@RequiredArgsConstructor
public class CustomerInsightService {

    private final MateAI ai;

    /** 分析客户跟进记录，生成客户画像 */
    public CustomerProfile analyzeCustomer(String customerId) {
        List<Activity> activities = activityRepo.findByCustomerId(customerId);
        String context = activities.stream()
            .map(a -> a.getTime() + " " + a.getType() + ": " + a.getContent())
            .collect(Collectors.joining("\n"));

        return ai.extract(
            "根据以下客户跟进记录，分析客户画像:\n" + context,
            CustomerProfile.class
        );
    }

    /** 预测商机成交概率 */
    public DealPrediction predictDeal(Opportunity opp) {
        return ai.extract(
            "根据以下商机信息预测成交概率和建议:\n" + toJson(opp),
            DealPrediction.class
        );
    }

    /** 自动生成跟进邮件 */
    public String draftFollowUpEmail(String customerId, String purpose) {
        Customer c = customerRepo.findById(customerId);
        return ai.chat(
            "你是一个专业的销售顾问。",
            "为客户 " + c.getName() + " 撰写一封" + purpose + "的跟进邮件。"
        );
    }
}

// 结构化输出类型
public record CustomerProfile(
    String personality,         // 决策风格
    String painPoints,          // 核心痛点
    String budget,              // 预算范围
    List<String> interests,     // 关注点
    int engagementScore         // 参与度 1-10
) {}

public record DealPrediction(
    int probability,            // 成交概率 0-100
    String stage,               // 建议阶段
    List<String> nextActions,   // 下一步行动
    List<String> risks          // 风险因素
) {}
```

### Mall: 智能商品运营

```java
@Service
@RequiredArgsConstructor
public class ProductAIService {

    private final MateAI ai;

    /** 根据关键词生成商品标题和描述 */
    public ProductCopy generateCopy(String keywords, String category) {
        return ai.extract(
            "为以下商品生成吸引人的标题和描述:\n类目:" + category + "\n关键词:" + keywords,
            ProductCopy.class
        );
    }

    /** 智能客服: 回答商品咨询 */
    public String answerProductQuestion(String productId, String question) {
        Product p = productRepo.findById(productId);
        return ai.chat(
            "你是商品客服。根据以下商品信息回答客户问题。只回答与商品相关的问题。\n" + toJson(p),
            question
        );
    }

    /** 评论情感分析 + 摘要 */
    public ReviewAnalysis analyzeReviews(String productId) {
        List<Review> reviews = reviewRepo.findByProductId(productId);
        String text = reviews.stream().map(Review::getContent).collect(joining("\n"));
        return ai.extract("分析以下商品评论的整体情感和关键反馈:\n" + text, ReviewAnalysis.class);
    }
}

public record ProductCopy(
    String title,           // 标题 (30字内)
    String subtitle,        // 副标题
    String description,     // 详细描述
    List<String> sellingPoints  // 卖点 (3-5个)
) {}
```

### OA: 智能办公

```java
@Service
@RequiredArgsConstructor
public class OaAIService {

    private final MateAI ai;

    /** 会议纪要自动生成 */
    public MeetingMinutes generateMinutes(String meetingTranscript) {
        return ai.extract(
            "根据以下会议录音转写，生成结构化会议纪要:\n" + meetingTranscript,
            MeetingMinutes.class
        );
    }

    /** 审批意见智能建议 */
    public ApprovalSuggestion suggestApproval(ApprovalForm form) {
        return ai.extract(
            "根据以下审批表单和公司规定，给出审批建议:\n" + toJson(form),
            ApprovalSuggestion.class
        );
    }

    /** 公告智能撰写 */
    public String draftAnnouncement(String topic, String keyPoints) {
        return ai.chat("你是企业行政专员。", "撰写一份关于'" + topic + "'的公告。要点:" + keyPoints);
    }
}

public record MeetingMinutes(
    String title,
    String date,
    List<String> participants,
    List<String> decisions,
    List<TodoItem> actionItems
) {}

public record TodoItem(String task, String assignee, String deadline) {}
```

### Video: 智能内容

```java
@Service
@RequiredArgsConstructor
public class VideoAIService {

    private final MateAI ai;

    /** 视频标题和标签自动生成 */
    public VideoMeta generateMeta(String transcript) {
        return ai.extract(
            "根据以下视频转写文本，生成标题、描述和标签:\n" + transcript,
            VideoMeta.class
        );
    }

    /** 内容审核 (文本层面) */
    public ContentReview reviewContent(String text) {
        return ai.extract(
            "审核以下内容是否合规，标记违规项:\n" + text,
            ContentReview.class
        );
    }

    /** 智能字幕翻译 */
    public String translateSubtitles(String subtitles, String targetLang) {
        return ai.chat("你是专业字幕翻译。保持口语化、简洁。", 
            "将以下字幕翻译为" + targetLang + ":\n" + subtitles);
    }
}

public record VideoMeta(
    String title, String description, List<String> tags, String category
) {}
public record ContentReview(
    boolean passed, List<String> violations, String reason
) {}
```

### Edu: 智能教学

```java
@Service
@RequiredArgsConstructor
public class EduAIService {

    private final MateAI ai;

    /** 自动出题 */
    public List<Question> generateQuestions(String courseContent, int count) {
        return ai.extractList(
            "根据以下课程内容生成" + count + "道选择题:\n" + courseContent,
            Question.class
        );
    }

    /** 作业批改 + 评语 */
    public GradeResult gradeAssignment(String question, String answer, String rubric) {
        return ai.extract(
            "题目:" + question + "\n评分标准:" + rubric + "\n学生答案:" + answer + "\n请打分并给出评语。",
            GradeResult.class
        );
    }

    /** 个性化学习路径推荐 */
    public LearningPath recommendPath(String studentId) {
        StudentProfile profile = studentRepo.getProfile(studentId);
        return ai.extract(
            "根据以下学生画像推荐学习路径:\n" + toJson(profile),
            LearningPath.class
        );
    }
}

public record Question(
    String stem, List<String> options, String answer, String explanation
) {}
public record GradeResult(int score, String comment, List<String> improvements) {}
public record LearningPath(List<String> courses, String reason, int estimatedHours) {}
```

---

## Part 3: CLI 也是基础设施 — mate-cli-starter

CLI 不只是开发工具。每个 App 都能暴露自己的 CLI 命令。

### mate-cli-starter (新增)

```java
package vip.mate.starter.cli;

/**
 * 任何服务引入此 starter，自动暴露 CLI/MCP 端点。
 * 服务启动时自动注册命令到 mate-cli 的服务发现。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "mate.cli", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CliAutoConfiguration {

    @Bean
    public CliCommandRegistry cliCommandRegistry() {
        return new CliCommandRegistry();
    }

    @Bean
    public CliEndpoint cliEndpoint(CliCommandRegistry registry) {
        return new CliEndpoint(registry); // Actuator endpoint: /actuator/cli
    }
}
```

### @CliCommand 注解

```java
/**
 * 标注在 Spring Bean 方法上，自动注册为 CLI 命令。
 * 同时也是 MCP Tool (MateClaw 可直接调用)。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CliCommand {
    String name();                  // 命令名: "crm:customer:list"
    String description();           // 描述 (MCP tool description)
    String group() default "";      // 分组: "crm"
}
```

### 子系统中使用

```java
// mate-crm-service
@Component
public class CrmCliCommands {

    @Autowired private ICustomerQueryService customerQuery;
    @Autowired private MateAI ai;

    @CliCommand(name = "crm:customer:list", description = "List customers with optional filter")
    public List<CustomerDTO> listCustomers(@Param("status") String status,
                                            @Param("limit") int limit) {
        return customerQuery.list(status, limit);
    }

    @CliCommand(name = "crm:customer:analyze", description = "AI-analyze a customer")
    public CustomerProfile analyzeCustomer(@Param("customerId") String id) {
        return insightService.analyzeCustomer(id);
    }

    @CliCommand(name = "crm:deal:predict", description = "Predict deal close probability")
    public DealPrediction predictDeal(@Param("opportunityId") String id) {
        Opportunity opp = oppRepo.findById(id);
        return insightService.predictDeal(opp);
    }
}
```

### 使用方式

```bash
# 终端直接调用
mate crm:customer:list --status ACTIVE --limit 20

# MateClaw Agent 调用 (MCP)
# "帮我分析客户 C001 的画像"
# → MateClaw 自动调用 crm:customer:analyze --customerId C001

# Claude Code 中调用 (通过 MCP Server)
# 开发者: "查一下活跃客户有多少"
# → Claude Code 调用 mate crm:customer:list --status ACTIVE
```

### 架构: CLI 命令自动发现

```
mate-cli (主进程)
    │
    ├── 内置命令 (new, service, rpc, db, cache, config)
    │
    └── 远程命令 (通过 Nacos 发现所有服务的 /actuator/cli 端点)
        ├── mate-crm:   crm:customer:*, crm:deal:*
        ├── mate-mall:  mall:product:*, mall:order:*
        ├── mate-oa:    oa:approval:*, oa:attendance:*
        ├── mate-edu:   edu:course:*, edu:exam:*
        └── mate-video: video:upload:*, video:review:*
```

---

## Part 4: 完整基础设施矩阵

每个 App 天然拥有的能力 (引入对应 starter 即可):

```
┌─────────────────────────────────────────────────────┐
│                   App (任意子系统)                    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌─── 数据层 ───┐  ┌─── 通信层 ───┐  ┌── 安全层 ──┐ │
│  │ ds-starter   │  │ rpc-starter  │  │ security   │ │
│  │ cache-starter│  │ mq-starter   │  │ sa-token   │ │
│  │ (lock+id)    │  │ nacos-starter│  │ datascope  │ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
│                                                     │
│  ┌─── AI 层 ────┐  ┌─── 运维层 ───┐  ┌── 工具层 ──┐ │
│  │ ai-starter   │  │ monitor      │  │ excel      │ │
│  │ (chat/embed/ │  │ job-starter  │  │ file       │ │
│  │  extract/rag)│  │ web-starter  │  │ cli-starter│ │
│  └──────────────┘  └──────────────┘  └────────────┘ │
│                                                     │
└─────────────────────────────────────────────────────┘

每个 starter 是一块乐高。拼出你需要的 App。
```

### 每个 App 的标准依赖

```xml
<!-- 每个子系统的 "基本套餐" -->
<dependencies>
    <!-- 必选 -->
    <dependency><artifactId>mate-base</artifactId></dependency>
    <dependency><artifactId>mate-ds-starter</artifactId></dependency>
    <dependency><artifactId>mate-web-starter</artifactId></dependency>
    <dependency><artifactId>mate-nacos-starter</artifactId></dependency>
    <dependency><artifactId>mate-rpc-starter</artifactId></dependency>
    <dependency><artifactId>mate-cache-starter</artifactId></dependency>
    <dependency><artifactId>mate-sa-token-starter</artifactId></dependency>
    <dependency><artifactId>mate-monitor-starter</artifactId></dependency>

    <!-- AI 能力 (标配) -->
    <dependency><artifactId>mate-ai-starter</artifactId></dependency>

    <!-- CLI 能力 (标配) -->
    <dependency><artifactId>mate-cli-starter</artifactId></dependency>

    <!-- 按需 -->
    <dependency><artifactId>mate-security-starter</artifactId></dependency>
    <dependency><artifactId>mate-mq-starter</artifactId></dependency>
    <dependency><artifactId>mate-job-starter</artifactId></dependency>
    <dependency><artifactId>mate-excel-starter</artifactId></dependency>
    <dependency><artifactId>mate-file-starter</artifactId></dependency>
</dependencies>
```

---

## Part 5: MateClaw 如何与所有 App 联动

```
用户: "帮我查一下上个月 CRM 里成交的订单，然后在商城里给这些客户发优惠券"

MateClaw Agent 执行链:
1. [MCP] crm:deal:list --status WON --month last
2. [MCP] mall:coupon:batch-create --userIds [...] --type DISCOUNT --value 50
3. [回答] 已为 23 位客户创建 ¥50 优惠券

背后原理:
- MateClaw 通过 MCP 发现所有 App 的 CLI 命令
- 每个 @CliCommand 自动注册为 MCP Tool
- Agent 自主编排跨 App 操作
- 用户只需说一句话
```

---

## 新增 Starter 清单 (平台需要添加)

| Starter | 作用 | 优先级 |
|---------|------|--------|
| **mate-ai-starter** | ChatModel + Embedding + 结构化输出 + RAG | P0 |
| **mate-cli-starter** | @CliCommand + Actuator CLI 端点 + MCP 暴露 | P0 |

两个 starter。不多不少。AI + CLI = 每个 App 天生会说话、天生能被操控。

---

## 验证方案

1. **AI**: mate-system 引入 mate-ai-starter → `mateAI.chat("Hello")` 返回回答
2. **结构化输出**: `mateAI.extract("...", CustomerProfile.class)` 返回 Java 对象
3. **CLI 注册**: mate-crm 启动 → `mate crm:customer:list` 可用
4. **MCP 联动**: MateClaw 中说 "列出活跃客户" → 自动调用 CRM CLI → 返回结果
5. **跨 App**: MateClaw 一句话触发 CRM + Mall 两个 App 的操作
