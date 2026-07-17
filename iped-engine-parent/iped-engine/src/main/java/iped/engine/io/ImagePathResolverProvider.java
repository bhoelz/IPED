package iped.engine.io;

/**
 * Holds the active {@link IImagePathResolver}.
 *
 * <p>The engine defaults to a no-op resolver (returns {@code null}) so it can run fully headless.
 * iped-app registers its Swing implementation during startup when a GUI is available.
 */
public final class ImagePathResolverProvider {

  private static IImagePathResolver resolver = (f, folder) -> null;

  private ImagePathResolverProvider() {}

  public static void set(IImagePathResolver r) {
    resolver = r;
  }

  public static IImagePathResolver get() {
    return resolver;
  }
}
