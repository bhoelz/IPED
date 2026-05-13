package iped.io;

import java.io.IOException;
import java.net.URI;

public interface ISeekableInputStreamFactory {

    SeekableInputStream getSeekableInputStream(String identifier) throws IOException;

    URI getDataSourceURI();

    default boolean returnsEmptyInputStream() {
        return false;
    }

}
