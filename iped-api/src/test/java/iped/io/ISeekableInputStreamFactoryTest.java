package iped.io;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ISeekableInputStreamFactoryTest {

    @Test
    void returnsEmptyInputStream_whenDefaultImplementation_thenFalse() {
        ISeekableInputStreamFactory factory = new ISeekableInputStreamFactory() {
            @Override
            public SeekableInputStream getSeekableInputStream(String identifier) throws IOException {
                return null;
            }

            @Override
            public URI getDataSourceURI() {
                return URI.create("file:///tmp");
            }
        };

        assertFalse(factory.returnsEmptyInputStream());
    }
}

