# ─────────────────────────────────────────────────────────────────────────────
# IPED Runner — Spring Boot SSR shell + Processing Dashboard
#
# Multi-stage build:
#   Stage 1 (build)   — Maven + JDK 25 (Alpine); builds only iped-runner.
#                        The frontend-maven-plugin downloads Node and runs the
#                        Vite bundle build inside this stage.
#   Stage 2 (runtime) — Minimal JRE 25 (Alpine) with the repackaged boot JAR.
#
# The runner has no IPED engine dependencies. It talks to the distributed
# pipeline only through the iped.status Kafka topic (JSON contract).
#
# Configuration (Spring relaxed binding from environment)
# ─────────────────────────────────────────────────────────────────────────────
#   SERVER_PORT                        8092
#   RUNNER_KAFKA_BOOTSTRAPSERVERS      kafka:9092   (empty = monitor disabled)
#   RUNNER_EXECUTABLE                  iped         (local job launching)
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /build

# Parent POM (resolved via relativePath) + the two runner workspaces
COPY pom.xml          ./pom.xml
COPY iped-runner/     ./iped-runner/
COPY iped-runner-ui/  ./iped-runner-ui/

# -f builds iped-runner standalone (no reactor), so the other repo modules
# don't need to be present in the build context.
RUN mvn -B -f iped-runner/pom.xml package -DskipTests --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.title="IPED Runner" \
      org.opencontainers.image.description="IPED Runner UI and Processing Dashboard (iped.status monitor)" \
      org.opencontainers.image.source="https://github.com/sepinf-inc/IPED"

RUN addgroup -S iped && adduser -S -G iped iped

WORKDIR /app

COPY --from=build /build/iped-runner/target/iped-runner-*.jar /app/iped-runner.jar

EXPOSE 8092

ENV SERVER_PORT=8092 \
    RUNNER_KAFKA_BOOTSTRAPSERVERS=""

USER iped

ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-XX:+UseContainerSupport", \
    "-Djava.awt.headless=true", \
    "-jar", "/app/iped-runner.jar"]
