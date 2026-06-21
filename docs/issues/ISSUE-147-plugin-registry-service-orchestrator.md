# ISSUE-147: PluginRegistryService — orquestração de instalação e resolução de compatibilidade

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 3 — Integração com Fluxo de Instalação/Atualização
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Integrar o registry ao fluxo de instalação de plugins (não apenas o loader local), implementando resolução de versão compatível (`iped`, `spi`, `java`) e resolução de dependências entre plugins por `id + versionRange`, via um serviço orquestrador (`PluginRegistryService`) no engine.

## Problem

Ainda não há integração fim-a-fim entre o registry e o fluxo de instalação/atualização de plugins; hoje apenas o loader local de plugins existe. Falta um serviço central que resolva compatibilidade e dependências antes de instalar/atualizar.

## Acceptance criteria

- [ ] Criar serviço orquestrador `PluginRegistryService` no engine.
- [ ] Implementar resolução de versão compatível considerando `iped`, `spi` e `java`.
- [ ] Resolver dependências entre plugins por `id + versionRange`.
- [ ] Integrar o serviço ao fluxo de instalação/atualização (substituindo o caminho apenas-local).

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 3), status set to `planned` (fase listada como "Não Iniciada").
