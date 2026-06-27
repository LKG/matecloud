#!/usr/bin/env bash
# =============================================================
# MateCloud 部署脚本
#
# 用法:
#   ./deploy/deploy.sh <命令> [参数]
#
# 命令:
#   infra              启动基础设施 (MySQL Redis RabbitMQ Nacos MinIO)
#   start              完整启动 (infra + Nacos配置 + 应用服务)
#   stop               停止所有容器
#   restart [svc]      重启指定服务，不指定则重启全部应用服务
#   build   [svc|all]  构建 Docker 镜像
#   push-config [env]  推送 Nacos 配置 (env: dev|prod，默认 dev)
#   logs    [svc]      查看日志
#   status             查看容器状态
#   health             检查各服务健康接口
#   clean              停止并删除所有容器和数据卷 (不可恢复!)
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "${ROOT_DIR}"

# 加载 .env（如果存在）
[[ -f .env ]] && { set -a; source .env; set +a; }

# ── 颜色 ──────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error() { echo -e "${RED}[ERROR]${NC} $*" >&2; }
step()  { echo -e "\n${CYAN}${BOLD}==> $*${NC}"; }
die()   { error "$*"; exit 1; }

# ── 服务定义 ──────────────────────────────────────────────────
INFRA_SERVICES="mysql redis rabbitmq nacos minio"
# mate-admin 已并入 mate-system (RFC-049),不再单独部署。
APP_SERVICES="mate-gateway mate-auth mate-system mate-notice mate-ai"
ALL_APP_SVCS="mate-gateway mate-auth mate-system mate-notice mate-ai"

# 服务名 → Maven 模块路径（替代关联数组）
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

# ── 前置检查 ──────────────────────────────────────────────────
check_deps() {
  local cmd missing=""
  for cmd in docker curl; do
    command -v "$cmd" &>/dev/null || missing="$missing $cmd"
  done
  [[ -n "$missing" ]] && die "缺少依赖:$missing"
  docker info &>/dev/null || die "Docker 未运行，请先启动 Docker Desktop"
}

# ── 等待 Nacos 就绪 ───────────────────────────────────────────
wait_nacos() {
  local host="${NACOS_HOST:-localhost}"
  local port="${NACOS_PORT:-8848}"
  step "等待 Nacos 就绪..."
  local i=0
  until curl -s --connect-timeout 2 "http://${host}:${port}/nacos/" -o /dev/null; do
    i=$((i+1))
    [[ $i -ge 60 ]] && die "Nacos 启动超时，执行 ./deploy/deploy.sh logs nacos 排查"
    echo -n "."
    sleep 3
  done
  echo ""
  info "Nacos 已就绪"
}

# ── 启动基础设施 ──────────────────────────────────────────────
cmd_infra() {
  step "启动基础设施..."
  docker-compose up -d $INFRA_SERVICES
  wait_nacos
  "${SCRIPT_DIR}/init-nacos.sh" dev
  echo ""
  info "基础设施已就绪"
  echo -e "  Nacos    → ${CYAN}http://localhost:8848/nacos${NC}"
  echo -e "  RabbitMQ → ${CYAN}http://localhost:15672${NC}  (guest/guest)"
  echo -e "  MinIO    → ${CYAN}http://localhost:9001${NC}"
}

# ── 完整启动 ──────────────────────────────────────────────────
cmd_start() {
  cmd_infra
  step "启动应用服务..."
  docker-compose up -d $APP_SERVICES
  info "全部启动完成"
  cmd_status
}

# ── 停止 ─────────────────────────────────────────────────────
cmd_stop() {
  step "停止所有服务..."
  docker-compose down
  info "已停止"
}

# ── 重启 ─────────────────────────────────────────────────────
cmd_restart() {
  local svc="${1:-}"
  if [[ -n "$svc" ]]; then
    step "重启 $svc..."
    docker-compose restart "$svc"
  else
    step "重启所有应用服务..."
    docker-compose restart $APP_SERVICES
  fi
  info "完成"
}

# ── 构建镜像 ──────────────────────────────────────────────────
cmd_build() {
  local target="${1:-all}"
  if [[ "$target" == "all" ]]; then
    "${SCRIPT_DIR}/build.sh" all
  elif [[ -n "$(get_module "$target")" ]]; then
    "${SCRIPT_DIR}/build.sh" "$target"
  else
    die "未知服务: $target  可选: $ALL_APP_SVCS all"
  fi
}

# ── 推送 Nacos 配置 ───────────────────────────────────────────
cmd_push_config() {
  local env="${1:-dev}"
  wait_nacos
  "${SCRIPT_DIR}/init-nacos.sh" "$env"
}

# ── 日志 ─────────────────────────────────────────────────────
cmd_logs() {
  local svc="${1:-}"
  if [[ -n "$svc" ]]; then
    docker-compose logs -f --tail=200 "$svc"
  else
    docker-compose logs -f --tail=100
  fi
}

# ── 状态 ─────────────────────────────────────────────────────
cmd_status() {
  echo ""
  echo -e "${BOLD}容器状态:${NC}"
  docker-compose ps
  echo ""
  echo -e "${BOLD}端口速查:${NC}"
  echo "  Gateway   http://localhost:9010"
  echo "  Auth      http://localhost:9020"
  echo "  System    http://localhost:9030"
  echo "  Notice    http://localhost:9050"
  echo "  AI        http://localhost:9060"
  echo "  ────────────────────────────────"
  echo "  Nacos     http://localhost:8848/nacos  (管理端口: 38080)"
  echo "  RabbitMQ  http://localhost:15672   (guest/guest)"
  echo "  MinIO     http://localhost:9001"
  echo "  MySQL     localhost:3306"
  echo "  Redis     localhost:6379"
}

# ── 健康检查 ──────────────────────────────────────────────────
cmd_health() {
  echo ""
  echo -e "${BOLD}服务健康检查:${NC}"

  check_http() {
    local name="$1" url="$2"
    local code
    code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 3 "$url" 2>/dev/null || echo "000")
    if [[ "$code" == "200" ]]; then
      echo -e "  ${GREEN}✓${NC} ${name}  ${url}"
    else
      echo -e "  ${RED}✗${NC} ${name}  HTTP ${code}  ${url}"
    fi
  }

  check_http "mate-gateway" "http://localhost:9010/actuator/health"
  check_http "mate-auth   " "http://localhost:9020/actuator/health"
  check_http "mate-system " "http://localhost:9030/actuator/health"
  check_http "mate-notice " "http://localhost:9050/actuator/health"
  check_http "mate-ai     " "http://localhost:9060/actuator/health"
  echo ""
  check_http "nacos       " "http://localhost:8848/nacos/v1/console/health/liveness"
  check_http "rabbitmq    " "http://localhost:15672/api/healthchecks/node"
  check_http "minio       " "http://localhost:9000/minio/health/live"
}

# ── 清理 ─────────────────────────────────────────────────────
cmd_clean() {
  warn "此操作将删除所有容器和数据卷，数据不可恢复！"
  read -rp "输入 yes 确认: " confirm
  [[ "$confirm" != "yes" ]] && { info "已取消"; return; }
  step "清理中..."
  docker-compose down -v --remove-orphans
  info "清理完成"
}

# ── 帮助 ─────────────────────────────────────────────────────
cmd_help() {
  cat <<'EOF'

MateCloud 部署脚本

用法:  ./deploy/deploy.sh <命令> [参数]

命令:
  infra               启动基础设施 (MySQL Redis RabbitMQ Nacos MinIO) + 初始化 Nacos 配置
  start               完整启动 = infra + 应用服务
  stop                停止所有容器
  restart [svc]       重启服务 (不填则重启全部应用)
  build   [svc|all]   构建 Docker 镜像 (svc: mate-gateway mate-auth mate-system mate-notice mate-ai)
  push-config [env]   推送 Nacos 配置 (env: dev|prod，默认 dev)
  logs    [svc]       查看日志 (不填则查看全部)
  status              查看容器状态和端口列表
  health              检查各服务 /actuator/health
  clean               删除所有容器和数据卷 (不可恢复!)

快速开始:
  # 本地开发 — 只起基础设施，Java 服务 IDE 里跑
  ./deploy/deploy.sh infra

  # 完整 Docker 部署
  ./deploy/deploy.sh build all
  ./deploy/deploy.sh start

EOF
}

# ── 主入口 ────────────────────────────────────────────────────
check_deps

case "${1:-help}" in
  infra)        cmd_infra ;;
  start)        cmd_start ;;
  stop)         cmd_stop ;;
  restart)      cmd_restart "${2:-}" ;;
  build)        cmd_build "${2:-all}" ;;
  push-config)  cmd_push_config "${2:-dev}" ;;
  logs)         cmd_logs "${2:-}" ;;
  status|ps)    cmd_status ;;
  health)       cmd_health ;;
  clean)        cmd_clean ;;
  help|--help|-h|*) cmd_help ;;
esac
