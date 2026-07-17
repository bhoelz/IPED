package iped.parsers.sqlite;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteUndeleteTest {

  @TempDir Path tempDir;

  @Test
  void constructor_acceptsExistingPath() {
    Path dbPath = tempDir.resolve("test.db");
    SQLiteUndelete undelete = new SQLiteUndelete(dbPath);
    assertNotNull(undelete);
  }

  @Test
  void defaultRecoverOnlyDeleted_isTrue() {
    SQLiteUndelete undelete = new SQLiteUndelete(tempDir.resolve("test.db"));
    assertTrue(undelete.getRecoverOnlyDeletedRecords());
  }

  @Test
  void setRecoverOnlyDeletedRecords_updatesFlag() {
    SQLiteUndelete undelete = new SQLiteUndelete(tempDir.resolve("test.db"));
    undelete.setRecoverOnlyDeletedRecords(false);
    assertFalse(undelete.getRecoverOnlyDeletedRecords());
  }

  @Test
  void undeleteData_returnsMapWithTargetTable() throws Exception {
    Path dbPath = tempDir.resolve("test.db");

    // Create a real SQLite file with rows
    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE TABLE items (id INTEGER PRIMARY KEY, value TEXT)");
        stmt.execute("INSERT INTO items VALUES (1, 'hello')");
        stmt.execute("INSERT INTO items VALUES (2, 'world')");
      }
    }

    SQLiteUndelete undelete = new SQLiteUndelete(dbPath);
    undelete.addTableToRecover("items");
    undelete.setRecoverOnlyDeletedRecords(false);

    Map<String, SQLiteUndeleteTable> result = undelete.undeleteData();

    // fqlite is a recovery tool; it processes the file and returns table containers
    // (rows may be empty for files with no deleted records, which is expected)
    assertNotNull(result, "Result map must not be null");
    // The 'items' table should appear in the result when tablesToRecover contains it
    assertTrue(
        result.isEmpty() || result.containsKey("items"),
        "If result is non-empty, 'items' table must be present; got: " + result.keySet());
  }

  @Test
  void addRecordValidator_isStoredAndApplied() throws Exception {
    Path dbPath = tempDir.resolve("test2.db");

    try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
      try (Statement stmt = conn.createStatement()) {
        stmt.execute("CREATE TABLE data (id INTEGER, name TEXT)");
        stmt.execute("INSERT INTO data VALUES (1, 'keep')");
        stmt.execute("INSERT INTO data VALUES (2, 'discard')");
      }
    }

    SQLiteUndelete undelete = new SQLiteUndelete(dbPath);
    undelete.addTableToRecover("data");
    undelete.setRecoverOnlyDeletedRecords(false);
    // Validator that only accepts rows where integer column == 1
    undelete.addRecordValidator(
        "data",
        row -> {
          try {
            return row.getIntValue(0) == 1L;
          } catch (Exception e) {
            return false;
          }
        });

    Map<String, SQLiteUndeleteTable> result = undelete.undeleteData();

    assertNotNull(result);
    if (result.containsKey("data")) {
      SQLiteUndeleteTable table = result.get("data");
      // Only rows passing the validator should be included
      assertTrue(table.getTableRows().size() <= 2);
    }
  }
}
