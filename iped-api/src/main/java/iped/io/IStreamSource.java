package iped.io;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 * A source of binary content that can be read repeatedly through seekable streams, channels, or a
 * temporary file.
 */
public interface IStreamSource {

  /**
   * Opens a new seekable stream over this source's content. The caller is responsible for closing
   * it.
   *
   * @return a new stream positioned at the start of the content
   * @throws IOException if the content cannot be opened
   */
  SeekableInputStream getSeekableInputStream() throws IOException;

  /**
   * Opens a new seekable byte channel over this source's content. The caller is responsible for
   * closing it.
   *
   * @return a new channel positioned at the start of the content
   * @throws IOException if the content cannot be opened
   */
  SeekableByteChannel getSeekableByteChannel() throws IOException;

  /**
   * Returns a temporary file with this source's content, creating it if needed. The file may be
   * cached and reused across calls; callers must not modify or delete it.
   *
   * @return a file containing this source's content
   * @throws IOException if the file cannot be created
   */
  File getTempFile() throws IOException;
}
