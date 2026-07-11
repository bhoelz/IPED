package iped.tasks.cli;

import iped.data.IItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check of the standalone wiring: a real conf directory, a real
 * file on disk, a real task class -- no case, no index, no SQLite.
 */
class TaskExecutorTest {

    @BeforeAll
    static void bootstrapConfig() throws Exception {
        File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();
        assertTrue(confDir.isDirectory(), "expected release dir at " + confDir
                + " -- run `mvn package` at the repo root first to produce it");
        ConfigBootstrap.load(confDir);
    }

    @Test
    void runsHashTaskAgainstARealFile() throws Exception {
        Path tempFile = Files.createTempFile("iped-tasks-cli-hash-test", ".txt");
        try {
            Files.writeString(tempFile, "content to hash");
            List<IItem> items = StandaloneItemFactory.fromInput(tempFile.toFile(), false);
            File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();

            RunResult result = TaskExecutor.run("iped.engine.task.HashTask", items, confDir);

            assertEquals(1, result.items().size());
            ItemResult itemResult = result.items().get(0);
            assertEquals(ItemResult.OK, itemResult.status());
            assertNotNull(itemResult.extraAttributes());
            assertTrue(itemResult.extraAttributes().containsKey("sha-256"),
                    "expected a sha-256 extra attribute, got: " + itemResult.extraAttributes().keySet());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void reportsKnownUnsupportedTaskWithoutInstantiatingIt() throws Exception {
        Path tempFile = Files.createTempFile("iped-tasks-cli-unsupported-test", ".txt");
        try {
            Files.writeString(tempFile, "content");
            List<IItem> items = StandaloneItemFactory.fromInput(tempFile.toFile(), false);
            File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();

            RunResult result = TaskExecutor.run("iped.engine.task.DuplicateTask", items, confDir);

            assertEquals(1, result.items().size());
            assertEquals(ItemResult.UNSUPPORTED, result.items().get(0).status());
            assertTrue(result.items().get(0).reason().contains("case"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
