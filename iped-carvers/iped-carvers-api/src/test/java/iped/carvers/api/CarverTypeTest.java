package iped.carvers.api;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iped.carvers.api.Signature.SignatureType;
import org.junit.jupiter.api.Test;

public class CarverTypeTest {

  @Test
  public void decodesPlainAsciiHeader() throws Exception {
    CarverType type = new CarverType();
    type.addHeader("ABC");

    Signature sig = type.getSignatures().get(0);
    assertTrue(sig.isHeader());
    assertFalse(sig.isFooter());
    assertEquals("ABC", sig.getSigString());
    assertEquals(3, sig.getLength());
    assertEquals(1, sig.seqs.length);
    assertArrayEquals(new byte[] {'A', 'B', 'C'}, sig.seqs[0]);
    assertArrayEquals(new int[] {3}, sig.seqEndPos);
  }

  @Test
  public void decodesHexEscapes() throws Exception {
    CarverType type = new CarverType();
    type.addHeader("\\FF\\D8\\FF");

    Signature sig = type.getSignatures().get(0);
    assertEquals(3, sig.getLength());
    assertArrayEquals(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, sig.seqs[0]);
    assertArrayEquals(new int[] {3}, sig.seqEndPos);
  }

  @Test
  public void wildcardSplitsSignatureIntoSequences() throws Exception {
    CarverType type = new CarverType();
    type.addHeader("AB?CD");

    Signature sig = type.getSignatures().get(0);
    assertEquals(5, sig.getLength());
    assertEquals(2, sig.seqs.length);
    assertArrayEquals(new byte[] {'A', 'B'}, sig.seqs[0]);
    assertArrayEquals(new byte[] {'C', 'D'}, sig.seqs[1]);
    // first sequence ends at the wildcard, second at the end of the signature
    assertArrayEquals(new int[] {2, 5}, sig.seqEndPos);
  }

  @Test
  public void trailingWildcardKeepsFullLength() throws Exception {
    CarverType type = new CarverType();
    type.addHeader("AB?");

    Signature sig = type.getSignatures().get(0);
    assertEquals(3, sig.getLength());
    assertEquals(1, sig.seqs.length);
    assertArrayEquals(new byte[] {'A', 'B'}, sig.seqs[0]);
    assertArrayEquals(new int[] {2}, sig.seqEndPos);
  }

  @Test
  public void addFooterSetsFooterFlag() throws Exception {
    CarverType type = new CarverType();
    assertFalse(type.hasFooter());

    type.addFooter("XY");

    assertTrue(type.hasFooter());
    assertTrue(type.getSignatures().get(0).isFooter());
  }

  @Test
  public void addSignatureTracksFooterAndLengthRef() throws Exception {
    CarverType type = new CarverType();
    assertFalse(type.hasLengthRef());

    type.addSignature("AA", SignatureType.LENGTHREF);
    assertTrue(type.hasLengthRef());
    assertFalse(type.hasFooter());

    type.addSignature("BB", SignatureType.FOOTER);
    assertTrue(type.hasFooter());
    assertEquals(2, type.getSignatures().size());
  }

  @Test
  public void equalityIsByName() {
    CarverType a = new CarverType();
    a.setName("jpeg");
    CarverType b = new CarverType();
    b.setName("jpeg");
    CarverType c = new CarverType();
    c.setName("png");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, c);
    assertNotEquals(a, "jpeg");
  }
}
