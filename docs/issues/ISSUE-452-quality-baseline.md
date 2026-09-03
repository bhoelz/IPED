# ISSUE-452 — Baseline de qualidade, segurança e cobertura

- Estado: `NEXT`
- Contrato: [QUALITY.md](../quality/QUALITY.md)

## Objetivo

Executar a primeira baseline do contrato de qualidade para `IPED`, registrar a dívida existente e preparar a ativação gradual dos gates.

## Critérios de aceite

- **Given** o workflow advisory, **When** ele executar em dois PRs distintos, **Then** haverá resumo, `quality-report.json` e artefatos de cobertura/segurança por 30 dias.
- Achados históricos terão baseline ou exceção justificada; achados novos não serão ocultados.
- Após 14 dias, cobertura não pode regredir e linhas novas devem atingir 80%.

## Evidências esperadas

- URLs dos dois runs verdes, baseline aprovada e data de revisão.
- Atualização de `docs/quality/QUALITY.md` e `docs/status/STATUS.md`.
