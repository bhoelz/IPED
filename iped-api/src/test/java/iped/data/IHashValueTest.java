package iped.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class IHashValueTest {

  @Test
  void toString_whenBytesPresent_thenFormatsAsUppercaseHex() {
    IHashValue hash = new HashA((byte) 0x01, (byte) 0xA2, (byte) 0x0F, (byte) 0xFF);

    assertEquals("01A20FFF", hash.toString());
  }

  @Test
  void compareTo_whenDifferentValues_thenRespectsUnsignedByteOrder() {
    IHashValue lower = new HashA((byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00);
    IHashValue higher = new HashA((byte) 0xFF, (byte) 0x10, (byte) 0x00, (byte) 0x00);

    assertTrue(lower.compareTo(higher) < 0);
    assertTrue(higher.compareTo(lower) > 0);
  }

  @Test
  void equals_whenSameClassAndBytes_thenTrue() {
    IHashValue a = new HashA((byte) 1, (byte) 2, (byte) 3, (byte) 4);
    IHashValue b = new HashA((byte) 1, (byte) 2, (byte) 3, (byte) 4);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void equals_whenDifferentConcreteClassWithSameBytes_thenFalse() {
    IHashValue a = new HashA((byte) 1, (byte) 2, (byte) 3, (byte) 4);
    IHashValue b = new HashB((byte) 1, (byte) 2, (byte) 3, (byte) 4);

    assertNotEquals(a, b);
  }

  private static final class HashA extends IHashValue {
    private final byte[] bytes;

    private HashA(byte... bytes) {
      this.bytes = bytes;
    }

    @Override
    public byte[] getBytes() {
      return bytes;
    }
  }

  private static final class HashB extends IHashValue {
    private final byte[] bytes;

    private HashB(byte... bytes) {
      this.bytes = bytes;
    }

    @Override
    public byte[] getBytes() {
      return bytes;
    }
  }
}
