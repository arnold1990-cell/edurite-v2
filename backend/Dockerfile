ARG BUILD_MODE=source
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system edurite \
    && useradd --system --gid edurite --home-dir /app --shell /usr/sbin/nologin edurite

RUN chown -R edurite:edurite /app

USER edurite

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

FROM runtime AS source
COPY --from=build --chown=edurite:edurite /app/target/*.jar app.jar

# CI supplies the artifact produced by the successful Maven verification job.
FROM runtime AS prebuilt
COPY --chown=edurite:edurite target/*.jar app.jar

FROM ${BUILD_MODE} AS final
