package iped.osint.spi;

public interface OsintPluginProvider {

    OsintPluginDescriptor descriptor();

    default String normalize(OsintIndicatorType indicatorType, String value) {
        return value == null ? null : value.trim();
    }

    OsintResult execute(OsintQuery query, OsintContext context) throws OsintPluginException;

    default OsintPluginHealth healthCheck(OsintContext context) {
        return OsintPluginHealth.ok();
    }
}
