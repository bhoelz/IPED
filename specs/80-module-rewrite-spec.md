# Especificação de Reescrita por Módulo (Engenharia Reversa)

Data: 2026-05-12  
Base analisada: `iped-parent 4.4.0-SNAPSHOT`

## 1. Escopo e critério
- Fonte principal: estrutura Maven, `README.md`, `pom.xml` de módulos e namespaces Java.
- Objetivo: separar requisitos e fronteiras por módulo para permitir migração incremental com rastreabilidade ao legado.
- Nível: especificação arquitetural e funcional por módulo/submódulo (não detalhamento de classe-a-classe).

## 2. Mapa de módulos legado
- Agregadores: `iped-parent`, `iped-parsers`, `iped-viewers`, `iped-carvers`.
- Módulos executáveis/centrais: `iped-app`, `iped-engine`, `iped-geo`.
- Módulos de contrato/base: `iped-api`, `iped-utils`, `iped-viewers-api`, `iped-carvers-api`.
- Módulos de implementação especializada: `iped-parsers-impl`, `iped-viewers-impl`, `iped-carvers-impl`, `iped-ahocorasick`, `java-dbf`.

## 3. Especificação por módulo

### M01 - API de domínio e contratos (`iped-api`)
Responsabilidade:
- Definir contratos comuns de configuração, dados de evidência, leitura de fontes, I/O, busca e propriedades de metadados.

Requisitos de reescrita:
- Manter camada sem dependência de UI e sem regras de infraestrutura.
- Preservar modelos e interfaces de busca para compatibilidade entre engine, app e web API.
- Manter taxonomia de propriedades (`BasicProps`, `ExtraProperties`, `MediaTypes`) como vocabulário canônico.

Fronteiras:
- Não implementa parsing, indexação, UI nem rendering.

### M02 - Utilitários transversais (`iped-utils`)
Responsabilidade:
- Serviços utilitários compartilhados (strings, IO auxiliar, recursos comuns, normalizações), consumidos por múltiplos módulos.

Requisitos de reescrita:
- Isolar utilidades puras e remover acoplamentos acidentais com camadas altas.
- Garantir API estável para `iped-engine`, `iped-parsers-impl`, `iped-carvers-api` e `iped-geo`.

### M03 - Parsing forense (`iped-parsers-impl` + `java-dbf`)
Responsabilidade:
- Extração de artefatos de múltiplos formatos e apps (browsers, chats, registry, sqlite, mail, torrent, WhatsApp, Telegram etc).
- Execução isolada/out-of-process para robustez (`fork/ForkServer`).

Requisitos de reescrita:
- Preservar modelo extensível por parser (plugin-like por tipo de evidência).
- Manter tolerância a falhas por parser sem interromper processamento global.
- Manter integração com scripts Python/JS para parsers customizados.

Fronteiras:
- Fornece extração estruturada; não decide priorização de fila nem UX.

### M04 - Visualização (`iped-viewers-api` + `iped-viewers-impl`)
Responsabilidade:
- Contratos e implementações para renderização/visualização de conteúdo e apoio à busca em visualizadores.

Requisitos de reescrita:
- Separar claramente API de visualização e implementações concretas.
- Manter suporte a fallback de visualização por tipo MIME/conteúdo.
- Preservar camada de localização e utilitários de viewer.

### M05 - Carving (`iped-carvers-api` + `iped-carvers-impl` + `iped-ahocorasick`)
Responsabilidade:
- Recuperação de arquivos por assinatura em dados brutos.
- Motor de matching eficiente com Aho-Corasick para padrões múltiplos.

Requisitos de reescrita:
- Manter pipeline de carving desacoplado do parser textual.
- Preservar desempenho (carving não pode dominar tempo total de processamento).
- Manter fronteira clara entre API de carvers e implementações de formato.

### M06 - Núcleo de processamento e busca (`iped-engine`)
Responsabilidade:
- Orquestração completa do caso: ingestão de fontes, filas, workers, tarefas, indexação e busca.
- Gestão de configuração de tarefas (`config/*TaskConfig`), fontes (`datasource/*`), dados do caso (`data/*`), busca/indexação (`search`, `lucene`), integração Sleuthkit e web API.

Requisitos de reescrita:
- Preservar execução concorrente orientada a filas com controle de ordem/prioridade.
- Preservar arquitetura de tarefas configuráveis (OCR, hash, regex, thumb, transcrição, IA etc).
- Manter pontos de entrada de processamento CLI e serviços auxiliares (web API, sleuthkit server, ferramentas hashdb/graph).
- Garantir restart/continue de processamento sem corrupção de estado.

### M07 - Geolocalização e mapa (`iped-geo`)
Responsabilidade:
- Extração e exibição geográfica (KML/openstreet/webkit), parsers geoespaciais e integração com engine/app.

Requisitos de reescrita:
- Preservar módulo opcional e integrável via API do engine.
- Manter separação entre parsing geo, camada JS/web e renderização de mapa.

### M08 - Aplicação e experiência do usuário (`iped-app`)
Responsabilidade:
- Bootstrap, configuração de execução, interface de análise, operações de processamento e exploração investigativa.
- Subdomínios explícitos: `ui`, `processing`, `graph`, `timelinegraph`, `metadata`.

Requisitos de reescrita:
- Preservar dois fluxos principais: processamento de caso e análise interativa.
- Preservar recursos investigativos avançados (grafo de vínculos, timeline, filtros, bookmarks, busca e exportações).
- Manter integração com scripts de tarefas (`resources/scripts/tasks`) e validadores regex customizados.

## 4. Dependências internas a preservar (alto nível)
- `iped-app -> iped-engine, iped-geo`
- `iped-engine -> iped-api, iped-utils, iped-parsers-impl, iped-viewers-api/impl, iped-carvers-api/impl, iped-ahocorasick`
- `iped-geo -> iped-api, iped-engine, iped-parsers-impl, iped-utils, iped-viewers-api`
- `iped-viewers-impl -> iped-viewers-api, iped-parsers-impl`
- `iped-carvers-impl -> iped-carvers-api`
- `iped-carvers-api -> iped-api, iped-utils, iped-ahocorasick`
- `iped-utils -> iped-api`

## 5. Regras de migração incremental
- Migrar por módulo mantendo contratos da `iped-api` como anti-corrupção.
- Substituir primeiro implementações internas (`-impl`) mantendo interfaces estáveis (`-api`).
- Validar paridade por capacidades: ingestão, parsing, carving, indexação, busca, visualização, grafo, timeline, web API.
- Tratar scripts (Python/JS) como extensão de 1ª classe, com contrato explícito de entrada/saída.

## 6. Riscos técnicos extraídos
- Alto acoplamento funcional entre `iped-engine` e implementações específicas (`parsers/viewers/carvers`).
- Ampla superfície de formatos/parsers (risco de regressão funcional em migração big-bang).
- Múltiplos entrypoints executáveis exigem padronização de inicialização e observabilidade.

## 7. Pendências para próxima iteração de especificação
- Detalhar casos de uso por módulo com critérios de aceite mensuráveis.
- Catalogar contratos públicos de cada API (assinaturas e invariantes).
- Inventariar testes legados por capability para suíte de regressão da reescrita.
