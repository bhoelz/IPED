# Reports

Dated, point-in-time reports that don't belong to a single module.

- `STATE-OF-IPED-<date>.md` — backups of the root `STATE-OF-IPED.md`, one per previous regeneration. The current/latest report always lives at the repo root, not here.
- `DEPENDENCY-UPDATES-<date>.md` — dependency-update audits across npm (`iped-ui`, `iped-webui`) and Maven. Regenerate by re-running the audit and saving a new dated file; don't edit an old one in place.
