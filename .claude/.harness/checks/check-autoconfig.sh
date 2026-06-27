#!/usr/bin/env bash
# 告警级：*AutoConfiguration.java 顶层应用 @AutoConfiguration（嵌套 @Configuration 允许）。规则 02。
. "$(dirname "$0")/lib.sh"
h_title "Starter 自动装配用 @AutoConfiguration"

bad=""
while IFS= read -r f; do
  [ -z "$f" ] && continue
  grep -q '@AutoConfiguration' "$f" || bad="$bad$f"$'\n'
done < <(grep -rl --include='*AutoConfiguration.java' '' \
           "$REPO_ROOT/mate-starters" "$REPO_ROOT/mate-starters-contrib" 2>/dev/null \
           | grep -vE "$SCAN_EXCLUDE")

if [ -n "${bad//$'\n'/}" ]; then
  printf '%s' "$bad" | grep -v '^$' | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  h_warn "上述 *AutoConfiguration.java 缺 @AutoConfiguration 注解"
  exit 0
fi
h_ok "所有 *AutoConfiguration.java 均使用 @AutoConfiguration"
