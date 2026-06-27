#!/usr/bin/env bash
# =============================================================
# MateCloud Nacos 配置初始化脚本
#
# 功能:
#   推送基础设施配置到 Nacos 的 public(默认)命名空间。
#     - dev : mate-infra-dev.yml
#     - prod: mate-infra-prod.yml
#
# 说明:
#   - 统一使用默认 public 命名空间，不再自建 dev/prod 命名空间，
#     与 mate-defaults.yml 中 NACOS_NAMESPACE 默认空值保持一致。
#   - Nacos 3.x 已移除 v1/v2 HTTP API，使用 v3 Admin API
#     (POST /nacos/v3/admin/cs/config)。
#   - Admin API 默认需鉴权；通过携带 serverIdentity 身份头放行
#     (对应 docker-compose 中 NACOS_AUTH_IDENTITY_KEY/VALUE)。
#
# 用法:
#   ./deploy/init-nacos.sh [dev|prod|all]   默认: dev
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

[[ -f "${ROOT_DIR}/.env" ]] && { set -a; source "${ROOT_DIR}/.env"; set +a; }

NACOS_HOST="${NACOS_HOST:-localhost}"
NACOS_PORT="${NACOS_PORT:-8848}"
NACOS_USERNAME="${NACOS_USERNAME:-nacos}"
NACOS_PASSWORD="${NACOS_PASSWORD:-ckjia123}"
NACOS_URL="http://${NACOS_HOST}:${NACOS_PORT}"

# Admin API 可信服务器身份头(与 docker-compose 的 NACOS_AUTH_IDENTITY_* 对应)
NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}"
NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-security}"

TEMPLATES_DIR="${ROOT_DIR}/docs/nacos-templates"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }

# ── Nacos Auth Token (开启认证时使用，v3 登录端点) ─────────────
get_auth_token() {
  local resp
  resp=$(curl -sf -X POST "${NACOS_URL}/nacos/v3/auth/user/login" \
    -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}" 2>/dev/null || true)
  if [[ -n "$resp" ]]; then
    echo "$resp" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4 || true
  fi
}

# ── 推送单个配置(Nacos 3.x v3 Admin API → public 命名空间) ─────
push_config() {
  local data_id="$1"
  local file="$2"
  local group="${3:-DEFAULT_GROUP}"

  [[ -f "$file" ]] || { error "模板文件不存在: $file"; return 1; }

  local content
  content=$(cat "$file")

  local body http_code resp
  body=$(curl -s -w "\n%{http_code}" \
    -X POST "${NACOS_URL}/nacos/v3/admin/cs/config" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -H "${NACOS_AUTH_IDENTITY_KEY}: ${NACOS_AUTH_IDENTITY_VALUE}" \
    ${AUTH_PARAM:+-H "accessToken: ${AUTH_PARAM}"} \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "groupName=${group}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=${content}" || true)

  http_code=$(echo "$body" | tail -1)
  resp=$(echo "$body" | head -1)

  # v3 成功返回 {"code":0,"data":true,...}
  if [[ "$http_code" == "200" ]] && echo "$resp" | grep -qE '"data":true|"code":0'; then
    info "  [OK] ${data_id}  →  namespace=public  group=${group}"
  else
    error "  [FAIL] ${data_id}  HTTP ${http_code}: ${resp}"
    return 1
  fi
}

# ── 推送 dev 环境配置 ─────────────────────────────────────────
push_dev() {
  echo ""
  info "== 推送 dev 配置 (public 命名空间) =="
  push_config "mate-infra-dev.yml" "${TEMPLATES_DIR}/mate-infra-dev.yml"
  # 按服务可选 override(文件存在才推),命名规则: <app-name>-<profile>.yml
  [[ -f "${TEMPLATES_DIR}/mate-ai-dev.yml" ]] && \
    push_config "mate-ai-dev.yml" "${TEMPLATES_DIR}/mate-ai-dev.yml"
}

# ── 推送 prod 环境配置 ────────────────────────────────────────
push_prod() {
  echo ""
  info "== 推送 prod 配置 (public 命名空间) =="
  push_config "mate-infra-prod.yml" "${TEMPLATES_DIR}/mate-infra-prod.yml"
  [[ -f "${TEMPLATES_DIR}/mate-ai-prod.yml" ]] && \
    push_config "mate-ai-prod.yml" "${TEMPLATES_DIR}/mate-ai-prod.yml"
}

# ── 主流程 ────────────────────────────────────────────────────
ENV="${1:-dev}"

# 尝试获取认证 Token（认证关闭时返回空，不影响流程）
AUTH_PARAM="$(get_auth_token)"

case "$ENV" in
  dev)  push_dev ;;
  prod) push_prod ;;
  all)  push_dev; push_prod ;;
  *)
    echo "用法: $0 [dev|prod|all]"
    exit 1
    ;;
esac

echo ""
info "Nacos 配置初始化完成"
echo -e "  控制台: \033[0;36mhttp://${NACOS_HOST}:38080/\033[0m  (Nacos 3.x 新控制台端口 8080)"
