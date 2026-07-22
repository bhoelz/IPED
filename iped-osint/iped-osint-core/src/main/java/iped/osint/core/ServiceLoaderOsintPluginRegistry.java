package iped.osint.core;

import iped.osint.spi.OsintPluginDescriptor;
import iped.osint.spi.OsintPluginProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

@Slf4j
public class ServiceLoaderOsintPluginRegistry implements OsintPluginRegistry {

    private final Map<String, OsintPluginProvider> providers;

    private ServiceLoaderOsintPluginRegistry(Map<String, OsintPluginProvider> providers) {
        this.providers = Map.copyOf(providers);
    }

    public static ServiceLoaderOsintPluginRegistry load(File[] pluginJars) {
        Map<String, OsintPluginProvider> providers = new LinkedHashMap<>();
        loadFromClassLoader(ServiceLoaderOsintPluginRegistry.class.getClassLoader(), providers, "classpath");
        if (pluginJars != null) {
            for (File candidate : pluginJars) {
                if (candidate == null || !candidate.isFile() || !candidate.getName().endsWith(".jar")) {
                    continue;
                }
                try (URLClassLoader cl = new URLClassLoader(new URL[]{candidate.toURI().toURL()},
                        ServiceLoaderOsintPluginRegistry.class.getClassLoader())) {
                    loadFromClassLoader(cl, providers, candidate.getName());
                } catch (Exception e) {
                    log.warn("Failed to load OSINT providers from {}", candidate.getAbsolutePath(), e);
                }
            }
        }
        return new ServiceLoaderOsintPluginRegistry(providers);
    }

    private static void loadFromClassLoader(ClassLoader cl, Map<String, OsintPluginProvider> providers, String source) {
        for (OsintPluginProvider provider : ServiceLoader.load(OsintPluginProvider.class, cl)) {
            String id = provider.descriptor().id();
            OsintPluginProvider previous = providers.putIfAbsent(id, provider);
            if (previous != null && previous.getClass().equals(provider.getClass())) {
                continue;
            }
            if (previous != null) {
                throw new IllegalStateException("Duplicate OSINT plugin id '" + id + "' from " + source);
            }
        }
    }

    @Override
    public List<OsintPluginDescriptor> descriptors() {
        List<OsintPluginDescriptor> out = new ArrayList<>(providers.size());
        for (OsintPluginProvider provider : providers.values()) {
            out.add(provider.descriptor());
        }
        return out;
    }

    @Override
    public Optional<OsintPluginProvider> provider(String pluginId) {
        return Optional.ofNullable(providers.get(pluginId));
    }
}
