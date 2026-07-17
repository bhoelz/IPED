package iped.engine.datasource;

/**
 * Static holder for the engine-side datasource support implementation.
 *
 * <p>iped-engine's Manager registers itself here at startup. Datasource modules (iped-sleuthkit,
 * iped-ufed, iped-ad1) call DatasourceRegistry.get() to obtain the registered implementation.
 */
public final class DatasourceRegistry {

  private static IDatasourceRegistry instance;

  private DatasourceRegistry() {}

  public static void set(IDatasourceRegistry registry) {
    instance = registry;
  }

  public static boolean isInitialized() {
    return instance != null;
  }

  public static IDatasourceRegistry get() {
    if (instance == null) {
      throw new IllegalStateException(
          "DatasourceRegistry has not been initialized. "
              + "Manager must call DatasourceRegistry.set() before any datasource reader is used.");
    }
    return instance;
  }
}
