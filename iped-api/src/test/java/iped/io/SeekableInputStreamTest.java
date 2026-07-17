package iped.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class SeekableInputStreamTest {

  @Test
  void markSupported_whenCalled_thenReturnsTrue() {
    try (InMemorySeekableInputStream stream =
        new InMemorySeekableInputStream(new byte[] {1, 2, 3})) {
      assertTrue(stream.markSupported());
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  void reset_whenNotMarked_thenThrowsIOException() {
    try (InMemorySeekableInputStream stream =
        new InMemorySeekableInputStream(new byte[] {1, 2, 3})) {
      assertThrows(IOException.class, stream::reset);
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  void markAndReset_whenMarked_thenReturnsToMarkedPosition() throws IOException {
    try (InMemorySeekableInputStream stream =
        new InMemorySeekableInputStream(new byte[] {10, 20, 30})) {
      stream.read();
      stream.mark(0);
      stream.read();
      stream.read();
      stream.reset();

      assertEquals(1, stream.position());
      assertEquals(20, stream.read());
    } catch (IOException e) {
      fail(e);
    }
  }

  private static class InMemorySeekableInputStream extends SeekableInputStream {
    private final byte[] data;
    private int pos;

    private InMemorySeekableInputStream(byte[] data) {
      this.data = data;
    }

    @Override
    public void seek(long pos) {
      this.pos = (int) pos;
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
      if (pos >= data.length) {
        return -1;
      }
      return data[pos++] & 0xFF;
    }
  }
}
