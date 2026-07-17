package iped.engine.io;

import static org.junit.jupiter.api.Assertions.*;

import iped.io.SeekableInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class LimitedSeekableInputStreamTest {

  @Test
  void read_whenWithinLimit_thenReadsCorrectly() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 3);
    assertEquals(1, lis.read());
    assertEquals(2, lis.read());
    assertEquals(3, lis.read());
  }

  @Test
  void read_whenAtLimit_thenEof() throws IOException {
    byte[] data = {1, 2, 3};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 3);
    lis.read();
    lis.read();
    lis.read();
    assertEquals(-1, lis.read());
  }

  @Test
  void read_withStart_thenReadsFromOffset() throws IOException {
    byte[] data = {10, 20, 30, 40, 50};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 2, 2);
    assertEquals(30, lis.read());
    assertEquals(40, lis.read());
    assertEquals(-1, lis.read());
  }

  @Test
  void size_reflectsLimit() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 1, 3);
    assertEquals(3, lis.size());
  }

  @Test
  void position_tracksProgress() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 5);
    assertEquals(0, lis.position());
    lis.read();
    assertEquals(1, lis.position());
  }

  @Test
  void seek_withinRange_thenPositionUpdated() throws IOException {
    byte[] data = {10, 20, 30, 40, 50};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 5);
    lis.seek(2);
    assertEquals(30, lis.read());
  }

  @Test
  void seek_beyondEnd_thenThrowsIOException() throws IOException {
    byte[] data = {1, 2, 3};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 3);
    assertThrows(IOException.class, () -> lis.seek(10));
  }

  @Test
  void skip_doesNotExceedLimit() throws IOException {
    byte[] data = {1, 2, 3, 4, 5};
    LimitedSeekableInputStream lis =
        new LimitedSeekableInputStream(new ArraySeekableStream(data), 0, 3);
    long skipped = lis.skip(100);
    assertTrue(skipped <= 3);
    assertEquals(-1, lis.read());
  }

  private static final class ArraySeekableStream extends SeekableInputStream {
    private final byte[] data;
    private int pos;

    ArraySeekableStream(byte[] data) {
      this.data = data;
    }

    @Override
    public void seek(long p) {
      this.pos = (int) p;
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
  }
}
