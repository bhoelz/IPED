package iped.parsers.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcatenatedResultSetAdapterTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE t (id INTEGER, name TEXT)");
            stmt.execute("INSERT INTO t VALUES (1, 'Alice')");
            stmt.execute("INSERT INTO t VALUES (2, 'Bob')");
            stmt.execute("INSERT INTO t VALUES (3, 'Carol')");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (conn != null) conn.close();
    }

    private ResultSet query(String sql) throws SQLException {
        return conn.createStatement().executeQuery(sql);
    }

    @Test
    void next_traversesBothResultSets() throws SQLException {
        ResultSet rs1 = query("SELECT id, name FROM t WHERE id <= 2");
        ResultSet rs2 = query("SELECT id, name FROM t WHERE id = 3");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);

        int count = 0;
        while (combined.next()) count++;
        assertEquals(3, count);
        combined.close();
    }

    @Test
    void next_firstResultSetExhausted_continuesWithSecond() throws SQLException {
        ResultSet rs1 = query("SELECT id, name FROM t WHERE id = 1");
        ResultSet rs2 = query("SELECT id, name FROM t WHERE id = 2");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);

        assertTrue(combined.next());
        assertEquals(1, combined.getInt(1));
        assertEquals("Alice", combined.getString(2));

        assertTrue(combined.next());
        assertEquals(2, combined.getInt(1));
        assertEquals("Bob", combined.getString(2));

        assertFalse(combined.next());
        combined.close();
    }

    @Test
    void next_whenBothEmpty_thenImmediatelyFalse() throws SQLException {
        ResultSet rs1 = query("SELECT id FROM t WHERE id = 999");
        ResultSet rs2 = query("SELECT id FROM t WHERE id = 998");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);

        assertFalse(combined.next());
        combined.close();
    }

    @Test
    void close_doesNotThrow() throws SQLException {
        ResultSet rs1 = query("SELECT id FROM t WHERE id = 1");
        ResultSet rs2 = query("SELECT id FROM t WHERE id = 2");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);
        assertDoesNotThrow(combined::close);
    }

    @Test
    void absolute_alwaysThrowsSQLException() throws SQLException {
        ResultSet rs1 = query("SELECT id FROM t WHERE id = 1");
        ResultSet rs2 = query("SELECT id FROM t WHERE id = 2");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);
        // ConcatenatedResultSetAdapter.absolute() always throws — by design
        assertThrows(SQLException.class, () -> combined.absolute(1));
        combined.close();
    }

    @Test
    void absolute_throwsSQLException() throws SQLException {
        ResultSet rs1 = query("SELECT id FROM t WHERE id = 1");
        ResultSet rs2 = query("SELECT id FROM t WHERE id = 2");
        ConcatenatedResultSetAdapter combined = new ConcatenatedResultSetAdapter(rs1, rs2);
        assertThrows(SQLException.class, () -> combined.absolute(1));
        combined.close();
    }
}
