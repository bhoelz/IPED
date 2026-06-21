# ISSUE-145: Suporte a múltiplos formatos de assinatura (minisign/cosign)

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 2 — Cliente de Registry e Verificação
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Adicionar uma camada de compatibilidade para suportar múltiplos formatos de assinatura (ex.: minisign/cosign), além do `SHA256withRSA` já implementado.

## Problem

O `SignatureVerifier` atual só suporta `SHA256withRSA` (e `none` para dev), limitando a interoperabilidade com publishers que já usam ferramentas de assinatura padrão da indústria como minisign ou cosign.

## Acceptance criteria

- [ ] Definir interface de compatibilidade para múltiplos formatos de assinatura.
- [ ] Implementar suporte a minisign.
- [ ] Implementar suporte a cosign (ou compat layer equivalente).
- [ ] Testes de assinatura válida/inválida para cada formato suportado.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 2, pendências), status set to `planned`.
