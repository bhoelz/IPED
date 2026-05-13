package iped.properties;

/**
 * Minimal metadata property descriptor used by API contracts.
 */
public final class MetadataProperty {

    private final String name;

    private MetadataProperty(String name) {
        this.name = name;
    }

    public static MetadataProperty internalDate(String name) {
        return new MetadataProperty(name);
    }

    public static MetadataProperty internalBoolean(String name) {
        return new MetadataProperty(name);
    }

    public static MetadataProperty internalInteger(String name) {
        return new MetadataProperty(name);
    }

    public String getName() {
        return name;
    }
}
