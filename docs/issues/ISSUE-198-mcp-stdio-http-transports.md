# ISSUE-198: stdio and HTTP transports for production deployment

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 4 — Production (5.0)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Support both stdio and HTTP transports for the MCP server with identical auth/audit guarantees, enabling both local desktop and networked deployment modes.

## Problem

Production deployments need an HTTP-reachable MCP server in addition to the default stdio transport used for local tool integration, without weakening security guarantees on either path.

## Acceptance criteria

- [x] stdio transport available by default (`--transport=stdio`).
- [x] HTTP transport available via embedded Jetty 12 + `HttpServletStreamableServerTransportProvider` (`--transport=http`).
- [x] Same auth/audit guarantees enforced on both transport paths.
- [x] HTTP endpoint exposed at `http://host:<port>/mcp`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
