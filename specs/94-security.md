# spec/94 — Security: TLS, SASL Auth, Payload Signing, Chain-of-Custody Audit Trail

**Branch:** `feature/kafka-distributed-processing`
**Packages:** `iped.distributed.security`, `iped.distributed.audit`

---

## Problem

The distributed pipeline carries sensitive forensic evidence data over Kafka.  Without
transport encryption an attacker on the same network can read item metadata, file paths, and
content references.  Without authentication any process on the network can inject fake work
units or impersonate agents.  Without an audit trail there is no forensic record of *which
process processed which evidence item*, which is required by chain-of-custody standards in
many jurisdictions.

---

## Component 1 — Kafka TLS / SASL (`KafkaSecurityConfigurer`)

A single static utility that applies Kafka client security properties to a `java.util.Properties` map.  Called from every Kafka client builder in the module.

### Supported modes

| `kafkaTlsEnabled` | `kafkaSaslMechanism` | `security.protocol` |
|:-----------------:|:--------------------:|:-------------------:|
| false             | (blank)              | *(not set — defaults to PLAINTEXT)* |
| true              | (blank)              | `SSL` |
| false             | PLAIN / SCRAM-*      | `SASL_PLAINTEXT` |
| true              | PLAIN / SCRAM-*      | `SASL_SSL` |

### TLS properties

| Config key | Kafka property |
|-----------|----------------|
| `kafkaTruststorePath` | `ssl.truststore.location` |
| `kafkaTruststorePassword` | `ssl.truststore.password` |
| `kafkaKeystorePath` | `ssl.keystore.location` *(optional — for mTLS)* |
| `kafkaKeystorePassword` | `ssl.keystore.password` |
| `kafkaKeyPassword` | `ssl.key.password` |

Truststore and keystore are both optional.  Omit keystore for one-way TLS; include it for mTLS.

### SASL mechanisms

| Mechanism | JAAS module |
|-----------|-------------|
| `PLAIN` | `PlainLoginModule` |
| `SCRAM-SHA-256` | `ScramLoginModule` |
| `SCRAM-SHA-512` | `ScramLoginModule` |

Credentials are set via `kafkaSaslUsername` / `kafkaSaslPassword`.
Any other value for `kafkaSaslMechanism` throws `IllegalArgumentException` at startup.

---

## Component 2 — Payload Signing (`PayloadSigner`)

HMAC-SHA256 over the immutable structural identity of a `KafkaItemMessage`:

```
iped-v1 | caseId | itemUuid | pipelineStage | path | lengthBytes
```

Fields joined with `|`; null → empty string.  Result stored as lowercase hex in
`KafkaItemMessage.signature`.

### Agent behaviour

| Event | Action |
|-------|--------|
| Forwarding processed item to next stage | `PayloadSigner.sign(updated, secret)` |
| Receiving item from previous stage | `PayloadSigner.verify(msg, secret)` |
| Verify fails | Log ERROR, publish status ERROR event, route to DLQ |
| Signing disabled (`payloadSigningSecret` blank) | Both sign and verify skipped |

### Schema compatibility

`KafkaItemMessage.signature` is `null` by default and is ignored (`@JsonIgnoreProperties`)
on agents running older code that doesn't recognise the field.  During a rolling upgrade,
verification must be disabled (`payloadSigningSecret=""`) until all agents are updated.

---

## Component 3 — Chain-of-Custody Audit Trail (`ProcessingAuditLog`)

### `ProcessingRecord`

Created from `COMPLETED`, `ERROR`, or `TIMEOUT` status events.  All other event types
(DISCOVERED, STARTED, SUBITEM_DISCOVERED, CASE_COMPLETED) are ignored.

Fields: `itemUuid`, `caseId`, `taskType`, `pipelineStage`, `agentId`, `processedAt`,
`durationMs`, `outcome` (COMPLETED / ERROR / TIMEOUT), `errorMessage`.

### `ProcessingAuditLog`

- Thread-safe in-memory accumulator (`ConcurrentHashMap` of `CopyOnWriteArrayList`).
- `forCase(caseId)` — all records for a case.
- `forItem(caseId, itemUuid)` — all records for one item (may span multiple task stages).
- `latestForItem(caseId, itemUuid)` — most-recent terminal record.
- `exportCsv(caseId)` — RFC-4180 CSV with header row; comma/quote escaping.
- Optional file persistence: `ProcessingAuditLog(Path auditLogFile)` — appends to CSV file
  on each record; file is opened in append mode across restarts; thread-safe via `synchronized`.

### `agentId` propagation

`ItemStatusEvent` gains an `agentId` field (non-breaking addition).
`ItemStatusProducer(bootstrapServers, agentId)` — 2-arg constructor stamps every published
event with the producer's agent ID.  `TaskAgent` passes its own UUID so every status event
(STARTED, COMPLETED, ERROR) carries the agent identity.

### REST API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/audit/{caseId}` | All processing records as JSON array |
| GET | `/api/v1/audit/{caseId}/csv` | CSV download (`Content-Disposition: attachment`) |
| GET | `/api/v1/audit/{caseId}/{itemUuid}` | All records for a single item |

### File persistence config

| Key | Default | Description |
|-----|---------|-------------|
| `auditLogPath` | `""` | Path to the persistent CSV audit file; blank = in-memory only |

---

## Configuration summary

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `kafkaTlsEnabled` | boolean | false | Enable TLS for Kafka clients |
| `kafkaTruststorePath` | string | `""` | JKS/PKCS12 truststore path |
| `kafkaTruststorePassword` | string | `""` | Truststore password |
| `kafkaKeystorePath` | string | `""` | Keystore path (mTLS) |
| `kafkaKeystorePassword` | string | `""` | Keystore password |
| `kafkaKeyPassword` | string | `""` | Private key password |
| `kafkaSaslMechanism` | string | `""` | PLAIN / SCRAM-SHA-256 / SCRAM-SHA-512 |
| `kafkaSaslUsername` | string | `""` | SASL username |
| `kafkaSaslPassword` | string | `""` | SASL password |
| `payloadSigningSecret` | string | `""` | HMAC-SHA256 shared secret (blank = disabled) |
| `auditLogPath` | string | `""` | Durable CSV audit log path |

---

## Scope limitations

- **Key rotation** is not automated.  Change `payloadSigningSecret` on a quiesced cluster.
- **SASL_OAUTHBEARER** and **GSSAPI/Kerberos** are not implemented.
- **Coordinator-to-Kafka security** (AdminClient, `TopicProvisioner`, `StatusTopicReplayer`,
  `ConsumerLagProvider`, `ItemStatusConsumer`) is documented here as the wiring pattern but
  is left as a follow-up; the `KafkaSecurityConfigurer.apply()` method is ready to be called
  anywhere that builds a `Properties` map.
- **Keystore format**: JKS and PKCS12 are both supported by the Kafka client; the config only
  stores the path.

---

## Tests (`SecurityTest`, 33 tests)

- `KafkaSecurityConfigurer`: no-op when disabled; SSL-only; TLS with truststore; TLS with
  keystore; SASL PLAIN plaintext; SASL SCRAM over TLS (SASL_SSL); SCRAM-SHA-512;
  unsupported mechanism throws.
- `PayloadSigner`: `isEnabled`; sign produces hex; verify succeeds; wrong secret fails;
  null signature fails; tampered UUID/path/length fail; null fields round-trip; signing
  content is deterministic.
- `ProcessingRecord.from()`: COMPLETED, ERROR, TIMEOUT events; DISCOVERED and STARTED
  return null.
- `ProcessingAuditLog`: accumulates terminal events; ignores non-terminal; empty for unknown
  case; `forItem` filters correctly; multiple cases isolated; CSV header; CSV record content;
  CSV comma escaping; file persistence; file append across instances.
