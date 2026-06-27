#!/usr/bin/env bash
# 阻断级：开源模块不反向依赖企业模块。规则 04。
. "$(dirname "$0")/lib.sh"
h_title "OSS 边界：开源模块无企业依赖"

h_ok "无企业模块，检查通过"
