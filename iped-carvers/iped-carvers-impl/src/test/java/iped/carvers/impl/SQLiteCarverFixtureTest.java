package iped.carvers.impl;

import static org.junit.jupiter.api.Assertions.*;

import iped.carvers.api.CarverType;
import iped.carvers.api.Signature;
import iped.carvers.custom.SQLiteCarver;
import org.apache.commons.codec.DecoderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fixture tests for SQLiteCarver: verifies CarverType metadata and that the SQLite magic signature
 * decodes correctly via the CarverType API.
 */
class SQLiteCarverFixtureTest {

  private SQLiteCarver carver;

  @BeforeEach
  void setUp() throws DecoderException {
    carver = new SQLiteCarver();
  }

  @Test
  void exposesOneCarverType() {
    assertNotNull(carver.getCarverTypes());
    assertEquals(1, carver.getCarverTypes().length);
  }

  @Test
  void carverTypeHasSqliteMimeType() {
    assertEquals("application/x-sqlite3", carver.getCarverTypes()[0].getMimeType().toString());
  }

  @Test
  void sizeLimitsAreSet() {
    CarverType ct = carver.getCarverTypes()[0];
    assertNotNull(ct.getMinLength(), "minLength must be set");
    assertNotNull(ct.getMaxLength(), "maxLength must be set");
    assertTrue(ct.getMinLength() > 0);
    assertTrue(ct.getMaxLength() > ct.getMinLength());
  }

  @Test
  void hasAtLeastOneHeaderSignature() {
    CarverType ct = carver.getCarverTypes()[0];
    assertNotNull(ct.getSignatures());
    long headers = ct.getSignatures().stream().filter(Signature::isHeader).count();
    assertTrue(headers >= 1, "at least one header signature required");
  }

  @Test
  void headerSignatureDecodesToSixteenBytes() {
    // "SQLite format 3\0" is 16 bytes
    CarverType ct = carver.getCarverTypes()[0];
    Signature header =
        ct.getSignatures().stream()
            .filter(Signature::isHeader)
            .findFirst()
            .orElseThrow(() -> new AssertionError("no header signature"));
    assertEquals(
        16, header.getLength(), "SQLite magic 'SQLite format 3\\0' must decode to 16 bytes");
  }

  @Test
  void noFooterSignaturePresent() {
    CarverType ct = carver.getCarverTypes()[0];
    long footers = ct.getSignatures().stream().filter(Signature::isFooter).count();
    assertEquals(0, footers, "SQLiteCarver uses length-from-header, not footer matching");
  }
}
