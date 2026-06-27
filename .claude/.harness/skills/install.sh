#!/usr/bin/env bash
# 把 harness 里的 skills 软链进 .claude/skills/，让它们能被 / 调用。
# 源在版本库 (.claude/.harness/skills/)，软链是本地的 (.claude/skills/ 被 gitignore)。
# 每次 clone 后跑一次即可：bash .claude/.harness/skills/install.sh
set -uo pipefail

SKILLS_SRC="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SKILLS_SRC" rev-parse --show-toplevel)"
DEST="$REPO_ROOT/.claude/skills"
mkdir -p "$DEST"

n=0
for d in "$SKILLS_SRC"/*/; do
  [ -f "$d/SKILL.md" ] || continue
  name="$(basename "$d")"
  ln -sfn "$d" "$DEST/$name"
  echo "  linked /$name"
  n=$((n+1))
done
echo "✓ 安装 $n 个 skill 到 $DEST （重启 Claude Code 会话后生效）"
