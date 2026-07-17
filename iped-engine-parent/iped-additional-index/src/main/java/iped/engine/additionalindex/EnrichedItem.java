package iped.engine.additionalindex;

import iped.data.IHashValue;
import iped.data.IItem;
import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.sql.SQLException;
import java.util.*;
import javax.imageio.stream.ImageInputStream;

/**
 * Read-optimised decorator around an existing {@link IItem} that transparently merges extra
 * attributes from one or more additional-processing sources.
 *
 * <p>All read methods that are NOT related to extra attributes delegate to the underlying item
 * unchanged. The two exceptions are:
 *
 * <ul>
 *   <li>{@link #getExtraAttributeMap()} – returns the delegate's map overlaid with {@code
 *       additionalExtraAttrs} (additional wins on conflict).
 *   <li>{@link #getExtraAttribute(String)} – checks the additional map first, then falls through to
 *       the delegate.
 * </ul>
 *
 * <p>All write methods delegate unchanged to the underlying item so that task-processing code can
 * still mutate the item normally. Callers that need a pristine, non-enriched item for task
 * execution should use {@code IPEDSource.getRawItemByID()} instead of {@code getItemByID()}.
 */
public class EnrichedItem implements IItem {

  private final IItem delegate;

  /** Pre-computed merged map from all additional sources for this item. */
  private final Map<String, Object> additionalExtraAttrs;

  /**
   * @param delegate the original item from the main Lucene index
   * @param additionalExtraAttrs merged extra attributes from additional sources
   */
  public EnrichedItem(IItem delegate, Map<String, Object> additionalExtraAttrs) {
    this.delegate = delegate;
    this.additionalExtraAttrs =
        additionalExtraAttrs != null ? additionalExtraAttrs : Collections.emptyMap();
  }

  // =========================================================================
  // Overridden extra-attribute accessors
  // =========================================================================

  @Override
  public Object getExtraAttribute(String key) {
    Object additional = additionalExtraAttrs.get(key);
    return additional != null ? additional : delegate.getExtraAttribute(key);
  }

  @Override
  public Map<String, Object> getExtraAttributeMap() {
    if (additionalExtraAttrs.isEmpty()) {
      return delegate.getExtraAttributeMap();
    }
    // Delegate's attributes first; additional attributes overlay them.
    Map<String, Object> merged = new LinkedHashMap<>(delegate.getExtraAttributeMap());
    merged.putAll(additionalExtraAttrs);
    return merged;
  }

  // =========================================================================
  // IItemReader – read-only delegates (no extra-attribute involvement)
  // =========================================================================

  @Override
  public SeekableInputStream getSeekableInputStream() throws IOException {
    return delegate.getSeekableInputStream();
  }

  @Override
  public int getId() {
    return delegate.getId();
  }

  @Override
  public Integer getParentId() {
    return delegate.getParentId();
  }

  @Override
  public Integer getSubitemId() {
    return delegate.getSubitemId();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public String getExt() {
    return delegate.getExt();
  }

  @Override
  public String getType() {
    return delegate.getType();
  }

  @Override
  public Object getMediaType() {
    return delegate.getMediaType();
  }

  @Override
  public HashSet<String> getCategorySet() {
    return delegate.getCategorySet();
  }

  @Override
  public String getPath() {
    return delegate.getPath();
  }

  @Override
  public Long getLength() {
    return delegate.getLength();
  }

  @Override
  public String getHash() {
    return delegate.getHash();
  }

  @Override
  public boolean isDeleted() {
    return delegate.isDeleted();
  }

  @Override
  public boolean isCarved() {
    return delegate.isCarved();
  }

  @Override
  public boolean isSubItem() {
    return delegate.isSubItem();
  }

  @Override
  public boolean isDir() {
    return delegate.isDir();
  }

  @Override
  public boolean isRoot() {
    return delegate.isRoot();
  }

  @Override
  public boolean isTimedOut() {
    return delegate.isTimedOut();
  }

  @Override
  public boolean hasChildren() {
    return delegate.hasChildren();
  }

  @Override
  public File getTempFile() throws IOException {
    return delegate.getTempFile();
  }

  @Override
  public String getIdInDataSource() {
    return delegate.getIdInDataSource();
  }

  @Override
  public ISeekableInputStreamFactory getInputStreamFactory() {
    return delegate.getInputStreamFactory();
  }

  @Override
  public File getViewFile() {
    return delegate.getViewFile();
  }

  @Override
  public boolean hasPreview() {
    return delegate.hasPreview();
  }

  @Override
  public File getPreviewBaseFolder() {
    return delegate.getPreviewBaseFolder();
  }

  @Override
  public String getPreviewExt() {
    return delegate.getPreviewExt();
  }

  @Override
  public SeekableInputStream getPreviewSeekeableInputStream() throws SQLException, IOException {
    return delegate.getPreviewSeekeableInputStream();
  }

  @Override
  public byte[] getThumb() {
    return delegate.getThumb();
  }

  @Override
  public BufferedInputStream getBufferedInputStream() throws IOException {
    return delegate.getBufferedInputStream();
  }

  @Override
  public ImageInputStream getImageInputStream() throws IOException {
    return delegate.getImageInputStream();
  }

  @Override
  public Date getModDate() {
    return delegate.getModDate();
  }

  @Override
  public Date getCreationDate() {
    return delegate.getCreationDate();
  }

  @Override
  public Date getAccessDate() {
    return delegate.getAccessDate();
  }

  @Override
  public Date getChangeDate() {
    return delegate.getChangeDate();
  }

  @Override
  public IDataSource getDataSource() {
    return delegate.getDataSource();
  }

  @Override
  public Object getMetadata() {
    return delegate.getMetadata();
  }

  @Override
  public Object getTempAttribute(String key) {
    return delegate.getTempAttribute(key);
  }

  // =========================================================================
  // IItem – additional read-only methods
  // =========================================================================

  @Override
  public String getCategories() {
    return delegate.getCategories();
  }

  @Override
  public long getFileOffset() {
    return delegate.getFileOffset();
  }

  @Override
  public IHashValue getHashValue() {
    return delegate.getHashValue();
  }

  @Override
  public List<String> getLabels() {
    return delegate.getLabels();
  }

  @Override
  public List<Integer> getParentIds() {
    return delegate.getParentIds();
  }

  @Override
  public String getParentIdsString() {
    return delegate.getParentIdsString();
  }

  @Override
  @Deprecated
  public String getParsedTextCache() {
    return delegate.getParsedTextCache();
  }

  @Override
  public Reader getTextReader() throws IOException {
    return delegate.getTextReader();
  }

  @Override
  public SeekableByteChannel getSeekableByteChannel() throws IOException {
    return delegate.getSeekableByteChannel();
  }

  @Override
  public InputStream getTikaStream() throws IOException {
    return delegate.getTikaStream();
  }

  @Override
  public boolean hasTmpFile() {
    return delegate.hasTmpFile();
  }

  @Override
  public boolean isParsed() {
    return delegate.isParsed();
  }

  @Override
  public boolean isQueueEnd() {
    return delegate.isQueueEnd();
  }

  @Override
  public boolean isToAddToCase() {
    return delegate.isToAddToCase();
  }

  @Override
  public boolean isToExtract() {
    return delegate.isToExtract();
  }

  @Override
  public boolean isToIgnore() {
    return delegate.isToIgnore();
  }

  @Override
  public boolean isToSumVolume() {
    return delegate.isToSumVolume();
  }

  // =========================================================================
  // IItem – write methods (delegate transparently)
  // =========================================================================

  @Override
  public void addCategory(String category) {
    delegate.addCategory(category);
  }

  @Override
  public void addParentId(int parentId) {
    delegate.addParentId(parentId);
  }

  @Override
  public void addParentIds(List<Integer> parentIds) {
    delegate.addParentIds(parentIds);
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }

  @Override
  public void removeCategory(String category) {
    delegate.removeCategory(category);
  }

  @Override
  public void setAccessDate(Date accessDate) {
    delegate.setAccessDate(accessDate);
  }

  @Override
  public void setAddToCase(boolean addToCase) {
    delegate.setAddToCase(addToCase);
  }

  @Override
  public void setCarved(boolean carved) {
    delegate.setCarved(carved);
  }

  @Override
  public void setCategory(String category) {
    delegate.setCategory(category);
  }

  @Override
  public void setCreationDate(Date creationDate) {
    delegate.setCreationDate(creationDate);
  }

  @Override
  public void setDataSource(IDataSource evidence) {
    delegate.setDataSource(evidence);
  }

  @Override
  public void setDeleted(boolean deleted) {
    delegate.setDeleted(deleted);
  }

  @Override
  public void setExtension(String ext) {
    delegate.setExtension(ext);
  }

  @Override
  public void setExtraAttribute(String key, Object value) {
    delegate.setExtraAttribute(key, value);
  }

  @Override
  public void setTempAttribute(String key, Object value) {
    delegate.setTempAttribute(key, value);
  }

  @Override
  public void setFileOffset(long fileOffset) {
    delegate.setFileOffset(fileOffset);
  }

  @Override
  public void setHasChildren(boolean hasChildren) {
    delegate.setHasChildren(hasChildren);
  }

  @Override
  public void setHash(String hash) {
    delegate.setHash(hash);
  }

  @Override
  public void setId(int id) {
    delegate.setId(id);
  }

  @Override
  public void setIsDir(boolean isDir) {
    delegate.setIsDir(isDir);
  }

  @Override
  public void setLabels(List<String> labels) {
    delegate.setLabels(labels);
  }

  @Override
  public void setLength(Long length) {
    delegate.setLength(length);
  }

  @Override
  public void setModificationDate(Date modificationDate) {
    delegate.setModificationDate(modificationDate);
  }

  @Override
  public void setName(String name) {
    delegate.setName(name);
  }

  @Override
  public void setParent(IItem parent) {
    delegate.setParent(parent);
  }

  @Override
  public void setParentId(Integer parentId) {
    delegate.setParentId(parentId);
  }

  @Override
  public void setParsed(boolean parsed) {
    delegate.setParsed(parsed);
  }

  @Override
  @Deprecated
  public void setParsedTextCache(String parsedTextCache) {
    delegate.setParsedTextCache(parsedTextCache);
  }

  @Override
  public void setPath(String path) {
    delegate.setPath(path);
  }

  @Override
  public void setQueueEnd(boolean isQueueEnd) {
    delegate.setQueueEnd(isQueueEnd);
  }

  @Override
  public void setChangeDate(Date changeDate) {
    delegate.setChangeDate(changeDate);
  }

  @Override
  public void setRoot(boolean isRoot) {
    delegate.setRoot(isRoot);
  }

  @Override
  public void setSubItem(boolean isSubItem) {
    delegate.setSubItem(isSubItem);
  }

  @Override
  public void setSumVolume(boolean sumVolume) {
    delegate.setSumVolume(sumVolume);
  }

  @Override
  public void setTimeOut(boolean timeOut) {
    delegate.setTimeOut(timeOut);
  }

  @Override
  public void setToExtract(boolean isToExtract) {
    delegate.setToExtract(isToExtract);
  }

  @Override
  public void setToIgnore(boolean toIgnore) {
    delegate.setToIgnore(toIgnore);
  }

  @Override
  public void setToIgnore(boolean toIgnore, boolean updateStats) {
    delegate.setToIgnore(toIgnore, updateStats);
  }

  @Override
  public void setType(String type) {
    delegate.setType(type);
  }

  @Override
  public void setViewFile(File viewFile) {
    delegate.setViewFile(viewFile);
  }

  @Override
  public void setHasPreview(boolean b) {
    delegate.setHasPreview(b);
  }

  @Override
  public void setPreviewExt(String viewExt) {
    delegate.setPreviewExt(viewExt);
  }

  @Override
  public void setInputStreamFactory(ISeekableInputStreamFactory inputStreamFactory) {
    delegate.setInputStreamFactory(inputStreamFactory);
  }

  @Override
  public void setIdInDataSource(String string) {
    delegate.setIdInDataSource(string);
  }

  @Override
  public void setThumb(byte[] thumb) {
    delegate.setThumb(thumb);
  }

  @Override
  public IItem createChildItem() {
    return delegate.createChildItem();
  }

  @Override
  public void setSubitemId(Integer id) {
    delegate.setSubitemId(id);
  }

  @Override
  public void setOpenContainer(Object container) {
    delegate.setOpenContainer(container);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
