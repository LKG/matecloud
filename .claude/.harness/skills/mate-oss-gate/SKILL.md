---
name: mate-oss-gate
description: 在准备把 MateCloud（或其子集）开源 / 发布到公开仓前使用。按 open-core 边界把关：剥离企业代码、清竞品名与内部路径、查密钥、确认 LICENSE。当用户说"要开源了""发布公开版""开源前检查""oss release"时触发。
---

# 开源 / 公开发布把关

MateCloud 是 **开源项目**：平台脚手架全部公开。
原则见 `.claude/.harness/rules/04-open-source.md`。

> 发布模型：**私有 monorepo 为唯一真源 → 过滤发布到公开镜像仓**（删企业目录 → 闸门 → squash 单 commit）。
> **绝不**用 worktree / 同仓双分支（共享 git 历史会泄露企业代码）。

## 第一步：跑边界与泄露校验

```bash
bash .claude/.harness/checks/check-competitor-names.sh   # 无 Dify/FastGPT/qKnow/sqlbot
bash .claude/.harness/checks/check-secrets.sh            # 无真实密钥
bash .claude/.harness/checks/check-oss-boundary.sh       # 开源模块不反向依赖企业模块
```
三项必须全绿。

## 第二步：内部路径泄露扫描（公开前手动确认）

```bash
grep -rniE "C:[\\\\/]+codes|/Users/[a-z]+/Codes|git\.mate\.vip" \
  --include="*.md" --include="*.java" --include="*.yml" . | grep -v "/target/"
```
命中的发布前清掉或剔除。

## 第三步：开源治理文件

- [ ] 根目录有 **`LICENSE`** 文件（README/pom 已声明 Apache-2.0，需落地文件）。
- [ ] README / CONTRIBUTING / CODE_OF_CONDUCT / SECURITY 齐全。
- [ ] 若接受外部贡献：CLA/DCO（open-core 需此才能把社区贡献用进商业版）。

## 出口判据

- [ ] 三项边界校验全绿
- [ ] 无内部路径 / 内网地址
- [ ] LICENSE 文件就位
