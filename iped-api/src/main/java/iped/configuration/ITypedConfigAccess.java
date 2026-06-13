package iped.configuration;

import java.util.Optional;

/**
 * API-level contract for typed access to loaded configuration objects.
 *
 * <p>Tasks, parsers, and scripts use this interface to retrieve their
 * configuration without importing the engine's {@code ConfigurationManager}
 * or {@code ConfigurationRegistry} internals. The engine binds a concrete
 * implementation via dependency injection or a thread-local context.
 *
 * <h2>Usage from a task</h2>
 * <pre>{@code
 * ITypedConfigAccess config = CaseContextThreadLocal.get().getConfigAccess();
 * MyTaskConfig cfg = config.getConfig(MyTaskConfig.class)
 *         .orElseThrow(() -> new IllegalStateException("MyTaskConfig not registered"));
 * }</pre>
 *
 * <p>Implementations are expected to be thread-safe and to return the same
 * instance on each call for the same class within a case context.
 */
public interface ITypedConfigAccess {

    /**
     * Returns the configuration object of the given type, if registered.
     *
     * @param <T>   the configuration type
     * @param clazz the {@link Configurable} implementation class
     * @return the configuration wrapped in an {@link Optional}, or
     *         {@link Optional#empty()} if no configurable of that type has been
     *         registered with the engine
     */
    <T extends Configurable<?>> Optional<T> getConfig(Class<T> clazz);

    /**
     * Returns the configuration object of the given type, throwing if absent.
     *
     * @param <T>   the configuration type
     * @param clazz the {@link Configurable} implementation class
     * @return the configuration object; never {@code null}
     * @throws IllegalStateException if no configurable of that type is registered
     */
    default <T extends Configurable<?>> T requireConfig(Class<T> clazz) {
        return getConfig(clazz).orElseThrow(() ->
                new IllegalStateException("Required configuration not found: " + clazz.getName()));
    }

    /**
     * Returns {@code true} if a named task-enable property is set to
     * {@code true} in the loaded configuration.
     *
     * @param propertyName the task-enable property name (as declared in the
     *                     task configuration file)
     * @return {@code true} if the task is enabled, {@code false} if disabled
     *         or not found
     */
    boolean isTaskEnabled(String propertyName);
}
