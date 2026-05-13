# Module Placement Rules

1. New task functionality implemented inside this repository must be created as a new submodule under `iped-tasks`.
2. Do not place new concrete task implementations in `iped-engine` unless explicitly performing migration compatibility work.
3. Each in-repo task submodule should follow naming: `iped-tasks-<feature>`.
4. Add every new in-repo task module to `iped-tasks/pom.xml` `<modules>`.
5. Keep `iped-engine` coupled only to SPI contracts (`iped-tasks.spi`) for plugin loading concerns.
