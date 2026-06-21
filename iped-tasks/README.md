# iped-tasks

Parent of the processing-task modules — `iped-tasks.spi` (task SPI), `iped-tasks-bom`, `iped-tasks-core`, and feature task modules (transcript, image, storage-index, carving, graph, report, forensics).

## Roadmap

See [iped-tasks-ROADMAP.md](../docs/roadmaps/iped-tasks-ROADMAP.md) for planned work, current phase status, and linked issues.

## Component plugin system (proposal, not started)

`iped-tasks.spi/` holds design docs for a much broader generic plugin SPI
(`ComponentProvider<T>` covering carvers, parsers, data sources, databases,
webhooks, metadata, file categories, and i18n) — distinct from this module's
actual, shipped task-only SPI (`TaskProvider`/`TaskDescriptor`). See
[iped-tasks.spi/COMPONENT-PLUGIN-SYSTEM-STATUS.md](iped-tasks.spi/COMPONENT-PLUGIN-SYSTEM-STATUS.md)
for the corrected, code-verified implementation status (effectively 0%,
despite the design docs' "100% complete" claims), and
[iped-tasks.spi/FASE1-COMPONENT-ARCHITECTURE.md](iped-tasks.spi/FASE1-COMPONENT-ARCHITECTURE.md)
onward for the original design.
