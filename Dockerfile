# ============================================================
# Stage 1: Maven Build
# ============================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy full source and build (simpler than pre-copy POMs for a reactor this size)
COPY . .

# Build argument: which module to package
ARG MODULE_PATH=mate-auth

RUN mvn clean package -pl ${MODULE_PATH} -am -DskipTests -B --no-transfer-progress

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="MateCloud Team"

RUN addgroup -S mate && adduser -S mate -G mate
# 换国内 Alpine 镜像源(默认 dl-cdn 在国内极慢), 再装基础包
RUN sed -i 's#dl-cdn.alpinelinux.org#mirrors.aliyun.com#g' /etc/apk/repositories \
    && apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone


WORKDIR /app

ARG MODULE_PATH=mate-auth

COPY --from=builder /build/${MODULE_PATH}/target/*.jar app.jar
# 非 root 用户跑, 需可写 HOME 供 npx/npm 缓存 (~/.npm)
RUN mkdir -p /home/mate/.npm && chown -R mate:mate /app /home/mate
ENV HOME=/home/mate

USER mate

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
