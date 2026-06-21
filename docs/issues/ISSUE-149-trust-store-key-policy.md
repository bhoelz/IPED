# ISSUE-149: Trust store real e política de confiança de chaves

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 4 — Segurança Operacional e Observabilidade
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar um trust store real (arquivo assinado, rotação de chave, revogação) e uma política de bloqueio de plugins por revogação/blacklist, a partir da base de interfaces `PublicKeyStore` já existente.

## Problem

A base de interfaces para chave pública (`PublicKeyStore`) já está pronta, mas não há implementação real de um trust store persistente, com suporte a rotação e revogação de chaves, nem um mecanismo de blacklist para bloquear plugins comprometidos.

## Acceptance criteria

- [x] Base de interfaces `PublicKeyStore` definida.
- [ ] Implementar trust store real com persistência em arquivo assinado.
- [ ] Implementar rotação de chave.
- [ ] Implementar bloqueio de plugins por revogação/blacklist.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 4), status set to `planned` (fase listada como "Não Iniciada"; a base de interfaces já implementada é registrada como item concluído).
