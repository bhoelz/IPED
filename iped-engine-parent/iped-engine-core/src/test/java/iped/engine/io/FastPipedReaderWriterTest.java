package iped.engine.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class FastPipedReaderWriterTest {

  @Test
  void shouldWriteAndReadSingleChars() throws Exception {
    FastPipedReader reader = new FastPipedReader(32, 1, 1);
    FastPipedWriter writer = new FastPipedWriter(reader);
    writer.write('a');
    writer.write('b');
    writer.flush();
    assertEquals('a', reader.read());
    assertEquals('b', reader.read());
    writer.close();
    assertEquals(-1, reader.read());
  }

  @Test
  void shouldReadArrayAndValidateBounds() throws Exception {
    FastPipedReader reader = new FastPipedReader(32, 1, 1);
    FastPipedWriter writer = new FastPipedWriter(reader);
    writer.write("abcd".toCharArray(), 0, 4);
    char[] out = new char[4];
    assertEquals(4, reader.read(out, 0, 4));
    assertEquals("abcd", new String(out));
    assertThrows(IndexOutOfBoundsException.class, () -> reader.read(out, -1, 1));
  }

  @Test
  void shouldThrowWhenWritingWithoutConnection() {
    FastPipedWriter writer = new FastPipedWriter();
    assertThrows(IOException.class, () -> writer.write('x'));
  }

  @Test
  void shouldTimeoutWhenNoWriterAndNotPaused() {
    FastPipedReader reader = new FastPipedReader(4, 0, 0);
    assertThrows(TimeoutException.class, reader::read);
  }

  @Test
  void shouldPauseTimeoutWhenConfigured() throws Exception {
    FastPipedReader reader = new FastPipedReader(8, 0, 0);
    assertTrue(reader.setTimeoutPaused(true));
    FastPipedWriter writer = new FastPipedWriter(reader);
    writer.write('z');
    assertEquals('z', reader.read());
  }
}
