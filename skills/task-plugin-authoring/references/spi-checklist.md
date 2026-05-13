# SPI Checklist

1. Implement `TaskProvider` and return a valid `TaskDescriptor` with unique `id`.
2. Provide dependency hints using `TaskDependency` (`requires`, `before`, `after`) only when needed.
3. Register provider in `META-INF/services/iped.tasks.spi.TaskProvider`.
4. Ensure provider `createTask()` returns an `AbstractTask` implementation.
5. Validate no duplicate task IDs against XML or existing providers.
6. Verify dependency graph has no cycles and required dependencies exist.
7. Confirm plugin jar is discoverable under configured `pluginFolder`.
8. Add startup log checks for loaded/skipped providers and resolved order.
