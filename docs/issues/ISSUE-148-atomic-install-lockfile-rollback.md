# ISSUE-148: Instalação atômica, lockfile local e rollback

- Status: planned
- Roadmap: [iped-engine-plugin-registry-ROADMAP.md](../roadmaps/iped-engine-plugin-registry-ROADMAP.md)
- Roadmap section: Fase 3 — Integração com Fluxo de Instalação/Atualização
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Implementar instalação atômica (temp dir → promote), rollback em caso de falha, e persistência de estado local (`installed-plugins.json`) com versão/hash/fonte de cada plugin instalado.

## Problem

Sem instalação atômica e lockfile, uma falha durante a instalação/atualização de um plugin pode deixar o sistema em estado inconsistente (plugin parcialmente copiado, versão desconhecida instalada), sem caminho de rollback seguro.

## Acceptance criteria

- [ ] Implementar fluxo de instalação atômica via diretório temporário + promote.
- [ ] Implementar rollback automático em caso de falha durante a instalação.
- [ ] Persistir `installed-plugins.json` com versão/hash/fonte de cada plugin.
- [ ] Definir estratégia de atualização automática vs. manual.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-plugin-registry-ROADMAP.md` (Fase 3), status set to `planned` (fase listada como "Não Iniciada").
