# MateCloud Harness · 约束工程

> 把散落在 `CLAUDE.md`、`docs/conventions/`、记忆库、各 RFC 里的**项目铁律**，
> 固化成一套「声明式规则 + 可执行校验」，让 AI 与人改这个仓库时都被同一套约束守住。

## 这是什么

"harness（约束马具）工程" = 给代理（Claude/Codex）和人套一层护栏：

- **规则（`rules/`）** — 人和 AI 都读的声明式约束。一个关注点一个文件，是事实来源。
- **校验（`checks/`）** — 把规则里**可机检**的部分写成 bash 脚本，能在提交/CI 时真正拦住违规。
- **技能（`skills/`）** — 高频/高危操作的**约束式流程 playbook**（建模块、提交前自检、开源前把关），可被 `/` 调用。
- **清单（`manifest.yml`）** — 规则 ↔ 校验 ↔ 技能 ↔ 严重级的索引。

规则讲"应该怎样、为什么"；校验讲"违反了就报错"；技能讲"做这件事的标准步骤"。三层互补。

## 怎么用

```bash
# 跑全部校验
bash .claude/.harness/checks/run-all.sh

# 跑单项
bash .claude/.harness/checks/check-domain-purity.sh
```

退出码：**阻断级**任一不过 → 退出码 1（适合卡 CI / pre-commit）；**告警级**只打印不影响退出码。

## 两档严重级

| 档 | 含义 | 现状 | 违反后果 |
|----|------|------|----------|
| **阻断级 (blocking)** | 当前为零、必须保持为零的不变量 | 全绿 | 提交 / CI 失败 |
| **告警级 (warning)** | 已知技术债，逐步还 | 有少量存量 | 仅提示，不阻断 |

## 接进自动化（可选）

**git pre-commit**：
```bash
ln -sf ../../.claude/.harness/hooks/pre-commit.sample.sh .git/hooks/pre-commit
```

**Claude Code Stop/PostToolUse hook**（在 `.claude/settings.json`）：让每次改完自动跑校验，
把违规作为反馈回灌给模型。参考 `hooks/pre-commit.sample.sh` 的命令。

## 技能（skills/）

约束式操作 playbook，源文件随 harness 进版本库。装成可 `/` 调用：

```bash
bash .claude/.harness/skills/install.sh   # 软链进 .claude/skills/，重启会话生效
```

| 技能 | 何时用 | 约束什么 |
|------|--------|----------|
| `mate-new-module` | 新建业务模块 | DDD 四层、命名、错误码、领域纯净 |
| `mate-preflight` | 提交 / 开 PR 前 | 跑校验 + 提交规范 + 范围干净 |
| `mate-oss-gate` | 开源 / 公开发布前 | 边界、剥离清单、竞品名/密钥/路径 |

即使不装，这些 SKILL.md 也是 harness 的流程事实来源，AI 读 harness 时会据其执行。

## 怎么扩展

1. 在 `rules/` 写清楚新约束（什么 / 为什么 / 例外）。
2. 在 `checks/` 加 `check-xxx.sh`，`. "$(dirname "$0")/lib.sh"`，违规 `exit 1`。
3. 在 `run-all.sh` 把它登记进 `BLOCKING` 或 `WARNING` 数组。
4. 在 `manifest.yml` 补一行索引。

新校验**先以告警级落地**，把存量清零后再升为阻断级——这样不会一上来就卡死所有人。
