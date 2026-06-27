# RFC-043: CI/CD Pipeline with GitHub Actions

- **Status**: Done
- **Created**: 2026-04-12
- **Author**: MateCloud Team
- **Wave**: 9
- **Dependencies**: RFC-038

## 背景

项目没有任何 CI/CD 配置。没有 `.github/` 目录，没有自动化构建，没有自动化测试。每次提交后全靠人工验证编译是否通过。

本 RFC 建立三条 GitHub Actions 工作流：

1. **ci.yml** — PR/push 触发：编译 + 测试
2. **build-images.yml** — tag 触发：构建 Docker 镜像
3. **fe-ci.yml** — 前端变更触发：lint + build

## 设计方案

### Change 1: 后端 CI 工作流

File: `.github/workflows/ci.yml`

```yaml
name: Backend CI

on:
  push:
    branches: [main, develop]
    paths:
      - '**/*.java'
      - '**/*.xml'
      - '**/*.yml'
      - '**/*.yaml'
      - '**/*.properties'
      - '!mate-ui/**'
  pull_request:
    branches: [main]
    paths:
      - '**/*.java'
      - '**/*.xml'
      - '**/*.yml'
      - '**/*.yaml'
      - '**/*.properties'
      - '!mate-ui/**'

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven

      - name: Compile
        run: mvn clean compile -B -q

      - name: Run tests
        run: mvn test -B -q
        continue-on-error: true

      - name: Check compilation result
        run: |
          echo "Build succeeded"
          mvn dependency:tree -B -q -pl mate-gateway | head -5
```

### Change 2: Docker 镜像构建工作流

File: `.github/workflows/build-images.yml`

```yaml
name: Build Docker Images

on:
  push:
    tags:
      - 'v*'

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: ghcr.io/${{ github.repository_owner }}/matecloud

jobs:
  build-services:
    runs-on: ubuntu-latest
    timeout-minutes: 20
    strategy:
      matrix:
        service:
          - { name: mate-gateway, path: mate-gateway }
          - { name: mate-auth, path: mate-auth }
          - { name: mate-admin, path: mate-admin }
          - { name: mate-system, path: mate-biz/mate-system }
          - { name: mate-notice, path: mate-biz/mate-notice }

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven

      - name: Build JAR
        run: mvn clean package -B -q -pl ${{ matrix.service.path }} -am -DskipTests

      - name: Login to GHCR
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract version
        id: version
        run: echo "TAG=${GITHUB_REF#refs/tags/v}" >> "$GITHUB_OUTPUT"

      - name: Build and push image
        uses: docker/build-push-action@v5
        with:
          context: ${{ matrix.service.path }}
          push: true
          tags: |
            ${{ env.IMAGE_PREFIX }}/${{ matrix.service.name }}:${{ steps.version.outputs.TAG }}
            ${{ env.IMAGE_PREFIX }}/${{ matrix.service.name }}:latest
```

### Change 3: 前端 CI 工作流

File: `.github/workflows/fe-ci.yml`

```yaml
name: Frontend CI

on:
  push:
    branches: [main, develop]
    paths:
      - 'mate-ui/**'
  pull_request:
    branches: [main]
    paths:
      - 'mate-ui/**'

defaults:
  run:
    working-directory: mate-ui

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 10

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Setup Node 20
        uses: actions/setup-node@v4
        with:
          node-version: 20

      - name: Setup pnpm
        uses: pnpm/action-setup@v4
        with:
          version: 9

      - name: Install dependencies
        run: pnpm install --frozen-lockfile

      - name: Lint
        run: pnpm lint
        continue-on-error: true

      - name: Type check
        run: pnpm typecheck
        continue-on-error: true

      - name: Build
        run: pnpm build
```

### Change 4: Dependabot 配置

File: `.github/dependabot.yml`

```yaml
version: 2
updates:
  - package-ecosystem: maven
    directory: "/"
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 5
    labels:
      - dependencies
      - java

  - package-ecosystem: npm
    directory: "/mate-ui"
    schedule:
      interval: weekly
      day: monday
    open-pull-requests-limit: 5
    labels:
      - dependencies
      - frontend

  - package-ecosystem: github-actions
    directory: "/"
    schedule:
      interval: monthly
    open-pull-requests-limit: 3
    labels:
      - dependencies
      - ci
```

### Change 5: 各服务 Dockerfile

每个服务需要一个 Dockerfile。使用统一模板。

File: `mate-gateway/Dockerfile`（其他服务同理）

```dockerfile
FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="MateCloud Team"

WORKDIR /app
COPY target/*.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseZGC"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 9010

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
```

各服务的 Dockerfile 仅 `EXPOSE` 端口不同：

| Service | Port |
|---------|------|
| mate-gateway | 9010 |
| mate-auth | 9020 |
| mate-system | 9030 |
| mate-admin | 9040 |
| mate-notice | 9050 |

## 涉及文件

| 文件 | 类型 | 说明 |
|------|------|------|
| `.github/workflows/ci.yml` | New | 后端 CI |
| `.github/workflows/build-images.yml` | New | Docker 镜像构建 |
| `.github/workflows/fe-ci.yml` | New | 前端 CI |
| `.github/dependabot.yml` | New | 依赖更新 |
| `mate-gateway/Dockerfile` | New | Gateway 镜像 |
| `mate-auth/Dockerfile` | New | Auth 镜像 |
| `mate-admin/Dockerfile` | New | Admin 镜像 |
| `mate-biz/mate-system/Dockerfile` | New | System 镜像 |
| `mate-biz/mate-notice/Dockerfile` | New | Notice 镜像 |

## 验证方案

1. Push 代码到 GitHub — ci.yml 应自动触发
2. 检查 Actions 面板中编译步骤为绿色
3. 修改 `mate-ui/` 中的文件并 push — fe-ci.yml 应触发
4. 创建 tag `v1.0.0` 并 push — build-images.yml 应构建并推送 5 个镜像
5. 检查 GHCR 中镜像存在且可拉取

## 注意事项

- **ci.yml 的 paths 过滤**：只有 Java/XML 相关文件变更才触发后端 CI，前端文件走 fe-ci.yml
- **continue-on-error: true**：测试步骤暂时不阻塞 CI（因为当前没有测试用例），后续写了测试后改为 false
- **Docker 镜像用 Alpine + JRE**：最小镜像体积，不包含 JDK
- **GHCR 认证**：使用 `GITHUB_TOKEN` 自动认证，无需额外 secrets
- **Dependabot**：每周检查 Maven 和 npm 依赖更新，自动创建 PR
