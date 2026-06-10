package iped.properties;

import lombok.Getter;

/**
 * Minimal metadata property descriptor used by API contracts.
 */
@Getter
public final class MetadataProperty {

    private final String name;

    private MetadataProperty(String name) {
        this.name = name;
    }

    /**
     * Creates a descriptor for an internal date-valued property.
     *
     * @param name the property name
     * @return a new descriptor for the given name
     */
    public static MetadataProperty internalDate(String name) {
        return new MetadataProperty(name);
    }

    /**
     * Creates a descriptor for an internal boolean-valued property.
     *
     * @param name the property name
     * @return a new descriptor for the given name
     */
    public static MetadataProperty internalBoolean(String name) {
        return new MetadataProperty(name);
    }

    /**
     * Creates a descriptor for an internal integer-valued property.
     *
     * @param name the property name
     * @return a new descriptor for the given name
     */
    public static MetadataProperty internalInteger(String name) {
        return new MetadataProperty(name);
    }

}
