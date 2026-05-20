# Roadmap de Implementação da Nova Interface Web

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

## Fases de Execução

### Fase 0 - Fundamentos e Descoberta
Objetivo:
- Fechar lacunas entre inventário Swing, contratos existentes e backlog executável da web UI.

Entregas:
- Matriz de rastreabilidade Swing -> capability web.
- Mapa de estado da aplicação web.
- Decisões de arquitetura frontend e convenções de pastas, estado e eventos.
- Priorização oficial do MVP.

Critérios de saída:
- Toda capability do MVP vinculada a endpoint/evento existente ou pendência explícita.
- Sem ambiguidade sobre MIME viewers suportados no MVP.

### Fase 1 - Skeleton Executável
Objetivo:
- Colocar de pé o fluxo fim a fim mínimo: UI -> API -> engine -> resposta.

Entregas:
- Shell Angular inicial.
- Bootstrap de sessão e abertura de caso.
- Busca inicial com listagem paginada.
- Seleção de item e painel simples de metadados.
- Abertura de viewer session com rendition básica.

Critérios de saída:
- Usuário consegue abrir um caso, executar busca, ver resultados e abrir item em viewer básico.
- Build e testes automatizados executam em CI.

### Fase 2 - MVP Operacional
Objetivo:
- Cobrir o núcleo do trabalho analítico com estabilidade suficiente para piloto.

Entregas:
- Result table completa com ordenação e seleção múltipla.
- Facets/filtros centrais.
- Bookmarks essenciais.
- Viewers text/html/image/pdf/email básico.
- Busca dentro do viewer com hits e navegação.
- Jobs de exportação simples com polling/status.

Critérios de saída:
- Fluxos críticos do backlog de fase 1 executam em caso real.
- Latência e estabilidade dentro dos limites acordados.
- Piloto restrito viável sem dependência diária da UI Swing para tarefas básicas.

### Fase 3 - Paridade Prioritária
Objetivo:
- Fechar as principais lacunas de usabilidade em relação à aplicação Swing.

Entregas:
- Painéis equivalentes para categorias, bookmarks, filtros e relacionamentos.
- Persistência de layout e preferências principais.
- Melhorias de navegação, atalhos prioritários e ações em lote.
- Viewer fallback mais robusto e integração inicial com companion app para casos bloqueados.

Critérios de saída:
- Maioria dos fluxos de análise recorrentes migra para web sem workaround manual.
- Gap remanescente documentado por capability e plano de fechamento.

### Fase 4 - Hardening e Rollout
Objetivo:
- Preparar adoção ampla com segurança operacional.

Entregas:
- Observabilidade completa de frontend e backend.
- Testes E2E com dataset de regressão.
- Playbook de rollout, rollback e suporte.
- Feature flags por capability e estratégia de convivência com legado.

Critérios de saída:
- Go-live controlado aprovado.
- Incidentes e regressões têm diagnóstico reproduzível via logs, métricas e traces.

## Backlog Prioritário por Sprint

### Sprint 1
- Estruturar projeto Angular e pipeline CI.
- Gerar client a partir do OpenAPI atual.
- Implementar bootstrap de sessão/caso.
- Implementar busca básica e listagem paginada.
- Implementar item details mínimo e viewer session mínima.

### Sprint 2
- Implementar facets e filtros centrais.
- Implementar viewers text/html/image/pdf.
- Implementar busca interna no viewer e navegação de hits.
- Implementar seleção múltipla e checked items.

### Sprint 3
- Implementar bookmarks essenciais.
- Implementar relationships e painéis auxiliares.
- Implementar export jobs e acompanhamento de status.
- Melhorar estados de erro, loading e empty state.

### Sprint 4
- Persistência de layout/preferências.
- Atalhos prioritários.
- Hardening de performance.
- Automação E2E dos fluxos críticos.

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

## Próximos Passos Imediatos
1. Validar oficialmente o escopo do MVP e a lista de viewers suportados.
2. Congelar a primeira versão do OpenAPI e dos schemas de eventos usados pela UI.
3. Criar o workspace frontend e o pipeline básico de build/test.
4. Implementar Sprint 1 sobre um caso de teste real e instrumentado.
