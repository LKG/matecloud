# RFC-023: Docker & Kubernetes Deployment

| Field       | Value                                     |
|-------------|-------------------------------------------|
| **RFC**     | 023                                       |
| **Title**   | Container Deployment Infrastructure       |
| **Status**  | Draft                                     |
| **Created** | 2026-04-11                                |
| **Scope**   | All services                              |

---

## 1. Overview

Provides the complete container deployment infrastructure for MateCloud:

1. **Dockerfile** -- Multi-stage build for each microservice (Maven build + JRE 21 Alpine runtime)
2. **docker-compose.yml** -- Full local development stack with all infrastructure and services
3. **Kubernetes manifests** -- Production-ready Deployment, Service, ConfigMap, Ingress
4. **Makefile** -- Common operations for build, deploy, and management

---

## 2. File Structure

```
matecloud/
  Dockerfile                    <-- Multi-stage, shared across services
  docker-compose.yml            <-- Full local stack
  .env.example                  <-- Required environment variables
  Makefile                      <-- Build/deploy automation
  deploy/
    k8s/
      namespace.yml
      configmap.yml
      mate-gateway/
        deployment.yml
        service.yml
        ingress.yml
      mate-auth/
        deployment.yml
        service.yml
      mate-system/
        deployment.yml
        service.yml
      infrastructure/
        nacos.yml
        mysql.yml
        redis.yml
        rabbitmq.yml
        sentinel.yml
        xxl-job.yml
        minio.yml
```

---

## 3. Dockerfile (Multi-Stage)

A single Dockerfile at the project root, parameterized with build args:

```dockerfile
# ============================================================
# Stage 1: Maven Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root POM and download dependencies first (layer caching)
COPY pom.xml .
COPY mate-common/pom.xml mate-common/pom.xml
COPY mate-common/mate-base/pom.xml mate-common/mate-base/pom.xml
COPY mate-common/mate-api/pom.xml mate-common/mate-api/pom.xml
COPY mate-starters/pom.xml mate-starters/pom.xml
COPY mate-starters/mate-web-starter/pom.xml mate-starters/mate-web-starter/pom.xml
COPY mate-starters/mate-ds-starter/pom.xml mate-starters/mate-ds-starter/pom.xml
COPY mate-starters/mate-nacos-starter/pom.xml mate-starters/mate-nacos-starter/pom.xml
COPY mate-starters/mate-rpc-starter/pom.xml mate-starters/mate-rpc-starter/pom.xml
COPY mate-starters/mate-cache-starter/pom.xml mate-starters/mate-cache-starter/pom.xml
COPY mate-starters/mate-lock-starter/pom.xml mate-starters/mate-lock-starter/pom.xml
COPY mate-starters/mate-sa-token-starter/pom.xml mate-starters/mate-sa-token-starter/pom.xml
COPY mate-starters/mate-mq-starter/pom.xml mate-starters/mate-mq-starter/pom.xml
COPY mate-starters/mate-job-starter/pom.xml mate-starters/mate-job-starter/pom.xml
COPY mate-starters/mate-file-starter/pom.xml mate-starters/mate-file-starter/pom.xml
COPY mate-starters/mate-monitor-starter/pom.xml mate-starters/mate-monitor-starter/pom.xml
COPY mate-starters/mate-doc-starter/pom.xml mate-starters/mate-doc-starter/pom.xml
COPY mate-gateway/pom.xml mate-gateway/pom.xml
COPY mate-auth/pom.xml mate-auth/pom.xml
COPY mate-biz/pom.xml mate-biz/pom.xml
COPY mate-biz/mate-system/pom.xml mate-biz/mate-system/pom.xml
COPY mate-admin/pom.xml mate-admin/pom.xml

RUN mvn dependency:go-offline -B --no-transfer-progress 2>/dev/null || true

# Copy full source and build
COPY . .

# Build argument: which module to package
ARG MODULE=mate-auth
ARG MODULE_PATH=mate-auth

RUN mvn clean package -pl ${MODULE_PATH} -am -DskipTests -B --no-transfer-progress

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="MateCloud Team"

# Add non-root user
RUN addgroup -S mate && adduser -S mate -G mate

# Install curl for health checks
RUN apk add --no-cache curl

WORKDIR /app

ARG MODULE=mate-auth
ARG MODULE_PATH=mate-auth

# Copy the built JAR
COPY --from=builder /build/${MODULE_PATH}/target/*.jar app.jar

# Set ownership
RUN chown -R mate:mate /app

USER mate

# JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Spring profiles
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
```

### 3.1 Per-Service Dockerfile (Alternative Lightweight Approach)

For services that prefer their own Dockerfile in their directory:

```dockerfile
# mate-auth/Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="MateCloud Team"

RUN addgroup -S mate && adduser -S mate -G mate
RUN apk add --no-cache curl

WORKDIR /app
COPY target/*.jar app.jar
RUN chown -R mate:mate /app

USER mate

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 9020

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
```

---

## 4. .env.example

```bash
# ============================================================
# MateCloud Docker Environment Variables
# ============================================================
# Copy this file to .env and fill in the values

# ==================== MySQL ====================
MYSQL_ROOT_PASSWORD=matecloud123
MYSQL_DATABASE=matecloud
MYSQL_USER=matecloud
MYSQL_PASSWORD=matecloud123
MYSQL_HOST=mysql
MYSQL_PORT=3306

# ==================== Redis ====================
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=matecloud123

# ==================== RabbitMQ ====================
RABBITMQ_HOST=rabbitmq
RABBITMQ_PORT=5672
RABBITMQ_USER=matecloud
RABBITMQ_PASS=matecloud123
RABBITMQ_MANAGEMENT_PORT=15672

# ==================== Nacos ====================
NACOS_HOST=nacos
NACOS_PORT=8848
NACOS_SERVER_ADDR=nacos:8848
NACOS_NAMESPACE=dev
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos

# ==================== MinIO ====================
MINIO_HOST=minio
MINIO_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ACCESS_KEY=matecloud
MINIO_SECRET_KEY=matecloud123
MINIO_BUCKET=matecloud

# ==================== Sentinel ====================
SENTINEL_DASHBOARD_HOST=sentinel
SENTINEL_DASHBOARD_PORT=8858

# ==================== XXL-Job ====================
XXL_JOB_ADMIN_HOST=xxl-job-admin
XXL_JOB_ADMIN_PORT=8080
XXL_JOB_ACCESS_TOKEN=matecloud-xxljob-token

# ==================== Service Ports ====================
GATEWAY_PORT=9000
AUTH_PORT=9020
SYSTEM_PORT=9030

# ==================== Spring Profiles ====================
SPRING_PROFILES_ACTIVE=dev

# ==================== JVM ====================
JAVA_OPTS=-Xms256m -Xmx512m
```

---

## 5. docker-compose.yml

```yaml
version: "3.9"

services:
  # ==================== Infrastructure ====================

  mysql:
    image: mysql:8.0
    container_name: mate-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-matecloud123}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-matecloud}
      MYSQL_USER: ${MYSQL_USER:-matecloud}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-matecloud123}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./deploy/sql:/docker-entrypoint-initdb.d
    command: >
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --lower_case_table_names=1
      --max_connections=500
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-matecloud123}"]
      interval: 10s
      timeout: 5s
      retries: 10
    networks:
      - mate-net

  redis:
    image: redis:7-alpine
    container_name: mate-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD:-matecloud123} --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD:-matecloud123}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - mate-net

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: mate-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_USER:-matecloud}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASS:-matecloud123}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 15s
      timeout: 10s
      retries: 5
    networks:
      - mate-net

  nacos:
    image: nacos/nacos-server:v2.4.3
    container_name: mate-nacos
    restart: unless-stopped
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: mysql
      MYSQL_SERVICE_PORT: 3306
      MYSQL_SERVICE_DB_NAME: nacos_config
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: ${MYSQL_ROOT_PASSWORD:-matecloud123}
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: "SecretKey012345678901234567890123456789012345678901234567890123456789"
      NACOS_AUTH_IDENTITY_KEY: serverIdentity
      NACOS_AUTH_IDENTITY_VALUE: security
      JVM_XMS: 256m
      JVM_XMX: 512m
    ports:
      - "8848:8848"
      - "9848:9848"
      - "9849:9849"
    depends_on:
      mysql:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8848/nacos/actuator/health"]
      interval: 15s
      timeout: 10s
      retries: 10
      start_period: 30s
    networks:
      - mate-net

  sentinel:
    image: bladex/sentinel-dashboard:1.8.8
    container_name: mate-sentinel
    restart: unless-stopped
    environment:
      JAVA_OPTS: "-Xms128m -Xmx256m"
    ports:
      - "8858:8858"
    networks:
      - mate-net

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.2
    container_name: mate-xxl-job-admin
    restart: unless-stopped
    environment:
      PARAMS: >
        --spring.datasource.url=jdbc:mysql://mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
        --spring.datasource.username=root
        --spring.datasource.password=${MYSQL_ROOT_PASSWORD:-matecloud123}
        --xxl.job.accessToken=${XXL_JOB_ACCESS_TOKEN:-matecloud-xxljob-token}
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - mate-net

  minio:
    image: minio/minio:latest
    container_name: mate-minio
    restart: unless-stopped
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-matecloud}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-matecloud123}
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data
    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 15s
      timeout: 10s
      retries: 5
    networks:
      - mate-net

  # ==================== MateCloud Services ====================

  mate-gateway:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: mate-gateway
        MODULE_PATH: mate-gateway
    image: matecloud/mate-gateway:latest
    container_name: mate-gateway
    restart: unless-stopped
    ports:
      - "${GATEWAY_PORT:-9000}:9000"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-matecloud123}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    depends_on:
      nacos:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    networks:
      - mate-net

  mate-auth:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: mate-auth
        MODULE_PATH: mate-auth
    image: matecloud/mate-auth:latest
    container_name: mate-auth
    restart: unless-stopped
    ports:
      - "${AUTH_PORT:-9020}:9020"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-matecloud123}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    depends_on:
      nacos:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9020/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    networks:
      - mate-net

  mate-system:
    build:
      context: .
      dockerfile: Dockerfile
      args:
        MODULE: mate-system
        MODULE_PATH: mate-biz/mate-system
    image: matecloud/mate-system:latest
    container_name: mate-system
    restart: unless-stopped
    ports:
      - "${SYSTEM_PORT:-9030}:9030"
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-dev}
      NACOS_SERVER_ADDR: nacos:8848
      NACOS_NAMESPACE: ${NACOS_NAMESPACE:-dev}
      MYSQL_HOST: mysql
      MYSQL_PORT: 3306
      MYSQL_USER: ${MYSQL_USER:-matecloud}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-matecloud123}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_PASSWORD: ${REDIS_PASSWORD:-matecloud123}
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USER: ${RABBITMQ_USER:-matecloud}
      RABBITMQ_PASS: ${RABBITMQ_PASS:-matecloud123}
      JAVA_OPTS: ${JAVA_OPTS:--Xms256m -Xmx512m}
    depends_on:
      nacos:
        condition: service_healthy
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9030/actuator/health"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    networks:
      - mate-net

# ==================== Volumes ====================
volumes:
  mysql-data:
    driver: local
  redis-data:
    driver: local
  rabbitmq-data:
    driver: local
  minio-data:
    driver: local

# ==================== Network ====================
networks:
  mate-net:
    driver: bridge
    name: matecloud
```

---

## 6. Kubernetes Manifests

### 6.1 Namespace

`deploy/k8s/namespace.yml`:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: matecloud
  labels:
    app.kubernetes.io/part-of: matecloud
```

### 6.2 ConfigMap

`deploy/k8s/configmap.yml`:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: matecloud-config
  namespace: matecloud
  labels:
    app.kubernetes.io/part-of: matecloud
data:
  # Nacos
  NACOS_SERVER_ADDR: "nacos.matecloud.svc.cluster.local:8848"
  NACOS_NAMESPACE: "prod"

  # MySQL
  MYSQL_HOST: "mysql.matecloud.svc.cluster.local"
  MYSQL_PORT: "3306"

  # Redis
  REDIS_HOST: "redis.matecloud.svc.cluster.local"
  REDIS_PORT: "6379"

  # RabbitMQ
  RABBITMQ_HOST: "rabbitmq.matecloud.svc.cluster.local"
  RABBITMQ_PORT: "5672"

  # MinIO
  MINIO_HOST: "minio.matecloud.svc.cluster.local"
  MINIO_PORT: "9000"

  # Sentinel
  SENTINEL_DASHBOARD_HOST: "sentinel.matecloud.svc.cluster.local"
  SENTINEL_DASHBOARD_PORT: "8858"

  # XXL-Job
  XXL_JOB_ADMIN_HOST: "xxl-job-admin.matecloud.svc.cluster.local"
  XXL_JOB_ADMIN_PORT: "8080"

  # Spring
  SPRING_PROFILES_ACTIVE: "prod"

  # JVM
  JAVA_OPTS: "-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"
```

### 6.3 Secret

`deploy/k8s/secret.yml`:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: matecloud-secret
  namespace: matecloud
type: Opaque
stringData:
  MYSQL_USER: "matecloud"
  MYSQL_PASSWORD: "changeme-in-production"
  REDIS_PASSWORD: "changeme-in-production"
  RABBITMQ_USER: "matecloud"
  RABBITMQ_PASS: "changeme-in-production"
  MINIO_ACCESS_KEY: "matecloud"
  MINIO_SECRET_KEY: "changeme-in-production"
  XXL_JOB_ACCESS_TOKEN: "changeme-in-production"
```

### 6.4 mate-gateway Deployment + Service + Ingress

`deploy/k8s/mate-gateway/deployment.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mate-gateway
  namespace: matecloud
  labels:
    app: mate-gateway
    app.kubernetes.io/part-of: matecloud
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mate-gateway
  template:
    metadata:
      labels:
        app: mate-gateway
    spec:
      containers:
        - name: mate-gateway
          image: matecloud/mate-gateway:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 9000
              name: http
          envFrom:
            - configMapRef:
                name: matecloud-config
            - secretRef:
                name: matecloud-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9000
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9000
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
      terminationGracePeriodSeconds: 30
```

`deploy/k8s/mate-gateway/service.yml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mate-gateway
  namespace: matecloud
  labels:
    app: mate-gateway
spec:
  type: ClusterIP
  ports:
    - port: 9000
      targetPort: 9000
      protocol: TCP
      name: http
  selector:
    app: mate-gateway
```

`deploy/k8s/mate-gateway/ingress.yml`:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: mate-gateway
  namespace: matecloud
  labels:
    app: mate-gateway
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "60"
    nginx.ingress.kubernetes.io/proxy-send-timeout: "60"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.matecloud.vip
      secretName: matecloud-tls
  rules:
    - host: api.matecloud.vip
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: mate-gateway
                port:
                  number: 9000
```

### 6.5 mate-auth Deployment + Service

`deploy/k8s/mate-auth/deployment.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mate-auth
  namespace: matecloud
  labels:
    app: mate-auth
    app.kubernetes.io/part-of: matecloud
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mate-auth
  template:
    metadata:
      labels:
        app: mate-auth
    spec:
      containers:
        - name: mate-auth
          image: matecloud/mate-auth:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 9020
              name: http
          envFrom:
            - configMapRef:
                name: matecloud-config
            - secretRef:
                name: matecloud-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9020
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9020
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
      terminationGracePeriodSeconds: 30
```

`deploy/k8s/mate-auth/service.yml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mate-auth
  namespace: matecloud
  labels:
    app: mate-auth
spec:
  type: ClusterIP
  ports:
    - port: 9020
      targetPort: 9020
      protocol: TCP
      name: http
  selector:
    app: mate-auth
```

### 6.6 mate-system Deployment + Service

`deploy/k8s/mate-system/deployment.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mate-system
  namespace: matecloud
  labels:
    app: mate-system
    app.kubernetes.io/part-of: matecloud
spec:
  replicas: 2
  selector:
    matchLabels:
      app: mate-system
  template:
    metadata:
      labels:
        app: mate-system
    spec:
      containers:
        - name: mate-system
          image: matecloud/mate-system:latest
          imagePullPolicy: Always
          ports:
            - containerPort: 9030
              name: http
          envFrom:
            - configMapRef:
                name: matecloud-config
            - secretRef:
                name: matecloud-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9030
            initialDelaySeconds: 60
            periodSeconds: 15
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9030
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          lifecycle:
            preStop:
              exec:
                command: ["sh", "-c", "sleep 10"]
      terminationGracePeriodSeconds: 30
```

`deploy/k8s/mate-system/service.yml`:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mate-system
  namespace: matecloud
  labels:
    app: mate-system
spec:
  type: ClusterIP
  ports:
    - port: 9030
      targetPort: 9030
      protocol: TCP
      name: http
  selector:
    app: mate-system
```

---

## 7. Infrastructure K8s Manifests

### 7.1 Redis

`deploy/k8s/infrastructure/redis.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: matecloud
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          command: ["redis-server", "--requirepass", "$(REDIS_PASSWORD)", "--appendonly", "yes"]
          ports:
            - containerPort: 6379
          env:
            - name: REDIS_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: matecloud-secret
                  key: REDIS_PASSWORD
          resources:
            requests:
              memory: "128Mi"
              cpu: "50m"
            limits:
              memory: "256Mi"
              cpu: "200m"
          volumeMounts:
            - name: redis-data
              mountPath: /data
      volumes:
        - name: redis-data
          persistentVolumeClaim:
            claimName: redis-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: redis
  namespace: matecloud
spec:
  type: ClusterIP
  ports:
    - port: 6379
      targetPort: 6379
  selector:
    app: redis
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: matecloud
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

### 7.2 MySQL

`deploy/k8s/infrastructure/mysql.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mysql
  namespace: matecloud
spec:
  replicas: 1
  selector:
    matchLabels:
      app: mysql
  strategy:
    type: Recreate
  template:
    metadata:
      labels:
        app: mysql
    spec:
      containers:
        - name: mysql
          image: mysql:8.0
          ports:
            - containerPort: 3306
          env:
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: matecloud-secret
                  key: MYSQL_PASSWORD
            - name: MYSQL_DATABASE
              value: matecloud
          args:
            - "--default-authentication-plugin=mysql_native_password"
            - "--character-set-server=utf8mb4"
            - "--collation-server=utf8mb4_unicode_ci"
            - "--lower_case_table_names=1"
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "1Gi"
              cpu: "500m"
          volumeMounts:
            - name: mysql-data
              mountPath: /var/lib/mysql
          livenessProbe:
            exec:
              command: ["mysqladmin", "ping", "-h", "localhost"]
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command: ["mysql", "-h", "localhost", "-e", "SELECT 1"]
            initialDelaySeconds: 15
            periodSeconds: 5
      volumes:
        - name: mysql-data
          persistentVolumeClaim:
            claimName: mysql-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: mysql
  namespace: matecloud
spec:
  type: ClusterIP
  ports:
    - port: 3306
      targetPort: 3306
  selector:
    app: mysql
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mysql-pvc
  namespace: matecloud
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 20Gi
```

### 7.3 RabbitMQ

`deploy/k8s/infrastructure/rabbitmq.yml`:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rabbitmq
  namespace: matecloud
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
        - name: rabbitmq
          image: rabbitmq:3.13-management-alpine
          ports:
            - containerPort: 5672
              name: amqp
            - containerPort: 15672
              name: management
          env:
            - name: RABBITMQ_DEFAULT_USER
              valueFrom:
                secretKeyRef:
                  name: matecloud-secret
                  key: RABBITMQ_USER
            - name: RABBITMQ_DEFAULT_PASS
              valueFrom:
                secretKeyRef:
                  name: matecloud-secret
                  key: RABBITMQ_PASS
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          volumeMounts:
            - name: rabbitmq-data
              mountPath: /var/lib/rabbitmq
      volumes:
        - name: rabbitmq-data
          persistentVolumeClaim:
            claimName: rabbitmq-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
  namespace: matecloud
spec:
  type: ClusterIP
  ports:
    - port: 5672
      targetPort: 5672
      name: amqp
    - port: 15672
      targetPort: 15672
      name: management
  selector:
    app: rabbitmq
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: rabbitmq-pvc
  namespace: matecloud
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

---

## 8. Makefile

```makefile
# ============================================================
# MateCloud Makefile
# ============================================================

.PHONY: help build build-all up down restart logs clean \
        build-gateway build-auth build-system \
        k8s-apply k8s-delete k8s-status

# Default target
help:
	@echo "MateCloud Build & Deploy Commands"
	@echo "================================="
	@echo ""
	@echo "Local (Docker Compose):"
	@echo "  make build           - Build all service images"
	@echo "  make build-<service> - Build a specific service (gateway, auth, system)"
	@echo "  make up              - Start full local stack"
	@echo "  make up-infra        - Start infrastructure only (mysql, redis, etc.)"
	@echo "  make down            - Stop all containers"
	@echo "  make restart SERVICE - Restart a specific service"
	@echo "  make logs SERVICE    - Tail logs for a service"
	@echo "  make clean           - Remove all containers and volumes"
	@echo ""
	@echo "Kubernetes:"
	@echo "  make k8s-apply       - Apply all K8s manifests"
	@echo "  make k8s-delete      - Delete all K8s resources"
	@echo "  make k8s-status      - Show status of all pods"
	@echo ""
	@echo "Maven:"
	@echo "  make mvn-build       - Maven clean package (skip tests)"
	@echo "  make mvn-test        - Maven verify with tests"

# ==================== Maven ====================

mvn-build:
	mvn clean package -DskipTests -B --no-transfer-progress

mvn-test:
	mvn clean verify -B --no-transfer-progress

# ==================== Docker Build ====================

build: mvn-build build-gateway build-auth build-system
	@echo "All images built successfully"

build-gateway:
	docker build -t matecloud/mate-gateway:latest \
		--build-arg MODULE=mate-gateway \
		--build-arg MODULE_PATH=mate-gateway .

build-auth:
	docker build -t matecloud/mate-auth:latest \
		--build-arg MODULE=mate-auth \
		--build-arg MODULE_PATH=mate-auth .

build-system:
	docker build -t matecloud/mate-system:latest \
		--build-arg MODULE=mate-system \
		--build-arg MODULE_PATH=mate-biz/mate-system .

# ==================== Docker Compose ====================

up:
	docker-compose up -d
	@echo "Stack started. Use 'make logs mate-gateway' to view logs."

up-infra:
	docker-compose up -d mysql redis rabbitmq nacos sentinel xxl-job-admin minio
	@echo "Infrastructure started. Waiting for services to be healthy..."

down:
	docker-compose down

restart:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make restart SERVICE=mate-auth"; exit 1; fi
	docker-compose restart $(SERVICE)

logs:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make logs SERVICE=mate-auth"; exit 1; fi
	docker-compose logs -f --tail=200 $(SERVICE)

clean:
	docker-compose down -v --rmi local
	@echo "All containers, volumes, and local images removed"

# ==================== Kubernetes ====================

k8s-apply:
	kubectl apply -f deploy/k8s/namespace.yml
	kubectl apply -f deploy/k8s/secret.yml
	kubectl apply -f deploy/k8s/configmap.yml
	kubectl apply -f deploy/k8s/infrastructure/
	@echo "Waiting for infrastructure pods..."
	kubectl wait --for=condition=ready pod -l app=mysql -n matecloud --timeout=120s
	kubectl wait --for=condition=ready pod -l app=redis -n matecloud --timeout=60s
	kubectl apply -f deploy/k8s/mate-gateway/
	kubectl apply -f deploy/k8s/mate-auth/
	kubectl apply -f deploy/k8s/mate-system/
	@echo "All K8s manifests applied"

k8s-delete:
	kubectl delete namespace matecloud --ignore-not-found

k8s-status:
	kubectl get all -n matecloud

k8s-logs:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make k8s-logs SERVICE=mate-auth"; exit 1; fi
	kubectl logs -f -l app=$(SERVICE) -n matecloud --tail=200

k8s-restart:
	@if [ -z "$(SERVICE)" ]; then echo "Usage: make k8s-restart SERVICE=mate-auth"; exit 1; fi
	kubectl rollout restart deployment/$(SERVICE) -n matecloud
```

---

## 9. Actuator Health Configuration

Each service must expose health endpoints for container probes. Add to each service's bootstrap.yml or Nacos config:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
    # Check downstream dependencies for readiness
    redis:
      enabled: true
    db:
      enabled: true
    rabbit:
      enabled: true
```

---

## 10. Quick Start Guide

### 10.1 First-Time Setup

```bash
# 1. Clone and enter project
cd matecloud

# 2. Copy environment file
cp .env.example .env
# Edit .env as needed

# 3. Start infrastructure
make up-infra

# 4. Wait for infrastructure to be healthy (check with docker-compose ps)
docker-compose ps

# 5. Initialize databases
# (SQL init scripts in deploy/sql/ are auto-executed by MySQL container)

# 6. Build and start all services
make build
make up

# 7. Verify
curl http://localhost:9000/actuator/health  # Gateway
curl http://localhost:9020/actuator/health  # Auth
curl http://localhost:9030/actuator/health  # System
```

### 10.2 Day-to-Day Development

```bash
# Rebuild and restart a single service
make build-auth
make restart SERVICE=mate-auth
make logs SERVICE=mate-auth

# View all logs
docker-compose logs -f

# Stop everything
make down
```

---

## 11. Testing Checklist

- [ ] `make up-infra` starts MySQL, Redis, RabbitMQ, Nacos, Sentinel, XXL-Job, MinIO
- [ ] All infrastructure containers reach healthy state
- [ ] `make build` builds all service Docker images
- [ ] `make up` starts all services and they register with Nacos
- [ ] Each service responds to `/actuator/health` with UP
- [ ] `make logs SERVICE=mate-auth` shows live log output
- [ ] `make down` stops all containers cleanly
- [ ] `make clean` removes volumes and images
- [ ] K8s: `make k8s-apply` creates namespace and all resources
- [ ] K8s: pods reach Ready state with liveness/readiness probes passing
- [ ] K8s: Ingress routes traffic to gateway
- [ ] K8s: `make k8s-delete` cleans up all resources
