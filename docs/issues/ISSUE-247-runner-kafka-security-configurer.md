# ISSUE-247: KafkaSecurityConfigurer for TLS/SASL client properties

- Status: done
- Roadmap: [iped-runner-ROADMAP.md](../roadmaps/iped-runner-ROADMAP.md)
- Roadmap section: Phase 5 — Kafka security and chain-of-custody audit
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

`KafkaSecurityConfigurer` is a static utility mapping a `KafkaSecurityConfig` record to Kafka client properties, supporting TLS and SASL (PLAIN, SCRAM-SHA-256, SCRAM-SHA-512), and is a no-op (PLAINTEXT) when both are disabled.

## Problem

Distributed Kafka traffic needed configurable transport and authentication security rather than running unauthenticated PLAINTEXT in all environments.

## Acceptance criteria

- [x] Maps `KafkaSecurityConfig` to Kafka client properties for TLS.
- [x] Supports SASL PLAIN, SCRAM-SHA-256, and SCRAM-SHA-512.
- [x] No-op (PLAINTEXT) when both TLS and SASL are disabled.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-runner-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
