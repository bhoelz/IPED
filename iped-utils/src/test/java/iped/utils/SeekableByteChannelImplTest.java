package iped.utils;

import static org.junit.jupiter.api.Assertions.*;

import iped.io.SeekableInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class SeekableByteChannelImplTest {

  @Test
  void isOpen_beforeClose_thenTrue() {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1, 2, 3}));
    assertTrue(ch.isOpen());
  }

  @Test
  void isOpen_afterClose_thenFalse() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1}));
    ch.close();
    assertFalse(ch.isOpen());
  }

  @Test
  void read_whenData_thenFillsBuffer() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {10, 20, 30}));
    ByteBuffer buf = ByteBuffer.allocate(3);
    int n = ch.read(buf);
    assertEquals(3, n);
    assertEquals(10, buf.get(0));
    assertEquals(20, buf.get(1));
    assertEquals(30, buf.get(2));
  }

  @Test
  void read_whenAtEof_thenMinusOne() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {}));
    ByteBuffer buf = ByteBuffer.allocate(4);
    assertEquals(-1, ch.read(buf));
  }

  @Test
  void size_returnsDataLength() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1, 2, 3, 4, 5}));
    assertEquals(5, ch.size());
  }

  @Test
  void position_afterRead_updatesCorrectly() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1, 2, 3}));
    ByteBuffer buf = ByteBuffer.allocate(2);
    ch.read(buf);
    assertEquals(2, ch.position());
  }

  @Test
  void position_seek_thenReadFromNewPosition() throws IOException {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {10, 20, 30, 40}));
    ch.position(2);
    ByteBuffer buf = ByteBuffer.allocate(1);
    ch.read(buf);
    assertEquals(30, buf.get(0));
  }

  @Test
  void write_thenThrowsIOException() {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1}));
    assertThrows(IOException.class, () -> ch.write(ByteBuffer.wrap(new byte[] {1})));
  }

  @Test
  void truncate_thenThrowsIOException() {
    SeekableByteChannelImpl ch = new SeekableByteChannelImpl(stream(new byte[] {1}));
    assertThrows(IOException.class, () -> ch.truncate(0));
  }

  private static SeekableInputStream stream(byte[] data) {
    return new SeekableInputStream() {
      int pos = 0;

      @Override
      public void seek(long p) {
        pos = (int) p;
      }

      @Override
      public long position() {
        return pos;
      }

      @Override
      public long size() {
        return data.length;
      }

      @Override
      public int read() {
        return pos >= data.length ? -1 : (data[pos++] & 0xFF);
      }
    };
  }
}
