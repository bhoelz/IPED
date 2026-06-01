package iped.engine.config.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class RegistrySchemaValidator {

    private RegistrySchemaValidator() {
    }

    static void validate(RegistryIndex index) {
        requireNotBlank(index.schemaVersion(), "schemaVersion");
        requireNotBlank(index.registryId(), "registryId");
        if (index.signature() == null) {
            throw new IllegalArgumentException("signature must not be null");
        }
        requireNotBlank(index.signature().type(), "signature.type");
        requireNotBlank(index.signature().keyId(), "signature.keyId");
        requireNotBlank(index.signature().sig(), "signature.sig");
        if (index.plugins() == null) {
            throw new IllegalArgumentException("plugins must not be null");
        }

        Set<String> pluginIds = new HashSet<>();
        for (RegistryIndex.Plugin plugin : index.plugins()) {
            requireNotBlank(plugin.id(), "plugin.id");
            requireNotBlank(plugin.latestVersion(), "plugin.latestVersion");
            if (!pluginIds.add(plugin.id())) {
                throw new IllegalArgumentException("Duplicate plugin id: " + plugin.id());
            }
            validatePluginVersions(plugin);
        }
    }

    private static void validatePluginVersions(RegistryIndex.Plugin plugin) {
        List<RegistryIndex.PluginVersion> versions = plugin.versions();
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("Plugin " + plugin.id() + " must have at least one version");
        }
        boolean hasLatest = false;
        for (RegistryIndex.PluginVersion version : versions) {
            requireNotBlank(version.version(), "plugin.version");
            if (plugin.latestVersion().equals(version.version())) {
                hasLatest = true;
            }
            if (version.artifact() == null) {
                throw new IllegalArgumentException("Plugin " + plugin.id() + " version " + version.version() + " missing artifact");
            }
            requireNotBlank(version.artifact().url(), "artifact.url");
            requireNotBlank(version.artifact().sha256(), "artifact.sha256");
            if (version.provider() == null) {
                throw new IllegalArgumentException("Plugin " + plugin.id() + " version " + version.version() + " missing provider");
            }
            requireNotBlank(version.provider().taskId(), "provider.taskId");
            requireNotBlank(version.provider().providerClass(), "provider.providerClass");
        }
        if (!hasLatest) {
            throw new IllegalArgumentException("Plugin " + plugin.id() + " latestVersion " + plugin.latestVersion() + " not found in versions list");
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
