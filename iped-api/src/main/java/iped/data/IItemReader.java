package iped.data;

import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.IStreamSource;
import iped.io.SeekableInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;

/**
 * Read-only view of an evidence item: identity, names, dates, hashes,
 * metadata and content streams. This is the interface exposed to scripts and
 * external consumers that must not modify the item.
 */
public interface IItemReader extends IStreamSource {
    /**
     *
     * @return item id
     */
    int getId();

    /**
     *
     * @return parent item id.
     */
    Integer getParentId();

    /**
     * @return the subitem order into its parent, null if this is not a subitem.
     */
    Integer getSubitemId();

    /**
     * @return file name
     */
    String getName();

    /**
     *
     * @return original item extension
     */
    String getExt();

    /**
     * @return the detected file type extension
     */
    String getType();

    /**
     *
     * @return file media type, as detected by signature analysis
     */
    Object getMediaType();

    /**
     * Neutral media type accessor for gradual decoupling from external libraries.
     */
    default MediaTypeValue getMediaTypeValue() {
        Object mediaType = getMediaType();
        return mediaType == null ? null : MediaTypeValue.of(mediaType.toString());
    }

    /**
     * @return the media type as a string, or {@code null} if not detected yet
     */
    default String getMediaTypeString() {
        MediaTypeValue mediaTypeValue = getMediaTypeValue();
        return mediaTypeValue == null ? null : mediaTypeValue.value();
    }

    /**
     * @return the categories assigned to this item
     */
    HashSet<String> getCategorySet();

    /**
     * @return full item path
     */
    String getPath();

    /**
     * @return file size in bytes
     */
    Long getLength();

    /**
     *
     * @return file hash, when available.
     */
    String getHash();

    /**
     *
     * @return true if the item is deleted
     */
    boolean isDeleted();

    /**
     *
     * @return true if the item comes from carving
     */
    boolean isCarved();

    /**
     * @return true if this is a subitem of a container
     */
    boolean isSubItem();

    /**
     *
     * @return true if the item is a directory
     */
    boolean isDir();

    /**
     * @return true if this is a root item
     */
    boolean isRoot();

    /**
     * @return true if item parsing timed out
     */
    boolean isTimedOut();

    /**
     *
     * @return true if the item has children, such as subitems or carved items
     */
    boolean hasChildren();

    /**
     * Returns a temp file with item content. This can spool data to the temp
     * directory, so avoid using this if you can work directly with item data
     * streams.
     */
    File getTempFile() throws IOException;

    /**
     * @return this item's identifier within its data source, e.g. a file path
     *         or object id, used with {@link #getInputStreamFactory()}
     */
    String getIdInDataSource();

    /**
     * @return the factory used to open content streams for this item
     */
    ISeekableInputStreamFactory getInputStreamFactory();

    /**
     * Gets the preview file. Returns null when unavailable.
     *
     * @return path of the preview file relative to the case
     */
    File getViewFile();

    /**
     * @return {@code true} if a preview was generated for this item
     */
    boolean hasPreview();

    /**
     * @return the case folder under which preview files are stored
     */
    File getPreviewBaseFolder();

    /**
     * @return the file extension of this item's preview, e.g. {@code "html"}
     */
    String getPreviewExt();

    /**
     * Opens a new seekable stream over this item's preview content.
     *
     * @return a new stream over the preview content
     * @throws SQLException if the preview is stored in a database and reading
     *                      it fails
     * @throws IOException  if the preview cannot be opened
     */
    SeekableInputStream getPreviewSeekeableInputStream() throws SQLException, IOException;

    /**
     * @return this item's thumbnail image bytes, or {@code null} if absent
     */
    byte[] getThumb();

    /**
     * Opens a new buffered stream over this item's content. The caller is
     * responsible for closing it.
     *
     * @return a new buffered stream over the content
     * @throws IOException if the content cannot be opened
     */
    BufferedInputStream getBufferedInputStream() throws IOException;

    /**
     * Opens a new image input stream over this item's content. The caller is
     * responsible for closing it.
     *
     * @return a new image input stream over the content
     * @throws IOException if the content cannot be opened
     */
    ImageInputStream getImageInputStream() throws IOException;

    /**
     * @return file last modification date
     */
    Date getModDate();

    /**
     * @return file creation date, or {@code null} if unknown
     */
    Date getCreationDate();

    /**
     * @return file last access date, or {@code null} if unknown
     */
    Date getAccessDate();

    /**
     * @return file metadata change date, or {@code null} if unknown
     */
    Date getChangeDate();

    /**
     * Gets an extra attribute set by a processing task.
     *
     * @param key the attribute name
     * @return the attribute value, or {@code null} if absent
     */
    Object getExtraAttribute(String key);

    /**
     * @return all extra attributes set by processing tasks, keyed by name
     */
    Map<String, Object> getExtraAttributeMap();

    /**
     * @return the data source (evidence) this item belongs to
     */
    IDataSource getDataSource();

    /**
     * @return Object containing the metadata of the item
     */
    Object getMetadata();

    /**
     * Neutral metadata view for gradual decoupling from external libraries.
     */
    default Map<String, List<String>> getMetadataMap() {
        Object metadata = getMetadata();
        Map<String, List<String>> map = new LinkedHashMap<>();
        if (metadata == null) {
            return map;
        }
        for (String name : metadataNames(metadata)) {
            map.put(name, List.of(metadataValues(metadata, name)));
        }
        return map;
    }

    /**
     * Gets the first value of a metadata entry reflectively, without a compile
     * dependency on the metadata implementation.
     *
     * @param key the metadata entry name
     * @return the first value, or {@code null} if absent or metadata is null
     */
    default String getMetadataValue(String key) {
        Object metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        try {
            Method method = metadata.getClass().getMethod("get", String.class); //$NON-NLS-1$
            Object value = method.invoke(metadata, key);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get metadata value", e); //$NON-NLS-1$
        }
    }

    /**
     * Gets all values of a metadata entry reflectively, without a compile
     * dependency on the metadata implementation.
     *
     * @param key the metadata entry name
     * @return all values of the entry, or {@code null} if metadata is null
     */
    default String[] getMetadataValues(String key) {
        Object metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        return metadataValues(metadata, key);
    }

    private static String[] metadataNames(Object metadata) {
        try {
            Method namesMethod = metadata.getClass().getMethod("names"); //$NON-NLS-1$
            return (String[]) namesMethod.invoke(metadata);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read metadata names", e); //$NON-NLS-1$
        }
    }

    private static String[] metadataValues(Object metadata, String key) {
        try {
            Method valuesMethod = metadata.getClass().getMethod("getValues", String.class); //$NON-NLS-1$
            return (String[]) valuesMethod.invoke(metadata, key);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read metadata values", e); //$NON-NLS-1$
        }
    }

}
