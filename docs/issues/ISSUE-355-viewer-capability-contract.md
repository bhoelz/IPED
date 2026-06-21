# ISSUE-355: Finalize the viewer capability contract in iped-viewers-api

- Status: done
- Roadmap: [iped-viewers-ROADMAP.md](../roadmaps/iped-viewers-ROADMAP.md)
- Roadmap section: Phase 1 — Contract-first viewer model
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Finalize the viewer capability contract in `iped-viewers-api`, covering MIME routing, the fallback chain, the rendering target (web HTML vs. native bridge), and lifecycle events (open/loading/ready/error/closed) per root roadmap workstream 2.

## Problem

There was no formal contract describing a viewer's capabilities (what it renders, where, and its lifecycle), which is required before viewers can be reliably classified and ported to the web.

## Acceptance criteria

- [x] `iped.viewers.api.capability` package added.
- [x] `RenderTarget` enum added (`WEB_HTML`, `NATIVE_SWING`, `NATIVE_BRIDGE`).
- [x] `ViewerLifecycleEvent` enum added (`OPEN`, `LOADING`, `READY`, `ERROR`, `CLOSED`).
- [x] `ViewerClassification` enum added (`WEB_PORTABLE`, `WEB_PORTABLE_WITH_WORK`, `NATIVE_ONLY`).
- [x] `ViewerCapabilityDescriptor` immutable builder record added: name, MIME set, render target, classification, hits/search/toolbar flags, classification note.
- [x] `IViewerCapability` interface added for viewers to self-declare their descriptor, without breaking existing `AbstractViewer` implementations.
- [x] `IViewerLifecycleListener` `@FunctionalInterface` added, mapping to the Angular island `viewerReadyEvent`/`islandErrorEvent`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-viewers-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
