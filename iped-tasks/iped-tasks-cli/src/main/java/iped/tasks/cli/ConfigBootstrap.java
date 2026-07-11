package iped.tasks.cli;

import iped.engine.config.Configuration;
import iped.engine.config.EngineConfigContributor;

import java.io.File;
import java.io.IOException;

/**
 * Populates {@link iped.engine.config.ConfigurationManager} with IPED's base
 * engine {@code Configurable}s from a conf directory, without touching
 * case/output state ({@code ipedroot}, Lucene index, SQLite).
 *
 * <p>Deliberately uses {@link EngineConfigContributor} rather than
 * {@code ProcessingConfigContributor}: the latter eagerly instantiates every
 * task in the configured pipeline <em>and calls {@code getConfigurables()} on
 * each one</em>, which for some tasks has real side effects (e.g.
 * {@code PythonTask} initializes a Jep/Python interpreter) that would make
 * loading config fail because of a task the user never asked to run. Standalone
 * execution only needs the single requested task's own {@code Configurable}s,
 * which {@code TaskExecutor} registers lazily right before running that task.
 */
public final class ConfigBootstrap {

    private ConfigBootstrap() {
    }

    public static void load(File confDir) throws IOException {
        if (!confDir.isDirectory()) {
            throw new IllegalArgumentException("Config directory not found: " + confDir);
        }
        ensureNativeLibDirsExist(confDir);
        Configuration.getInstance().loadConfigurables(confDir.getAbsolutePath(), true, EngineConfigContributor.INSTANCE);
    }

    /**
     * {@code Configuration.loadNativeLibs()} lists {@code <confDir>/tools/tsk/<arch>}
     * unconditionally on Windows and NPEs if the directory doesn't exist (as is the
     * case for a bare conf directory, e.g. {@code iped-app/resources/config}, rather
     * than a full release layout). Standalone task execution never needs Sleuthkit's
     * native libs, so pre-creating the (empty) expected directories is enough to make
     * {@code listFiles()} return an empty array instead of {@code null}.
     */
    private static void ensureNativeLibDirsExist(File confDir) throws IOException {
        for (String arch : new String[] { "x86", "x64" }) {
            File dir = new File(confDir, "tools/tsk/" + arch);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new IOException("Could not create " + dir);
            }
        }
    }
}
