# 规则 04 · 开源 / 商业边界（open-core）

> 详版见 RFC-026（产品生态）、记忆 `no-competitor-names-in-code`。

## 开源范围

开源模块：`mate-common` / `mate-starters` / `mate-starters-contrib` /
`mate-gateway` / `mate-auth` / `mate-cli` / `mate-biz/mate-system` / `mate-notice` / `mate-ui` /
`mate-monolith`。

## 不变量

1. **开源模块不反向依赖企业模块**。
   - ✅ 机检：`check-oss-boundary`（阻断级）

2. **代码文件里禁出现外部产品名**（`Dify` / `FastGPT` / `qKnow` / `sqlbot` 等）。
   - 代码注释/字符串/SQL/前端只写**技术本身**（"左右双栏""引用明细形态"），不写来源产品；
     参考来源记录在 commit message 与会话里即可。
   - 合法例外：`coze` / `n8n` 作为**工作流导入的格式标识符**（互操作）可保留，它们是输入格式名而非"借鉴"。
   - ✅ 机检：`check-competitor-names`（阻断级，词表见 `policy/banned-terms.txt`）

3. **禁内部本地路径泄露**（`C:\codes\...`、`/Users/xxx/Codes/...`、内网仓地址）进入将公开的文件。
   已知待清理点见会话；新增不要再写入。

## 发布流程（参考，落地见 `.oss-publish/` 若已建）

私有 monorepo 为唯一真源 → 打 tag 触发"过滤发布"：按 exclude 清单删企业目录与敏感文档 →
gitleaks 闸门 → squash 成单 commit 推公开镜像仓。**绝不**用 worktree / 同仓双分支（共享 git 历史会泄露企业代码）。

剥离清单（开源时从公开仓移除）：
```
docs/tasks/   .oss-publish/
```
