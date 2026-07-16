# Web UI capability gap matrix

This matrix is the baseline for ISSUE-438 parity work and ISSUE-439 rollout gates.

| Capability | Current path | Gap / next validation |
|---|---|---|
| Categories and filters | SSR sidebar + HTMX | Validate persistence across case/session reload |
| Bookmarks | Web API + SSR sidebar | Validate create/edit/delete and bulk assignment |
| Viewer search | Viewer session API contract | Validate hit navigation in browser regression case |
| Export | `/workspace/export` + async job polling | Validate download/result handoff and failure recovery |
| Layout/preferences | Server-rendered layout | Add persisted per-user preferences |
| Native-only viewers | MIME fallback | Define companion-app handoff contract |
| Rollout controls | Deployment playbook | Add runtime capability flags before broad go-live |
