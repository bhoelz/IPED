# ISSUE-142: Validação formal de schema em runtime e regra semver descendente

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 1 — Especificação e Modelagem de Registry
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Garantir validação formal do `index.json` contra o JSON Schema em tempo de execução e impor a regra de ordenação semver descendente no campo `versions`.

## Problem

Atualmente a validação do índice é feita por código (validação semântica ad-hoc), não pela aplicação formal do schema Draft 2020-12 em runtime. Também não há garantia de que a lista `versions` respeita ordenação semver descendente, o que pode causar resolução de versão incorreta.

## Acceptance criteria

- [ ] Validação formal contra o JSON Schema aplicada em runtime ao consumir o índice.
- [ ] Regra de ordenação semver descendente em `versions` implementada e testada.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 1, pendências), status set to `planned`.
