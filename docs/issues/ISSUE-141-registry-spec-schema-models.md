# ISSUE-141: Especificação e modelagem do Registry (index.json + schema + modelos Java)

- Status: done
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 1 — Especificação e Modelagem de Registry
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Definir o formato `index.json` com metadados de plugin/versão/artefato/assinatura/compatibilidade, publicar o JSON Schema Draft 2020-12 formal, e criar os modelos Java de desserialização.

## Problem

Sem um formato de índice formalmente especificado e validável, não é possível construir um cliente de registry confiável nem garantir compatibilidade entre publishers e consumidores do índice de plugins.

## Acceptance criteria

- [x] Schema Draft 2020-12 publicado em `src/main/resources/iped/engine/plugins/registry/index.schema.json`.
- [x] Modelo `RegistryIndex` criado.
- [x] Modelo `RegistryJson` criado.
- [x] Parser/validador `RegistrySchemaValidator` criado.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 1), status set to `done` (MVP).
