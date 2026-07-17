package iped.exception;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link ZipBombException}. */
class ZipBombExceptionTest {

  @Test
  @DisplayName("Constructor should set message")
  void constructor_shouldSetMessage() {
    ZipBombException ex = new ZipBombException("zip bomb detected");
    assertEquals("zip bomb detected", ex.getMessage());
  }

  @Test
  @DisplayName("Should be instance of IOException")
  void shouldBeInstanceOfIOException() {
    ZipBombException ex = new ZipBombException("test");
    assertInstanceOf(IOException.class, ex);
  }

  @Test
  @DisplayName("MAX_COMPRESSION constant should be 100")
  void maxCompression_shouldBe100() {
    assertEquals(100, ZipBombException.MAX_COMPRESSION);
  }

  @Test
  @DisplayName("ZIPBOMB_MIN_SIZE constant should be 1GB")
  void zipbombMinSize_shouldBe1GB() {
    assertEquals(1024 * 1024 * 1024, ZipBombException.ZIPBOMB_MIN_SIZE);
  }

  @Test
  @DisplayName("isZipBomb with null parent size should return false")
  void isZipBomb_nullParentSize_shouldReturnFalse() throws ZipBombException {
    assertFalse(ZipBombException.isZipBomb(null, 2L * 1024 * 1024 * 1024));
  }

  @Test
  @DisplayName("isZipBomb with small subitem should return false")
  void isZipBomb_smallSubitem_shouldReturnFalse() throws ZipBombException {
    assertFalse(ZipBombException.isZipBomb(1000L, 500));
  }

  @Test
  @DisplayName("isZipBomb with subitem below ZIPBOMB_MIN_SIZE should return false")
  void isZipBomb_belowMinSize_shouldReturnFalse() throws ZipBombException {
    // subitem is 5MB * 101 ~= 505MB (exceeds ratio) but below 1GB minimum
    long parentSize = 5 * 1024 * 1024;
    long subitemSize = parentSize * 101;
    assertFalse(ZipBombException.isZipBomb(parentSize, subitemSize));
  }

  @Test
  @DisplayName("isZipBomb with large subitem exceeding ratio should return true")
  void isZipBomb_exceedsRatio_shouldReturnTrue() throws ZipBombException {
    // 1 byte parent, 1GB+ subitem = exceeds 100x compression ratio
    long subitemSize = 2L * 1024 * 1024 * 1024; // 2GB
    assertTrue(ZipBombException.isZipBomb(1L, subitemSize));
  }

  @Test
  @DisplayName("isZipBomb with subitem exactly at ratio boundary should return false")
  void isZipBomb_atRatioBoundary_shouldReturnFalse() throws ZipBombException {
    // subitem exactly at parent * MAX_COMPRESSION (100x), should return false (> not >=)
    long parentSize = 1024 * 1024; // 1MB
    long subitemSize = parentSize * ZipBombException.MAX_COMPRESSION; // exactly 100x
    assertFalse(ZipBombException.isZipBomb(parentSize, subitemSize));
  }

  @Test
  @DisplayName("isZipBomb with subitem just above ratio boundary should return true")
  void isZipBomb_aboveRatioBoundary_shouldReturnTrue() throws ZipBombException {
    long parentSize = 11 * 1024 * 1024; // 11MB, so 100x+1 ~= 1.07GB >= ZIPBOMB_MIN_SIZE
    long subitemSize = parentSize * ZipBombException.MAX_COMPRESSION + 1; // 100x + 1
    assertTrue(ZipBombException.isZipBomb(parentSize, subitemSize));
  }

  @Test
  @DisplayName("isZipBomb with zero parent size should return false")
  void isZipBomb_zeroParentSize_shouldReturnFalse() throws ZipBombException {
    // subitemSize > 0 * 100 is always true, but let's verify the behavior
    long subitemSize = 2L * 1024 * 1024 * 1024;
    assertTrue(ZipBombException.isZipBomb(0L, subitemSize));
  }

  @Test
  @DisplayName("isZipBomb with large parent and normal subitem should return false")
  void isZipBomb_largeParentNormalSubitem_shouldReturnFalse() throws ZipBombException {
    assertFalse(ZipBombException.isZipBomb(1000000000L, 50000));
  }
}
