package iped.engine.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GraphFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void constants_nodeCsvPrefix() {
        assertEquals("nodes", GraphFileWriter.NODE_CSV_PREFIX);
    }

    @Test
    void constants_replaceName() {
        assertEquals("replace.csv", GraphFileWriter.REPLACE_NAME);
    }

    @Test
    void constructor_withNullDefaultEntity_doesNotThrow() {
        assertDoesNotThrow(() -> {
            GraphFileWriter writer = new GraphFileWriter(tempDir.toFile(), null);
            writer.close(true);
        });
    }

    @Test
    void constructor_createsRootDirectory() throws Exception {
        Path subDir = tempDir.resolve("csvOutput");
        GraphFileWriter writer = new GraphFileWriter(subDir.toFile(), null);
        writer.close(true);
        assertTrue(Files.isDirectory(subDir), "Root directory should be created");
    }

    @Test
    void writeNode_createsCSVFile() throws Exception {
        GraphFileWriter writer = new GraphFileWriter(tempDir.toFile(), null);
        writer.writeNode(DynLabel.label("PERSON"), "nodeId", "abc123");
        writer.close(true);

        File[] files = tempDir.toFile().listFiles();
        assertNotNull(files);
        boolean hasNodesCsv = Arrays.stream(files)
                .anyMatch(f -> f.getName().startsWith(GraphFileWriter.NODE_CSV_PREFIX)
                        && f.getName().endsWith(".csv"));
        assertTrue(hasNodesCsv, "A nodes_*.csv file should be created after writeNode()");
    }

    @Test
    void writeNode_andClose_createReplaceFile() throws Exception {
        GraphFileWriter writer = new GraphFileWriter(tempDir.toFile(), null);
        writer.writeNode(DynLabel.label("PERSON"), "nodeId", "x");
        writer.close(true);

        File replaceFile = new File(tempDir.toFile(), GraphFileWriter.REPLACE_NAME);
        assertTrue(replaceFile.exists(), "replace.csv should be created after close()");
    }

    @Test
    void writeRelationship_createsRelationshipCSVFile() throws Exception {
        GraphFileWriter writer = new GraphFileWriter(tempDir.toFile(), null);

        DynLabel personLabel = DynLabel.label("PERSON");
        DynLabel emailLabel = DynLabel.label("EMAIL");
        DynRelationshipType relType = new DynRelationshipType("SENT");

        writer.writeCreateRelationship(
                personLabel, "nodeId", "alice",
                emailLabel, "nodeId", "alice@example.com",
                relType);
        writer.close(true);

        File[] files = tempDir.toFile().listFiles();
        assertNotNull(files);
        boolean hasRelCsv = Arrays.stream(files)
                .anyMatch(f -> f.getName().startsWith("relationships") && f.getName().endsWith(".csv"));
        assertTrue(hasRelCsv, "A relationships_*.csv file should be created after writeRelationship()");
    }

    @Test
    void writeMultipleNodes_sameLabel_allBuffered() throws Exception {
        GraphFileWriter writer = new GraphFileWriter(tempDir.toFile(), null);
        writer.writeNode(DynLabel.label("PERSON"), "nodeId", "alice");
        writer.writeNode(DynLabel.label("PERSON"), "nodeId", "bob");
        writer.writeNode(DynLabel.label("PERSON"), "nodeId", "carol");
        // Flush only — should not throw even with multiple writes
        assertDoesNotThrow(() -> writer.close(true));
    }
}
