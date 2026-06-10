# ─────────────────────────────────────────────────────────────────────────────
# IPED Distributed — Coordinator Service
#
# Multi-stage build:
#   Stage 1 (build)   — Maven + JDK 25 (Alpine); compiles only iped-distributed
#                        and its pure-Java prerequisites (no native libraries).
#   Stage 2 (runtime) — Minimal JRE 25 (Alpine); contains only the fat-JAR.
#
# Runtime size: ~100–150 MB (JRE + fat JAR with Kafka/Jetty/Jersey/Jackson).
#
# Configuration (priority: env var > DistributedConfig.toml > built-in default)
# ─────────────────────────────────────────────────────────────────────────────
#   KAFKA_BOOTSTRAP_SERVERS   kafka:9092
#   COORDINATOR_PORT          8484
#   AGENT_EXPIRY_SECONDS      30
#   TOPIC_PARTITIONS          8
#   TOPIC_REPLICATION_FACTOR  1
#   HEARTBEAT_INTERVAL_SECONDS 10
#   ITEM_TIMEOUT_SECONDS      3600
#
# Mount an optional config file:
#   docker run -v /host/iped-config:/etc/iped:ro iped-coordinator:latest /etc/iped
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
# eclipse-temurin:25-jdk-alpine + Maven (no maven:3.9-eclipse-temurin-25-alpine tag exists)
FROM eclipse-temurin:25-jdk-alpine AS build

RUN apk add --no-cache maven

WORKDIR /build

# ── 1a. Copy POM files first — Docker layer-cache for dependency downloads ──
COPY pom.xml                                              ./pom.xml
COPY iped-api/pom.xml                                     ./iped-api/pom.xml
COPY iped-utils/pom.xml                                   ./iped-utils/pom.xml
COPY iped-engine-parent/pom.xml                           ./iped-engine-parent/pom.xml
COPY iped-engine-parent/iped-engine-core/pom.xml          ./iped-engine-parent/iped-engine-core/pom.xml
COPY iped-engine-parent/iped-distributed/pom.xml          ./iped-engine-parent/iped-distributed/pom.xml

# Pre-resolve dependencies before copying sources (cache-friendly)
RUN mvn -B dependency:resolve \
    -pl iped-engine-parent/iped-distributed --also-make \
    --no-transfer-progress -q 2>/dev/null || true

# ── 1b. Copy only the sources needed for the coordinator ────────────────────
COPY iped-api/src/              ./iped-api/src/
COPY iped-utils/src/            ./iped-utils/src/
COPY iped-engine-parent/iped-engine-core/src/ \
                                ./iped-engine-parent/iped-engine-core/src/
COPY iped-engine-parent/iped-distributed/src/ \
                                ./iped-engine-parent/iped-distributed/src/

# ── 1c. Build the coordinator fat-JAR ───────────────────────────────────────
RUN mvn -B package -DskipTests \
    -pl iped-engine-parent/iped-distributed --also-make \
    --no-transfer-progress

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine

LABEL org.opencontainers.image.title="IPED Distributed Coordinator" \
      org.opencontainers.image.description="REST server for IPED distributed agent registration and case lifecycle" \
      org.opencontainers.image.source="https://github.com/sepinf-inc/IPED"

RUN addgroup -S iped && adduser -S -G iped iped

WORKDIR /app

# Copy fat-JAR (classifier=coordinator, produced by maven-shade-plugin)
COPY --from=build \
    /build/iped-engine-parent/iped-distributed/target/iped-distributed-*-coordinator.jar \
    /app/iped-coordinator.jar

# Optional config directory (mount a DistributedConfig.toml here)
RUN mkdir -p /etc/iped && chown iped:iped /etc/iped

# Coordinator REST port
EXPOSE 8484

# ── Default environment (all overridable at runtime) ─────────────────────────
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    COORDINATOR_PORT=8484 \
    AGENT_EXPIRY_SECONDS=30 \
    TOPIC_PARTITIONS=8 \
    TOPIC_REPLICATION_FACTOR=1 \
    HEARTBEAT_INTERVAL_SECONDS=10 \
    ITEM_TIMEOUT_SECONDS=3600

USER iped

# Pass optional config directory as first argument; env vars always take effect.
ENTRYPOINT ["java", \
    "-Xmx512m", \
    "-XX:+UseContainerSupport", \
    "-Djava.awt.headless=true", \
    "-cp", "/app/iped-coordinator.jar", \
    "iped.distributed.coordinator.CoordinatorServer"]

# Default: no config dir → pure env vars / defaults
# Override: docker run iped-coordinator /etc/iped
CMD []
