package iped.engine.webapi;

/**
 * Holds the engine-side {@link IProcessingService} implementation. The engine module registers an
 * adapter at startup; webapi endpoints retrieve it here, keeping the webapi free of direct engine
 * imports.
 */
public class ProcessingServiceRegistry {

  private static volatile IProcessingService service;

  public static void register(IProcessingService impl) {
    service = impl;
  }

  public static IProcessingService get() {
    return service;
  }
}
