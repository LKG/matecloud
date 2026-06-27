#!/usr/bin/env bash
# 阻断级：非测试代码里禁硬编码真实密钥。规则 03。
. "$(dirname "$0")/lib.sh"
h_title "无硬编码密钥（非测试代码）"

# 真实密钥特征：OpenAI/Anthropic sk-、AWS AKIA、GitHub ghp_/gho_/ghs_、PEM 私钥
pattern='sk-[A-Za-z0-9]{20,}|AKIA[0-9A-Z]{16}|gh[posu]_[A-Za-z0-9]{30,}|-----BEGIN [A-Z ]*PRIVATE KEY-----'

hits="$(grep -rnE "$pattern" \
        --include='*.java' --include='*.ts' --include='*.vue' --include='*.yml' \
        --include='*.yaml' --include='*.properties' --include='*.env' \
        "$REPO_ROOT" 2>/dev/null \
        | grep -vE "$SCAN_EXCLUDE" \
        | grep -vE '/src/test/' \
        | grep -vE 'AKIAIOSFODNN7EXAMPLE' || true)"   # AWS 官方示例假值

if [ -n "$hits" ]; then
  echo "$hits" | sed 's|'"$REPO_ROOT"'/||' | sed 's/^/    /'
  h_bad "疑似真实密钥硬编码 —— 改用环境变量 / Nacos 占位符 \${...}"
  exit 1
fi
h_ok "未发现硬编码密钥"
