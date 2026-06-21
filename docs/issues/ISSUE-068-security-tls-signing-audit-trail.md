# ISSUE-068: Security — TLS/auth on Kafka, signed payloads, chain-of-custody audit trail

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 4 — Production rollout (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Secure the distributed pipeline with TLS/SASL on Kafka, HMAC-signed work-unit payloads, and an audit trail of which agent processed which evidence, satisfying the chain-of-custody requirement.

## Problem

A forensics pipeline carries a hard chain-of-custody requirement: every processing step on every piece of evidence must be attributable and tamper-evident, and the Kafka transport itself needed to be securable for production deployment.

## Acceptance criteria

- [x] `iped.distributed.security.KafkaSecurityConfigurer` applies TLS (`ssl.*`) and/or SASL (PLAIN/SCRAM-SHA-256/SCRAM-SHA-512) properties to Kafka `Properties`; wired into `TaskAgent.buildConsumer()`/`buildProducer()`.
- [x] New config keys: `kafkaTlsEnabled`, `kafkaTruststorePath/Password`, `kafkaKeystorePath/Password/Key`, `kafkaSaslMechanism/Username/Password`.
- [x] `PayloadSigner` (HMAC-SHA256 over immutable message fields): `sign()` before forwarding, `verify()` on receive — failure routes to DLQ; enabled via `payloadSigningSecret`; `KafkaItemMessage.signature` field added.
- [x] `iped.distributed.audit.ProcessingRecord` (immutable per-event chain-of-custody snapshot) and `ProcessingAuditLog` (thread-safe accumulator; `forCase`/`forItem`/`latestForItem` queries; `exportCsv()` with RFC-4180 escaping; optional durable file append via `auditLogPath`).
- [x] `ItemStatusEvent` enriched with `agentId`; `ItemStatusProducer(bootstrapServers, agentId)` 2-arg constructor.
- [x] Three coordinator REST endpoints: `GET /api/v1/audit/{caseId}` (JSON), `GET /api/v1/audit/{caseId}/csv` (CSV download), `GET /api/v1/audit/{caseId}/{itemUuid}`.
- [x] 33 broker-free tests in `SecurityTest`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 4), status set to `done`.
