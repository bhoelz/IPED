package iped.tasks.cli;

import iped.data.IItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandaloneItemFactoryTest {

    @Test
    void singleFileHasReadableContentAndMetadata() throws Exception {
        Path tempFile = Files.createTempFile("iped-tasks-cli-test", ".txt");
        try {
            Files.writeString(tempFile, "hello standalone task");

            List<IItem> items = StandaloneItemFactory.fromInput(tempFile.toFile(), false);

            assertEquals(1, items.size());
            IItem item = items.get(0);
            assertEquals(tempFile.toFile().getName(), item.getName());
            assertEquals(Files.size(tempFile), item.getLength());
            assertEquals("hello standalone task", readAll(item));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void directoryNonRecursiveOnlyListsTopLevelFiles() throws Exception {
        Path dir = Files.createTempDirectory("iped-tasks-cli-test-dir");
        try {
            Files.writeString(dir.resolve("a.txt"), "A");
            Path nested = Files.createDirectory(dir.resolve("nested"));
            Files.writeString(nested.resolve("b.txt"), "B");

            List<IItem> items = StandaloneItemFactory.fromInput(dir.toFile(), false);

            assertEquals(1, items.size());
            assertEquals("a.txt", items.get(0).getName());
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    void directoryRecursiveListsNestedFiles() throws Exception {
        Path dir = Files.createTempDirectory("iped-tasks-cli-test-dir");
        try {
            Files.writeString(dir.resolve("a.txt"), "A");
            Path nested = Files.createDirectory(dir.resolve("nested"));
            Files.writeString(nested.resolve("b.txt"), "B");

            List<IItem> items = StandaloneItemFactory.fromInput(dir.toFile(), true);

            assertEquals(2, items.size());
            assertTrue(items.stream().anyMatch(i -> i.getName().equals("a.txt")));
            assertTrue(items.stream().anyMatch(i -> i.getName().equals("b.txt")));
            assertFalse(items.stream().anyMatch(IItem::isDir));
        } finally {
            deleteRecursively(dir);
        }
    }

    private static String readAll(IItem item) throws IOException {
        try (InputStream is = item.getBufferedInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        }
    }
}
