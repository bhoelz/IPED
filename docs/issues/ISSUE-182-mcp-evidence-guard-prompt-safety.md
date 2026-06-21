# ISSUE-182: Prompt-safe data shaping with EvidenceGuard

- Status: done
- Roadmap: [iped-mcp-ROADMAP.md](../roadmaps/iped-mcp-ROADMAP.md)
- Roadmap section: Phase 1 — Read-focused MVP (root roadmap Phase B)
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Ensure evidence text returned to LLM clients is wrapped safely so it cannot be confused with instructions, and is bounded in size.

## Problem

Raw evidence text passed directly into LLM context is a prompt-injection and context-bloat risk if not delimited and capped.

## Acceptance criteria

- [x] `EvidenceGuard` wraps all evidence text in `<iped-evidence>` delimiters.
- [x] 50,000-character cap enforced on evidence text.
- [x] Binary placeholder returned for non-text content.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-mcp-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
