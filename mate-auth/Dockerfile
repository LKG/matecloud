FROM eclipse-temurin:21-jre-alpine
LABEL maintainer="MateCloud Team"

WORKDIR /app
# Runnable fat jar carries the 'exec' classifier (the plain jar is the thin library).
COPY target/*-exec.jar app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseZGC"
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 9020

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
