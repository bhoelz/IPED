# Qualidade — IPED

> Atualizado em: 2026-09-03  
> Conformidade: `ADVISORY`

## Contrato aplicável

- Stack: `java`
- Comandos e ferramentas: `./mvnw verify` (ou `mvn verify`), Checkstyle, SpotBugs, JaCoCo, Semgrep, Gitleaks e OSV Scanner.
- Cobertura: baseline aprovado não pode cair; linhas novas exigem 80%.
- Segurança: Gitleaks bloqueia imediatamente; SAST e dependências são advisory até duas execuções verdes em PRs distintos e 14 dias de observação.
- Workflow: `.github/workflows/quality-contract.yml`
- Issue de baseline: [Baseline de qualidade](../issues/ISSUE-452-quality-baseline.md)

## Relatórios e exceções

- CI publica resumo Markdown e artefatos por 30 dias, incluindo `quality-report.json`.
- Relatórios gerados devem permanecer em `.quality/`, que é ignorado pelo Git.
- Exceções exigem justificativa, dono e expiração máxima de 90 dias. Nenhuma exceção pode ocultar segredo novo.

## Evidência mais recente

- Primeira execução do contrato pendente de CI remoto acessível.
