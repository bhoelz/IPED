package iped.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TomlPropertiesTest {

    @TempDir
    Path tempDir;

    private Path writeToml(String name, String... lines) throws IOException {
        Path f = tempDir.resolve(name);
        Files.write(f, String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
        return f;
    }

    @Test
    void load_whenScalars_thenFlattenedToCanonicalStrings() throws Exception {
        Path f = writeToml("a.toml",
                "robustImageReading = true",
                "unallocatedFragSize = 1073741824",
                "minOrphanSizeToIgnore = -1",
                "numImageReaders = \"auto\"",
                "skipFolderRegex = \"\"");
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertEquals("true", props.getProperty("robustImageReading"));
        assertEquals("1073741824", props.getProperty("unallocatedFragSize"));
        assertEquals("-1", props.getProperty("minOrphanSizeToIgnore"));
        assertEquals("auto", props.getProperty("numImageReaders"));
        assertEquals("", props.getProperty("skipFolderRegex"));
    }

    @Test
    void load_whenArray_thenJoinedWithLegacySeparator() throws Exception {
        Path f = writeToml("a.toml", "mimesToProcess = [\"audio/3gpp\", \"audio/mp4\", \"audio/ogg\"]");
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertEquals("audio/3gpp; audio/mp4; audio/ogg", props.getProperty("mimesToProcess"));
        assertEquals(List.of("audio/3gpp", "audio/mp4", "audio/ogg"), props.getListProperty("mimesToProcess"));
    }

    @Test
    void load_whenNestedTable_thenDottedKeys() throws Exception {
        Path f = writeToml("a.toml", "[server]", "host = \"localhost\"", "port = 9200");
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertEquals("localhost", props.getProperty("server.host"));
        assertEquals("9200", props.getProperty("server.port"));
    }

    @Test
    void load_whenUTF8Values_thenReadCorrectly() throws Exception {
        Path f = writeToml("a.toml", "name = \"café\"");
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertEquals("café", props.getProperty("name"));
    }

    @Test
    void load_whenMultipleLayers_thenKeyLevelLastWins() throws Exception {
        Path defaults = writeToml("defaults.toml", "a = 1", "b = 2", "c = \"x\"");
        Path deviation = writeToml("deviation.toml", "b = 99");
        TomlProperties props = new TomlProperties();
        props.load(defaults);
        props.load(deviation);
        assertEquals("1", props.getProperty("a"));
        assertEquals("99", props.getProperty("b"));
        assertEquals("x", props.getProperty("c"));
    }

    @Test
    void load_whenInvalidToml_thenThrows() throws Exception {
        Path f = writeToml("bad.toml", "key = ");
        TomlProperties props = new TomlProperties();
        assertThrows(IOException.class, () -> props.load(f));
    }

    @Test
    void load_whenArrayOfTables_thenRejected() throws Exception {
        Path f = writeToml("a.toml", "[[filter]]", "name = \"n\"");
        TomlProperties props = new TomlProperties();
        assertThrows(IOException.class, () -> props.load(f));
    }

    @Test
    void typedAccessors_returnParsedValuesOrDefaults() throws Exception {
        Path f = writeToml("a.toml", "enabled = true", "threads = 8", "size = 1073741824", "ratio = 0.5");
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertTrue(props.getBooleanProperty("enabled", false));
        assertEquals(8, props.getIntProperty("threads", 1));
        assertEquals(1073741824L, props.getLongProperty("size", 0));
        assertEquals(0.5, props.getDoubleProperty("ratio", 0));
        assertFalse(props.getBooleanProperty("missing", false));
        assertEquals(7, props.getIntProperty("missing", 7));
        assertNull(props.getProperty("missing"));
    }

    @Test
    void store_writesFlatTomlReadableBack() throws Exception {
        Path f = tempDir.resolve("out.toml");
        TomlProperties.store(Map.of("enabled", true, "threads", 8, "name", "café"), f);
        TomlProperties props = new TomlProperties();
        props.load(f);
        assertEquals("true", props.getProperty("enabled"));
        assertEquals("8", props.getProperty("threads"));
        assertEquals("café", props.getProperty("name"));
    }
}
