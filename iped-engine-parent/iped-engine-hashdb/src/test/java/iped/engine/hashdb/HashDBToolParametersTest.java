package iped.engine.hashdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests parameter parsing in HashDBTool.run() without hitting the database.
 * All test cases trigger parseParameters() to return false, so the method
 * returns before any file I/O or System.exit() is called.
 */
class HashDBToolParametersTest {

    @TempDir
    Path tempDir;

    @Test
    void run_whenNoArgs_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{}));
    }

    @Test
    void run_whenUnknownParam_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-unknown"}));
    }

    @Test
    void run_whenDWithoutValue_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-d"}));
    }

    @Test
    void run_whenOWithoutValue_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-o"}));
    }

    @Test
    void run_whenDWithNonExistentFile_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-d", "/no/such/file/exists.csv", "-o", "out.db"}));
    }

    @Test
    void run_whenMissingO_thenFalse() throws Exception {
        File input = tempDir.resolve("in.csv").toFile();
        Files.writeString(input.toPath(), "MD5,status\n");
        assertFalse(new HashDBTool().run(new String[]{"-d", input.getAbsolutePath()}));
    }

    @Test
    void run_whenMissingD_thenFalse() {
        String outPath = tempDir.resolve("out.db").toString();
        assertFalse(new HashDBTool().run(new String[]{"-o", outPath}));
    }

    @Test
    void run_whenDuplicateO_thenFalse() throws Exception {
        File input = tempDir.resolve("in.csv").toFile();
        Files.writeString(input.toPath(), "MD5,status\n");
        String out1 = tempDir.resolve("out1.db").toString();
        String out2 = tempDir.resolve("out2.db").toString();
        assertFalse(new HashDBTool().run(new String[]{
            "-d", input.getAbsolutePath(), "-o", out1, "-o", out2
        }));
    }

    @Test
    void run_whenConflictingModes_thenFalse() throws Exception {
        File input = tempDir.resolve("in.csv").toFile();
        Files.writeString(input.toPath(), "MD5,status\n");
        String out = tempDir.resolve("out.db").toString();
        assertFalse(new HashDBTool().run(new String[]{
            "-d", input.getAbsolutePath(), "-o", out, "-replace", "-remove"
        }));
    }

    @Test
    void run_whenDelimiterMissingValue_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-delimiter"}));
    }

    @Test
    void run_whenSkipColMissingValue_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-skipCol"}));
    }

    @Test
    void run_whenRenameColMissingValues_thenFalse() {
        assertFalse(new HashDBTool().run(new String[]{"-renameCol", "old"}));
    }
}
