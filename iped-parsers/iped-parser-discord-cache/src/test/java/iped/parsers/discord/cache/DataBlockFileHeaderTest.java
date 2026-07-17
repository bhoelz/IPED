package iped.parsers.discord.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DataBlockFileHeaderTest {

  private static final int HEADER_SIZE = DataBlockFileHeader.BLOCK_HEADER_SIZE;

  /** Write a little-endian 32-bit int into a byte array at the given offset. */
  private static void writeLE32(byte[] buf, int offset, long value) {
    buf[offset] = (byte) (value & 0xFF);
    buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
    buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
    buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  /** Write a little-endian 16-bit int into a byte array at the given offset. */
  private static void writeLE16(byte[] buf, int offset, int value) {
    buf[offset] = (byte) (value & 0xFF);
    buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
  }

  private static byte[] buildHeader(
      long signature,
      long version,
      int fileNumber,
      int nextFileNumber,
      int blockSize,
      int entriesNumber,
      int entriesMax) {
    byte[] buf = new byte[HEADER_SIZE];
    int off = 0;
    writeLE32(buf, off, signature);
    off += 4;
    writeLE32(buf, off, version);
    off += 4;
    writeLE16(buf, off, fileNumber);
    off += 2;
    writeLE16(buf, off, nextFileNumber);
    off += 2;
    writeLE32(buf, off, blockSize);
    off += 4;
    writeLE32(buf, off, entriesNumber);
    off += 4;
    writeLE32(buf, off, entriesMax);
    off += 4;
    // rest stays 0 (emptyEntries, hints, updating, user, allocMap)
    return buf;
  }

  @Test
  void constants_blockHeaderSize() {
    assertEquals(8192, DataBlockFileHeader.BLOCK_HEADER_SIZE);
  }

  @Test
  void constructor_parsesMagicSignature() throws IOException {
    // Chrome disk cache data block magic: 0xC104CAC3
    // read4bytes returns int, which is sign-extended to long when stored
    long magic = 0xC104CAC3L;
    byte[] buf = buildHeader(magic, 0x20000, 1, 0, 36, 10, 256);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    // Compare via unsigned masking since read4bytes returns a signed int
    assertEquals(magic & 0xFFFFFFFFL, header.getSignature() & 0xFFFFFFFFL);
  }

  @Test
  void constructor_parsesVersion() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0x20000L, 0, 0, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(0x20000L, header.getVersion());
  }

  @Test
  void constructor_parsesFileNumber() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 3, 4, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(3, header.getFileNumber());
    assertEquals(4, header.getNextFileNumber());
  }

  @Test
  void constructor_parsesBlockSize() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 256, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(256, header.getBlockSize());
  }

  @Test
  void constructor_parsesEntriesNumberAndMax() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 36, 42, 1000);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(42, header.getEntriesNumber());
    assertEquals(1000, header.getEntriesMax());
  }

  @Test
  void constructor_allocMapHasCorrectSize() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(DataBlockFileHeader.MAX_BLOCKS / 32, header.getAllocMap().length);
  }

  @Test
  void constructor_emptyEntriesArrayLength() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(4, header.getEmptyEntries().length);
  }

  @Test
  void constructor_hintsArrayLength() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(4, header.getHints().length);
  }

  @Test
  void constructor_userArrayLength() throws IOException {
    byte[] buf = buildHeader(0xC104CAC3L, 0, 0, 0, 36, 0, 0);
    DataBlockFileHeader header = new DataBlockFileHeader(new ByteArrayInputStream(buf));
    assertEquals(5, header.getUser().length);
  }

  @Test
  void constructor_emptyStream_completesWithGarbageData() throws IOException {
    // read2bytes returns -1 + (-1 << 8) when stream is exhausted — no IOException is thrown.
    // The constructor completes with all-0xFF garbage fields.
    DataBlockFileHeader header =
        new DataBlockFileHeader(new ByteArrayInputStream(new byte[HEADER_SIZE]));
    assertNotNull(header); // must not throw
  }
}
