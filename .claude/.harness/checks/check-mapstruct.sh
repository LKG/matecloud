#!/usr/bin/env bash
# 告警级：禁 BeanUtils.copyProperties（应走 MapStruct）。规则 01。mate-admin 有存量遗留。
. "$(dirname "$0")/lib.sh"
h_title "对象转换走 MapStruct（非 BeanUtils.copyProperties）"

hits="$(grep -rn 'BeanUtils\.copyProperties' --include='*.java' "$REPO_ROOT" 2>/dev/null \
        | grep -vE "$SCAN_EXCLUDE" | grep -vE '/src/test/' || true)"

if [ -n "$hits" ]; then
  echo "$hits" | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  n="$(printf '%s\n' "$hits" | grep -c .)"
  h_warn "$n 处 BeanUtils.copyProperties（遗留技术债，逐步换成 MapStruct）"
  exit 0   # 告警级不阻断
fi
h_ok "无 BeanUtils.copyProperties"
