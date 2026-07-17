package iped.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} of known size whose read position can be moved freely. Mark/reset is
 * supported and implemented on top of {@link #seek(long)}.
 */
public abstract class SeekableInputStream extends InputStream {

  private long markedPos = -1;

  /**
   * Moves the read position to the given offset.
   *
   * @param pos new position, in bytes from the start of the stream
   * @throws IOException if the position cannot be changed
   */
  public abstract void seek(long pos) throws IOException;

  /**
   * @return the current read position, in bytes from the start of the stream
   * @throws IOException if the position cannot be read
   */
  public abstract long position() throws IOException;

  /**
   * @return the total size of this stream, in bytes
   * @throws IOException if the size cannot be determined
   */
  public abstract long size() throws IOException;

  @Override
  public boolean markSupported() {
    return true;
  }

  @Override
  public void mark(int readLimit) {
    try {
      markedPos = this.position();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void reset() throws IOException {
    if (markedPos == -1) {
      throw new IOException("stream must be marked before reset");
    }
    this.seek(markedPos);
  }
}
