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

    default String getMediaTypeString() {
        MediaTypeValue mediaTypeValue = getMediaTypeValue();
        return mediaTypeValue == null ? null : mediaTypeValue.value();
    }

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

    String getIdInDataSource();

    ISeekableInputStreamFactory getInputStreamFactory();

    /**
     * Gets the preview file. Returns null when unavailable.
     *
     * @return path of the preview file relative to the case
     */
    File getViewFile();

    boolean hasPreview();

    File getPreviewBaseFolder();

    String getPreviewExt();

    SeekableInputStream getPreviewSeekeableInputStream() throws SQLException, IOException;

    byte[] getThumb();

    BufferedInputStream getBufferedInputStream() throws IOException;

    ImageInputStream getImageInputStream() throws IOException;

    /**
     * @return file last modification date
     */
    Date getModDate();

    Date getCreationDate();

    Date getAccessDate();

    Date getChangeDate();

    Object getExtraAttribute(String key);

    Map<String, Object> getExtraAttributeMap();

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
