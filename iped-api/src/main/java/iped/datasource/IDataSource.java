package iped.datasource;

import java.io.File;

/**
 * Represents a forensic evidence data source (e.g., a disk image, UFED
 * extraction, or AD1 file) from which items are extracted during a case.
 *
 * <p>Each data source has a stable UUID that persists across case re-opens and
 * is used to correlate items back to the evidence they came from.
 */
public interface IDataSource {

    /**
     * @return human-readable display name of this evidence source
     */
    String getName();

    /**
     * @return the file on disk backing this data source (e.g., the image file),
     *         or {@code null} for virtual/remote sources
     */
    File getSourceFile();

    /**
     * @return the stable UUID that uniquely identifies this evidence source
     *         within and across cases
     */
    String getUUID();

    /**
     * @param name human-readable display name for this evidence source
     */
    void setName(String name);

    /**
     * @param uuid stable UUID for this evidence source
     */
    void setUUID(String uuid);
}
