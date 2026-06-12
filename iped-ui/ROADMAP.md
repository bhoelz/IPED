# iped-ui — Evolution Roadmap

> Module purpose: React-based configuration web UI talking to the JAX-RS Configuration
> API on embedded Jetty — editing IPED's (now TOML-based) configuration in a browser.
> Status legend: `[ ]` planned · `[~]` in progress · `[x]` done

## Current state (2026-06)
- Standalone React app (npm workspace, not in the Maven reactor) targeting the
  ConfigurationServer at `localhost:8080`.
- Overlaps conceptually with the main web UI effort (`iped-webui`/`iped-webui-server`)
  and with the runner dashboard's profile-selection plans.

## Phase 0 — Strategic decision (blocks everything else)
- [ ] Decide this module's fate explicitly:
      (a) fold configuration editing into the SSR web UI as fragments/islands,
      (b) keep it as a standalone admin tool, or
      (c) retire it in favor of editing TOML files directly with schema validation.
      Recommendation: (a) — one web stack, one auth model, one deployment; config
      editing becomes an admin area of `iped-webui-server`.

## Phase 1 — If kept (interim hardening)
- [ ] Align with the TOML config model: render module-owned defaults as read-only
      baseline, edit only the deviation layer (locals/profiles) — never flatten the
      layering into one file.
- [ ] Schema-driven forms: generate editors from the config schema in
      `iped-engine-core` (`iped.engine.config.schema`) instead of hand-built forms.
- [ ] Validation before save using the same engine-side validators (no divergent rules).

## Phase 2 — Migration (if decision = fold into web UI)
- [ ] Port the configuration screens as SSR fragments (HTMX forms suit config editing
      well) under `iped-webui-server`; reuse the schema-driven approach.
- [ ] Retire this workspace; record the removal in `MIGRATION_GUIDE.md`.

## Progress checks
- Decision recorded here with date and rationale.
- No second source of config-editing truth once the decision is executed.
