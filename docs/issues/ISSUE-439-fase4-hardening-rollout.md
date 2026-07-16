# ISSUE-439: Fase 4 — Hardening e Rollout da nova Web UI

- Status: in_progress
- Roadmap: [web-ui-implementation-ROADMAP.md](../roadmaps/web-ui-implementation-ROADMAP.md)
- Roadmap section: Fase 4 - Hardening e Rollout
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Preparar a adoção ampla da nova web UI com segurança operacional: observabilidade completa, testes E2E com dataset de regressão, playbook de rollout/rollback e feature flags por capability.

## Problem

Sem hardening e um plano de rollout/rollback claro, um go-live amplo arrisca incidentes sem diagnóstico reproduzível e sem caminho de reversão seguro.

## Acceptance criteria

- [x] Observabilidade de backend (tracing entre proxy/Jersey/engine) implementada — ver `iped-webui-server-ROADMAP.md`.
- [ ] Observabilidade completa também no frontend (telemetria de UX/performance).
- [ ] Testes E2E com dataset de regressão.
- [ ] Playbook de rollout, rollback e suporte.
- [ ] Feature flags por capability e estratégia de convivência com o legado Swing.
- [ ] Go-live controlado aprovado.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `web-ui-implementation-ROADMAP.md` (Fase 4). Marked `in_progress` — backend observability/CSP/CI gates are already `done` per `iped-webui-server-ROADMAP.md` Phase 3-4, but frontend telemetry, E2E regression suite, and the formal rollout playbook are not yet evidenced.

### 2026-07-16
- Added `iped-webui-server/ROLLOUT-PLAYBOOK.md` with pilot, rollout, rollback, and support gates.
- Remaining work: frontend telemetry, automated E2E dataset execution, and runtime capability flags.
