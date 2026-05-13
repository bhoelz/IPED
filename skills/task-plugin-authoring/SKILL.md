---
name: task-plugin-authoring
description: Author new IPED processing tasks as plugins using the iped-tasks.spi contract and ServiceLoader packaging. Use when creating a new task plugin jar or when adding in-repo tasks as dedicated submodules under iped-tasks.
---

# Task Plugin Authoring

Use this skill to create new processing tasks that are decoupled from the main engine and loaded through SPI providers.

## Core Rule

- If the new task must live in this repository, implement it as a new submodule under `iped-tasks`.
- Do not implement new concrete task classes directly inside `iped-engine` for new functionality.

## Capabilities

1. Create a plugin task module that depends on `iped-tasks.spi` and exposes `TaskProvider`.
2. Register providers with `META-INF/services/iped.tasks.spi.TaskProvider`.
3. Model task ordering/dependencies with `TaskDependency` (`REQUIRES`, `BEFORE`, `AFTER`, optional).
4. Keep backward compatibility with existing `TaskInstaller.xml` while onboarding plugins.
5. Scaffold in-repo task modules under `iped-tasks/iped-tasks-<name>`.

## Inputs

- Required:
- task name/id
- task intent (what it processes)
- placement choice: `in-repo` or `external plugin`

- Optional:
- dependency hints (runs before/after/requires)
- enable property name
- config files needed by the task

## Outputs

1. Module/plugin scaffold with Maven files and Java package skeleton.
2. `TaskProvider` implementation and service registration file.
3. Task dependency declarations via `TaskDescriptor`.
4. Integration notes for `TaskInstaller.xml` coexistence when needed.

## Workflow

1. Choose placement.
- `in-repo`: create a new `iped-tasks` submodule (`iped-tasks-<feature>`).
- `external plugin`: create a standalone Maven module producing a plugin JAR.

2. Scaffold module.
- Start from templates in `assets/templates`.
- Ensure dependency on `iped-tasks.spi` and required engine/api modules only.

3. Implement task and provider.
- Create `AbstractTask` implementation.
- Create `TaskProvider` returning `TaskDescriptor` and task instance.
- Add `META-INF/services/iped.tasks.spi.TaskProvider` with provider FQCN.

4. Declare dependencies.
- Encode task graph intent with `TaskDependency`.
- Use optional dependencies only when degraded behavior is acceptable.

5. Wire in-repo module.
- Add `<module>iped-tasks-<feature></module>` to `iped-tasks/pom.xml`.
- If needed, add module to BOM and dependency management.

6. Validate packaging and loading.
- Confirm provider is discoverable via `ServiceLoader`.
- Validate no duplicate task IDs and no dependency cycles.

## Constraints

- New tasks in this repository must be submodules of `iped-tasks`.
- Avoid adding compile-time dependencies from main engine to concrete task modules.
- Plugin IDs must be unique across XML tasks and SPI providers.

## References

- `references/module-placement-rules.md`
- `references/spi-checklist.md`
- `assets/templates/task-provider-template.java`
- `assets/templates/service-loader-template.txt`
- `assets/templates/pom-inrepo-submodule-template.xml`
