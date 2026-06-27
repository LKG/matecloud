#!/usr/bin/env bash
# 告警级：flyway 迁移禁 DROP TABLE / TRUNCATE TABLE。规则 01。
. "$(dirname "$0")/lib.sh"
h_title "Flyway 迁移无破坏性语句（DROP/TRUNCATE TABLE）"

hits="$(grep -rniE 'drop[[:space:]]+table|truncate[[:space:]]+table' --include='*.sql' "$REPO_ROOT" 2>/dev/null \
        | grep -iE '/db/migration/' \
        | grep -vE "$SCAN_EXCLUDE" || true)"

if [ -n "$hits" ]; then
  echo "$hits" | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  h_warn "迁移含 DROP/TRUNCATE TABLE —— 确认是否必要，避免线上数据丢失"
  exit 0
fi
h_ok "迁移无 DROP/TRUNCATE TABLE"
