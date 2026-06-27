# syntax=docker/dockerfile:1
# ---------- build stage ----------
FROM node:22-alpine AS build
WORKDIR /app

# pnpm via corepack (version pinned to match packageManager in package.json)
RUN corepack enable && corepack prepare pnpm@10.11.0 --activate

# Copy the whole pnpm monorepo (node_modules/dist excluded via .dockerignore)
COPY . .

RUN pnpm install --frozen-lockfile
RUN pnpm --filter @matecloud/admin build

# ---------- runtime stage ----------
FROM nginx:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/apps/admin/dist /usr/share/nginx/html
EXPOSE 80
# 用 127.0.0.1(而非 localhost):Alpine/musl 下 localhost 可能先解析为 IPv6 ::1，
# 而 nginx `listen 80` 仅绑 IPv4 → wget 连 ::1 被拒 → 误报 unhealthy。
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null 2>&1 || exit 1
