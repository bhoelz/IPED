# ISSUE-144: Canonicalização JSON real (RFC 8785) para verificação de assinatura

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 2 — Cliente de Registry e Verificação
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar canonicalização JSON real conforme RFC 8785, substituindo o placeholder atual em `JsonCanonicalizer`, e reforçar a verificação de assinatura do índice sobre essa representação canônica.

## Problem

`JsonCanonicalizer` hoje é um placeholder, não uma implementação real de RFC 8785. Sem canonicalização correta, a verificação de assinatura do índice pode ser contornada por reformatações triviais do JSON (espaços, ordenação de chaves), comprometendo a garantia de integridade.

## Acceptance criteria

- [ ] Implementar `JsonCanonicalizer` conforme RFC 8785.
- [ ] Reforçar `PluginVerifier`/`SignatureVerifier` para assinar/verificar sobre a forma canônica.
- [ ] Testes cobrindo reformatações equivalentes (ordem de chaves, espaçamento) que devem produzir a mesma assinatura válida.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 2, pendências), status set to `planned`.
