# Plugin Registry Roadmap (IPED)

## Objetivo
Implementar um registro de plugins baseado em GitHub (index + releases) com validação de integridade, assinatura, compatibilidade e carregamento seguro no IPED.

## Estado Atual (Resumo)
- Base SPI de tasks já criada (`iped-tasks.spi`) e loader de providers no engine já implementado.
- Schema formal do `index.json` (Draft 2020-12) já adicionado.
- Esqueleto de `RegistryClient` + `PluginVerifier` já implementado.
- Validações semânticas básicas e testes unitários iniciais já implementados.
- Integração completa com fluxo de instalação/atualização automática ainda pendente.

## Roadmap por Fase

### Fase 0 - Fundação SPI e Loader
Status: **Concluída**

Itens:
- Criar módulo `iped-tasks.spi` com contratos (`TaskProvider`, `TaskDescriptor`, `TaskDependency`).
- Adicionar suporte a `ServiceLoader` para providers de task em plugins.
- Implementar ordenação/validação de dependências de tasks (ciclo, dependência ausente, opcional).
- Manter compatibilidade com `TaskInstaller.xml` (modo híbrido XML + SPI).

Implementado:
- `iped-tasks/iped-tasks.spi/*` criado.
- `iped-engine` com `PluginTaskLoader`, `TaskRegistry`, `ChildFirstClassLoader`.
- `TaskInstallerConfig` refatorado para merge XML + providers SPI.

Pendências:
- Expandir testes de integração para cenários multi-plugin reais.

---

### Fase 1 - Especificação e Modelagem de Registry
Status: **Concluída (MVP)**

Itens:
- Definir `index.json` com metadados de plugin/versão/artefato/assinatura/compatibilidade.
- Publicar JSON Schema Draft 2020-12 formal.
- Criar modelos Java para desserialização do índice.

Implementado:
- Schema: `src/main/resources/iped/engine/plugins/registry/index.schema.json`.
- Modelos e parser:
  - `RegistryIndex`
  - `RegistryJson`
  - `RegistrySchemaValidator`

Pendências:
- Validação formal contra schema em runtime (atualmente há validação semântica por código).
- Garantir regra semver descendente em `versions`.

---

### Fase 2 - Cliente de Registry e Verificação
Status: **Parcialmente Concluída**

Itens:
- Implementar `RegistryClient` para fetch do índice e download de artefatos.
- Implementar `PluginVerifier` para:
  - assinatura do índice,
  - checksum SHA-256 do artefato,
  - assinatura do artefato,
  - contrato `ServiceLoader`.

Implementado:
- `RegistryClient` (fetch, download, hash, validação encadeada).
- `PluginVerifier` (assinatura índice/artefato, SHA-256, ServiceLoader contract).
- `SignatureVerifier` com suporte atual a `SHA256withRSA` (e `none` para dev).

Pendências:
- Canonicalização JSON real RFC 8785 (hoje placeholder em `JsonCanonicalizer`).
- Suporte a múltiplos formatos de assinatura (ex.: minisign/cosign compat layer).
- Política configurável por ambiente (dev/prod) para exigir assinatura de artefato.

---

### Fase 3 - Integração com Fluxo de Instalação/Atualização
Status: **Não Iniciada**

Itens:
- Integrar registry ao fluxo de instalação de plugins (não apenas loader local).
- Implementar resolução de versão compatível (`iped`, `spi`, `java`).
- Resolver dependências entre plugins por `id + versionRange`.
- Instalação atômica (temp dir -> promote), rollback e lockfile local.

Implementado:
- Nenhuma integração fim-a-fim ainda.

Pendências:
- Criar serviço orquestrador (ex.: `PluginRegistryService`) no engine.
- Persistir estado local (`installed-plugins.json`) com versão/hash/fonte.
- Definir estratégia de atualização automática/manual.

---

### Fase 4 - Segurança Operacional e Observabilidade
Status: **Não Iniciada**

Itens:
- Política de confiança de chaves (registry/publishers).
- Bloqueio de plugins por revogação/blacklist.
- Logs estruturados e métricas de validação/instalação.
- Cache offline do último índice válido.

Implementado:
- Base de interfaces para chave pública (`PublicKeyStore`) pronta.

Pendências:
- Implementar trust store real (arquivo assinado, rotação de chave, revogação).
- Telemetria e auditoria de eventos de plugin.

---

### Fase 5 - Publicação e Governança GitHub Registry
Status: **Não Iniciada**

Itens:
- Criar repositório `iped-plugin-registry` com `index.json` assinado.
- Definir fluxo de contribuição (PR, validação CI, assinatura).
- Padronizar release de plugin (JAR + sig + checksum + SBOM).

Implementado:
- Contrato técnico já definido (schema + verificador).

Pendências:
- Pipeline CI do registry para validação automática do índice e artefatos.
- Guia de publicação para mantenedores de plugins.

## Testes: Estado Atual
Status: **Parcialmente Concluído**

Implementado:
- Testes unitários iniciais para `RegistrySchemaValidator` e `RegistryClient.sha256`.

Pendências:
- Testes de integração com JARs reais de plugin.
- Testes de assinatura válida/inválida (índice e artefato).
- Testes de fallback para cache de índice válido.
- Testes de concorrência e atomicidade de instalação.

Nota de ambiente:
- A execução local dos testes foi bloqueada por resolução de dependências externas/internas ausentes no ambiente atual.

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
