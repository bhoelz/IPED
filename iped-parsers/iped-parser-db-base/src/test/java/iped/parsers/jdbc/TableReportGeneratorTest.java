package iped.parsers.jdbc;

import iped.parsers.util.IgnoreContentHandler;
import org.apache.tika.io.TemporaryResources;
import org.apache.tika.parser.ParseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableReportGeneratorTest {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE products (id INTEGER, name TEXT, price REAL)");
            stmt.execute("INSERT INTO products VALUES (1, 'Widget', 9.99)");
            stmt.execute("INSERT INTO products VALUES (2, 'Gadget', 19.99)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    void getCols_afterCreateHtmlReport_returnsColumnCount() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "products", new ParseContext());
        TableReportGenerator gen = new TableReportGenerator(reader);
        TemporaryResources tmp = new TemporaryResources();
        try {
            gen.createHtmlReport(100, new IgnoreContentHandler(), new ParseContext(), tmp);
            assertEquals(3, gen.getCols());
        } finally {
            tmp.close();
        }
    }

    @Test
    void getRows_afterCreateHtmlReport_returnsRowCount() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "products", new ParseContext());
        TableReportGenerator gen = new TableReportGenerator(reader);
        TemporaryResources tmp = new TemporaryResources();
        try {
            gen.createHtmlReport(100, new IgnoreContentHandler(), new ParseContext(), tmp);
            assertEquals(2, gen.getRows());
        } finally {
            tmp.close();
        }
    }

    @Test
    void createHtmlReport_producesHtmlWithTable() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "products", new ParseContext());
        TableReportGenerator gen = new TableReportGenerator(reader);
        TemporaryResources tmp = new TemporaryResources();
        try (InputStream is = gen.createHtmlReport(100, new IgnoreContentHandler(), new ParseContext(), tmp)) {
            String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains("<table"), "Expected <table in output");
            assertTrue(html.contains("<th>"), "Expected <th> headers in output");
            assertTrue(html.contains("<tr>"), "Expected <tr> rows in output");
        } finally {
            tmp.close();
        }
    }

    @Test
    void createHtmlReport_maxRowsLimitsOutput() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "products", new ParseContext());
        TableReportGenerator gen = new TableReportGenerator(reader);
        TemporaryResources tmp = new TemporaryResources();
        try {
            gen.createHtmlReport(1, new IgnoreContentHandler(), new ParseContext(), tmp);
            assertEquals(1, gen.getRows()); // only 1 row even though 2 exist
        } finally {
            tmp.close();
        }
    }

    @Test
    void getTotRows_accumulatesAcrossCalls() throws Exception {
        JDBCTableReader reader = new JDBCTableReader(conn, "products", new ParseContext());
        TableReportGenerator gen = new TableReportGenerator(reader);
        TemporaryResources tmp = new TemporaryResources();
        try {
            gen.createHtmlReport(1, new IgnoreContentHandler(), new ParseContext(), tmp);
            assertEquals(1, gen.getTotRows());
        } finally {
            tmp.close();
        }
    }
}
