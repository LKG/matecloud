#!/usr/bin/env bash
# 告警级：.vue 里硬编码颜色字面量(#rgb/#rrggbb)——暗黑模式穿帮风险，应走 --mc-* token。规则 05。
# 仅作趋势提示，不阻断（存量较多，逐步还）。
. "$(dirname "$0")/lib.sh"
h_title "前端硬编码颜色（应走 --mc-* token）"

UI="$REPO_ROOT/mate-ui"
[ -d "$UI" ] || { h_ok "无 mate-ui，跳过"; exit 0; }

# 近似：含 16 进制颜色的 .vue（token 定义在 .css 里，不在此列）。
files="$(grep -rlE '#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b' "$UI/apps" "$UI/packages" \
           --include='*.vue' 2>/dev/null \
         | grep -vE "$SCAN_EXCLUDE" || true)"
n="$(printf '%s\n' "$files" | grep -c . || true)"

if [ "$n" -gt 0 ]; then
  printf '%s\n' "$files" | sed "s|$REPO_ROOT/||" | head -8 | sed 's/^/    /'
  [ "$n" -gt 8 ] && echo "    … 共 $n 个 .vue"
  h_warn "$n 个 .vue 含硬编码颜色 —— 新代码改用 --mc-* token，暗黑才不穿帮"
else
  h_ok "无硬编码颜色"
fi
exit 0
