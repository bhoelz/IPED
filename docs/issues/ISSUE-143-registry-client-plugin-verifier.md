# ISSUE-143: RegistryClient, PluginVerifier e SignatureVerifier

- Status: in_progress
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 2 — Cliente de Registry e Verificação
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar `RegistryClient` para fetch do índice e download de artefatos, e `PluginVerifier` para validar assinatura do índice, checksum SHA-256 do artefato, assinatura do artefato e contrato `ServiceLoader`.

## Problem

Para instalar plugins com segurança a partir do GitHub Registry, é necessário um cliente capaz de buscar o índice e os artefatos, e um verificador que confirme integridade (checksum), autenticidade (assinatura) e conformidade de contrato (`ServiceLoader`) antes de qualquer instalação.

## Acceptance criteria

- [x] `RegistryClient` implementado (fetch, download, hash, validação encadeada).
- [x] `PluginVerifier` implementado (assinatura de índice/artefato, SHA-256, contrato `ServiceLoader`).
- [x] `SignatureVerifier` implementado com suporte a `SHA256withRSA` (e modo `none` para dev).
- [ ] Cobertura de cenários de falha (assinatura inválida, checksum incorreto, contrato `ServiceLoader` ausente) em testes dedicados.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 2), status set to `in_progress` (a fase está listada como "Parcialmente Concluída").
