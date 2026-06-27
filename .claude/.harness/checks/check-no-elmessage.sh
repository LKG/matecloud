#!/usr/bin/env bash
# 阻断级：mate-ui 里禁用 ElMessage/ElMessageBox，改用 @matecloud/ui 的
# MateMessage / MateMessageBox。规则 05。
. "$(dirname "$0")/lib.sh"
h_title "无 ElMessage/ElMessageBox（用 MateMessage/MateMessageBox）"

UI="$REPO_ROOT/mate-ui"
[ -d "$UI" ] || { h_ok "无 mate-ui，跳过"; exit 0; }

# 真实用法：方法调用 `ElMessage.x` / 可调用 `ElMessage(` / 从 element-plus 显式 import。
# 排除：构建产物、MateMessage 组件自身、文档注释里对 EP API 的引用（@code/@link、注释行）。
hits="$(grep -rEn "\bElMessage(Box)?[.(]|import[^\"']*\bElMessage(Box)?\b[^\"']*from ['\"]element-plus['\"]" \
          "$UI/apps" "$UI/packages" --include='*.vue' --include='*.ts' 2>/dev/null \
        | grep -vE "$SCAN_EXCLUDE" \
        | grep -vE '/MateMessage/' \
        | grep -vE '@code|@link' \
        | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*)' \
        || true)"

if [ -n "$hits" ]; then
  echo "$hits" | sed "s|$REPO_ROOT/||" | sed 's/^/    /'
  h_bad "发现 ElMessage/ElMessageBox —— 换成 @matecloud/ui 的 MateMessage / MateMessageBox（含 EP 自动导入手滑）"
  exit 1
fi
h_ok "干净（消息/弹窗全走 MateMessage / MateMessageBox）"
