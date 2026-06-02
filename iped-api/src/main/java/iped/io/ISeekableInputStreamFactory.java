package iped.io;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

public interface ISeekableInputStreamFactory {

    SeekableInputStream getSeekableInputStream(String identifier) throws IOException;

    URI getDataSourceURI();

    default boolean returnsEmptyInputStream() {
        return false;
    }

    /**
     * Returns the parameters needed to reconstruct this factory on a remote node,
     * keyed by parameter name.
     *
     * <p>Used by the distributed pipeline to serialise factory references into
     * Kafka messages. The default implementation returns an empty map, meaning
     * the factory is not reconstructable remotely. Override in datasource-specific
     * factory implementations (e.g. {@code SleuthkitInputStreamFactory}) to provide
     * the necessary parameters (e.g. database path, object ID).
     *
     * @return serializable parameter map; never {@code null}
     */
    default Map<String, String> getSerializableParams() {
        return Collections.emptyMap();
    }

}
