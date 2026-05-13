# iped-api

Módulo de contratos públicos do IPED.

## Objetivo

O `iped-api` concentra interfaces, tipos de valor e exceções compartilhadas entre módulos do ecossistema IPED.  
Ele define o "núcleo de comunicação" da aplicação, reduzindo acoplamento entre implementação (`iped-engine`, `iped-app`, `iped-parsers`, etc.) e consumidores de API.

## O que este módulo contém

- Contratos de domínio e processamento:
  - `iped.data.*` (ex.: `IItem`, `IItemReader`, `ICaseData`, `IIPEDSource`)
- Contratos de busca:
  - `iped.search.*` (ex.: `IIPEDSearcher`, `IItemSearcher`, `SearchResult`)
- Contratos de I/O e datasource:
  - `iped.io.*`, `iped.datasource.*`
- Tipos utilitários de API:
  - `MediaTypeValue`, `SearchQueryDefinition`
- Exceções públicas:
  - `iped.exception.*`
- Propriedades e chaves semânticas compartilhadas:
  - `iped.properties.*`

## Papel arquitetural

- Define **contratos estáveis** para integração entre módulos.
- Evita dependência direta de implementações concretas.
- Permite evolução gradual com compatibilidade retroativa quando necessário.

## Dependências e runtime

- Java 25
- JUnit 6 (testes)
- Surefire e JaCoCo configurados no `pom.xml`
- Lombok disponível como `provided` para suporte a geração de código quando necessário

## Diretrizes de evolução

- Mudanças em interfaces públicas devem priorizar compatibilidade.
- Quando necessário, introduzir métodos neutros/adaptadores antes de remoções definitivas.
- Evitar inserir lógica de negócio pesada neste módulo; foco em contrato e tipos.
- Novas dependências externas devem ser avaliadas com rigor para preservar o papel de núcleo.

## Build local do módulo

```bash
mvn -pl iped-api -DskipTests compile
```

Para executar testes do módulo:

```bash
mvn -pl iped-api test
```
