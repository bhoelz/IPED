package iped.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UTF8PropertiesTest {

    @TempDir
    Path tempDir;

    @Test
    void load_whenAsciiProperties_thenParsedCorrectly() throws Exception {
        File f = writeLines("key1 = value1", "key2 = value2");
        UTF8Properties props = new UTF8Properties();
        props.load(f);
        assertEquals("value1", props.getProperty("key1"));
        assertEquals("value2", props.getProperty("key2"));
    }

    @Test
    void load_whenUTF8Values_thenReadCorrectly() throws Exception {
        File f = writeLines("name = café");
        UTF8Properties props = new UTF8Properties();
        props.load(f);
        assertEquals("café", props.getProperty("name"));
    }

    @Test
    void load_whenCommentLine_thenSkipped() throws Exception {
        File f = writeLines("# this is a comment", "key = value");
        UTF8Properties props = new UTF8Properties();
        props.load(f);
        assertNull(props.getProperty("# this is a comment"));
        assertEquals("value", props.getProperty("key"));
    }

    @Test
    void load_whenBlankLine_thenSkipped() throws Exception {
        File f = writeLines("", "key = value");
        UTF8Properties props = new UTF8Properties();
        props.load(f);
        assertEquals("value", props.getProperty("key"));
    }

    @Test
    void load_whenMissingKey_thenNull() throws Exception {
        File f = writeLines("key = value");
        UTF8Properties props = new UTF8Properties();
        props.load(f);
        assertNull(props.getProperty("missing"));
    }

    @Test
    void store_thenLoadRoundTrip() throws Exception {
        UTF8Properties original = new UTF8Properties();
        original.setProperty("greeting", "héllo");
        original.setProperty("number", "42");

        File f = tempDir.resolve("stored.properties").toFile();
        original.store(f);

        UTF8Properties loaded = new UTF8Properties();
        loaded.load(f);

        assertEquals("héllo", loaded.getProperty("greeting"));
        assertEquals("42", loaded.getProperty("number"));
    }

    private File writeLines(String... lines) throws Exception {
        File f = tempDir.resolve("test.properties").toFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(f, StandardCharsets.UTF_8))) {
            for (String line : lines) {
                pw.println(line);
            }
        }
        return f;
    }
}
