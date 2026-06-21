# ISSUE-146: Política configurável por ambiente (dev/prod) para assinatura de artefato

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 2 — Cliente de Registry e Verificação
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar uma política configurável por ambiente (dev/prod) que determine se a assinatura de artefato é obrigatória.

## Problem

Hoje o modo `none` do `SignatureVerifier` existe apenas para desenvolvimento, mas não há uma política formal e configurável que distinga ambientes de produção (onde a assinatura deve ser obrigatória) de ambientes de desenvolvimento (onde pode ser relaxada).

## Acceptance criteria

- [ ] Definir chave de configuração para o modo de ambiente (dev/prod).
- [ ] Em modo prod, rejeitar instalação de plugin sem assinatura de artefato válida.
- [ ] Em modo dev, permitir o modo `none` com aviso explícito nos logs.
- [ ] Testes cobrindo ambos os modos.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 2, pendências), status set to `planned`.
