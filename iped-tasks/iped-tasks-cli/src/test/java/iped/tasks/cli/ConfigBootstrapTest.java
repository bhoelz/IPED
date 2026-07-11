package iped.tasks.cli;

import iped.engine.config.ConfigurationManager;
import iped.engine.config.OCRConfig;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that base engine Configurables can be loaded from a real IPED
 * release directory (the {@code --conf} argument must be a release root
 * containing {@code conf/}, {@code scripts/}, {@code tools/}, {@code profiles/}
 * as siblings -- not the bare {@code iped-app/resources/config} source folder,
 * which is missing {@code scripts/} and breaks {@code ScriptTask} lookups)
 * without creating a case (no ipedroot, no Lucene index, no SQLite) and
 * without instantiating the task pipeline (see {@link ConfigBootstrap}'s
 * javadoc for why that matters). {@link iped.engine.config.Configuration} is a
 * process-wide singleton that only loads once, so this is the sole test
 * exercising it in this module.
 */
class ConfigBootstrapTest {

    @Test
    void loadsBaseEngineConfigWithoutACaseOrTaskPipeline() throws Exception {
        File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();
        assertTrue(confDir.isDirectory(), "expected release dir at " + confDir
                + " -- run `mvn package` at the repo root first to produce it");

        ConfigBootstrap.load(confDir);

        OCRConfig ocrConfig = ConfigurationManager.get().findObject(OCRConfig.class);
        assertNotNull(ocrConfig, "OCRConfig should be loaded from the conf directory");
    }
}
