# ISSUE-437: Fase 2 — MVP Operacional da nova Web UI

- Status: in_progress
- Roadmap: [web-ui-implementation-ROADMAP.md](../roadmaps/web-ui-implementation-ROADMAP.md)
- Roadmap section: Fase 2 - MVP Operacional
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Cobrir o núcleo do trabalho analítico (result table completa, facets/filtros, bookmarks, viewers básicos, busca dentro do viewer, jobs de exportação) com estabilidade suficiente para um piloto restrito.

## Problem

O skeleton da Fase 1 prova o fluxo fim a fim, mas ainda não cobre o conjunto de capacidades mínimas que um analista precisa no dia a dia para abandonar o Swing em tarefas básicas.

## Acceptance criteria

- [x] Result table completa com ordenação e seleção múltipla.
- [x] Facets/filtros centrais.
- [ ] Bookmarks essenciais com paridade completa de fluxo.
- [x] Viewers text/html/image/pdf/email básico.
- [ ] Busca dentro do viewer com hits e navegação — parcialmente coberto, ver `iped-webui-ROADMAP.md`.
- [ ] Jobs de exportação simples com polling/status.
- [ ] Piloto restrito viável sem dependência diária da UI Swing para tarefas básicas.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `web-ui-implementation-ROADMAP.md` (Fase 2). Marked `in_progress` — several deliverables (result table, basic viewers, real data wiring) already appear `done` in `iped-webui-ROADMAP.md`/`iped-webui-server-ROADMAP.md`, but export jobs and full bookmark/viewer-search parity are still tracked as open issues there.

### 2026-07-16
- Confirmed server-side export submission/status polling and bookmark/sidebar wiring are present.
- Added client-side viewer search with hit count and previous/next navigation for text and HTML renditions.
- Remaining acceptance work is browser-level validation of bookmark mutations and a restricted pilot dataset.
