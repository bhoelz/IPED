package iped.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HashValueTest {

  @Test
  void constructor_byteArray_thenGetBytesReturnsSame() {
    byte[] bytes = {0x01, (byte) 0xA2, 0x0F, (byte) 0xFF};
    HashValue hv = new HashValue(bytes);
    assertArrayEquals(bytes, hv.getBytes());
  }

  @Test
  void constructor_hexString_thenGetBytesCorrect() {
    HashValue hv = new HashValue("01A20FFF");
    byte[] expected = {0x01, (byte) 0xA2, 0x0F, (byte) 0xFF};
    assertArrayEquals(expected, hv.getBytes());
  }

  @Test
  void constructor_hexStringLowercase_thenGetBytesCorrect() {
    HashValue hv = new HashValue("01a20fff");
    byte[] expected = {0x01, (byte) 0xA2, 0x0F, (byte) 0xFF};
    assertArrayEquals(expected, hv.getBytes());
  }

  @Test
  void constructor_invalidHexString_thenThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new HashValue("ZZZZ"));
  }

  @Test
  void toString_formatsAsUppercaseHex() {
    HashValue hv = new HashValue(new byte[] {0x00, (byte) 0xFF, 0x10});
    assertEquals("00FF10", hv.toString());
  }

  @Test
  void equals_sameBytes_thenTrue() {
    HashValue a = new HashValue("DEADBEEF");
    HashValue b = new HashValue("deadbeef");
    assertEquals(a, b);
  }

  @Test
  void equals_differentBytes_thenFalse() {
    HashValue a = new HashValue("DEADBEEF");
    HashValue b = new HashValue("CAFEBABE");
    assertNotEquals(a, b);
  }

  @Test
  void hashCode_sameBytes_thenSame() {
    HashValue a = new HashValue("ABCD1234");
    HashValue b = new HashValue("ABCD1234");
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void compareTo_ordersCorrectly() {
    HashValue lower = new HashValue("00000001");
    HashValue higher = new HashValue("FF000000");
    assertTrue(lower.compareTo(higher) < 0);
    assertTrue(higher.compareTo(lower) > 0);
    assertEquals(0, lower.compareTo(lower));
  }
}
