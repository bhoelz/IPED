package iped.osint.spi;

public record OsintPluginHealth(boolean healthy, String message) {

    public static OsintPluginHealth ok() {
        return new OsintPluginHealth(true, "ok");
    }

    public static OsintPluginHealth unhealthy(String message) {
        return new OsintPluginHealth(false, message);
    }
}
