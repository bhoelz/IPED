# IPED Distributed Processing — Deployment Guide

This guide covers everything needed to run IPED in distributed Kafka mode: topic setup,
coordinator and agent startup, security hardening, monitoring, dual-run validation, and
production cutover.

---

## Table of Contents

1. [Architecture overview](#1-architecture-overview)
2. [Prerequisites](#2-prerequisites)
3. [Kafka setup](#3-kafka-setup)
   - 3.1 [Broker configuration](#31-broker-configuration)
   - 3.2 [Topic naming convention](#32-topic-naming-convention)
   - 3.3 [Global topics](#33-global-topics)
   - 3.4 [Per-case topics](#34-per-case-topics)
   - 3.5 [Partitioning and replication](#35-partitioning-and-replication)
   - 3.6 [Retention and message size](#36-retention-and-message-size)
4. [Shared storage](#4-shared-storage)
5. [Coordinator startup](#5-coordinator-startup)
6. [Agent (worker) startup](#6-agent-worker-startup)
7. [Security hardening](#7-security-hardening)
   - 7.1 [TLS transport encryption](#71-tls-transport-encryption)
   - 7.2 [SASL authentication](#72-sasl-authentication)
   - 7.3 [Payload signing](#73-payload-signing)
   - 7.4 [Chain-of-custody audit log](#74-chain-of-custody-audit-log)
8. [Monitoring](#8-monitoring)
9. [Docker Compose quick-start](#9-docker-compose-quick-start)
10. [Production rollout checklist](#10-production-rollout-checklist)
    - 10.1 [Pre-flight](#101-pre-flight)
    - 10.2 [Dual-run validation](#102-dual-run-validation)
    - 10.3 [Cutover](#103-cutover)
11. [Reference: DistributedConfig.toml](#11-reference-distributedconfigtoml)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. Architecture overview

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │                        Shared NFS / CIFS mount                      │
 │              (every node sees evidence files at the same path)      │
 └──────────────────────────────────┬──────────────────────────────────┘
                                    │
          ┌─────────────────────────┼───────────────────────────┐
          │                         │                           │
   ┌──────▼──────┐           ┌──────▼──────┐           ┌───────▼──────┐
   │   Reader    │           │ Task Agent  │           │ Task Agent   │
   │  (IPED CLI  │           │ (HashTask)  │           │(ParsingTask) │  …
   │   --case)   │           └──────┬──────┘           └───────┬──────┘
   └──────┬──────┘                  │heartbeat                 │heartbeat
          │publish items            │                          │
          │                  ┌──────▼──────────────────────────▼──────┐
          │                  │        Coordinator Server              │
          │                  │  REST :8484 • schedules • monitors     │
          │                  └──────────────────────┬─────────────────┘
          │                                         │reads iped.status
          ▼                                         ▼
 ┌─────────────────────────────────────────────────────────────────────┐
 │                          Apache Kafka                               │
 │  iped.{caseId}.stage.0 → stage.1 → … → stage.N   iped.status      │
 └─────────────────────────────────────────────────────────────────────┘
```

- **Reader** submits items to `iped.{caseId}.stage.0`.
- Each **Task Agent** subscribes to one stage topic, processes items, and publishes to the
  next stage topic while emitting status events to `iped.status`.
- The **Coordinator** watches heartbeats and the status stream; it schedules work, detects
  timeouts, and exposes the REST API.

---

## 2. Prerequisites

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| Apache Kafka | 3.6 | 4.3 (KRaft, no Zookeeper) |
| Java on every node | 17 | 21 (LTS) |
| Shared storage | NFS v4 | NFS v4 or CIFS with read-write access |
| Coordinator disk | 1 GB | 10 GB (for `coordinatorStateDir` and audit log) |

All **reader nodes** and all **task agent nodes** must be able to reach:
- Kafka brokers on the configured bootstrap port (default 9092).
- The Coordinator REST server on the configured port (default 8484).

---

## 3. Kafka setup

### 3.1 Broker configuration

Set these on every broker. The Docker Compose stack in `docker/docker-compose.yml` sets
equivalent environment variables.

```properties
# Disable auto-creation — the coordinator creates topics explicitly.
auto.create.topics.enable=false

# Pipeline topics carry large forensic payloads.
message.max.bytes=104857600          # 100 MB
replica.fetch.max.bytes=104857600    # match on all brokers

# Log segments and retention (see §3.6).
log.segment.bytes=1073741824         # 1 GB
log.retention.hours=168              # 7 days
```

### 3.2 Topic naming convention

| Pattern | Purpose |
|---------|---------|
| `iped.{caseId}.stage.{N}` | Pipeline input/output for stage N of case `caseId` |
| `iped.{caseId}.stage.{N}.dlq` | Dead-letter queue for stage N items that exhausted retries |
| `iped.status` | Global status event stream (one per cluster) |

`caseId` may contain alphanumeric characters, dashes (`-`), and underscores (`_`).  Avoid
dots — they interact with Kafka's topic-naming rules.

For a case with K tasks, topics `stage.0` through `stage.K` are created (K+1 stage topics).
`stage.0` is the raw input; `stage.K` is the final output consumed by the index-merge step.

### 3.3 Global topics

The following topics are cluster-wide (not per-case) and must exist before the coordinator
starts.  **The coordinator creates `iped.status` automatically** when it provisions the
first case, but you may create it in advance to control its configuration:

```bash
kafka-topics.sh --bootstrap-server localhost:9092 \
  --create \
  --topic iped.status \
  --partitions 4 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config segment.bytes=536870912
```

| Parameter | Recommendation |
|-----------|---------------|
| Partitions | 4 (status events are small; no parallelism needed) |
| Replication | 3 (must survive broker loss without replay gaps) |
| Retention | 7 days (covers coordinator crash recovery window) |

### 3.4 Per-case topics

The coordinator calls `TopicProvisioner.provisionCase()` when a case is started via
`POST /api/v1/cases/start`.  You can also create topics manually before starting the case
(the coordinator will detect they already exist and reuse them):

```bash
CASE_ID=case-2024-001
PARTITIONS=8
REPLICATION=3
TASK_COUNT=5   # number of pipeline task stages

for i in $(seq 0 $TASK_COUNT); do
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --create \
    --topic "iped.${CASE_ID}.stage.${i}" \
    --partitions $PARTITIONS \
    --replication-factor $REPLICATION \
    --config retention.ms=604800000 \
    --config segment.bytes=1073741824
done

# DLQ topics (one per stage):
for i in $(seq 0 $TASK_COUNT); do
  kafka-topics.sh --bootstrap-server localhost:9092 \
    --create \
    --topic "iped.${CASE_ID}.stage.${i}.dlq" \
    --partitions 1 \
    --replication-factor $REPLICATION \
    --config retention.ms=2592000000   # 30 days — operators need time to review
    --config cleanup.policy=delete
done
```

To list topics for a case after it completes:

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --list \
  | grep "^iped.${CASE_ID}"
```

To delete after archiving:

```bash
kafka-topics.sh --bootstrap-server localhost:9092 --delete \
  --topic "iped.${CASE_ID}.stage.*"
```

### 3.5 Partitioning and replication

**Partition count** controls the maximum parallelism for each task type.  A partition can
be consumed by at most one agent at a time per consumer group.

| Cluster size | Partitions per topic |
|:------------:|:--------------------:|
| 1–4 agents | 8 |
| 5–16 agents | 16 |
| 17–64 agents | 32 |

Set `topicPartitions` in `DistributedConfig.toml` to match.  Partitions cannot be reduced
after creation; over-partition rather than under-partition.

**Replication factor**:

| Environment | Replication factor |
|-------------|:-----------------:|
| Development (single broker) | 1 |
| Production (≥ 3 brokers) | 3 |
| Minimum ISR | `replication_factor - 1` |

Set `topicReplicationFactor` in `DistributedConfig.toml` to match.

### 3.6 Retention and message size

Pipeline topics hold large forensic item payloads (metadata + file content references).

| Broker property | Value | Rationale |
|-----------------|-------|-----------|
| `message.max.bytes` | 104857600 (100 MB) | Largest single evidence item chunk |
| `replica.fetch.max.bytes` | 104857600 | Must match `message.max.bytes` |
| `log.retention.hours` | 168 (7 days) | Keep for operator replay after agent crash |
| `log.segment.bytes` | 1073741824 (1 GB) | Avoid many tiny segment files |

For long-running cases (weeks), increase `log.retention.hours` or enable size-based
retention with `log.retention.bytes` per partition.

DLQ topics should use longer retention (30 days minimum) so operators have time to
review, requeue, or discard failed items via the REST API.

---

## 4. Shared storage

Every reader node and every task agent node must mount the same evidence directory at the
**same absolute path**.

```
/mnt/iped-shared/
  cases/
    case-2024-001/
      evidence/
        disk1.E01
        disk2.E01
  output/
    case-2024-001/       ← final IPED output (Lucene index etc.)
```

Set `sharedStorageRoot` in `DistributedConfig.toml` to the mount point (e.g.
`/mnt/iped-shared`).  All paths stored in Kafka messages are relative to this root.

**NFS export example** (on the NFS server):

```
/mnt/evidence  *(rw,sync,no_subtree_check,no_root_squash)
```

**Mount on each node**:

```bash
mount -t nfs4 nfs-server:/mnt/evidence /mnt/iped-shared
```

---

## 5. Coordinator startup

The coordinator is a lightweight HTTP server.  It must start **before** any readers or agents.

```bash
java -Xmx2g \
     -Diped.distributed.config=/etc/iped/DistributedConfig.toml \
     -jar iped-distributed-coordinator.jar
```

Key settings in `DistributedConfig.toml`:

```toml
enableDistributed = true
kafkaBootstrapServers = "broker1:9092,broker2:9092,broker3:9092"
coordinatorPort = 8484
topicPartitions = 8
topicReplicationFactor = 3
itemTimeoutSeconds = 3600

# Durable crash recovery — point to a persistent volume:
coordinatorStateDir = "/var/iped/coordinator-state"

# Chain-of-custody audit log:
auditLogPath = "/var/iped/audit/processing-audit.csv"
```

Verify the coordinator is healthy:

```bash
curl http://coordinator:8484/api/v1/agents
# → {"agents":[]}
```

**Coordinator state directory** (`coordinatorStateDir`): stores `cases.json` with the
ordered task list and priorities for every active case.  This is the only state not
reconstructable from Kafka.  Point this to a durable volume.  On restart, the coordinator
replays `iped.status` to rebuild completion progress.

---

## 6. Agent (worker) startup

Each agent process handles one task type.  Start as many agent processes as needed.

```bash
java -Xmx8g \
     -Diped.distributed.config=/etc/iped/DistributedConfig.toml \
     -Diped.distributed.taskType=HashTask \
     -Diped.distributed.taskClass=iped.engine.task.HashTask \
     -jar iped-distributed-agent.jar
```

| JVM flag | Purpose |
|----------|---------|
| `-Xmx` | Maximum heap — tune per task type (hash: 4 GB, parse: 8 GB, index: 12 GB) |
| `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75` | Kubernetes / container deployments |
| `-Djava.awt.headless=true` | Required for server environments |

Key per-agent settings in `DistributedConfig.toml`:

```toml
agentParallelism = 4       # Worker threads inside this agent
maxRetries = 3             # Retry attempts before DLQ
retryBackoffBaseSeconds = 30
heartbeatIntervalSeconds = 10
agentExpirySeconds = 30

# Backpressure thresholds (agent will stop taking work when exceeded):
backpressureHeapSoftRatio = 0.75
backpressureHeapHardRatio = 0.90
backpressureDiskSoftFreeBytes = 10737418240   # 10 GB
backpressureDiskHardFreeBytes = 5368709120    # 5 GB
```

Agents register automatically with the coordinator via heartbeat.  Verify:

```bash
curl http://coordinator:8484/api/v1/agents
```

---

## 7. Security hardening

### 7.1 TLS transport encryption

Enable TLS to prevent eavesdropping of evidence metadata on the network.

**Broker side** — configure the Kafka broker to use an SSL listener:

```properties
listeners=SSL://:9093
ssl.keystore.location=/etc/kafka/ssl/broker.jks
ssl.keystore.password=broker-ks-pass
ssl.key.password=broker-key-pass
ssl.truststore.location=/etc/kafka/ssl/truststore.jks
ssl.truststore.password=truststore-pass
```

**Client side** — `DistributedConfig.toml`:

```toml
kafkaBootstrapServers = "broker1:9093,broker2:9093,broker3:9093"
kafkaTlsEnabled = true
kafkaTruststorePath = "/etc/iped/ssl/truststore.jks"
kafkaTruststorePassword = "truststore-pass"

# Omit keystore fields for one-way TLS.
# Add them for mTLS (client certificate authentication):
kafkaKeystorePath = "/etc/iped/ssl/client.jks"
kafkaKeystorePassword = "client-ks-pass"
kafkaKeyPassword = "client-key-pass"
```

### 7.2 SASL authentication

Use SASL to authenticate Kafka clients.  Combine with TLS for encrypted credentials.

**Broker side** — enable SCRAM-SHA-256:

```bash
# Create users (Kafka AdminClient or broker CLI):
kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter --entity-type users --entity-name iped-coordinator \
  --add-config 'SCRAM-SHA-256=[password=coord-secret]'

kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter --entity-type users --entity-name iped-agent \
  --add-config 'SCRAM-SHA-256=[password=agent-secret]'
```

**Client side** — `DistributedConfig.toml`:

```toml
kafkaTlsEnabled = true
kafkaSaslMechanism = "SCRAM-SHA-256"
kafkaSaslUsername = "iped-agent"
kafkaSaslPassword = "agent-secret"
```

Supported `kafkaSaslMechanism` values:

| Value | JAAS module | Notes |
|-------|-------------|-------|
| `PLAIN` | `PlainLoginModule` | Sends password in plaintext — **use with TLS only** |
| `SCRAM-SHA-256` | `ScramLoginModule` | Recommended |
| `SCRAM-SHA-512` | `ScramLoginModule` | Strongest; higher CPU cost |

### 7.3 Payload signing

Payload signing uses HMAC-SHA256 to detect tampering of work-unit messages in transit.
The signature covers `caseId`, `itemUuid`, `pipelineStage`, `path`, and `lengthBytes`.

```toml
# A cryptographically random string of ≥ 32 characters:
payloadSigningSecret = "change-me-to-a-random-secret-at-least-32-chars"
```

Generate a secret:

```bash
openssl rand -hex 32
```

**All agents and the coordinator must share the same secret.**  A message that fails
signature verification is routed to the DLQ rather than retried.

**Rolling upgrade procedure** when rotating the secret:
1. Quiesce the cluster — wait for all in-flight items to complete.
2. Deploy the new secret to all nodes simultaneously.
3. Resume processing.

### 7.4 Chain-of-custody audit log

The audit log records every terminal processing event (COMPLETED / ERROR / TIMEOUT) with
the agent ID, timestamp, duration, and outcome.  It is required for forensic chain-of-custody.

```toml
auditLogPath = "/var/iped/audit/processing-audit.csv"
```

The file is opened in append mode across coordinator restarts.  Query via REST:

```bash
# All records for a case:
curl http://coordinator:8484/api/v1/audit/case-2024-001

# Download as CSV for archiving:
curl -O http://coordinator:8484/api/v1/audit/case-2024-001/csv

# Records for a specific item:
curl http://coordinator:8484/api/v1/audit/case-2024-001/{itemUuid}
```

Archive `processing-audit.csv` alongside the IPED case output as part of the evidence
package.

---

## 8. Monitoring

The coordinator exposes a Prometheus metrics endpoint:

```
GET http://coordinator:8484/metrics
```

Response format: `text/plain; version=0.0.4` (Prometheus exposition format).

### Key metrics

| Metric name | Type | Description |
|-------------|------|-------------|
| `iped_items_processed_total` | counter | Items that completed successfully |
| `iped_items_failed_total` | counter | Items that errored (before DLQ) |
| `iped_processing_duration_ms_total` | counter | Cumulative processing time |
| `iped_agent_free_slots` | gauge | Free worker slots per agent |
| `iped_agent_in_flight` | gauge | Items currently in-flight per agent |
| `iped_consumer_lag` | gauge | Kafka consumer lag per topic/partition |
| `iped_pressured_agents` | gauge | Agents reporting HARD backpressure |

### Prometheus scrape config

```yaml
# prometheus.yml
scrape_configs:
  - job_name: iped-coordinator
    static_configs:
      - targets: ['coordinator:8484']
    metrics_path: /metrics
    scrape_interval: 15s
```

### Grafana dashboard (minimal)

Import the following panel queries to monitor a case:

```promql
# Items processed per minute:
rate(iped_items_processed_total[1m])

# Error rate:
rate(iped_items_failed_total[1m])

# Consumer lag (pipeline stall indicator):
iped_consumer_lag

# Agent backpressure events:
iped_pressured_agents > 0
```

### Runner dashboard

The IPED Runner provides a higher-level case progress dashboard aggregated from the
`iped.status` Kafka topic:

```
http://runner:8092/dashboard
```

---

## 9. Docker Compose quick-start

The `docker/docker-compose.yml` stack starts Kafka (KRaft mode), the coordinator, four
worker types (hash, parsing, index, export), and the runner dashboard.

```bash
# Set case ID:
export CASE_ID=case-2024-001

# Start the full stack:
docker compose -f docker/docker-compose.yml up -d

# With Kafka UI (optional — for topic inspection):
docker compose -f docker/docker-compose.yml --profile monitoring up -d

# Scale parsing workers to 4 instances:
docker compose -f docker/docker-compose.yml up -d --scale worker-parsing=4

# View coordinator logs:
docker compose -f docker/docker-compose.yml logs -f coordinator

# Tear down (keep volumes):
docker compose -f docker/docker-compose.yml down
```

Ports exposed to the host:

| Port | Service |
|------|---------|
| 9094 | Kafka (external listener for readers on the Docker host) |
| 8484 | Coordinator REST API |
| 8092 | Runner Dashboard |
| 8080 | Kafka UI (--profile monitoring) |

---

## 10. Production rollout checklist

### 10.1 Pre-flight

- [ ] Kafka cluster is reachable from all coordinator and agent nodes.
- [ ] `iped.status` topic exists with the correct partitions and replication factor.
- [ ] Shared storage mount is readable and writable on every node.
- [ ] `sharedStorageRoot` resolves to the same absolute path on all nodes.
- [ ] Coordinator is running and `/api/v1/agents` returns HTTP 200.
- [ ] At least one agent per task type is registered.
- [ ] `coordinatorStateDir` points to a durable volume with write access.
- [ ] `auditLogPath` points to a durable volume with write access.
- [ ] If `payloadSigningSecret` is set, all agents have the same value.
- [ ] If TLS is enabled, certificates are valid and trusted on all nodes.
- [ ] Prometheus is scraping the coordinator `/metrics` endpoint.
- [ ] Grafana or equivalent is alerting on error rate and consumer lag.

### 10.2 Dual-run validation

Dual-run mode processes the same case through both the distributed pipeline and the
monolithic IPED path simultaneously, then automatically compares results.  Use this as the
exit criterion before decommissioning the monolithic path.

**Step 1 — Enable dual-run for a representative case:**

```toml
# DistributedConfig.toml on the coordinator:
dualRunEnabled = true
```

Or start a session manually for a specific case:

```bash
curl -X POST http://coordinator:8484/api/v1/dualrun/case-2024-001/start
```

**Step 2 — Run the distributed pipeline normally.**  Items discovered by agents are
tracked automatically.

**Step 3 — Run the monolithic IPED pipeline** on the same evidence.  After it completes,
submit its item list as the reference:

```bash
# item-list.json: [{"path":"/evidence/file.E01","mediaType":"application/x-e01","lengthBytes":1234567890}, ...]
curl -X POST http://coordinator:8484/api/v1/dualrun/case-2024-001/reference \
     -H 'Content-Type: application/json' \
     -d @item-list.json
```

**Step 4 — Check the comparison report:**

```bash
curl http://coordinator:8484/api/v1/dualrun/case-2024-001
```

A MATCH report indicates zero missing, extra, or mismatched items.  A MISMATCH or
INCOMPLETE report must be investigated before cutover.

Example MATCH response:

```json
{
  "verdict": "MATCH",
  "distributedCount": 142867,
  "referenceCount": 142867,
  "totalMissing": 0,
  "totalExtra": 0,
  "totalAttributeMismatches": 0,
  "missing": [],
  "extra": [],
  "mismatches": [],
  "distributedComplete": true,
  "summary": "MATCH: 142867 items processed on both sides. No discrepancies."
}
```

### 10.3 Cutover

Once dual-run reports MATCH on all representative cases:

1. **Disable dual-run**: set `dualRunEnabled = false` and restart the coordinator.
2. **Decommission monolithic path**: remove the monolithic IPED invocations from your
   workflows or batch scripts.
3. **Archive audit logs**: copy `processing-audit.csv` to the evidence archive for each
   case before deleting Kafka topics.
4. **Delete completed case topics**:
   ```bash
   curl -X DELETE http://coordinator:8484/api/v1/cases/case-2024-001
   ```
   This triggers `TopicProvisioner.deprovisionCase()` and removes all stage topics.

---

## 11. Reference: DistributedConfig.toml

Complete reference with all supported keys and their defaults.

```toml
# ── Master switch ──────────────────────────────────────────────────────────────
enableDistributed = false

# ── Kafka connectivity ─────────────────────────────────────────────────────────
kafkaBootstrapServers = "localhost:9092"
coordinatorServerUrl = "http://localhost:8484"
sharedStorageRoot = "/mnt/iped-shared"

# ── Topic configuration ────────────────────────────────────────────────────────
topicPartitions = 8
topicReplicationFactor = 1
deadLetterTopicSuffix = ".dlq"

# ── Processing behaviour ───────────────────────────────────────────────────────
agentParallelism = 4
itemTimeoutSeconds = 3600
maxRetries = 3
retryBackoffBaseSeconds = 30
exactlyOnce = false

# ── Coordinator ────────────────────────────────────────────────────────────────
coordinatorPort = 8484
heartbeatIntervalSeconds = 10
agentExpirySeconds = 30
coordinatorStateDir = ""          # blank = in-memory only (lost on restart)

# ── Adaptive work-unit sizing ──────────────────────────────────────────────────
workUnitTargetBytes = 67108864    # 64 MB soft target per work unit
workUnitMaxItems = 256            # hard cap on items per work unit
workUnitOversizedBytes = 134217728  # 128 MB — isolate large items into own unit

# ── Backpressure thresholds ────────────────────────────────────────────────────
backpressureHeapSoftRatio = 0.75
backpressureHeapHardRatio = 0.90
backpressureDiskSoftFreeBytes = 10737418240   # 10 GB
backpressureDiskHardFreeBytes = 5368709120    # 5 GB

# ── Dual-run mode ──────────────────────────────────────────────────────────────
dualRunEnabled = false

# ── Kafka security (TLS) ───────────────────────────────────────────────────────
kafkaTlsEnabled = false
kafkaTruststorePath = ""
kafkaTruststorePassword = ""
kafkaKeystorePath = ""            # Optional — for mTLS client certificates
kafkaKeystorePassword = ""
kafkaKeyPassword = ""

# ── Kafka security (SASL) ─────────────────────────────────────────────────────
kafkaSaslMechanism = ""           # PLAIN | SCRAM-SHA-256 | SCRAM-SHA-512
kafkaSaslUsername = ""
kafkaSaslPassword = ""

# ── Payload signing ────────────────────────────────────────────────────────────
payloadSigningSecret = ""         # blank = disabled; ≥ 32 random chars recommended

# ── Chain-of-custody audit log ─────────────────────────────────────────────────
auditLogPath = ""                 # blank = in-memory only (lost on restart)
```

---

## 12. Troubleshooting

### Agent does not appear in `GET /api/v1/agents`

- Check that the agent can reach the coordinator URL (`coordinatorServerUrl`).
- Check the agent log for connection errors on startup.
- Verify `agentExpirySeconds` is long enough for the agent to send its first heartbeat.

### Consumer lag is growing (pipeline stall)

- Check `GET /metrics` for `iped_pressured_agents > 0` — agents may be under HARD
  backpressure (heap or disk full).
- Check agent logs for `HARD backpressure` warnings.
- Scale up agents for the bottleneck task type.

### Items landing in DLQ

```bash
# List DLQ entries:
curl "http://coordinator:8484/api/v1/dlq/case-2024-001?limit=50"

# Requeue selected positions:
curl -X POST http://coordinator:8484/api/v1/dlq/case-2024-001/requeue \
     -H 'Content-Type: application/json' \
     -d '{"positions":[{"topic":"iped.case-2024-001.stage.2.dlq","partition":0,"offset":5}]}'

# Discard (write off as unprocessable):
curl -X POST http://coordinator:8484/api/v1/dlq/case-2024-001/discard \
     -H 'Content-Type: application/json' \
     -d '{"positions":[...]}'
```

If items consistently fail a specific task type, check the agent logs for the error
stored in the DLQ entry's `extraAttributes.__error` field.

### Payload signature verification failures

- Ensure all agents and the coordinator share **exactly the same** `payloadSigningSecret`.
- During a rolling upgrade, temporarily set `payloadSigningSecret = ""` until all nodes
  are on the new version, then re-enable signing.

### Coordinator crash recovery

On restart, the coordinator:
1. Loads `cases.json` from `coordinatorStateDir` (case definitions and priorities).
2. Replays `iped.status` from the beginning to rebuild completion progress.
3. Begins accepting heartbeats — agents self-register within one `heartbeatIntervalSeconds`.

If `coordinatorStateDir` is empty (in-memory only), any cases that were active at crash
time must be re-started via `POST /api/v1/cases/start`.

### Topic creation fails

- Check that `auto.create.topics.enable=false` is set on the broker (prevents accidental
  topic creation with wrong settings).
- Verify the Kafka user/role has `CREATE` permission on the `iped.*` topic namespace.
- Check for existing topics with the same name but different partition counts:
  ```bash
  kafka-topics.sh --bootstrap-server localhost:9092 --describe \
    --topic "iped.case-2024-001.stage.0"
  ```
