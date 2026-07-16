# Web UI rollout and rollback playbook

## Pilot

1. Build and run the release candidate with `mvn -pl iped-webui-server -am verify`.
2. Enable the server for one analyst group and one regression case.
3. Verify search, selection, bookmarks, viewers, export jobs, authentication, and trace IDs.
4. Review browser errors, request latency, failed jobs, and security logs for one business day.

## Rollout

- Expand by case group, keeping the Swing workflow available during the pilot.
- Promote only after the regression suite and support checklist are green.
- Record the release, configuration, case cohort, and operator sign-off.

## Rollback

1. Stop routing new users to the web UI server.
2. Keep the current process available for active read-only sessions when safe.
3. Restore the previous application artifact and configuration.
4. Reopen the affected cases through Swing and preserve trace/audit logs.
5. Record the incident, failed capability, and remediation owner before retrying.

## Support gates

- Authentication and CSRF failures have an owner and alert.
- `/api/**` proxy errors retain `X-Trace-Id` correlation.
- Export jobs expose a terminal status and do not silently disappear.
- The regression case remains immutable and is refreshed before each release.
