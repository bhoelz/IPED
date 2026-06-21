# Plugin Registry Roadmap (IPED)

> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Objetivo
Implementar um registro de plugins baseado em GitHub (index + releases) com validação de integridade, assinatura, compatibilidade e carregamento seguro no IPED.

## Estado Atual (Resumo)
- Base SPI de tasks já criada (`iped-tasks.spi`) e loader de providers no engine já implementado.
- Schema formal do `index.json` (Draft 2020-12) já adicionado.
- Esqueleto de `RegistryClient` + `PluginVerifier` já implementado.
- Validações semânticas básicas e testes unitários iniciais já implementados.
- Integração completa com fluxo de instalação/atualização automática ainda pendente.

## Roadmap por Fase

### Fase 0 - Fundação SPI e Loader — `done`
- [ISSUE-140](../issues/ISSUE-140-spi-loader-foundation.md) — Fundação SPI e Loader (`iped-tasks.spi`) — `done`

---

### Fase 1 - Especificação e Modelagem de Registry — `in_progress`
- [ISSUE-141](../issues/ISSUE-141-registry-spec-schema-models.md) — Especificação e modelagem do Registry (index.json + schema + modelos Java) — `done`
- [ISSUE-142](../issues/ISSUE-142-registry-runtime-schema-validation-semver.md) — Validação formal de schema em runtime e regra semver descendente — `planned`

---

### Fase 2 - Cliente de Registry e Verificação — `in_progress`
- [ISSUE-143](../issues/ISSUE-143-registry-client-plugin-verifier.md) — RegistryClient, PluginVerifier e SignatureVerifier — `in_progress`
- [ISSUE-144](../issues/ISSUE-144-json-canonicalization-rfc8785.md) — Canonicalização JSON real (RFC 8785) para verificação de assinatura — `planned`
- [ISSUE-145](../issues/ISSUE-145-multi-signature-format-support.md) — Suporte a múltiplos formatos de assinatura (minisign/cosign) — `planned`
- [ISSUE-146](../issues/ISSUE-146-configurable-security-policy-dev-prod.md) — Política configurável por ambiente (dev/prod) para assinatura de artefato — `planned`

---

### Fase 3 - Integração com Fluxo de Instalação/Atualização — `planned`
- [ISSUE-147](../issues/ISSUE-147-plugin-registry-service-orchestrator.md) — PluginRegistryService — orquestração de instalação e resolução de compatibilidade — `planned`
- [ISSUE-148](../issues/ISSUE-148-atomic-install-lockfile-rollback.md) — Instalação atômica, lockfile local e rollback — `planned`

---

### Fase 4 - Segurança Operacional e Observabilidade — `planned`
- [ISSUE-149](../issues/ISSUE-149-trust-store-key-policy.md) — Trust store real e política de confiança de chaves — `planned`
- [ISSUE-150](../issues/ISSUE-150-structured-logs-audit-telemetry.md) — Logs estruturados, métricas e auditoria de eventos de plugin — `planned`
- [ISSUE-151](../issues/ISSUE-151-offline-index-cache.md) — Cache offline do último índice válido — `planned`

---

### Fase 5 - Publicação e Governança GitHub Registry — `planned`
- [ISSUE-152](../issues/ISSUE-152-plugin-registry-repo-signed-index.md) — Repositório iped-plugin-registry com index.json assinado — `planned`
- [ISSUE-153](../issues/ISSUE-153-registry-ci-contribution-flow.md) — Pipeline CI de validação do registry e fluxo de contribuição — `planned`
- [ISSUE-154](../issues/ISSUE-154-plugin-release-packaging-standard.md) — Padronização de release de plugin (JAR + sig + checksum + SBOM) e guia de publicação — `planned`

## Testes: Estado Atual — `planned`
- [ISSUE-155](../issues/ISSUE-155-plugin-registry-test-suite-expansion.md) — Expansão da suíte de testes (integração, assinatura, cache, concorrência) — `planned`

## Próximos Passos Recomendados (Ordem)
1. Implementar canonicalização RFC 8785 e reforçar verificação de assinatura do índice.
2. Criar `PluginRegistryService` com resolução de compatibilidade e dependências entre plugins.
3. Implementar instalação atômica + lockfile (`installed-plugins.json`) + rollback.
4. Adicionar política de segurança configurável (assinatura obrigatória em produção).
5. Construir suíte de testes de integração com plugins de exemplo publicados via GitHub Releases.
6. Subir repositório de registry e pipeline CI para validação do `index.json`.

## Critérios de Pronto para Produção
- Validação criptográfica do índice ativa e obrigatória.
- Verificação de checksum e assinatura de artefato aplicada conforme política.
- Resolução de compatibilidade e dependências estável.
- Instalação/atualização com rollback seguro.
- Observabilidade mínima: logs estruturados + eventos de auditoria.
- Documentação pública de publicação e operação do registry.
