package iped.tasks.cli;

import iped.data.IItem;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the one-level-extraction mechanism directly (without depending
 * on carving signature detection actually triggering, which needs specific
 * byte patterns/config thresholds -- see BaseCarveTask/CarverTask): builds a
 * real IItem and calls processNewItem() the same way
 * BaseCarveTask.addOffsetFile() does, verifying the child ends up on disk
 * with its content and a metadata sidecar.
 */
class CarvingOutputWorkerTest {

    @Test
    void writesCarvedChildContentAndMetadataToOutputDirectory() throws Exception {
        Path sourceFile = Files.createTempFile("iped-carving-test", ".bin");
        Path outputDir = Files.createTempDirectory("iped-carving-test-out");
        Path childrenDir = Files.createTempDirectory("iped-carving-test-children");
        try {
            Files.writeString(sourceFile, "fake carved payload bytes");
            List<IItem> items = StandaloneItemFactory.fromInput(sourceFile.toFile(), false);
            IItem carvedChild = items.get(0);

            CarvingOutputWorker worker = new CarvingOutputWorker(0, outputDir.toFile(), childrenDir.toFile());
            worker.processNewItem(carvedChild);

            File[] written = childrenDir.toFile().listFiles();
            assertTrue(written != null && written.length == 2, "expected the content file + its metadata sidecar");

            File contentFile = null;
            File sidecarFile = null;
            for (File f : written) {
                if (f.getName().endsWith(".metadata.txt")) {
                    sidecarFile = f;
                } else {
                    contentFile = f;
                }
            }
            assertTrue(contentFile != null, "content file should exist");
            assertTrue(sidecarFile != null, "metadata sidecar should exist");

            assertEquals("fake carved payload bytes", Files.readString(contentFile.toPath()));

            String metadata = Files.readString(sidecarFile.toPath());
            assertTrue(metadata.contains("name: " + carvedChild.getName()));
            assertTrue(metadata.contains("parentPath:"));
        } finally {
            Files.deleteIfExists(sourceFile);
            deleteRecursively(outputDir);
            deleteRecursively(childrenDir);
        }
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        }
    }
}
