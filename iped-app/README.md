# iped-app

The desktop application and distribution assembly: the Swing analysis UI, processing UI, timeline graph, bootstrap/CLI entry points, and the release packaging (`release.dir` assembly with `conf`/`lib`/`tools`/`plugins`).

## Roadmap

See [iped-app-ROADMAP.md](../docs/roadmaps/iped-app-ROADMAP.md) for planned work, current phase status, and linked issues.

## Scripting

See [SCRIPTING.md](SCRIPTING.md) for IPED's scripting/extensibility points (task scripts, Python parsers, regex validators, JS carvers) and where each is implemented. Placed here because the example scripts referenced throughout live under `iped-app/resources/scripts/`, even though the loading code spans several engine/task modules.
