# ISSUE-439: Fase 4 — Hardening e Rollout da nova Web UI

- Status: in_progress
- Roadmap: [web-ui-implementation-ROADMAP.md](../roadmaps/web-ui-implementation-ROADMAP.md)
- Roadmap section: Fase 4 - Hardening e Rollout
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-07-17

## Summary

Preparar a adoção ampla da nova web UI com segurança operacional: observabilidade completa, testes E2E com dataset de regressão, playbook de rollout/rollback e feature flags por capability.

## Problem

Sem hardening e um plano de rollout/rollback claro, um go-live amplo arrisca incidentes sem diagnóstico reproduzível e sem caminho de reversão seguro.

## Acceptance criteria

- [x] Observabilidade de backend (tracing entre proxy/Jersey/engine) implementada — ver `iped-webui-server-ROADMAP.md`.
- [x] Observabilidade completa também no frontend (telemetria de UX/performance).
- [x] Testes de piloto com fixture de regressão.
- [x] Playbook de rollout, rollback e suporte.
- [x] Feature flags por capability e estratégia de convivência com o legado Swing.
- [ ] Go-live controlado aprovado.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `web-ui-implementation-ROADMAP.md` (Fase 4). Marked `in_progress` — backend observability/CSP/CI gates are already `done` per `iped-webui-server-ROADMAP.md` Phase 3-4, but frontend telemetry, E2E regression suite, and the formal rollout playbook are not yet evidenced.

### 2026-07-16
- Added `iped-webui-server/ROLLOUT-PLAYBOOK.md` with pilot, rollout, rollback, and support gates.
- Added privacy-preserving UX telemetry events from Angular islands, buffered per browser session.
- Added capability endpoint, regression fixture, and go-live checklist. Final go-live approval remains an operator sign-off gate.

### 2026-07-17
- Executed `mvn --% -pl iped-webui-server -am verify` on the release JDK; build and test gates passed.
- Recorded the regression fixture journeys and added an explicit operator sign-off block to
  `iped-webui-server/ROLLOUT-CHECKLIST.md`.
- Status remains `in_progress`: capability review, rollback artifact recording and analyst pilot
  approval require an authorized operator decision and cannot be inferred from automated tests.
