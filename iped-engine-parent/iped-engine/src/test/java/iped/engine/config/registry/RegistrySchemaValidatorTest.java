package iped.engine.config.registry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class RegistrySchemaValidatorTest {

    @Test
    void shouldValidateMinimalRegistryIndex() {
        RegistryIndex index = sampleIndex("1.0.0");
        assertDoesNotThrow(() -> iped.engine.config.registry.RegistrySchemaValidator.validate(index));
    }

    @Test
    void shouldFailWhenLatestVersionIsMissingFromVersions() {
        RegistryIndex index = sampleIndex("9.9.9");
        assertThrows(IllegalArgumentException.class, () -> RegistrySchemaValidator.validate(index));
    }

    @Test
    void shouldFailOnDuplicatePluginIds() {
        RegistryIndex.PluginVersion v = sampleVersion("1.0.0");
        RegistryIndex.Plugin p1 = samplePlugin("same.id", "1.0.0", List.of(v));
        RegistryIndex.Plugin p2 = samplePlugin("same.id", "1.0.0", List.of(v));
        RegistryIndex index = new RegistryIndex("1.0.0", "registry", "2026-05-14T12:00:00Z",
                new RegistryIndex.SignatureInline("none", "k1", "ignored"), List.of(p1, p2));

        assertThrows(IllegalArgumentException.class, () -> RegistrySchemaValidator.validate(index));
    }

    private static RegistryIndex sampleIndex(String latestVersion) {
        RegistryIndex.PluginVersion version = sampleVersion("1.0.0");
        RegistryIndex.Plugin plugin = samplePlugin("iped.tasks.example", latestVersion, List.of(version));
        return new RegistryIndex("1.0.0", "registry", "2026-05-14T12:00:00Z",
                new RegistryIndex.SignatureInline("none", "k1", "ignored"), List.of(plugin));
    }

    private static RegistryIndex.Plugin samplePlugin(String id, String latestVersion, List<RegistryIndex.PluginVersion> versions) {
        return new RegistryIndex.Plugin(id, "name", "desc", "owner", "https://example.org/repo", "https://example.org/home", "GPL-3.0", List.of("a"),
                latestVersion, versions);
    }

    private static RegistryIndex.PluginVersion sampleVersion(String version) {
        return new RegistryIndex.PluginVersion(version, "2026-05-14T12:00:00Z", new RegistryIndex.Compatibility("[4.4.0,5.0.0)", "[1.0.0,2.0.0)",
                "[25,26)"), new RegistryIndex.Artifact("https://example.org/artifact.jar", 10, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"),
                null, new RegistryIndex.Provider("task.id", "a.b.Provider"), List.of(), new RegistryIndex.Security(List.of("network:outbound"), null), false);
    }
}
