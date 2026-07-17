# Web UI go-live checklist

- [x] `mvn -pl iped-webui-server -am verify` passed on the release JDK.
- [x] `regression-case.json` completed for search, selection, viewer and export.
- [ ] Capability flags reviewed for the target deployment.
- [ ] Trace IDs, authentication failures and export failures visible to support.
- [ ] Rollback artifact and previous configuration recorded.
- [ ] Analyst pilot sign-off recorded before broad rollout.

## Evidence log

### 2026-07-17

- Release gate: `mvn --% -pl iped-webui-server -am verify` — **BUILD SUCCESS**.
- The build regenerated the Angular islands, ran the server test suite, and executed the
  configured quality gates without a failing check.
- Regression fixture: `src/test/resources/regression-case.json` covers the pilot journeys
  `open-case`, `search`, `select-result`, `open-viewer` and `export`.

## Operator sign-off

- Release candidate: ____________________
- Target cohort / analyst group: ____________________
- Capability flags reviewed by: ____________________
- Rollback artifact and previous configuration: ____________________
- Analyst pilot approver: ____________________
- Approval date/time: ____________________
- Decision (`go` / `no-go`): ____________________
