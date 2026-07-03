package iped.osint.core;

import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintPluginDescriptor;
import iped.osint.spi.OsintPolicyDecision;

public class OsintPolicyEngine {

    private final OsintPluginsConfig config;

    public OsintPolicyEngine(OsintPluginsConfig config) {
        this.config = config;
    }

    public OsintPolicyDecision evaluate(OsintPluginDescriptor descriptor, OsintExecutionMode mode) {
        if (!config.enabledModes().contains(mode)) {
            return OsintPolicyDecision.deny("Execution mode disabled: " + mode);
        }
        if (!descriptor.supportedExecutionModes().isEmpty() && !descriptor.supportedExecutionModes().contains(mode)) {
            return OsintPolicyDecision.deny("Plugin does not support mode: " + mode);
        }
        if (!config.allowlist().isEmpty() && !config.allowlist().contains(descriptor.id())) {
            return OsintPolicyDecision.deny("Plugin not in allowlist");
        }
        if (config.denylist().contains(descriptor.id())) {
            return OsintPolicyDecision.deny("Plugin denied by policy");
        }
        return OsintPolicyDecision.allow();
    }
}
