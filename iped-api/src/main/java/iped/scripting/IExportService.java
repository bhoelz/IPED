package iped.scripting;

import iped.data.IItemId;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * Scripting-SDK service for exporting item content and metadata from a case.
 *
 * @since 4.2
 */
public interface IExportService {

  /**
   * Exports the raw content of the given item to {@code destination}. Creates any missing parent
   * directories.
   *
   * @param itemId item to export; must not be {@code null}
   * @param destination target file path; must not be {@code null}
   * @throws IOException if the content cannot be read or written
   */
  void exportContent(IItemId itemId, File destination) throws IOException;

  /**
   * Exports the content of each item in {@code itemIds} under {@code destinationDir}, preserving
   * the relative case path as a sub-directory structure.
   *
   * @param itemIds items to export; must not be {@code null}
   * @param destinationDir root directory for the export; must not be {@code null}
   * @throws IOException if any content cannot be read or written
   */
  void exportContents(Collection<IItemId> itemIds, File destinationDir) throws IOException;

  /**
   * Exports the metadata of all items in {@code itemIds} as a CSV file written to {@code
   * destination}.
   *
   * @param itemIds items to export metadata for; must not be {@code null}
   * @param destination target CSV file path; must not be {@code null}
   * @throws IOException if the file cannot be written
   */
  void exportMetadataCsv(Collection<IItemId> itemIds, File destination) throws IOException;

  /**
   * Exports the items in {@code bookmarkName} as a self-contained HTML report written under {@code
   * destinationDir}.
   *
   * @param bookmarkName source bookmark; must not be {@code null}
   * @param destinationDir root directory for the report; must not be {@code null}
   * @throws IOException if the report cannot be written
   */
  void exportBookmarkReport(String bookmarkName, File destinationDir) throws IOException;
}
