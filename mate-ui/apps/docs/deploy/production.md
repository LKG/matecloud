# 生产部署

## 安全清单

部署到生产环境前，确保完成以下配置：

| 配置项 | 环境变量 | 说明 |
|--------|---------|------|
| Nacos 密码 | `NACOS_PASSWORD` | 修改默认密码 |
| Sa-Token JWT 密钥 | `SA_TOKEN_JWT_SECRET` | 至少 32 字符，`openssl rand -base64 48` |
| 数据库凭证 | Nacos `mate-infra-prod.yml` | 使用强密码 |
| 应用签名密钥 | `mate_app_key` | 替换内置的 `mate-internal` 密钥 |

## 推送生产配置

```bash
# 编辑生产模板
vi docs/nacos-templates/mate-infra-prod.yml

# 推送到 Nacos prod 命名空间
NACOS_NAMESPACE=prod \
  java -jar mate-cli/target/mate-cli.jar config push mate-infra-prod.yml \
    --file docs/nacos-templates/mate-infra-prod.yml
```

## 启动服务

```bash
SPRING_PROFILES_ACTIVE=prod NACOS_NAMESPACE=prod \
  java -jar mate-biz/mate-system/target/mate-system-1.0.0.jar
```

## 安全默认值

以下安全特性默认开启，**不要随意关闭**：

- **多租户隔离**（`mate.tenant.*`）：启用后，租户 ID 经过白名单验证，行/数据源访问采用 fail-closed 策略
- **数据权限**（`@DataPermission`）：Scope ID 经过校验和引号转义，防止 SQL 注入
- **网关鉴权**：`spring.web.resources.add-mappings=false`，网关认证默认开启

## HTTPS

生产环境必须启用 HTTPS。推荐在网关前使用 Nginx 或负载均衡器做 TLS 终结：

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate     /etc/ssl/certs/api.example.com.pem;
    ssl_certificate_key /etc/ssl/private/api.example.com.key;

    location / {
        proxy_pass http://127.0.0.1:9010;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 前端部署

```bash
cd mate-ui
pnpm build
# 将 apps/admin/dist/ 部署到 CDN 或 Nginx 静态服务
```

Nginx 配置：

```nginx
server {
    listen 80;
    server_name admin.example.com;
    root /var/www/matecloud-admin/dist;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:9010;
    }
}
```
