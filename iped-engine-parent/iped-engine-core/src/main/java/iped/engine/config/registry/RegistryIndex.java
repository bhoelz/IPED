package iped.engine.config.registry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = false)
public record RegistryIndex(String schemaVersion, String registryId, String generatedAt, SignatureInline signature, List<Plugin> plugins) {

    public record SignatureInline(String type, String keyId, String sig) {
    }

    public record SignatureExternal(String type, String keyId, String sigUrl) {
    }

    public record Plugin(String id, String name, String description, String owner, String repo, String homepage, String license, List<String> tags,
            String latestVersion, List<PluginVersion> versions) {
    }

    public record PluginVersion(String version, String releasedAt, Compatibility compatibility, Artifact artifact, SignatureExternal signature,
            Provider provider, List<Dependency> dependencies, Security security, boolean deprecated) {
    }

    public record Compatibility(String ipedVersionRange, String spiVersionRange, String javaVersionRange) {
    }

    public record Artifact(String url, long size, String sha256) {
    }

    public record Provider(String taskId, String providerClass) {
    }

    public record Dependency(String id, String versionRange, boolean optional) {
    }

    public record Security(List<String> permissions, String sbomUrl) {
    }
}
