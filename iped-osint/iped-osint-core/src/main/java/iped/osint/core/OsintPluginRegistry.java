package iped.osint.core;

import iped.osint.spi.OsintPluginDescriptor;
import iped.osint.spi.OsintPluginProvider;

import java.util.List;
import java.util.Optional;

public interface OsintPluginRegistry {

    List<OsintPluginDescriptor> descriptors();

    Optional<OsintPluginProvider> provider(String pluginId);
}
