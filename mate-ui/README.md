# mate-ui

> MateCloud 前端 — 基于 Vite + Vue 3 + TypeScript + Element Plus 的登录/注册/仪表盘。

## 技术栈

- **Vite 6** + **Vue 3.5** + **TypeScript 5.7**
- **Element Plus** — UI 组件库 + 自动按需引入 (`unplugin-auto-import` + `unplugin-vue-components`)
- **Pinia** — 状态管理
- **Vue Router 4** — 路由 + `beforeEach` 守卫
- **Axios** — HTTP 客户端,拦截器统一处理 `Result<T>` 封装和 401 重定向

## 功能

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录 | `/login` | 账号密码 / 手机验证码双 tab,图形验证码,表单校验 |
| 注册 | `/register` | 用户名+密码+手机+邮箱,图形验证码,注册后自动登录 |
| 仪表盘 | `/dashboard` | 显示当前用户信息、角色、权限 |

路由守卫自动跳转未登录用户到 `/login?redirect=...`。

## 开发

```bash
# 1. 安装依赖
cd mate-ui
npm install           # 或 pnpm install / yarn install

# 2. 启动后端 (另一个终端)
# 确保 mate-gateway (9010) + mate-auth (9020) + mate-system (9030) 都在运行
# 基础设施:
make infra-up
java -jar mate-cli/target/mate-cli.jar config init
make up

# 3. 启动前端
npm run dev           # → http://localhost:3000

# 4. 默认账号 (首次启动 mate-system 时 AdminUserSeeder 会自动创建)
#   用户名: admin
#   密码:   admin123
#   手机:   13800138000
```

Vite dev server 已配置 `/api` 代理到 `http://127.0.0.1:9010` (mate-gateway),
所以前端直接调 `/api/v1/auth/login` 就会路由到后端。

## 生产构建

```bash
npm run build         # → dist/
npm run preview       # 本地预览
```

将 `dist/` 拷贝到任意静态文件服务器 (nginx / mate-gateway 静态资源路由 / CDN) 即可。

## 目录结构

```
mate-ui/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src/
    ├── main.ts                      # 入口,注册 Pinia + Router + Element Plus
    ├── App.vue                      # 根组件
    ├── env.d.ts                     # Vite 类型声明
    ├── api/
    │   ├── http.ts                  # Axios 实例 + 拦截器 + token 本地存储
    │   └── auth.ts                  # authApi (login/register/captcha/info/logout)
    ├── router/
    │   └── index.ts                 # 路由 + 守卫
    ├── stores/
    │   └── auth.ts                  # Pinia 登录状态 store
    └── views/
        ├── LoginView.vue            # 登录页 (密码/短信双 tab)
        ├── RegisterView.vue         # 注册页
        └── DashboardView.vue        # 登录后首页
```

## API 约定

所有接口返回统一封装:

```json
{
  "code": "200",
  "msg": "Operation successful",
  "success": true,
  "data": { ... }
}
```

前端 `http.ts` 的响应拦截器会自动:
1. 如果 `success === false`,弹出错误 toast 并 reject
2. 如果 HTTP 状态 `401`,清除本地 token 并跳转 `/login`
3. 其它情况返回 `response`,由业务代码从 `response.data.data` 取真正数据

登录成功后,`LoginResult.tokenValue` 被存入 `localStorage`
(键: `mate:token`),每次请求自动添加为请求头 (`Authorization` by default,
实际 header 名由后端的 `tokenName` 字段决定)。
