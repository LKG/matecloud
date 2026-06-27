#!/usr/bin/env bash
# 示例 git pre-commit：提交前跑 harness 阻断级校验。
# 启用：ln -sf ../../.claude/.harness/hooks/pre-commit.sample.sh .git/hooks/pre-commit
#       chmod +x .git/hooks/pre-commit
ROOT="$(git rev-parse --show-toplevel)"
bash "$ROOT/.claude/.harness/checks/run-all.sh" || {
  echo
  echo "✗ Harness 校验未通过，已阻止提交。修复后重试，或 git commit --no-verify 跳过（不建议）。"
  exit 1
}
