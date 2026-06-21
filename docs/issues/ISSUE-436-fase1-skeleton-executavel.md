# ISSUE-436: Fase 1 — Skeleton Executável da nova Web UI

- Status: done
- Roadmap: [web-ui-implementation-ROADMAP.md](../roadmaps/web-ui-implementation-ROADMAP.md)
- Roadmap section: Fase 1 - Skeleton Executável
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Colocar de pé o fluxo fim a fim mínimo: UI -> API -> engine -> resposta, com shell Angular inicial, bootstrap de sessão, busca paginada inicial e abertura de viewer com rendition básica.

## Problem

Sem um esqueleto executável de ponta a ponta, não é possível validar a integração real entre frontend, `iped-webapi` e o engine antes de expandir cobertura funcional.

## Acceptance criteria

- [x] Shell Angular inicial implementado.
- [x] Bootstrap de sessão e abertura de caso funcionando.
- [x] Busca inicial com listagem paginada.
- [x] Seleção de item e painel simples de metadados.
- [x] Abertura de viewer session com rendition básica.
- [x] Usuário consegue abrir um caso, buscar, ver resultados e abrir item em viewer básico.
- [x] Build e testes automatizados executam em CI.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `web-ui-implementation-ROADMAP.md` (Fase 1). Marked `done` — the vertical-slice pilot described here matches the "pilot complete and verified" current state already recorded in `iped-webui-server-ROADMAP.md`.
