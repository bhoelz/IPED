package iped.engine.config.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RegistryClientTest {

    @Test
    void shouldComputeSha256() throws Exception {
        Path file = Files.createTempFile("registry-client-test", ".txt");
        Files.writeString(file, "abc");
        String sha = RegistryClient.sha256(file);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha);
    }
}
