package iped.engine.config.registry;

import iped.tasks.spi.TaskProvider;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.List;
import java.util.ServiceLoader;

public class PluginVerifier {

    public void verifyIndexSignature(String rawJson, RegistryIndex.SignatureInline sig, PublicKeyStore keyStore) {
        PublicKey key = keyStore.find(sig.keyId());
        if (key == null) {
            throw new IllegalStateException("Unknown index keyId: " + sig.keyId());
        }

        String canonical = JsonCanonicalizer.canonicalize(rawJson);
        boolean ok = SignatureVerifier.verifyDetached(sig.type(), key, canonical.getBytes(), sig.sig());
        if (!ok) {
            throw new IllegalStateException("Invalid index signature");
        }
    }

    public void verifyArtifactSignature(Path jar, Path sigFile, RegistryIndex.SignatureExternal sig, PublicKeyStore keyStore) throws IOException {
        PublicKey key = keyStore.find(sig.keyId());
        if (key == null) {
            throw new IllegalStateException("Unknown artifact keyId: " + sig.keyId());
        }

        byte[] bytes = Files.readAllBytes(jar);
        String detachedSig = Files.readString(sigFile);
        boolean ok = SignatureVerifier.verifyDetached(sig.type(), key, bytes, detachedSig);
        if (!ok) {
            throw new IllegalStateException("Invalid artifact signature for " + jar.getFileName());
        }
    }

    public void verifyFileSha256(Path jar, String expectedSha256) throws Exception {
        String actual = RegistryClient.sha256(jar);
        if (!actual.equalsIgnoreCase(expectedSha256)) {
            throw new IllegalStateException("SHA-256 mismatch: expected=" + expectedSha256 + ", actual=" + actual);
        }
    }

    public void verifyServiceLoaderContract(Path jar, String expectedProviderClass, String expectedTaskId) throws Exception {
        try (URLClassLoader cl = new URLClassLoader(new URL[] { jar.toUri().toURL() }, TaskProvider.class.getClassLoader())) {
            List<TaskProvider> providers = ServiceLoader.load(TaskProvider.class, cl).stream().map(ServiceLoader.Provider::get).toList();

            if (providers.isEmpty()) {
                throw new IllegalStateException("No TaskProvider found in " + jar.getFileName());
            }

            boolean providerClassFound = providers.stream().anyMatch(p -> p.getClass().getName().equals(expectedProviderClass));
            if (!providerClassFound) {
                throw new IllegalStateException("Expected providerClass not found: " + expectedProviderClass);
            }

            boolean taskIdFound = providers.stream().map(p -> p.descriptor().id()).anyMatch(id -> id.equals(expectedTaskId));
            if (!taskIdFound) {
                throw new IllegalStateException("Expected taskId not found: " + expectedTaskId);
            }
        }
    }
}
