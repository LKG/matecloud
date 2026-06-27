#!/usr/bin/env bash
# 共享工具，被 checks/*.sh source。注意：不开 set -e（grep 无命中返回 1 会误中断）。
set -uo pipefail

CHECKS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HARNESS_DIR="$(cd "$CHECKS_DIR/.." && pwd)"
REPO_ROOT="$(git -C "$HARNESS_DIR" rev-parse --show-toplevel 2>/dev/null || (cd "$HARNESS_DIR/../.." && pwd))"

# 扫描时排除的路径（构建产物 / 依赖 / git）
SCAN_EXCLUDE='/target/|/node_modules/|/dist/|/\.git/'

if [ -t 1 ]; then
  C_RED=$'\033[31m'; C_GRN=$'\033[32m'; C_YEL=$'\033[33m'; C_DIM=$'\033[2m'; C_RST=$'\033[0m'
else
  C_RED=''; C_GRN=''; C_YEL=''; C_DIM=''; C_RST=''
fi

h_title(){ printf '%s▸ %s%s\n' "$C_DIM" "$1" "$C_RST"; }
h_ok(){   printf '  %s✓%s %s\n' "$C_GRN" "$C_RST" "$1"; }
h_bad(){  printf '  %s✗%s %s\n' "$C_RED" "$C_RST" "$1"; }
h_warn(){ printf '  %s!%s %s\n' "$C_YEL" "$C_RST" "$1"; }

# 过滤掉被排除的路径
strip_excluded(){ grep -vE "$SCAN_EXCLUDE" || true; }
