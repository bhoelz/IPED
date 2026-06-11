#!/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
# IPED Task Agent — container entrypoint
#
# Validates required environment variables, writes DistributedConfig.toml into
# the IPED config directory, then starts TaskAgentLauncher.
# ─────────────────────────────────────────────────────────────────────────────
set -e

# ── Validate required variables ───────────────────────────────────────────────
: "${CASE_ID:?CASE_ID is required (e.g. my-case-001)}"
: "${TASK_TYPE:?TASK_TYPE is required (e.g. HashTask)}"
: "${TASK_CLASS:?TASK_CLASS is required (e.g. iped.engine.task.HashTask)}"

# ── Apply defaults for optional variables ─────────────────────────────────────
KAFKA_BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
COORDINATOR="${COORDINATOR_URL:-http://coordinator:8484}"
CONFIG_DIR="${IPED_CONFIG_DIR:-/etc/iped}"
SHARED="${SHARED_STORAGE_ROOT:-/mnt/iped-shared}"
PARALLELISM="${AGENT_PARALLELISM:-4}"
TIMEOUT="${ITEM_TIMEOUT_SECONDS:-3600}"
EXACTLY_ONCE="${EXACTLY_ONCE:-false}"
MAX_RETRIES="${MAX_RETRIES:-3}"
RETRY_BACKOFF="${RETRY_BACKOFF_BASE_SECONDS:-30}"

# ── Generate DistributedConfig.toml ───────────────────────────────────────────
mkdir -p "$CONFIG_DIR"

cat > "$CONFIG_DIR/DistributedConfig.toml" << EOF
enableDistributed = true
kafkaBootstrapServers = "$KAFKA_BOOTSTRAP"
coordinatorServerUrl = "$COORDINATOR"
sharedStorageRoot = "$SHARED"
agentParallelism = $PARALLELISM
itemTimeoutSeconds = $TIMEOUT
exactlyOnce = $EXACTLY_ONCE
maxRetries = $MAX_RETRIES
retryBackoffBaseSeconds = $RETRY_BACKOFF
EOF

echo "──────────────────────────────────────────────"
echo " IPED Task Agent"
echo "  caseId       : $CASE_ID"
echo "  taskType     : $TASK_TYPE"
echo "  taskClass    : $TASK_CLASS"
echo "  kafka        : $KAFKA_BOOTSTRAP"
echo "  coordinator  : $COORDINATOR"
echo "  sharedStorage: $SHARED"
echo "  parallelism  : $PARALLELISM"
echo "──────────────────────────────────────────────"

# ── Wait for Coordinator to be reachable ─────────────────────────────────────
RETRIES=30
until wget -qO- "$COORDINATOR/api/v1/agents" > /dev/null 2>&1; do
    RETRIES=$((RETRIES - 1))
    if [ "$RETRIES" -le 0 ]; then
        echo "ERROR: Coordinator at $COORDINATOR is not reachable. Giving up." >&2
        exit 1
    fi
    echo "Waiting for coordinator ($RETRIES retries left)..."
    sleep 2
done
echo "Coordinator is up."

# ── Launch TaskAgentLauncher ──────────────────────────────────────────────────
# shellcheck disable=SC2086
exec java \
    $JAVA_OPTS \
    -cp '/opt/iped/lib/*' \
    iped.distributed.agent.TaskAgentLauncher \
        --caseId        "$CASE_ID" \
        --taskType      "$TASK_TYPE" \
        --taskClass     "$TASK_CLASS" \
        --kafka         "$KAFKA_BOOTSTRAP" \
        --coordinator   "$COORDINATOR" \
        --config        "$CONFIG_DIR" \
        --parallelism   "$PARALLELISM" \
        --sharedStorage "$SHARED"
