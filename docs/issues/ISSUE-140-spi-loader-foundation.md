# ISSUE-140: Fundação SPI e Loader (`iped-tasks.spi`)

- Status: done
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 0 — Fundação SPI e Loader
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Criar o módulo `iped-tasks.spi` com os contratos de task (`TaskProvider`, `TaskDescriptor`, `TaskDependency`), suporte a `ServiceLoader` para providers de task em plugins, ordenação/validação de dependências, e compatibilidade híbrida com `TaskInstaller.xml`.

## Problem

O carregamento de plugins de task precisava de uma base SPI formal, em vez de depender só do `TaskInstaller.xml` legado, para permitir descoberta dinâmica via `ServiceLoader` e validação de dependências entre tasks (ciclo, dependência ausente, opcional).

## Acceptance criteria

- [x] Módulo `iped-tasks/iped-tasks.spi` criado com `TaskProvider`, `TaskDescriptor`, `TaskDependency`.
- [x] Suporte a `ServiceLoader` para providers de task em plugins.
- [x] Ordenação/validação de dependências de tasks (ciclo, dependência ausente, opcional).
- [x] `PluginTaskLoader`, `TaskRegistry`, `ChildFirstClassLoader` implementados em `iped-engine`.
- [x] `TaskInstallerConfig` refatorado para fazer merge entre XML e providers SPI (modo híbrido).
- [ ] Expandir testes de integração para cenários multi-plugin reais.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 0), status set to `done` (a pendência de testes multi-plugin é tratada como item aberto dentro deste issue).
