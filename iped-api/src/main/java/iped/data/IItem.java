package iped.data;

import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.channels.SeekableByteChannel;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Interface that defines an evidence file, which is a case file accompanied by
 * all available properties. Some properties considered essential, such as file
 * name, file type, and exported link, are represented as class attributes.
 * Other "basic" properties (precomputed for preprocessing) are stored in a
 * property list.
 *
 * @author Wladimir Leite (GPINF/SP)
 * @author Nassif (GPINF/SP)
 */
public interface IItem extends IItemReader {

    /**
     * Adds the item to a category.
     *
     * @param category category the item will be added to
     */
    void addCategory(String category);

    /**
     * Adds one parent id of the item in a hierarchical structure.
     *
     * @param parentId one parent id
     */
    void addParentId(int parentId);

    /**
     * Adds a list of parent item ids in a hierarchical structure.
     *
     * @param parentIds list of parent item ids
     */
    void addParentIds(List<Integer> parentIds);

    /**
     * Releases used resources such as temporary files and handles.
     *
     * @throws IOException if an I/O error occurs
     */
    void dispose();

    /**
     * @return last access date
     */
    @Override
    Date getAccessDate();

    /**
     * @return a BufferedInputStream with item content
     * @throws IOException if an I/O error occurs
     */
    @Override
    BufferedInputStream getBufferedInputStream() throws IOException;

    /**
     * @return concatenated item category names
     */
    String getCategories();

    /**
     * @return item category set
     */
    HashSet<String> getCategorySet();

    /**
     * @return file creation date
     */
    @Override
    Date getCreationDate();

    String getIdInDataSource();

    ISeekableInputStreamFactory getInputStreamFactory();

    /**
     * Processing modules can set extra attributes on the item to store processing output.
     *
     * @param key extra attribute name
     * @return extra attribute value
     */
    Object getExtraAttribute(String key);

    Object getTempAttribute(String key);

    /**
     * @return map of extra item attributes. Processing modules can store
     * processing output in extra attributes.
     */
    Map<String, Object> getExtraAttributeMap();

    /**
     * @return offset in the parent item where this item was recovered by carving.
     * Returns -1 if the item does not come from carving.
     */
    long getFileOffset();

    IHashValue getHashValue();

    /**
     * @return concatenated item labels
     */
    List<String> getLabels();

    /**
     * @return list containing parent item ids
     */
    List<Integer> getParentIds();

    /**
     * @return parent item ids concatenated with spaces
     */
    String getParentIdsString();

    /**
     * @return extracted item text cached by expansion tasks for text-based
     * containers (eml, ppt, etc.)
     */
    @Deprecated
    String getParsedTextCache();

    Reader getTextReader() throws IOException;

    Date getChangeDate();

    @Override
    SeekableByteChannel getSeekableByteChannel() throws IOException;

    /**
     * @return InputStream with file content.
     */
    @Override
    SeekableInputStream getSeekableInputStream() throws IOException;

    /**
     * Used by modules that can process only a File and not an InputStream.
     * May impact performance because it creates a temporary file.
     *
     * @return temporary file with item content
     * @throws IOException if an I/O error occurs
     */
    @Override
    File getTempFile() throws IOException;

    /**
     * @return Tika-compatible InputStream with file content
     * @throws IOException if an I/O error occurs
     */
    InputStream getTikaStream() throws IOException;

    /**
     * Neutral stream accessor for gradual decoupling from external libraries.
     */
    default SeekableInputStream getItemInputStream() throws IOException {
        return getSeekableInputStream();
    }

    boolean hasTmpFile();

    /**
     * @return true if the item was parsed
     */
    boolean isParsed();

    /**
     * @return true if this is a processing queue-end item
     */
    boolean isQueueEnd();

    /**
     * @return true if the item must be added to the case
     */
    boolean isToAddToCase();

    /**
     * @return true if the item must be exported
     */
    boolean isToExtract();

    /**
     * @return true if the item must be ignored by subsequent processing tasks
     * and removed from the case
     */
    boolean isToIgnore();

    boolean isToSumVolume();

    /**
     * Removes the item from a category.
     *
     * @param category category to remove
     */
    void removeCategory(String category);

    /**
     * @param accessDate new last access date
     */
    void setAccessDate(Date accessDate);

    /**
     * @param addToCase whether the item must be added to the case
     */
    void setAddToCase(boolean addToCase);

    /**
     * Sets whether this is a carved item.
     *
     * @param carved whether this is a carved item
     */
    void setCarved(boolean carved);

    /**
     * Replaces the item category.
     *
     * @param category new category
     */
    void setCategory(String category);

    /**
     * @param creationDate new file creation date
     */
    void setCreationDate(Date creationDate);

    void setDataSource(IDataSource evidence);

    /**
     * Sets whether the item is deleted.
     *
     * @param deleted whether it is deleted
     */
    void setDeleted(boolean deleted);

    /**
     * Sets the item extension.
     *
     * @param ext extension
     */
    void setExtension(String ext);

    /**
     * Sets an extra attribute on the item.
     *
     * @param key attribute name
     * @param value attribute value
     */
    void setExtraAttribute(String key, Object value);

    void setTempAttribute(String key, Object value);

    /**
     * Sets the offset where carved items are found in the parent item.
     *
     * @param fileOffset item offset
     */
    void setFileOffset(long fileOffset);

    /**
     * Sets whether the item has children, such as subitems or carved items.
     *
     * @param hasChildren whether it has children
     */
    void setHasChildren(boolean hasChildren);

    /**
     * Sets the item hash.
     *
     * @param hash item hash
     */
    void setHash(String hash);

    /**
     * @param id item identifier
     */
    void setId(int id);

    /**
     * Sets whether the item is a directory.
     *
     * @param isDir whether it is a directory
     */
    void setIsDir(boolean isDir);

    /**
     * Sets item labels.
     *
     * @param labels concatenated labels
     */
    void setLabels(List<String> labels);

    /**
     * @param length file size
     */
    void setLength(Long length);

    /**
     * Sets the item media type based on signature detection.
     *
     * @param mediaType internet media type
     */
    default void setMediaType(Object mediaType) {
        try {
            Method method = getClass().getMethod("setMediaType", mediaType == null ? Object.class : mediaType.getClass()); //$NON-NLS-1$
            method.invoke(this, mediaType);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("setMediaType(Object) is not implemented", e); //$NON-NLS-1$
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set media type", e); //$NON-NLS-1$
        }
    }

    /**
     * Neutral media type mutator for gradual decoupling from external libraries.
     */
    default void setMediaTypeValue(MediaTypeValue mediaTypeValue) {
        setMediaType(mediaTypeValue == null ? null : mediaTypeValue.value());
    }

    default void setMetadata(Object metadata) {
        try {
            Method method = getClass().getMethod("setMetadata", metadata == null ? Object.class : metadata.getClass()); //$NON-NLS-1$
            method.invoke(this, metadata);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("setMetadata(Object) is not implemented", e); //$NON-NLS-1$
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set metadata", e); //$NON-NLS-1$
        }
    }

    /**
     * Neutral metadata mutator for gradual decoupling from external libraries.
     */
    default void setMetadataMap(Map<String, List<String>> metadataMap) {
        for (Map.Entry<String, List<String>> entry : metadataMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                setMetadataValue(entry.getKey(), ""); //$NON-NLS-1$
                continue;
            }
            setMetadataValue(entry.getKey(), entry.getValue().get(0));
            for (int i = 1; i < entry.getValue().size(); i++) {
                addMetadataValue(entry.getKey(), entry.getValue().get(i));
            }
        }
    }

    default void setMetadataValue(String key, String value) {
        Object metadata = getMetadata();
        if (metadata == null) {
            throw new UnsupportedOperationException("Cannot set metadata value on null metadata object"); //$NON-NLS-1$
        }
        try {
            Method method = metadata.getClass().getMethod("set", String.class, String.class); //$NON-NLS-1$
            method.invoke(metadata, key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to set metadata value", e); //$NON-NLS-1$
        }
    }

    default void addMetadataValue(String key, String value) {
        Object metadata = getMetadata();
        if (metadata == null) {
            throw new UnsupportedOperationException("Cannot add metadata value on null metadata object"); //$NON-NLS-1$
        }
        try {
            Method method = metadata.getClass().getMethod("add", String.class, String.class); //$NON-NLS-1$
            method.invoke(metadata, key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to add metadata value", e); //$NON-NLS-1$
        }
    }

    default void removeMetadataValue(String key) {
        Object metadata = getMetadata();
        if (metadata != null) {
            try {
                Method method = metadata.getClass().getMethod("remove", String.class); //$NON-NLS-1$
                method.invoke(metadata, key);
            } catch (Exception e) {
                throw new IllegalStateException("Unable to remove metadata value", e); //$NON-NLS-1$
            }
        }
    }

    /**
     * @param modificationDate file last modification date
     */
    void setModificationDate(Date modificationDate);

    /**
     * @param name file name
     */
    void setName(String name);

    void setParent(IItem parent);

    /**
     * @param parentId parent item identifier
     */
    void setParentId(Integer parentId);

    /**
     * @param parsed whether the item was parsed
     */
    void setParsed(boolean parsed);

    /**
     * @param parsedTextCache text extracted after parsing
     */
    @Deprecated
    void setParsedTextCache(String parsedTextCache);

    /**
     * @param path item path
     */
    void setPath(String path);

    /**
     * @param isQueueEnd whether this is a special queue-end item
     */
    void setQueueEnd(boolean isQueueEnd);

    void setChangeDate(Date changeDate);

    /**
     * @param isRoot whether the item is root
     */
    void setRoot(boolean isRoot);

    /**
     * @param isSubItem whether the item is a subitem
     */
    void setSubItem(boolean isSubItem);

    void setSumVolume(boolean sumVolume);

    /**
     * @param timeOut whether item parsing timed out
     */
    void setTimeOut(boolean timeOut);

    /**
     * @param isToExtract whether the item must be extracted
     */
    void setToExtract(boolean isToExtract);

    /**
     * @param toIgnore whether the item must be ignored by subsequent processing
     *                 tasks and removed from the case
     */
    void setToIgnore(boolean toIgnore);

    /**
     * @param toIgnore whether the item must be ignored by subsequent processing
     *                 tasks and removed from the case
     */
    void setToIgnore(boolean toIgnore, boolean updateStats);

    /**
     * @param type the detected file type extension
     */
    void setType(String type);

    /**
     * @param viewFile file path for preview.
     */
    void setViewFile(File viewFile);

    void setHasPreview(boolean b);

    void setPreviewExt(String viewExt);

    void setInputStreamFactory(ISeekableInputStreamFactory inputStreamFactory);

    void setIdInDataSource(String string);

    void setThumb(byte[] thumb);

    /**
     * @return returns the created evidenceFile.
     */
    IItem createChildItem();

    /**
     * Returns a String with data contained in this object.
     *
     * @return String listing file properties.
     */
    @Override
    String toString();

}
