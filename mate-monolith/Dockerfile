# ============================================================
# MateCloud Monolith image — all business modules in one JVM.
# Build from the REPO ROOT:  docker-compose --profile monolith build
# (the monolith module only exists under the `-Pmonolith` Maven profile).
# ============================================================

# ------------------------------------------------------------
# Stage 1: Maven build
# ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY . .

# -Pmonolith activates the mate-monolith module; -am pulls in its dependencies.
RUN mvn -Pmonolith clean package -pl mate-monolith -am -DskipTests -B --no-transfer-progress

# The monolith jar has no classifier (it IS the runnable app, not a library).
RUN cp "$(ls mate-monolith/target/mate-monolith-*.jar | grep -v -- '-exec' | head -n1)" /build/app.jar

# ------------------------------------------------------------
# Stage 2: Runtime
# ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="MateCloud Team"

RUN addgroup -S mate && adduser -S mate -G mate
RUN sed -i 's#dl-cdn.alpinelinux.org#mirrors.aliyun.com#g' /etc/apk/repositories \
    && apk add --no-cache curl tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app
COPY --from=builder /build/app.jar app.jar
RUN mkdir -p /home/mate/.npm && chown -R mate:mate /app /home/mate
ENV HOME=/home/mate

USER mate

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 9010

# Mode switches (mate.rpc.mode=local, Nacos off) come from mate-infra-local.yml
# on the classpath — no extra env needed. Infra endpoints are overridable below.
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -jar app.jar"]
