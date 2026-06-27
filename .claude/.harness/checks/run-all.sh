#!/usr/bin/env bash
# MateCloud Harness 入口：跑全部校验。阻断级任一失败 → 退出码 1。
. "$(dirname "$0")/lib.sh"

# 阻断级（当前为零的不变量，违反则提交/CI 失败）
BLOCKING=(check-competitor-names check-secrets check-oss-boundary check-domain-purity check-no-elmessage check-vite-dedupe)
# 告警级（已知技术债，仅提示）
WARNING=(check-mapstruct check-autoconfig check-flyway check-frontend-hardcoded-color)

printf '%sMateCloud Harness · 约束校验%s\n' "$C_DIM" "$C_RST"
printf '%srepo: %s%s\n\n' "$C_DIM" "$REPO_ROOT" "$C_RST"

fail=0

echo "== 阻断级 =="
for c in "${BLOCKING[@]}"; do
  bash "$CHECKS_DIR/$c.sh" || fail=1
done

echo
echo "== 告警级 =="
for c in "${WARNING[@]}"; do
  bash "$CHECKS_DIR/$c.sh" || true
done

echo
if [ "$fail" -ne 0 ]; then
  printf '%s✗ 阻断级校验未通过%s\n' "$C_RED" "$C_RST"
  exit 1
fi
printf '%s✓ 阻断级校验全部通过%s\n' "$C_GRN" "$C_RST"
