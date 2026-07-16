package iped.engine.additionalindex;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class AdditionalIndexSourceOfTruthTest {

    @Test
    void additionalEntriesKeepTheOriginalEvidenceItemId() throws Exception {
        var path = Files.createTempDirectory("additional-index-source-of-truth");
        try (var source = new LuceneAdditionalDataSource(path)) {
            source.storeTaskResult(42, "ExampleTask", java.util.Map.of("answer", "ok"));
            source.commit();

            var result = source.getTaskResult(42, "ExampleTask");
            assertTrue(result.isPresent());
            assertEquals("ExampleTask", result.get().taskName());
            assertTrue(source.hasTaskResult(42, "ExampleTask"));
            assertFalse(source.hasTaskResult(43, "ExampleTask"));
        } finally {
            try (var files = Files.walk(path)) {
                files.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                });
            }
        }
    }
}
