package iped.parsers.jdbc;

import iped.parsers.util.IgnoreContentHandler;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JDBCTableReaderTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE people (id INTEGER, name TEXT, age INTEGER)");
            stmt.execute("INSERT INTO people VALUES (1, 'Alice', 30)");
            stmt.execute("INSERT INTO people VALUES (2, 'Bob', 25)");
            stmt.execute("INSERT INTO people VALUES (3, 'Carol', 35)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    void getTableName_returnsConstructorArg() {
        JDBCTableReader reader = new JDBCTableReader(conn, "people", new ParseContext());
        assertEquals("people", reader.getTableName());
    }

    @Test
    void getHeaders_returnsColumnNames() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "people", new ParseContext());
        List<String> headers = reader.getHeaders();
        assertEquals(3, headers.size());
        assertEquals("id", headers.get(0));
        assertEquals("name", headers.get(1));
        assertEquals("age", headers.get(2));
    }

    @Test
    void nextRow_iteratesRows() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "people", new ParseContext());
        IgnoreContentHandler handler = new IgnoreContentHandler();
        ParseContext context = new ParseContext();

        int count = 0;
        while (reader.nextRow(handler, context)) count++;
        assertEquals(3, count);
        reader.closeReader();
    }

    @Test
    void getTableData_returnsResultSet() {
        JDBCTableReader reader = new JDBCTableReader(conn, "people", new ParseContext());
        assertNotNull(reader.getTableData());
    }

    @Test
    void getHeaders_whenTableDoesNotExist_returnsEmpty() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "nonexistent_table", new ParseContext());
        // getTableData() catches the SQLException and returns null, so getHeaders() returns empty list
        List<String> headers = reader.getHeaders();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    void hasDateGuessed_defaultFalse() {
        JDBCTableReader reader = new JDBCTableReader(conn, "people", new ParseContext());
        assertFalse(reader.hasDateGuessed());
    }
}
