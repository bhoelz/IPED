package iped.engine.hashdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for HashDBTool.run() — creates a real CSV file in a temp
 * directory, runs the tool, and verifies the resulting SQLite database
 * contains the expected rows.
 */
class HashDBToolIntegrationTest {

    @TempDir
    Path tempDir;

    private HashDBTool tool;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (tool != null) {
            tool.finish(false); // rollback + close connection safely
            tool = null;
        }
        // On Windows, SQLite's EXCLUSIVE locking mode can hold file handles briefly after
        // connection.close(). Force GC + short sleep so @TempDir cleanup succeeds.
        System.gc();
        System.runFinalization();
        Thread.sleep(200);
    }

    @Test
    void run_withMinimalMd5Csv_insertsHashIntoDatabase() throws Exception {
        // CSV with one MD5 hash and one property column.
        // Using MD5 of "hello" — NOT the zero-length hash (d41d8cd9...) which is skipped by design.
        Path csvFile = tempDir.resolve("hashes.csv");
        Files.writeString(csvFile,
                "MD5,status\r\n" +
                "5d41402abc4b2a76b9719d911017c592,known\r\n",
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("hashdb.db");

        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        assertTrue(success, "run() should return true for a valid CSV file");

        tool.finish(true);
        tool = null; // prevent double-finish in tearDown

        // Verify database contents
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
            // HASHES table should have one row
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM HASHES")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "HASHES table should have exactly 1 row");
            }

            // PROPERTIES table should have at least one row (the 'status' property)
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PROPERTIES")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 1, "PROPERTIES table should have at least 1 row");
            }

            // HASHES_PROPERTIES should link the hash to its property
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM HASHES_PROPERTIES")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "HASHES_PROPERTIES should have exactly 1 row");
            }
        }
    }

    @Test
    void run_withSha1Csv_insertsHashIntoDatabase() throws Exception {
        // SHA1 of "hello" (NOT the zero-length SHA1 which is da39a3ee... and is skipped by design)
        Path csvFile = tempDir.resolve("sha1_hashes.csv");
        Files.writeString(csvFile,
                "SHA1,category\r\n" +
                "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d,safe\r\n",
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("sha1db.db");

        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        assertTrue(success, "run() should return true for a valid SHA1 CSV file");

        tool.finish(true);
        tool = null;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM HASHES")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    void run_withMultipleHashes_insertsAllRows() throws Exception {
        // Use 3 real non-zero-length file hashes (zero-length hashes are skipped by design)
        Path csvFile = tempDir.resolve("multi.csv");
        Files.writeString(csvFile,
                "MD5,status\r\n" +
                "5d41402abc4b2a76b9719d911017c592,safe\r\n" +    // MD5("hello")
                "7d793037a0760186574b0282f2f435e7,known\r\n" +   // MD5("world")
                "900150983cd24fb0d6963f7d28e17f72,flagged\r\n",  // MD5("abc")
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("multi.db");

        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        assertTrue(success);

        tool.finish(true);
        tool = null;

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath())) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM HASHES")) {
                assertTrue(rs.next());
                assertEquals(3, rs.getInt(1), "All 3 hashes should be inserted");
            }
        }
    }

    @Test
    void run_withMissingInputFile_returnsFalse() {
        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", tempDir.resolve("nonexistent.csv").toAbsolutePath().toString(),
                "-o", tempDir.resolve("out.db").toAbsolutePath().toString(),
                "-noOpt"
        });
        assertFalse(success, "run() should return false when input file does not exist");
    }

    @Test
    void run_withNoHashColumn_returnsFalse() throws Exception {
        // A CSV with no recognized hash column
        Path csvFile = tempDir.resolve("no_hash.csv");
        Files.writeString(csvFile,
                "name,value\r\n" +
                "alice,42\r\n",
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("nohash.db");

        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        assertFalse(success, "run() should return false when CSV has no hash column");
    }

    @Test
    void run_withNoPropertyColumn_returnsFalse() throws Exception {
        // A CSV with only a hash column, no property
        Path csvFile = tempDir.resolve("only_hash.csv");
        Files.writeString(csvFile,
                "MD5\r\n" +
                "d41d8cd98f00b204e9800998ecf8427e\r\n",
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("noprop.db");

        tool = new HashDBTool();
        boolean success = tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        assertFalse(success, "run() should return false when CSV has no property column");
    }

    @Test
    void run_createsOutputDatabase() throws Exception {
        Path csvFile = tempDir.resolve("simple.csv");
        Files.writeString(csvFile,
                "MD5,status\r\n" +
                "5d41402abc4b2a76b9719d911017c592,known\r\n",  // MD5("hello")
                StandardCharsets.UTF_8);

        Path dbFile = tempDir.resolve("created.db");
        assertFalse(dbFile.toFile().exists(), "DB file should not exist before run()");

        tool = new HashDBTool();
        tool.run(new String[]{
                "-d", csvFile.toAbsolutePath().toString(),
                "-o", dbFile.toAbsolutePath().toString(),
                "-noOpt"
        });
        tool.finish(true);
        tool = null;

        assertTrue(dbFile.toFile().exists(), "DB file should be created by run()");
    }
}
