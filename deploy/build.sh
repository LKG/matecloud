#!/usr/bin/env bash
# =============================================================
# MateCloud Docker 镜像构建脚本
#
# 用法:
#   ./deploy/build.sh [svc|all] [版本标签] [镜像仓库前缀]
#
# 示例:
#   ./deploy/build.sh all                          # 构建全部，tag=latest
#   ./deploy/build.sh mate-auth                    # 构建单个服务
#   ./deploy/build.sh all 1.2.0                    # 指定版本号
#   ./deploy/build.sh all 1.2.0 registry.io/mate   # 构建并推送到仓库
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

TARGET="${1:-all}"
VERSION="${2:-latest}"
REGISTRY="${3:-}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }
step()  { echo -e "\n${CYAN}${BOLD}==> $*${NC}"; }

# mate-admin 已并入 mate-system (RFC-049)
ALL_SERVICES="mate-gateway mate-auth mate-system mate-notice mate-ai"

# 服务名 → Maven 模块路径
get_module() {
  case "$1" in
    mate-gateway) echo "mate-gateway" ;;
    mate-auth)    echo "mate-auth" ;;
    mate-system)  echo "mate-biz/mate-system" ;;
    mate-notice)  echo "mate-biz/mate-notice" ;;
    mate-ai)      echo "mate-biz/mate-ai" ;;
    *) echo "" ;;
  esac
}

# ── 构建单个服务 ──────────────────────────────────────────────
build_service() {
  local svc="$1"
  local module
  module="$(get_module "$svc")"
  [[ -z "$module" ]] && { error "未知服务: $svc"; return 1; }

  local full_tag="${svc}:${VERSION}"
  step "构建 ${svc}  (module: ${module})"

  DOCKER_BUILDKIT=1 docker build \
    --build-arg MODULE_PATH="${module}" \
    -t "${full_tag}" \
    -f Dockerfile \
    .

  info "镜像: ${full_tag}"

  if [[ "$VERSION" != "latest" ]]; then
    docker tag "${full_tag}" "${svc}:latest"
    info "别名: ${svc}:latest"
  fi

  if [[ -n "$REGISTRY" ]]; then
    local registry_tag="${REGISTRY}/${full_tag}"
    docker tag "${full_tag}" "${registry_tag}"
    step "推送 ${registry_tag}..."
    docker push "${registry_tag}"
    [[ "$VERSION" != "latest" ]] && docker push "${REGISTRY}/${svc}:latest"
  fi
}

# ── 构建入口 ──────────────────────────────────────────────────
START_TIME=$(date +%s)
FAILED=""

if [[ "$TARGET" == "all" ]]; then
  step "构建全部服务 (version=${VERSION})"
  for svc in $ALL_SERVICES; do
    build_service "$svc" || FAILED="$FAILED $svc"
  done
elif [[ -n "$(get_module "$TARGET")" ]]; then
  build_service "$TARGET"
else
  error "未知服务: ${TARGET}"
  echo "可选: $ALL_SERVICES all"
  exit 1
fi

ELAPSED=$(( $(date +%s) - START_TIME ))

if [[ -n "$FAILED" ]]; then
  error "以下服务构建失败:$FAILED"
  exit 1
fi

echo ""
info "构建完成  耗时 ${ELAPSED}s"

if [[ "$TARGET" == "all" ]]; then
  echo ""
  echo -e "${BOLD}镜像列表:${NC}"
  for svc in $ALL_SERVICES; do
    echo "  ${svc}:${VERSION}"
  done
  [[ -n "$REGISTRY" ]] && echo "" && info "已推送至: ${REGISTRY}"
fi
