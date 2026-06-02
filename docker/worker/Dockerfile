# ─────────────────────────────────────────────────────────────────────────────
# IPED Distributed — Task Agent Worker
#
# This image requires a pre-built IPED distribution because IPED depends on
# native Sleuthkit libraries (libsleuthkit, libewf, etc.) that must be compiled
# for the target platform.  The Maven build is done OUTSIDE Docker (on the build
# host or in a CI/CD pipeline), and the resulting JARs are copied in.
#
# Expected input (relative to build context = repo root):
#   dist/lib/   — all IPED JARs (engine, tasks, parsers, distributed, deps)
#   dist/native/ — optional: native .so files if not available via apt
#
# Build:
#   mvn -B package -DskipTests          # from repo root
#   docker build -f docker/worker/Dockerfile -t iped-worker:latest .
#
# Run (example — HashTask):
#   docker run --rm \
#     -e CASE_ID=my-case-001 \
#     -e TASK_TYPE=HashTask \
#     -e TASK_CLASS=iped.engine.task.HashTask \
#     -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
#     -e COORDINATOR_URL=http://coordinator:8484 \
#     -v /mnt/evidence:/mnt/iped-shared:ro \
#     -v /etc/iped-config:/etc/iped:ro \
#     iped-worker:latest
# ─────────────────────────────────────────────────────────────────────────────

FROM eclipse-temurin:21-jre-jammy

LABEL org.opencontainers.image.title="IPED Distributed Task Agent" \
      org.opencontainers.image.description="Kafka-driven task executor for IPED distributed processing" \
      org.opencontainers.image.source="https://github.com/sepinf-inc/IPED"

# ── System dependencies ───────────────────────────────────────────────────────
# Sleuthkit and forensic image library runtimes
RUN apt-get update && apt-get install -y --no-install-recommends \
        sleuthkit \
        libafflib0v5 \
        libewf2 \
        libvmdk1 \
        libvhdi1 \
        libmagic1 \
        libtsk19 \
        # Useful for diagnostics inside the container
        curl \
        wget \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r iped && useradd -r -g iped -m iped

# ── IPED JARs (pre-built outside Docker) ─────────────────────────────────────
WORKDIR /opt/iped

# Copy the complete JAR set: all iped-* modules + their runtime dependencies.
# Populate dist/lib/ by running:  mvn dependency:copy-dependencies
#   -DoutputDirectory=dist/lib -DincludeScope=runtime
# Then also copy the assembled module JARs there.
COPY dist/lib/ /opt/iped/lib/

# Optional: additional native libraries not available via apt
# COPY dist/native/ /opt/iped/native/
# ENV LD_LIBRARY_PATH=/opt/iped/native:${LD_LIBRARY_PATH}

# ── Runtime directories ───────────────────────────────────────────────────────
# /mnt/iped-shared — shared evidence storage (NFS / GlusterFS mount in prod)
# /etc/iped        — IPED configuration directory (DistributedConfig.txt, etc.)
# /var/log/iped    — worker logs
RUN mkdir -p /mnt/iped-shared /etc/iped /var/log/iped \
    && chown -R iped:iped /mnt/iped-shared /etc/iped /var/log/iped /opt/iped

COPY docker/worker/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# ── Environment defaults ──────────────────────────────────────────────────────
# Required (must be set at runtime):
#   CASE_ID        — case identifier, must match the IPED reader's case ID
#   TASK_TYPE      — simple class name, e.g. HashTask
#   TASK_CLASS     — fully-qualified class name, e.g. iped.engine.task.HashTask
#
# Optional (have sensible defaults):
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
    COORDINATOR_URL=http://coordinator:8484 \
    SHARED_STORAGE_ROOT=/mnt/iped-shared \
    IPED_CONFIG_DIR=/etc/iped \
    AGENT_PARALLELISM=4 \
    ITEM_TIMEOUT_SECONDS=3600 \
    EXACTLY_ONCE=false \
    JAVA_OPTS="-Xmx4g -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -Djava.awt.headless=true"

VOLUME ["/mnt/iped-shared", "/etc/iped"]

USER iped

ENTRYPOINT ["entrypoint.sh"]
