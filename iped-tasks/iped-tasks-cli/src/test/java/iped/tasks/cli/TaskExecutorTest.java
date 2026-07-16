package iped.tasks.cli;

import iped.data.IItem;
import iped.configuration.Configurable;
import iped.engine.config.ConfigurationManager;
import iped.engine.task.AbstractTask;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.tika.mime.MediaType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

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

    @Test
    void reportsNpeFromCompatibleTaskAsErrorButKeepsUnclearTaskHeuristic() throws Exception {
        Path tempFile = Files.createTempFile("iped-tasks-cli-npe-test", ".txt");
        try {
            Files.writeString(tempFile, "content");
            List<IItem> items = StandaloneItemFactory.fromInput(tempFile.toFile(), false);
            File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();

            RunResult compatibleResult = TaskExecutor.run(CompatibleNpeTask.class.getName(), items, confDir, null,
                    new TaskCompatibility.Classification(TaskCompatibility.Status.COMPATIBLE, "test-compatible"));
            assertEquals(ItemResult.ERROR, compatibleResult.items().get(0).status());

            RunResult unclearResult = TaskExecutor.run(UnclearNpeTask.class.getName(), items, confDir, null,
                    new TaskCompatibility.Classification(TaskCompatibility.Status.UNCLEAR, "test-unclear"));
            assertEquals(ItemResult.UNSUPPORTED, unclearResult.items().get(0).status());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void createsThumbnailsForTwoRealImagesWithoutCaseState() throws Exception {
        Path tempDir = Files.createTempDirectory("iped-tasks-cli-image-thumb-test");
        try {
            IItem first = standaloneJpeg(tempDir.resolve("first.jpg"), uniqueHash());
            IItem second = standaloneJpeg(tempDir.resolve("second.jpg"), uniqueHash());
            File confDir = new File("../../target/release/iped-4.4.0-SNAPSHOT").getCanonicalFile();

            RunResult result = TaskExecutor.run("iped.engine.task.ImageThumbTask", List.of(first, second), confDir);

            assertEquals(2, result.items().size());
            assertTrue(result.items().stream().allMatch(item -> ItemResult.OK.equals(item.status())),
                    () -> "unexpected standalone image results: " + result.items());
            assertNotNull(first.getThumb(), "first image should have a generated thumbnail");
            assertTrue(first.getThumb().length > 0, "first thumbnail should contain JPEG data");
            assertNotNull(second.getThumb(), "second image should share the standalone accumulator before finish");
            assertTrue(second.getThumb().length > 0, "second thumbnail should contain JPEG data");
        } finally {
            try (var files = Files.walk(tempDir)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    private static IItem standaloneJpeg(Path path, String hash) throws Exception {
        BufferedImage image = new BufferedImage(20, 15, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, ((x * 12) << 16) | ((y * 16) << 8) | ((x + y) * 7));
            }
        }
        assertTrue(ImageIO.write(image, "jpg", path.toFile()));

        IItem item = StandaloneItemFactory.fromInput(path.toFile(), false).getFirst();
        item.setMediaType(MediaType.image("jpeg"));
        item.setHash(hash);
        item.setAddToCase(true);
        return item;
    }

    private static String uniqueHash() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return uuid + uuid;
    }

    public static class CompatibleNpeTask extends AbstractTask {
        @Override
        public List<Configurable<?>> getConfigurables() {
            return List.of();
        }

        @Override
        public void init(ConfigurationManager configurationManager) {
        }

        @Override
        public void finish() {
        }

        @Override
        protected void process(IItem evidence) {
            throw new NullPointerException("compatible task defect");
        }
    }

    public static final class UnclearNpeTask extends CompatibleNpeTask {
    }
}
