package iped.engine.config;

import java.io.IOException;

/**
 * Lets a caller of {@link Configuration#loadConfigurables(String, boolean, ConfigContributor)}
 * register additional {@link iped.configuration.Configurable}s into the supplied
 * {@link ConfigurationManager} before the batched load/validation pass runs.
 *
 * <p>Exists because {@code iped-engine-core} cannot construct every Configurable
 * itself: some implementations (e.g. task configs, {@code ProcessingPriorityConfig})
 * live in {@code iped-engine}, which depends on {@code iped-engine-core} and not the
 * other way around. Callers that already depend on those higher modules implement
 * this interface to bridge the gap without creating a cyclic module dependency.
 */
public interface ConfigContributor {
    void contribute(ConfigurationManager configManager) throws IOException;
}
