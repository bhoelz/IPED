# ISSUE-438: Fase 3 — Paridade Prioritária com o Swing

- Status: planned
- Roadmap: [web-ui-implementation-ROADMAP.md](../roadmaps/web-ui-implementation-ROADMAP.md)
- Roadmap section: Fase 3 - Paridade Prioritária
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Fechar as principais lacunas de usabilidade da web UI em relação à aplicação Swing: painéis equivalentes, persistência de layout/preferências, navegação/atalhos e um fallback de viewer mais robusto com bridge para o companion app.

## Problem

O MVP operacional cobre o núcleo analítico, mas a maioria dos fluxos recorrentes de análise ainda depende de paridade de usabilidade (atalhos, layout, ações em lote) para que analistas migrem de fato do Swing.

## Acceptance criteria

- [ ] Painéis equivalentes para categorias, bookmarks, filtros e relacionamentos.
- [ ] Persistência de layout e preferências principais.
- [ ] Melhorias de navegação, atalhos prioritários e ações em lote.
- [ ] Viewer fallback mais robusto com integração inicial ao companion app para casos bloqueados.
- [ ] Gap remanescente documentado por capability com plano de fechamento.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `web-ui-implementation-ROADMAP.md` (Fase 3), status `planned` — no evidence found yet in `iped-webui-ROADMAP.md`/`iped-app-ROADMAP.md` that this phase's deliverables (companion app bridge, full layout persistence) are underway.

### 2026-07-16
- Added `docs/web-ui-capability-gap-matrix.md` as the baseline for parity implementation and acceptance tracking.
