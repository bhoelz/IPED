# ISSUE-155: Expansão da suíte de testes (integração, assinatura, cache, concorrência)

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Testes — Estado Atual
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Expandir a suíte de testes do plugin registry para cobrir integração com JARs reais de plugin, assinatura válida/inválida (índice e artefato), fallback de cache de índice, e concorrência/atomicidade de instalação — além dos testes unitários iniciais já existentes.

## Problem

Hoje só existem testes unitários iniciais para `RegistrySchemaValidator` e `RegistryClient.sha256`. Faltam testes de integração ponta-a-ponta, testes de cenários de assinatura inválida, testes de fallback de cache, e testes de concorrência/atomicidade — itens críticos para confiar no registry em produção. Nota: a execução local dos testes foi bloqueada por resolução de dependências externas/internas ausentes no ambiente atual.

## Acceptance criteria

- [x] Testes unitários iniciais para `RegistrySchemaValidator` e `RegistryClient.sha256`.
- [ ] Testes de integração com JARs reais de plugin.
- [ ] Testes de assinatura válida/inválida (índice e artefato).
- [ ] Testes de fallback para cache de índice válido.
- [ ] Testes de concorrência e atomicidade de instalação.
- [ ] Resolver bloqueio de dependências externas/internas que impede execução local dos testes.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Testes: Estado Atual), status set to `planned` (fase listada como "Parcialmente Concluído", com a maioria dos itens ainda pendente).
