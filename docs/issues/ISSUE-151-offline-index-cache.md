# ISSUE-151: Cache offline do último índice válido

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 4 — Segurança Operacional e Observabilidade
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar um cache local do último `index.json` válido para permitir operação offline ou degradação graciosa quando o GitHub Registry estiver inacessível.

## Problem

Sem cache offline, qualquer indisponibilidade do registry remoto bloquearia completamente operações de listagem/instalação de plugins, mesmo quando um índice válido já foi obtido anteriormente.

## Acceptance criteria

- [ ] Persistir o último índice validado com sucesso em cache local.
- [ ] Servir o índice em cache quando o fetch remoto falhar.
- [ ] Testes de fallback para cache de índice válido.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 4), status set to `planned` (fase listada como "Não Iniciada").
