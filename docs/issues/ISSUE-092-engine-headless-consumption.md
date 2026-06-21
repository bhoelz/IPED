# ISSUE-092: Engine consumed headless by iped-webapi/iped-mcp/iped-runner

- Status: done
- Roadmap: [iped-engine-ROADMAP.md](../roadmaps/iped-engine-ROADMAP.md)
- Roadmap section: Phase 4 — 5.0 platform role
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Make the engine consumable headlessly by `iped-webapi`, `iped-mcp`, and `iped-runner` as the only entry points, isolating Swing-specific hooks behind listeners.

## Problem

The engine needed to be usable without any Swing/AWT GUI dependency so headless consumers (`iped-webapi`, `iped-mcp`, `iped-runner`) can drive it directly.

## Acceptance criteria

- [x] `UIPropertyListenerProvider.uiDispatcher` defaults to `Runnable::run` (headless-safe); `iped-app` overrides with `SwingUtilities::invokeLater`.
- [x] Zero `javax.swing` imports in engine code confirmed by grep.
- [x] `java.awt` usages in `IPEDReader`, `Bookmarks`, `ImageSimilarity` confirmed safe under `-Djava.awt.headless=true` (image-processing APIs only).
- [x] `EngineBoundaryTest` enforces no Swing in core/config/lucene/task packages.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-engine-ROADMAP.md`, status set to `done`.
