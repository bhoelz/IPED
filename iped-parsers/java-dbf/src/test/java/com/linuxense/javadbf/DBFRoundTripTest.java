package com.linuxense.javadbf;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Writes a DBF with DBFWriter and reads it back with DBFReader.
 */
public class DBFRoundTripTest {

    // pin a comma-decimal locale to verify the write/read round trip is
    // locale-independent (DBFReader always parses with Double.parseDouble)
    private static Locale defaultLocale;

    @BeforeAll
    static void forceCommaLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(new Locale("pt", "BR"));
    }

    @AfterAll
    static void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    private static DBFField field(String name, byte type, int length, int decimals) {
        DBFField f = new DBFField();
        f.setName(name);
        f.setDataType(type);
        if (type != DBFField.FIELD_TYPE_D && type != DBFField.FIELD_TYPE_L) {
            f.setFieldLength(length);
        }
        if (decimals > 0) {
            f.setDecimalCount(decimals);
        }
        return f;
    }

    private static byte[] writeDbf(DBFField[] fields, Object[]... records) throws DBFException {
        DBFWriter writer = new DBFWriter();
        writer.setFields(fields);
        for (Object[] record : records) {
            writer.addRecord(record);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(out);
        return out.toByteArray();
    }

    @Test
    public void roundTripsCharacterNumericAndLogicalFields() throws Exception {
        DBFField[] fields = {
                field("NAME", DBFField.FIELD_TYPE_C, 10, 0),
                field("AMOUNT", DBFField.FIELD_TYPE_N, 12, 2),
                field("ACTIVE", DBFField.FIELD_TYPE_L, 1, 0),
        };
        byte[] dbf = writeDbf(fields,
                new Object[] { "alice", 12.5, Boolean.TRUE },
                new Object[] { "bob", -3.0, Boolean.FALSE });

        DBFReader reader = new DBFReader(new ByteArrayInputStream(dbf));
        assertEquals(2, reader.getRecordCount());
        assertEquals(3, reader.getFieldCount());
        assertEquals("NAME", reader.getField(0).getName());
        assertEquals('C', reader.getField(0).getDataType());

        Object[] row1 = reader.nextRecord();
        assertEquals("alice", ((String) row1[0]).trim());
        assertEquals(12.5, (Double) row1[1], 0.001);
        assertEquals(Boolean.TRUE, row1[2]);

        Object[] row2 = reader.nextRecord();
        assertEquals("bob", ((String) row2[0]).trim());
        assertEquals(-3.0, (Double) row2[1], 0.001);
        assertEquals(Boolean.FALSE, row2[2]);

        assertNull(reader.nextRecord(), "no third record");
    }

    @Test
    public void roundTripsDateField() throws Exception {
        DBFField[] fields = {
                field("WHEN", DBFField.FIELD_TYPE_D, 8, 0),
        };
        Date date = new GregorianCalendar(2024, Calendar.MARCH, 15).getTime();
        byte[] dbf = writeDbf(fields, new Object[] { date });

        DBFReader reader = new DBFReader(new ByteArrayInputStream(dbf));
        Object[] row = reader.nextRecord();

        Calendar read = Calendar.getInstance();
        read.setTime((Date) row[0]);
        assertEquals(2024, read.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, read.get(Calendar.MONTH));
        assertEquals(15, read.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void nullValuesReadBackAsNull() throws Exception {
        DBFField[] fields = {
                field("NAME", DBFField.FIELD_TYPE_C, 5, 0),
                field("AMOUNT", DBFField.FIELD_TYPE_N, 8, 0),
        };
        byte[] dbf = writeDbf(fields, new Object[] { null, null });

        DBFReader reader = new DBFReader(new ByteArrayInputStream(dbf));
        Object[] row = reader.nextRecord();
        assertEquals("", ((String) row[0]).trim());
        assertNull(row[1]);
    }

    @Test
    public void writerRejectsWrongValueType() throws Exception {
        DBFWriter writer = new DBFWriter();
        writer.setFields(new DBFField[] { field("N", DBFField.FIELD_TYPE_N, 8, 0) });
        assertThrows(DBFException.class, () -> writer.addRecord(new Object[] { "not a number" }));
    }

    @Test
    public void writerRejectsWrongFieldCount() throws Exception {
        DBFWriter writer = new DBFWriter();
        writer.setFields(new DBFField[] { field("A", DBFField.FIELD_TYPE_C, 5, 0) });
        assertThrows(DBFException.class, () -> writer.addRecord(new Object[] { "x", "y" }));
    }

    @Test
    public void writerRequiresFieldsBeforeRecords() {
        DBFWriter writer = new DBFWriter();
        assertThrows(DBFException.class, () -> writer.addRecord(new Object[] { "x" }));
        assertThrows(DBFException.class, () -> writer.setFields(new DBFField[0]));
    }
}
