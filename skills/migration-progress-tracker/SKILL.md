---
name: migration-progress-tracker
description: Track rewrite progress against legacy features using a global epic roadmap and module-level PR-sized backlogs, then generate an HTML progress report after every implementation update. Use when migrating a legacy system and needing parity visibility, execution status, and auditable delivery reporting.
---

# Migration Progress Tracker

Use this skill to keep migration execution synchronized with legacy parity goals.

## Workflow

1. Initialize tracker state.
- Create `migration-progress.json` with project metadata, epic roadmap, module backlogs, and event history.

2. Maintain global roadmap.
- Keep one roadmap item per epic.
- Store status, progress percent, and references to module backlogs.

3. Maintain module backlogs.
- Keep module items sized for one PR.
- Link each module item to an epic and legacy references.
- Update status as implementation evolves.

4. Register implementation events.
- Append event entries for completed or updated work items.
- Include timestamp, module, item ID, summary, PR link, and key files.

5. Regenerate HTML report after each update.
- Render `migration-progress-report.html`.
- Include summary metrics, epic table, module backlog details, and recent events.

## Output Contract

Maintain these files:
- State file: `docs/migration/progress/migration-progress.json`
- HTML report: `docs/migration/progress/migration-progress-report.html`

Use `scripts/update_progress.py` for all state changes and report generation.

## Data Discipline

- Do not update JSON manually when script-based update is available.
- Keep IDs stable:
  - Epic: `EP-###`
  - Module item: `MB-<module>-###`
- Always keep module item `epic_id` populated.
- Keep backlog item granularity aligned to a single PR.
- Render HTML report immediately after every `upsert-item`, `upsert-epic`, or `add-event`.

## References

- Read `references/progress-data-model.md` for required schema.
- Use `assets/templates/migration-progress.example.json` as baseline state.

## Quick Start

Initialize:
```bash
python scripts/update_progress.py init --file docs/migration/progress/migration-progress.json --project ace-monorepo
```

Upsert epic:
```bash
python scripts/update_progress.py upsert-epic --file docs/migration/progress/migration-progress.json --epic-id EP-001 --title "Autenticacao e Login" --legacy-scope "ace-login, ace-auth-java" --module ace-login --module ace-auth-java
```

Upsert module backlog item:
```bash
python scripts/update_progress.py upsert-item --file docs/migration/progress/migration-progress.json --module ace-login --item-id MB-ace-login-001 --epic-id EP-001 --title "Implementar fluxo OAuth2 PKCE" --status in_progress --legacy-ref BR-017 --acceptance-ref AC-021
```

Add event:
```bash
python scripts/update_progress.py add-event --file docs/migration/progress/migration-progress.json --module ace-login --item-id MB-ace-login-001 --summary "Fluxo PKCE implementado e coberto por testes de integracao" --pr-url "https://example/pr/123"
```

Each mutating command automatically refreshes the HTML report in the same folder.
