package iped.io;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * Creates seekable streams for items of a data source (image file, folder, etc.), given each item's
 * identifier within that source.
 */
public interface ISeekableInputStreamFactory {

  /**
   * Opens a new seekable stream over the content of the identified item. The caller is responsible
   * for closing it.
   *
   * @param identifier item identifier within the data source, e.g. a file path or object id
   * @return a new stream positioned at the start of the item's content
   * @throws IOException if the content cannot be opened
   */
  SeekableInputStream getSeekableInputStream(String identifier) throws IOException;

  /**
   * @return the URI of the data source this factory reads from
   */
  URI getDataSourceURI();

  /**
   * @return {@code true} if this factory always returns empty streams, so callers can skip content
   *     processing
   */
  default boolean returnsEmptyInputStream() {
    return false;
  }

  /**
   * Returns the parameters needed to reconstruct this factory on a remote node, keyed by the
   * parameter name.
   *
   * <p>Used by the distributed pipeline to serialize factory references into Kafka messages. The
   * default implementation returns an empty map, meaning the factory is not reconstructable
   * remotely. Override in datasource-specific factory implementations (e.g. {@code
   * SleuthkitInputStreamFactory}) to provide the necessary parameters (e.g., database path, object
   * ID).
   *
   * @return serializable parameter map; never {@code null}
   */
  default Map<String, String> getSerializableParams() {
    return Collections.emptyMap();
  }
}
