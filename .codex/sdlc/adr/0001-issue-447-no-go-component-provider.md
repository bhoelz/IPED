# ADR-0001: Não construir `ComponentProvider<T>` genérico neste ciclo

- Status: accepted
- Date: 2026-07-16
- Issue: ISSUE-447

## Contexto

O repositório já possui um SPI funcional para extensões de tarefas:
`iped.tasks.spi.TaskProvider<T>`, descoberto por `ServiceLoader` e integrado ao
ciclo de vida de tarefas. A proposta histórica de `ComponentProvider<T>`
pretendia unificar carvers, parsers, fontes de dados, bancos, webhooks,
metadados, categorias e bundles de idioma, mas os artefatos centrais da
proposta não existem no código atual.

Os domínios existentes possuem contratos, escopos, configuração e ciclos de
vida diferentes. Não há dois consumidores concretos que atualmente exijam a
mesma abstração genérica.

## Opções consideradas

1. Construir o sistema genérico completo: alto custo de migração e risco para
   APIs públicas, sem consumidores concretos.
2. Criar apenas um registry mínimo: adicionaria uma camada sem semântica e
   duplicaria o papel do `TaskProvider`.
3. Manter SPIs especializados: menor risco, preserva compatibilidade e permite
   evoluir cada domínio conforme necessidades reais.

## Decisão proposta

Escolher a opção 3. Não criar `ComponentProvider<T>`, `ComponentRegistry` ou
aliases vazios neste ciclo. Manter `TaskProvider` como contrato oficial para
extensões de tarefas e preservar registries especializados por domínio.

## Consequências

Positivas:

- evita uma API pública ampla sem consumidores;
- não exige migração nem quebra de compatibilidade;
- reduz o escopo de manutenção;
- permite que os issues 445 e 446 avancem sem depender de uma plataforma nova.

Negativas:

- não haverá descoberta uniforme entre domínios;
- futuras integrações cross-component precisarão de contratos próprios.

## Guardrails

Reabrir a decisão somente quando pelo menos dois domínios concretos exigirem o
mesmo contrato de descoberta, configuração e ciclo de vida, com casos de
migração e compatibilidade definidos. Nesse caso, iniciar por um spike e um
adapter sobre APIs existentes.
