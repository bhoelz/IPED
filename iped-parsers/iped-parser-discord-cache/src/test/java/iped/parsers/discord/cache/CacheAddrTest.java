package iped.parsers.discord.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class CacheAddrTest {

  @Test
  void constructor_whenNotInitializedBit_thenNotInitialized() throws IOException {
    // Bit 31 not set → not initialized
    CacheAddr addr = new CacheAddr(0L);
    assertFalse(addr.isInitialized());
  }

  @Test
  void constructor_whenInitializedBit_thenInitialized() throws IOException {
    // Bit 31 set, fileType=0 (DATA_STREAM_FILE)
    CacheAddr addr = new CacheAddr(0x80000000L);
    assertTrue(addr.isInitialized());
  }

  @Test
  void fileType_zero_isDataStreamFile() throws IOException {
    // bits 28-30 = 000 → fileType=0
    CacheAddr addr = new CacheAddr(0x80000000L);
    assertEquals(CacheAddr.DATA_STREAM_FILE, addr.getFileType());
  }

  @Test
  void fileType_one_isBlock36() throws IOException {
    // bits 28-30 = 001 → fileType=1 (BLOCK_36)
    CacheAddr addr = new CacheAddr(0x90000000L);
    assertEquals(CacheAddr.BLOCK_36, addr.getFileType());
  }

  @Test
  void fileType_two_isBlock256() throws IOException {
    // bits 28-30 = 010 → fileType=2 (BLOCK_256)
    CacheAddr addr = new CacheAddr(0xA0000000L);
    assertEquals(CacheAddr.BLOCK_256, addr.getFileType());
  }

  @Test
  void dataStreamFile_fileNameStr_prefixedWithF() throws IOException {
    // fileType=0, fileName=0 → "f_000000"
    CacheAddr addr = new CacheAddr(0x80000000L);
    assertTrue(addr.getFileNameStr().startsWith("f_"));
  }

  @Test
  void blockFile_fileNameStr_prefixedWithData() throws IOException {
    // fileType=1, fileSelector=2 → "data_2"
    // address: init(bit31) | type(001 in 28-30) | numBlocks=1(00 in 24-25) | fileSelector=2(in
    // 16-23) | startBlock=0
    long address = 0x80000000L | (1L << 28) | (2L << 16);
    CacheAddr addr = new CacheAddr(address);
    assertEquals("data_2", addr.getFileNameStr());
  }

  @Test
  void numBlocks_encodedCorrectly() throws IOException {
    // numBlocks-1 stored in bits 24-25; value=1 means numBlocks=2
    long address = 0x80000000L | (1L << 28) | (1L << 24);
    CacheAddr addr = new CacheAddr(address);
    assertEquals(2, addr.getNumBlocks());
  }

  @Test
  void startBlock_encodedCorrectly() throws IOException {
    // startBlock in bits 0-15
    long address = 0x80000000L | (1L << 28) | 5L;
    CacheAddr addr = new CacheAddr(address);
    assertEquals(5, addr.getStartBlock());
  }

  @Test
  void fileSelector_encodedCorrectly() throws IOException {
    // fileSelector in bits 16-23
    long address = 0x80000000L | (1L << 28) | (7L << 16);
    CacheAddr addr = new CacheAddr(address);
    assertEquals(7, addr.getFileSelector());
  }

  @Test
  void getAddress_returnsOriginalValue() throws IOException {
    long value = 0x91020005L;
    CacheAddr addr = new CacheAddr(value);
    assertEquals(value, addr.getAddress());
  }

  @Test
  void setAddress_updatesValue() throws IOException {
    CacheAddr addr = new CacheAddr(0L);
    addr.setAddress(0x80000001L);
    assertEquals(0x80000001L, addr.getAddress());
  }

  @Test
  void toString_containsAddress() throws IOException {
    CacheAddr addr = new CacheAddr(0x80000000L);
    assertTrue(addr.toString().contains("CacheAddr"));
  }
}
