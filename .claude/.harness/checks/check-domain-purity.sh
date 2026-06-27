#!/usr/bin/env bash
# 阻断级：domain/model/**（聚合/实体/值对象）零框架 import。规则 01。
. "$(dirname "$0")/lib.sh"
h_title "领域纯净：domain/model/** 无框架依赖"

# 框架包前缀（domain/model 里出现任一即违规）
fw='org\.springframework|com\.baomidou|cn\.dev33\.satoken|jakarta\.persistence|javax\.persistence|org\.apache\.ibatis|org\.mybatis'

hits="$(grep -rnE "^import ($fw)" --include='*.java' "$REPO_ROOT" 2>/dev/null \
        | grep -E '/domain/model/' \
        | grep -vE "$SCAN_EXCLUDE" || true)"

if [ -n "$hits" ]; then
  echo "$hits" | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  h_bad "聚合/实体/值对象出现框架 import —— 框架注解只应在 PO(infrastructure/dao/po)"
  exit 1
fi
h_ok "domain/model/** 保持框架无关"
