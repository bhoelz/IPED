# ISSUE-034: Decouple CLI processing entry point from Swing classes

- Status: done
- Roadmap: [iped-app-ROADMAP.md](../roadmaps/iped-app-ROADMAP.md)
- Roadmap section: Phase 1 — Separate assembly from application
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Decouple the `iped-app` headless CLI processing entry point from Swing classes so that processing a case on a server never initializes any UI toolkit. Implemented via a template-method refactor of `Bootstrap.java` with UI-specific behavior isolated to `BootstrapUI.java`.

## Problem

The headless CLI processing path needs to run without ever touching AWT/Swing/LibreOffice classes, but the bootstrap code mixed headless and UI concerns together, risking accidental UI toolkit initialization on servers.

## Acceptance criteria

- [x] `Bootstrap.java` refactored with template-method hooks: `configLoaded()`, `onChildProcessStarted(Process)`, `extendClasspath(Main, String)` — all no-ops in the headless base class.
- [x] `BootstrapUI.java` overrides all three hooks: starts splash screen, registers child PID, discovers LibreOffice UNO JARs.
- [x] All AWT/Swing/LibreOffice imports confined to `BootstrapUI`; `Bootstrap` imports only iped-engine and JDK classes.
- [x] `separator` promoted to `protected static final` for subclass access.
- [x] `IpedAppBootstrapArchitectureTest` adds three ArchUnit rules enforcing no `java.awt`, `javax.swing`, `javafx`, or `iped.app.ui` deps in any bootstrap class other than `BootstrapUI`.
- [x] Test dependencies (`junit-jupiter`, `archunit-junit5`) added to `pom.xml`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-app-ROADMAP.md`, status set to `done` based on the original `[x]` marker.
