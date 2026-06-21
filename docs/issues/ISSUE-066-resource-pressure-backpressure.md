# ISSUE-066: Backpressure to the coordinator on agent resource pressure

- Status: done
- Roadmap: [iped-distributed-ROADMAP.md](../roadmaps/iped-distributed-ROADMAP.md)
- Roadmap section: Phase 3 — Scale and scheduling
- Owner: unassigned
- Created: 2026-06-21
- Updated: 2026-06-21

## Summary

Signal backpressure to the coordinator when an agent's storage/memory nears configured limits, integrated with the engine's `ResourceManager`, so the coordinator stops scheduling new work to pressured agents.

## Problem

Without backpressure signalling, an agent running low on heap or disk could keep being assigned new work, risking OOM or disk-exhaustion failures instead of gracefully shedding load.

## Acceptance criteria

- [x] `iped.distributed.resource` package: `PressureLevel` (NONE/SOFT/HARD), `ResourcePressure` (snapshot record), `ResourcePressurePolicy` (thresholds: heap 75%/90%, disk 10GB/5GB soft/hard), `ResourcePressureMonitor` (injected samplers; `forAgent()` factory wires live samplers).
- [x] `TaskAgent.heartbeatLoop()` samples on every heartbeat tick, logs level transitions (WARN on HARD, INFO on recovery), calls the 5-arg `coordinator.heartbeat()`.
- [x] Main poll loop pauses all Kafka partitions while `lastPressure==HARD` (in-flight items always finish); resumes automatically when pressure drops.
- [x] Coordinator side: `AgentRegistry.schedulableFreeSlots(taskType)` excludes HARD agents; `pressuredAgentCount(taskType)` for observability; HARD transition logged at WARN.
- [x] Heartbeat carries `pressureLevel`/`pressureRatio`; all thresholds configurable via `DistributedConfig` (`backpressure*` keys).
- [x] 25 broker-free tests in `ResourcePressureTest`; documented in `specs/92-backpressure.md`.

## Updates

### 2026-06-21
- Issue created during roadmap consolidation, extracted from `iped-distributed-ROADMAP.md` (Phase 3), status set to `done`.
