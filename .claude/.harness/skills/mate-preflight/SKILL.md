---
name: mate-preflight
description: 在 MateCloud 提交代码或开 PR 前使用。跑 harness 约束校验、修掉违规、按规范写提交信息。当用户说"提交前检查""preflight""要提交了""commit 之前自检"时触发。
---

# 提交 / PR 前自检

目标：让每次提交都过得了 harness，并符合提交规范。

## 第一步：跑约束校验

```bash
bash .claude/.harness/checks/run-all.sh
```

- **阻断级**（竞品名 / 硬编码密钥 / OSS 边界 / 领域纯净）任一红 → **必须修到绿**才提交。
- **告警级**（MapStruct / AutoConfig / Flyway）→ 看是不是你这次引入的；是就一并修，
  历史存量可暂留但别新增。

修法对照 `.claude/.harness/rules/`：每个 check 的 `rule:` 字段（见 `manifest.yml`）指向对应规则文件。

## 第二步：范围自查（别捎带无关改动）

```bash
git status --short
git diff --cached --stat
```
只暂存本次任务相关文件。**不要**顺手提交他人工作区改动、`docs/tasks/`（含内部路径）、
或 `.claude/` 下除 `.harness` 外的本地配置。

## 第三步：提交信息规范

格式 `{type}({scope}): {description}`，中文描述 OK：

- type：`feat` / `fix` / `refactor` / `docs` / `chore` / `test` / `style` / `perf`
- scope：模块名（`system` / `auth` / `gateway` / `harness` …）
- 示例：`fix(system): 用户查询分页参数校验缺失`

提交信息末尾按本仓约定加 `Co-Authored-By` 行（若由 AI 协作）。

## 第四步：分支

当前在默认分支（main）就先开 `feature/{name}` 或 `fix/{issue}`；
本项目日常工作流是提交并推送到 `dev`（除非另有要求）。

## 一键清单

- [ ] `run-all.sh` 阻断级全绿
- [ ] 未新增告警级违规
- [ ] 暂存范围干净，无无关文件 / 内部路径
- [ ] 提交信息 `{type}({scope}): ...`
