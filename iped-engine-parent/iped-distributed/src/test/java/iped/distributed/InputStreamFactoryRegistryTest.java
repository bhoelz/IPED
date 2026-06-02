package iped.distributed;

import iped.distributed.agent.InputStreamFactoryRegistry;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InputStreamFactoryRegistryTest {

    @AfterEach
    void cleanup() {
        InputStreamFactoryRegistry.clearForTests();
    }

    @Test
    void registerAndReconstruct() {
        String fqcn = "com.example.FakeFactory";
        InputStreamFactoryRegistry.register(fqcn, params -> new FakeFactory(params.get("path")));

        assertTrue(InputStreamFactoryRegistry.isRegistered(fqcn));

        ISeekableInputStreamFactory factory =
                InputStreamFactoryRegistry.reconstruct(fqcn, Map.of("path", "/evidence/test.e01"));
        assertNotNull(factory);
        assertEquals("/evidence/test.e01", ((FakeFactory) factory).path);
    }

    @Test
    void unknownClassThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                InputStreamFactoryRegistry.reconstruct("com.unknown.Factory", Map.of()));
    }

    // Minimal fake factory for tests
    static class FakeFactory implements ISeekableInputStreamFactory {
        final String path;
        FakeFactory(String path) { this.path = path; }

        @Override
        public SeekableInputStream getSeekableInputStream(String id) { return null; }

        @Override
        public URI getDataSourceURI() { return URI.create("file:" + path); }
    }
}
