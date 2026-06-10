package com.linuxense.javadbf;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UtilsTest {

    // pin a comma-decimal locale to verify doubleFormating always emits a dot
    // separator regardless of the JVM default locale
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

    @Test
    public void readsLittleEndianInt() throws Exception {
        byte[] bytes = { 0x78, 0x56, 0x34, 0x12 };
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        assertEquals(0x12345678, Utils.readLittleEndianInt(in));
    }

    @Test
    public void readsLittleEndianShort() throws Exception {
        byte[] bytes = { 0x34, 0x12 };
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        assertEquals(0x1234, Utils.readLittleEndianShort(in));
    }

    @Test
    public void littleEndianSwapsShortBytes() {
        assertEquals((short) 0x3412, Utils.littleEndian((short) 0x1234));
        assertEquals((short) 0x1234, Utils.littleEndian((short) 0x3412));
    }

    @Test
    public void littleEndianSwapsIntBytes() {
        assertEquals(0x78563412, Utils.littleEndian(0x12345678));
        assertEquals(0x12345678, Utils.littleEndian(0x78563412));
    }

    @Test
    public void trimLeftSpacesRemovesSpaces() {
        assertArrayEquals("123".getBytes(), Utils.trimLeftSpaces("  123".getBytes()));
        assertArrayEquals(new byte[0], Utils.trimLeftSpaces("   ".getBytes()));
        assertArrayEquals("abc".getBytes(), Utils.trimLeftSpaces("abc".getBytes()));
    }

    @Test
    public void containsFindsByte() {
        byte[] arr = { 1, 2, 3 };
        assertTrue(Utils.contains(arr, (byte) 2));
        assertFalse(Utils.contains(arr, (byte) 9));
        assertFalse(Utils.contains(new byte[0], (byte) 0));
    }

    @Test
    public void textPaddingPadsLeftAlignedByDefault() throws Exception {
        byte[] padded = Utils.textPadding("ab", "ISO-8859-1", 5);
        assertArrayEquals("ab   ".getBytes("ISO-8859-1"), padded);
    }

    @Test
    public void textPaddingPadsRightAligned() throws Exception {
        byte[] padded = Utils.textPadding("ab", "ISO-8859-1", 5, Utils.ALIGN_RIGHT);
        assertArrayEquals("   ab".getBytes("ISO-8859-1"), padded);
    }

    @Test
    public void textPaddingTruncatesLongText() throws Exception {
        byte[] padded = Utils.textPadding("abcdef", "ISO-8859-1", 3);
        assertArrayEquals("abc".getBytes("ISO-8859-1"), padded);
    }

    @Test
    public void doubleFormatingAlignsRight() throws Exception {
        byte[] formatted = Utils.doubleFormating(12.5, "ISO-8859-1", 8, 2);
        String s = new String(formatted, "ISO-8859-1");
        assertEquals(8, s.length());
        assertTrue(s.endsWith("12.50"), "got: '" + s + "'");
    }
}
