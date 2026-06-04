package iped.parsers.sqlite;

import fqlite.base.SqliteRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SQLiteRecordValidatorTest {

    private static SqliteRow rowWithData(Object... data) {
        SqliteRow row = new SqliteRow();
        row.setRowData(data);
        return row;
    }

    @Test
    void canBeImplementedAsLambda() {
        SQLiteRecordValidator alwaysTrue = row -> true;
        assertNotNull(alwaysTrue);
    }

    @Test
    void lambdaAlwaysTrue_withNullRow_returnsTrue() {
        SQLiteRecordValidator alwaysTrue = row -> true;
        assertTrue(alwaysTrue.validateRecord(null));
    }

    @Test
    void lambdaAlwaysFalse_withNullRow_returnsFalse() {
        SQLiteRecordValidator alwaysFalse = row -> false;
        assertFalse(alwaysFalse.validateRecord(null));
    }

    @Test
    void lambdaCheckNull_rejectsNullRow() {
        SQLiteRecordValidator rejectNull = row -> row != null;
        assertFalse(rejectNull.validateRecord(null));
    }

    @Test
    void lambdaCheckNull_acceptsRealRow() {
        SQLiteRecordValidator rejectNull = row -> row != null;
        assertTrue(rejectNull.validateRecord(rowWithData(1L, "hello")));
    }

    @Test
    void lambdaCheckIntValue_filtersCorrectly() {
        SQLiteRecordValidator onlyPositive = row -> row.getIntValue(0) > 0;
        assertTrue(onlyPositive.validateRecord(rowWithData(42L)));
        assertFalse(onlyPositive.validateRecord(rowWithData(-1L)));
    }

    @Test
    void isFunctionalInterface() {
        long abstractMethods = java.util.Arrays.stream(SQLiteRecordValidator.class.getMethods())
                .filter(m -> java.lang.reflect.Modifier.isAbstract(m.getModifiers()))
                .count();
        assertEquals(1, abstractMethods);
    }
}
