package iped.carvers.api;

/**
 * SPI contract for third-party carver plugins, mirroring how Tika {@code Parser}
 * implementations are discovered (one jar on the plugins classpath + a
 * {@code META-INF/services/iped.carvers.api.CarverPlugin} entry — no edits to
 * {@code CarverConfig.toml}/{@code CarverConfig.xml} required).
 *
 * <p>Discovered via {@link java.util.ServiceLoader} by the carver configuration
 * loader at startup. Each plugin contributes its own signatures/{@link CarverType}
 * definitions and may attach {@link CarvedItemValidator}s to them before they are
 * merged into the active carver type table.
 */
public interface CarverPlugin {

    /**
     * Returns the carver type definitions contributed by this plugin. Each
     * {@link CarverType} carries its own signatures, size bounds, and carver class
     * (or carver script); validators can be attached via
     * {@link CarverType#addValidator(CarvedItemValidator)} before returning.
     */
    CarverType[] getCarverTypes();
}
