#!/usr/bin/env bash
# 阻断级：代码文件(java/vue/ts/sql)里禁出现外部产品名。规则 04。
. "$(dirname "$0")/lib.sh"
h_title "无第三方产品名（代码文件）"

terms="$(grep -vE '^\s*#|^\s*$' "$HARNESS_DIR/policy/banned-terms.txt" | paste -sd'|' -)"
if [ -z "$terms" ]; then h_ok "词表为空，跳过"; exit 0; fi

hits="$(grep -rniE "\b(${terms})" \
        --include='*.java' --include='*.vue' --include='*.ts' --include='*.sql' \
        "$REPO_ROOT" 2>/dev/null | grep -vE "$SCAN_EXCLUDE" | grep -vE '/src/test/' || true)"

if [ -n "$hits" ]; then
  echo "$hits" | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  h_bad "发现竞品名（词表 policy/banned-terms.txt）—— 注释只写技术，不写来源产品"
  exit 1
fi
h_ok "干净（词表：${terms}）"
