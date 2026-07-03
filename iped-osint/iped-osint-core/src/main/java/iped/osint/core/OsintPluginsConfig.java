package iped.osint.core;

import iped.osint.spi.OsintExecutionMode;
import iped.utils.TomlProperties;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class OsintPluginsConfig {

    public static final String CONFIG_FILE = "OsintPluginsConfig.toml";

    private final Set<String> allowlist = new LinkedHashSet<>();
    private final Set<String> denylist = new LinkedHashSet<>();
    private final EnumSet<OsintExecutionMode> enabledModes = EnumSet.allOf(OsintExecutionMode.class);
    private final Map<String, Integer> timeoutsSeconds = new HashMap<>();
    private final Map<String, Integer> rateLimitsPerMinute = new HashMap<>();
    private final Map<String, Integer> maxBatchSizes = new HashMap<>();
    private final Map<String, Map<String, String>> credentials = new HashMap<>();
    private boolean persistResults = true;
    private boolean enablePromotion = false;

    public static OsintPluginsConfig load(File configDir, File appRoot) {
        OsintPluginsConfig cfg = new OsintPluginsConfig();
        cfg.loadFile(new File(appRoot, CONFIG_FILE));
        if (configDir != null) {
            cfg.loadFile(new File(configDir, CONFIG_FILE));
        }
        return cfg;
    }

    private void loadFile(File file) {
        if (!file.isFile()) {
            return;
        }
        try {
            TomlProperties props = new TomlProperties();
            props.load(new ByteArrayInputStream(Files.readString(file.toPath(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8)));
            allowlist.clear();
            allowlist.addAll(props.getListProperty("allowlist"));
            denylist.clear();
            denylist.addAll(props.getListProperty("denylist"));
            persistResults = Boolean.parseBoolean(props.getProperty("persistResults", Boolean.toString(persistResults)));
            enablePromotion = Boolean.parseBoolean(props.getProperty("enablePromotion", Boolean.toString(enablePromotion)));
            String modes = props.getProperty("enabledModes");
            if (modes != null && !modes.isBlank()) {
                enabledModes.clear();
                Arrays.stream(modes.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(String::toUpperCase)
                        .map(OsintExecutionMode::valueOf)
                        .forEach(enabledModes::add);
            }
            for (String key : props.stringPropertyNames()) {
                if (key.startsWith("plugins.") && key.endsWith(".timeoutSeconds")) {
                    timeoutsSeconds.put(pluginId(key, ".timeoutSeconds"), Integer.parseInt(props.getProperty(key).trim()));
                } else if (key.startsWith("plugins.") && key.endsWith(".rateLimitPerMinute")) {
                    rateLimitsPerMinute.put(pluginId(key, ".rateLimitPerMinute"), Integer.parseInt(props.getProperty(key).trim()));
                } else if (key.startsWith("plugins.") && key.endsWith(".maxBatchSize")) {
                    maxBatchSizes.put(pluginId(key, ".maxBatchSize"), Integer.parseInt(props.getProperty(key).trim()));
                } else if (key.startsWith("plugins.") && key.contains(".credentials.")) {
                    String pluginId = key.substring("plugins.".length(), key.indexOf(".credentials."));
                    String name = key.substring(key.indexOf(".credentials.") + ".credentials.".length());
                    credentials.computeIfAbsent(pluginId, ignored -> new HashMap<>()).put(name, props.getProperty(key));
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static String pluginId(String key, String suffix) {
        return key.substring("plugins.".length(), key.length() - suffix.length());
    }

    public Set<String> allowlist() {
        return allowlist;
    }

    public Set<String> denylist() {
        return denylist;
    }

    public EnumSet<OsintExecutionMode> enabledModes() {
        return enabledModes.clone();
    }

    public int timeoutSeconds(String pluginId, int defaultValue) {
        return timeoutsSeconds.getOrDefault(pluginId, defaultValue);
    }

    public int rateLimitPerMinute(String pluginId, int defaultValue) {
        return rateLimitsPerMinute.getOrDefault(pluginId, defaultValue);
    }

    public int maxBatchSize(String pluginId, int defaultValue) {
        return maxBatchSizes.getOrDefault(pluginId, defaultValue);
    }

    public Map<String, String> credentials(String pluginId) {
        return credentials.getOrDefault(pluginId, Map.of());
    }

    public boolean persistResults() {
        return persistResults;
    }

    public boolean enablePromotion() {
        return enablePromotion;
    }
}
