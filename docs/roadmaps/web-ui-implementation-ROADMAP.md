# web-ui-implementation — Roadmap de Implementação da Nova Interface Web

> Plano de entrega cross-cutting para a nova web UI (não específico de um único módulo).
> Tracking granular do dia a dia vive em `iped-webui-ROADMAP.md` e
> `iped-webui-server-ROADMAP.md`; este documento mantém a visão estratégica por fase/onda.
> Status legend: see linked issues in docs/issues/ for per-item status (canonical statuses: proposed, planned, in_progress, blocked, done, cancelled).

## Objetivo
- Entregar a nova interface web do IPED como interface principal para análise investigativa, substituindo gradualmente o fluxo Swing sem perder confiabilidade forense, rastreabilidade e paridade funcional nas jornadas críticas.

## Contexto
- O repositório já contém um roadmap geral de modernização em `ROADMAP.md`.
- O repositório já contém contratos iniciais de API e eventos em `specs/84-phase-1-openapi-initial.yaml` e `specs/85-phase-1-events-schema.md`.
- O repositório já contém um inventário da UI Swing em `specs/82-phase-0-swing-inventory.md`.
- O repositório já contém um backlog inicial orientado a contratos em `specs/86-phase-1-implementation-backlog.md`.
- Este documento converte esse direcionamento em um plano específico para a implementação da web UI.

## Princípios
- `iped-engine` continua sendo a fonte autoritativa das regras de busca, navegação e semântica de caso.
- A implementação deve ser contract-first: OpenAPI e schemas de eventos vêm antes da UI.
- A migração deve preservar workflows críticos antes de expandir para recursos avançados.
- A interface web deve nascer observável, testável e preparada para rollout incremental.
- Toda funcionalidade da UI deve ter mapeamento explícito com capacidades existentes do Swing.

## Escopo da Primeira Onda
- Sessão de caso e bootstrap da aplicação.
- Busca, resultados, paginação, ordenação e seleção.
- Painéis principais: resultados, metadados, bookmarks, filtros e viewers básicos.
- Viewers iniciais: texto, HTML, imagem, PDF e email básico.
- Ações investigativas essenciais: abrir item, navegar hits, marcar itens, bookmarks e exportações simples.

## Fora do Escopo Inicial
- Paridade completa de graph e timeline.
- Viewers especializados de cauda longa.
- Equivalência integral de atalhos e customizações avançadas de layout.
- Substituição imediata de viewers dependentes de runtime nativo.

## Arquitetura-Alvo

### Frontend
- Shell SPA em Angular 21 + PrimeNG 21, conforme direcionamento do roadmap geral.
- Estado organizado por domínios funcionais: `session`, `search`, `results`, `filters`, `selection`, `bookmarks`, `viewer`, `jobs`, `layout`.
- Geração de cliente API a partir do OpenAPI para evitar drift entre backend e UI.
- Viewer host desacoplado por capacidades e MIME type, com fallback explícito.

### Backend de Suporte à UI
- `iped-webapi` expõe contratos estáveis para sessão, busca, itens, relacionamentos, viewers, renditions e jobs.
- Stream de eventos cobre mudanças assíncronas de sessão, seleção, viewer e layout.
- Adaptação progressiva do legado Swing para DTOs e eventos web, sem expor referências Swing/AWT.

### Estratégia de Migração
- Primeiro reproduzir fluxos centrais de análise.
- Depois expandir cobertura funcional por capability.
- Por fim tratar lacunas com companion app ou fallback nativo onde navegador não for suficiente.

## Trilhas de Trabalho

### 1. Plataforma Frontend
- Inicializar workspace Angular, convenções de módulos e design tokens.
- Definir shell base: app frame, navegação, área de trabalho, notificações e estados vazios.
- Implantar infraestrutura de autenticação/sessão, feature flags e tratamento global de erro.
- Configurar geração de client SDK, lint, testes unitários e build CI.

### 2. Contratos e Integração Backend
- Consolidar OpenAPI como fonte única para endpoints da UI.
- Consolidar schema versionado para eventos.
- Implementar endpoints mínimos de bootstrap, busca, resultados e viewer session.
- Padronizar `correlationId`, paginação, erros estruturados e métricas por endpoint.

### 3. Estado e Fluxos Investigativos
- Modelar ciclo de sessão: abrir caso, restaurar contexto e sincronizar layout.
- Implementar fluxo de busca: query, filtros, ordenação, paginação e atualização de resultados.
- Implementar fluxo de seleção: item atual, seleção múltipla, checked items e persistência de bookmarks.
- Implementar fluxo de viewer: abrir sessão, carregar rendition, pesquisar dentro do viewer e navegar hits.

### 4. UX e Componentes de Análise
- Resultado tabular com colunas configuráveis e seleção consistente.
- Painel de metadados e relacionamentos do item selecionado.
- Árvores ou painéis equivalentes para categorias, bookmarks e filtros.
- Layout responsivo para desktop amplo e resolução mínima operacional.
- Feedback operacional claro para carregamento, erros, jobs e ações demoradas.

### 5. Viewers e Renditions
- Definir contrato comum para viewers web com capacidades declarativas.
- Entregar viewers básicos: texto, HTML sanitizado, imagem, PDF e email básico.
- Implementar fallback por MIME type e mensagem explícita para formatos não suportados.
- Preparar ponto de extensão para companion app nos casos nativos bloqueados.

### 6. Qualidade, Segurança e Operação
- Cobertura de testes por fluxo crítico.
- Telemetria de UX e performance de viewer/resultados.
- Guardrails de autorização, isolamento por caso/sessão e trilha de auditoria.
- Estratégia de rollout progressivo com feature flags e piloto controlado.

## Fases de Execução — kanban
- [ISSUE-435](../issues/ISSUE-435-fase0-fundamentos-descoberta.md) — Fase 0 - Fundamentos e Descoberta — `done`
- [ISSUE-436](../issues/ISSUE-436-fase1-skeleton-executavel.md) — Fase 1 - Skeleton Executável — `done`
- [ISSUE-437](../issues/ISSUE-437-fase2-mvp-operacional.md) — Fase 2 - MVP Operacional — `done`
- [ISSUE-438](../issues/ISSUE-438-fase3-paridade-prioritaria.md) — Fase 3 - Paridade Prioritária — `done`
- [ISSUE-439](../issues/ISSUE-439-fase4-hardening-rollout.md) — Fase 4 - Hardening e Rollout — `in_progress`

Day-to-day PR-sized work for fases 1-4 above is tracked as its own set of issues in
`iped-webui-ROADMAP.md` and `iped-webui-server-ROADMAP.md` — the sprint-by-sprint backlog
that used to live in this section is superseded by that more granular tracking and was
removed here to avoid two sources of truth drifting apart.

## Dependências Técnicas
- Estabilidade dos contratos em `specs/84-phase-1-openapi-initial.yaml`.
- Disponibilização dos eventos definidos em `specs/85-phase-1-events-schema.md`.
- Evolução de `iped-webapi` sem acoplamento direto ao módulo `iped-engine`, preservando a restrição arquitetural já testada.
- Renditions confiáveis para texto, HTML, imagem, PDF e bytes.
- Dataset de regressão com casos representativos e formatos diversos.

## Riscos e Mitigações
- Risco: drift entre frontend e backend.
- Mitigação: geração de client/server stubs, contract tests e versionamento explícito.
- Risco: viewers web não cobrirem formatos críticos.
- Mitigação: matriz de MIME por prioridade, fallback explícito e bridge com companion app.
- Risco: performance degradar em casos grandes.
- Mitigação: paginação server-side, virtualização de lista, carregamento lazy e métricas desde o início.
- Risco: replicar comportamento Swing de forma implícita e inconsistente.
- Mitigação: rastreabilidade capability por capability a partir do inventário da fase 0.
- Risco: MVP crescer sem fechamento do fluxo principal.
- Mitigação: gate rigoroso de escopo e aceite por jornada crítica.

## Critérios de Aceite do MVP Web UI
- Abrir um caso e iniciar sessão com identidade consistente.
- Executar busca com paginação determinística.
- Selecionar resultado e visualizar metadados principais.
- Abrir viewers básicos por MIME suportado.
- Pesquisar dentro do viewer e navegar hits.
- Aplicar filtros centrais e manter consistência do estado da sessão.
- Criar e consultar ao menos um job simples de exportação.
- Executar fluxos críticos com telemetria, logs e testes automatizados.

## Métricas de Sucesso
- Tempo para primeira busca após abrir caso.
- Latência de paginação e ordenação.
- Latência para abrir viewer por tipo de rendition.
- Taxa de erro por endpoint e por fluxo de UI.
- Percentual de workflows críticos executáveis sem fallback Swing.
- Taxa de sucesso dos testes E2E e contract tests por release.

## Próximos Passos
Ver os issues `planned`/`in_progress` ligados acima (Fase 2-4) e os roadmaps de
`iped-webui` e `iped-webui-server` para o backlog acionável atual — os "próximos passos"
originais deste documento (validar MVP, congelar OpenAPI, criar workspace, Sprint 1) já
foram concluídos (Fase 0/1 `done`).
