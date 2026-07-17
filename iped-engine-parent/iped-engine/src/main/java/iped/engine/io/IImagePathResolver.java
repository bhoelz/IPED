package iped.engine.io;

import java.io.File;

/**
 * Resolves a missing forensic image or data source path. The engine calls this when a referenced
 * file cannot be found; the UI layer (iped-app) provides a Swing implementation that shows a
 * file-chooser dialog. In headless mode the default implementation returns {@code null}.
 *
 * @see ImagePathResolverProvider
 */
@FunctionalInterface
public interface IImagePathResolver {

  /**
   * @param missingPath the path that could not be found
   * @param selectFolder {@code true} if a folder (or file) may be selected; {@code false} if only a
   *     file is expected
   * @return the replacement path chosen by the user, or {@code null} if the user cancelled or no UI
   *     is available
   */
  File resolve(File missingPath, boolean selectFolder);
}
