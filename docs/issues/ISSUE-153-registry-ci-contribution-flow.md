# ISSUE-153: Pipeline CI de validação do registry e fluxo de contribuição

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 5 — Publicação e Governança GitHub Registry
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Definir o fluxo de contribuição (PR, validação CI, assinatura) para o repositório do registry, e construir o pipeline de CI que valida automaticamente o `index.json` e os artefatos publicados.

## Problem

Sem um pipeline CI dedicado, publicações no índice do registry dependeriam de validação manual, criando risco de publicar um índice malformado ou com assinatura inválida.

## Acceptance criteria

- [ ] Definir fluxo de contribuição via PR para o repositório do registry.
- [ ] Implementar pipeline CI que valida o `index.json` contra o schema formal.
- [ ] Pipeline CI valida assinaturas dos artefatos referenciados antes de aceitar a publicação.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 5), status set to `planned` (fase listada como "Não Iniciada").
