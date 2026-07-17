package iped.data;

import static org.junit.jupiter.api.Assertions.*;

import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;
import java.io.*;
import java.nio.channels.SeekableByteChannel;
import java.util.*;
import javax.imageio.stream.ImageInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for IItem interface default methods. The default methods use reflection to delegate to
 * concrete implementations.
 */
class IItemDefaultMethodsTest {

  // ============ setMediaType(Object) default method tests ============

  @Test
  @DisplayName("setMediaType(Object) with String should invoke concrete setMediaType(String)")
  void setMediaType_withString_shouldDelegate() {
    StubItem item = new StubItem();
    item.setMediaType("application/pdf");
    assertEquals("application/pdf", item.mediaTypeValue);
  }

  @Test
  @DisplayName("setMediaType(Object) with null should invoke concrete setMediaType with null")
  void setMediaType_withNull_shouldDelegate() {
    StubItem item = new StubItem();
    item.mediaTypeValue = "something";
    item.setMediaType((Object) null);
    assertNull(item.mediaTypeValue);
  }

  @Test
  @DisplayName(
      "setMediaType(Object) when no matching method found should throw UnsupportedOperationException")
  void setMediaType_whenNoMatchingMethod_shouldThrow() {
    NoMediaTypeItem item = new NoMediaTypeItem();
    assertThrows(UnsupportedOperationException.class, () -> item.setMediaType("test"));
  }

  // ============ setMediaTypeValue(MediaTypeValue) default method tests ============

  @Test
  @DisplayName("setMediaTypeValue with valid value should delegate to setMediaType with string")
  void setMediaTypeValue_withValidValue_shouldDelegate() {
    StubItem item = new StubItem();
    item.setMediaTypeValue(MediaTypeValue.of("text/plain"));
    assertEquals("text/plain", item.mediaTypeValue);
  }

  @Test
  @DisplayName("setMediaTypeValue with null should set media type to null")
  void setMediaTypeValue_withNull_shouldSetNull() {
    StubItem item = new StubItem();
    item.mediaTypeValue = "something";
    item.setMediaTypeValue(null);
    assertNull(item.mediaTypeValue);
  }

  // ============ setMetadata(Object) default method tests ============

  @Test
  @DisplayName("setMetadata(Object) should delegate to concrete setMetadata")
  void setMetadata_withObject_shouldDelegate() {
    StubItem item = new StubItem();
    Object metadata = new Object();
    item.setMetadata(metadata);
    assertSame(metadata, item.metadataValue);
  }

  @Test
  @DisplayName("setMetadata(Object) with null should invoke concrete method with null")
  void setMetadata_withNull_shouldDelegate() {
    StubItem item = new StubItem();
    item.setMetadata(null);
    assertNull(item.metadataValue);
  }

  @Test
  @DisplayName("setMetadata(Object) when no matching method should throw")
  void setMetadata_whenNoMatchingMethod_shouldThrow() {
    NoMetadataItem item = new NoMetadataItem();
    assertThrows(UnsupportedOperationException.class, () -> item.setMetadata(new Object()));
  }

  // ============ setMetadataMap(Map) default method tests ============

  @Test
  @DisplayName("setMetadataMap with single values should call setMetadataValue")
  void setMetadataMap_withSingleValues_shouldSetValues() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    Map<String, List<String>> map = Map.of("key1", List.of("val1"), "key2", List.of("val2"));
    item.setMetadataMap(map);

    assertEquals("val1", meta.get("key1"));
    assertEquals("val2", meta.get("key2"));
  }

  @Test
  @DisplayName("setMetadataMap with multiple values should call then add")
  void setMetadataMap_withMultipleValues_shouldSetAndAdd() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    Map<String, List<String>> map = Map.of("tags", List.of("tag1", "tag2", "tag3"));
    item.setMetadataMap(map);

    assertEquals("tag1", meta.get("tags"));
    assertArrayEquals(new String[] {"tag1", "tag2", "tag3"}, meta.getValues("tags"));
  }

  @Test
  @DisplayName("setMetadataMap with empty list should set empty string")
  void setMetadataMap_withEmptyList_shouldSetEmpty() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    Map<String, List<String>> map = Map.of("empty", List.of());
    item.setMetadataMap(map);

    assertEquals("", meta.get("empty"));
  }

  @Test
  @DisplayName("setMetadataMap with null list should set empty string")
  void setMetadataMap_withNullList_shouldSetEmpty() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    Map<String, List<String>> map = new HashMap<>();
    map.put("nullkey", null);
    item.setMetadataMap(map);

    assertEquals("", meta.get("nullkey"));
  }

  // ============ setMetadataValue(String, String) default method tests ============

  @Test
  @DisplayName("setMetadataValue should invoke set on metadata via reflection")
  void setMetadataValue_shouldInvokeSetOnMetadata() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    item.setMetadataValue("author", "John");

    assertEquals("John", meta.get("author"));
  }

  @Test
  @DisplayName("setMetadataValue when metadata is null should throw")
  void setMetadataValue_whenMetadataNull_shouldThrow() {
    StubItem item = new StubItem();
    item.metadata = null;

    assertThrows(UnsupportedOperationException.class, () -> item.setMetadataValue("key", "value"));
  }

  @Test
  @DisplayName("setMetadataValue should overwrite existing value")
  void setMetadataValue_shouldOverwrite() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    item.setMetadataValue("key", "old");
    item.setMetadataValue("key", "new");

    assertEquals("new", meta.get("key"));
  }

  // ============ addMetadataValue(String, String) default method tests ============

  @Test
  @DisplayName("addMetadataValue should invoke add on metadata via reflection")
  void addMetadataValue_shouldInvokeAddOnMetadata() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    item.setMetadataValue("tags", "first");
    item.addMetadataValue("tags", "second");

    assertArrayEquals(new String[] {"first", "second"}, meta.getValues("tags"));
  }

  @Test
  @DisplayName("addMetadataValue when metadata is null should throw")
  void addMetadataValue_whenMetadataNull_shouldThrow() {
    StubItem item = new StubItem();
    item.metadata = null;

    assertThrows(UnsupportedOperationException.class, () -> item.addMetadataValue("key", "value"));
  }

  // ============ removeMetadataValue(String) default method tests ============

  @Test
  @DisplayName("removeMetadataValue should invoke remove on metadata via reflection")
  void removeMetadataValue_shouldInvokeRemove() {
    StubItem item = new StubItem();
    FakeMetadata meta = new FakeMetadata();
    item.metadata = meta;

    meta.set("toRemove", "value");
    assertNotNull(meta.get("toRemove"));

    item.removeMetadataValue("toRemove");

    assertNull(meta.get("toRemove"));
  }

  @Test
  @DisplayName("removeMetadataValue when metadata is null should not throw")
  void removeMetadataValue_whenMetadataNull_shouldNotThrow() {
    StubItem item = new StubItem();
    item.metadata = null;

    assertDoesNotThrow(() -> item.removeMetadataValue("key"));
  }

  // ============ getItemInputStream() default method tests ============

  @Test
  @DisplayName("getItemInputStream should delegate to getSeekableInputStream")
  void getItemInputStream_shouldDelegateToGetSeekableInputStream() throws IOException {
    StubItem item = new StubItem();
    SeekableInputStream result = item.getItemInputStream();
    assertNull(result);
  }

  // ============ Test helper classes ============

  /**
   * Fake metadata object with set/add/get/getValues/remove/names methods that match the reflective
   * calls from IItem/IItemReader default methods.
   */
  static class FakeMetadata {
    private final Map<String, java.util.List<String>> values = new HashMap<>();

    public String[] names() {
      return values.keySet().toArray(new String[0]);
    }

    public String get(String key) {
      java.util.List<String> list = values.get(key);
      return list == null || list.isEmpty() ? null : list.getFirst();
    }

    public String[] getValues(String key) {
      java.util.List<String> list = values.get(key);
      return list == null ? new String[0] : list.toArray(new String[0]);
    }

    public void set(String key, String value) {
      values.put(key, new java.util.ArrayList<>(java.util.List.of(value)));
    }

    public void add(String key, String value) {
      values.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
    }

    public void remove(String key) {
      values.remove(key);
    }
  }

  /** Stub IItem implementation with proper method signatures for reflection. */
  static class StubItem implements IItem {
    String mediaTypeValue;
    Object metadataValue;
    Object metadata;

    // Concrete setMediaType(String) for the reflection-based setMediaType(Object) default
    public void setMediaType(String mediaType) {
      this.mediaTypeValue = mediaType;
    }

    // Concrete setMetadata(Object) for the reflection-based setMetadata(Object) default
    public void setMetadata(Object metadata) {
      this.metadataValue = metadata;
    }

    @Override
    public Object getMetadata() {
      return metadata;
    }

    @Override
    public Object getMediaType() {
      return mediaTypeValue;
    }

    @Override
    public void addCategory(String category) {}

    @Override
    public void addParentId(int parentId) {}

    @Override
    public void addParentIds(List<Integer> parentIds) {}

    @Override
    public void dispose() {}

    @Override
    public Date getAccessDate() {
      return null;
    }

    @Override
    public BufferedInputStream getBufferedInputStream() {
      return null;
    }

    @Override
    public String getCategories() {
      return null;
    }

    @Override
    public HashSet<String> getCategorySet() {
      return new HashSet<>();
    }

    @Override
    public Date getCreationDate() {
      return null;
    }

    @Override
    public String getIdInDataSource() {
      return null;
    }

    @Override
    public ISeekableInputStreamFactory getInputStreamFactory() {
      return null;
    }

    @Override
    public Object getExtraAttribute(String key) {
      return null;
    }

    @Override
    public Object getTempAttribute(String key) {
      return null;
    }

    @Override
    public Map<String, Object> getExtraAttributeMap() {
      return Map.of();
    }

    @Override
    public long getFileOffset() {
      return 0;
    }

    @Override
    public IHashValue getHashValue() {
      return null;
    }

    @Override
    public List<String> getLabels() {
      return List.of();
    }

    @Override
    public List<Integer> getParentIds() {
      return List.of();
    }

    @Override
    public String getParentIdsString() {
      return null;
    }

    @Override
    public String getParsedTextCache() {
      return null;
    }

    @Override
    public Reader getTextReader() {
      return null;
    }

    @Override
    public Date getChangeDate() {
      return null;
    }

    @Override
    public SeekableByteChannel getSeekableByteChannel() {
      return null;
    }

    @Override
    public SeekableInputStream getSeekableInputStream() {
      return null;
    }

    @Override
    public File getTempFile() {
      return null;
    }

    @Override
    public InputStream getTikaStream() {
      return null;
    }

    @Override
    public boolean hasTmpFile() {
      return false;
    }

    @Override
    public boolean isParsed() {
      return false;
    }

    @Override
    public boolean isQueueEnd() {
      return false;
    }

    @Override
    public boolean isToAddToCase() {
      return false;
    }

    @Override
    public boolean isToExtract() {
      return false;
    }

    @Override
    public boolean isToIgnore() {
      return false;
    }

    @Override
    public boolean isToSumVolume() {
      return false;
    }

    @Override
    public void removeCategory(String category) {}

    @Override
    public void setAccessDate(Date accessDate) {}

    @Override
    public void setAddToCase(boolean addToCase) {}

    @Override
    public void setCarved(boolean carved) {}

    @Override
    public void setCategory(String category) {}

    @Override
    public void setCreationDate(Date creationDate) {}

    @Override
    public void setDataSource(IDataSource evidence) {}

    @Override
    public void setDeleted(boolean deleted) {}

    @Override
    public void setExtension(String ext) {}

    @Override
    public void setExtraAttribute(String key, Object value) {}

    @Override
    public void setTempAttribute(String key, Object value) {}

    @Override
    public void setFileOffset(long fileOffset) {}

    @Override
    public void setHasChildren(boolean hasChildren) {}

    @Override
    public void setHash(String hash) {}

    @Override
    public void setId(int id) {}

    @Override
    public void setIsDir(boolean isDir) {}

    @Override
    public void setLabels(List<String> labels) {}

    @Override
    public void setLength(Long length) {}

    @Override
    public void setModificationDate(Date modificationDate) {}

    @Override
    public void setName(String name) {}

    @Override
    public void setParent(IItem parent) {}

    @Override
    public void setParentId(Integer parentId) {}

    @Override
    public void setParsed(boolean parsed) {}

    @Override
    public void setParsedTextCache(String parsedTextCache) {}

    @Override
    public void setPath(String path) {}

    @Override
    public void setQueueEnd(boolean isQueueEnd) {}

    @Override
    public void setChangeDate(Date changeDate) {}

    @Override
    public void setRoot(boolean isRoot) {}

    @Override
    public void setSubItem(boolean isSubItem) {}

    @Override
    public void setSumVolume(boolean sumVolume) {}

    @Override
    public void setTimeOut(boolean timeOut) {}

    @Override
    public void setToExtract(boolean isToExtract) {}

    @Override
    public void setToIgnore(boolean toIgnore) {}

    @Override
    public void setToIgnore(boolean toIgnore, boolean updateStats) {}

    @Override
    public void setType(String type) {}

    @Override
    public void setViewFile(File viewFile) {}

    @Override
    public void setHasPreview(boolean b) {}

    @Override
    public void setPreviewExt(String viewExt) {}

    @Override
    public void setInputStreamFactory(ISeekableInputStreamFactory inputStreamFactory) {}

    @Override
    public void setIdInDataSource(String string) {}

    @Override
    public void setThumb(byte[] thumb) {}

    @Override
    public IItem createChildItem() {
      return null;
    }

    @Override
    public int getId() {
      return 0;
    }

    @Override
    public Integer getParentId() {
      return null;
    }

    @Override
    public Integer getSubitemId() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getExt() {
      return null;
    }

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String getPath() {
      return null;
    }

    @Override
    public Long getLength() {
      return null;
    }

    @Override
    public String getHash() {
      return null;
    }

    @Override
    public boolean isDeleted() {
      return false;
    }

    @Override
    public boolean isCarved() {
      return false;
    }

    @Override
    public boolean isSubItem() {
      return false;
    }

    @Override
    public boolean isDir() {
      return false;
    }

    @Override
    public boolean isRoot() {
      return false;
    }

    @Override
    public boolean isTimedOut() {
      return false;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public File getViewFile() {
      return null;
    }

    @Override
    public boolean hasPreview() {
      return false;
    }

    @Override
    public File getPreviewBaseFolder() {
      return null;
    }

    @Override
    public String getPreviewExt() {
      return null;
    }

    @Override
    public SeekableInputStream getPreviewSeekeableInputStream() {
      return null;
    }

    @Override
    public byte[] getThumb() {
      return null;
    }

    @Override
    public ImageInputStream getImageInputStream() {
      return null;
    }

    @Override
    public Date getModDate() {
      return null;
    }

    @Override
    public IDataSource getDataSource() {
      return null;
    }

    @Override
    public void setSubitemId(Integer id) {}

    @Override
    public void setOpenContainer(Object container) {}
  }

  /**
   * IItem stub that does NOT have setMediaType(String), causing the default setMediaType(Object) to
   * throw UnsupportedOperationException.
   */
  static class NoMediaTypeItem implements IItem {
    @Override
    public Object getMediaType() {
      return null;
    }

    @Override
    public Object getMetadata() {
      return null;
    }

    @Override
    public void addCategory(String category) {}

    @Override
    public void addParentId(int parentId) {}

    @Override
    public void addParentIds(List<Integer> parentIds) {}

    @Override
    public void dispose() {}

    @Override
    public Date getAccessDate() {
      return null;
    }

    @Override
    public BufferedInputStream getBufferedInputStream() {
      return null;
    }

    @Override
    public String getCategories() {
      return null;
    }

    @Override
    public HashSet<String> getCategorySet() {
      return new HashSet<>();
    }

    @Override
    public Date getCreationDate() {
      return null;
    }

    @Override
    public String getIdInDataSource() {
      return null;
    }

    @Override
    public ISeekableInputStreamFactory getInputStreamFactory() {
      return null;
    }

    @Override
    public Object getExtraAttribute(String key) {
      return null;
    }

    @Override
    public Object getTempAttribute(String key) {
      return null;
    }

    @Override
    public Map<String, Object> getExtraAttributeMap() {
      return Map.of();
    }

    @Override
    public long getFileOffset() {
      return 0;
    }

    @Override
    public IHashValue getHashValue() {
      return null;
    }

    @Override
    public List<String> getLabels() {
      return List.of();
    }

    @Override
    public List<Integer> getParentIds() {
      return List.of();
    }

    @Override
    public String getParentIdsString() {
      return null;
    }

    @Override
    public String getParsedTextCache() {
      return null;
    }

    @Override
    public Reader getTextReader() {
      return null;
    }

    @Override
    public Date getChangeDate() {
      return null;
    }

    @Override
    public SeekableByteChannel getSeekableByteChannel() {
      return null;
    }

    @Override
    public SeekableInputStream getSeekableInputStream() {
      return null;
    }

    @Override
    public File getTempFile() {
      return null;
    }

    @Override
    public InputStream getTikaStream() {
      return null;
    }

    @Override
    public boolean hasTmpFile() {
      return false;
    }

    @Override
    public boolean isParsed() {
      return false;
    }

    @Override
    public boolean isQueueEnd() {
      return false;
    }

    @Override
    public boolean isToAddToCase() {
      return false;
    }

    @Override
    public boolean isToExtract() {
      return false;
    }

    @Override
    public boolean isToIgnore() {
      return false;
    }

    @Override
    public boolean isToSumVolume() {
      return false;
    }

    @Override
    public void removeCategory(String category) {}

    @Override
    public void setAccessDate(Date accessDate) {}

    @Override
    public void setAddToCase(boolean addToCase) {}

    @Override
    public void setCarved(boolean carved) {}

    @Override
    public void setCategory(String category) {}

    @Override
    public void setCreationDate(Date creationDate) {}

    @Override
    public void setDataSource(IDataSource evidence) {}

    @Override
    public void setDeleted(boolean deleted) {}

    @Override
    public void setExtension(String ext) {}

    @Override
    public void setExtraAttribute(String key, Object value) {}

    @Override
    public void setTempAttribute(String key, Object value) {}

    @Override
    public void setFileOffset(long fileOffset) {}

    @Override
    public void setHasChildren(boolean hasChildren) {}

    @Override
    public void setHash(String hash) {}

    @Override
    public void setId(int id) {}

    @Override
    public void setIsDir(boolean isDir) {}

    @Override
    public void setLabels(List<String> labels) {}

    @Override
    public void setLength(Long length) {}

    @Override
    public void setModificationDate(Date modificationDate) {}

    @Override
    public void setName(String name) {}

    @Override
    public void setParent(IItem parent) {}

    @Override
    public void setParentId(Integer parentId) {}

    @Override
    public void setParsed(boolean parsed) {}

    @Override
    public void setParsedTextCache(String parsedTextCache) {}

    @Override
    public void setPath(String path) {}

    @Override
    public void setQueueEnd(boolean isQueueEnd) {}

    @Override
    public void setChangeDate(Date changeDate) {}

    @Override
    public void setRoot(boolean isRoot) {}

    @Override
    public void setSubItem(boolean isSubItem) {}

    @Override
    public void setSumVolume(boolean sumVolume) {}

    @Override
    public void setTimeOut(boolean timeOut) {}

    @Override
    public void setToExtract(boolean isToExtract) {}

    @Override
    public void setToIgnore(boolean toIgnore) {}

    @Override
    public void setToIgnore(boolean toIgnore, boolean updateStats) {}

    @Override
    public void setType(String type) {}

    @Override
    public void setViewFile(File viewFile) {}

    @Override
    public void setHasPreview(boolean b) {}

    @Override
    public void setPreviewExt(String viewExt) {}

    @Override
    public void setInputStreamFactory(ISeekableInputStreamFactory inputStreamFactory) {}

    @Override
    public void setIdInDataSource(String string) {}

    @Override
    public void setThumb(byte[] thumb) {}

    @Override
    public IItem createChildItem() {
      return null;
    }

    @Override
    public int getId() {
      return 0;
    }

    @Override
    public Integer getParentId() {
      return null;
    }

    @Override
    public Integer getSubitemId() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getExt() {
      return null;
    }

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String getPath() {
      return null;
    }

    @Override
    public Long getLength() {
      return null;
    }

    @Override
    public String getHash() {
      return null;
    }

    @Override
    public boolean isDeleted() {
      return false;
    }

    @Override
    public boolean isCarved() {
      return false;
    }

    @Override
    public boolean isSubItem() {
      return false;
    }

    @Override
    public boolean isDir() {
      return false;
    }

    @Override
    public boolean isRoot() {
      return false;
    }

    @Override
    public boolean isTimedOut() {
      return false;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public File getViewFile() {
      return null;
    }

    @Override
    public boolean hasPreview() {
      return false;
    }

    @Override
    public File getPreviewBaseFolder() {
      return null;
    }

    @Override
    public String getPreviewExt() {
      return null;
    }

    @Override
    public SeekableInputStream getPreviewSeekeableInputStream() {
      return null;
    }

    @Override
    public byte[] getThumb() {
      return null;
    }

    @Override
    public ImageInputStream getImageInputStream() {
      return null;
    }

    @Override
    public Date getModDate() {
      return null;
    }

    @Override
    public IDataSource getDataSource() {
      return null;
    }

    @Override
    public void setSubitemId(Integer id) {}

    @Override
    public void setOpenContainer(Object container) {}
  }

  /**
   * IItem stub that does NOT have setMetadata(Object), causing the default setMetadata(Object) to
   * throw UnsupportedOperationException.
   */
  static class NoMetadataItem implements IItem {
    @Override
    public Object getMediaType() {
      return null;
    }

    @Override
    public Object getMetadata() {
      return null;
    }

    @Override
    public void addCategory(String category) {}

    @Override
    public void addParentId(int parentId) {}

    @Override
    public void addParentIds(List<Integer> parentIds) {}

    @Override
    public void dispose() {}

    @Override
    public Date getAccessDate() {
      return null;
    }

    @Override
    public BufferedInputStream getBufferedInputStream() {
      return null;
    }

    @Override
    public String getCategories() {
      return null;
    }

    @Override
    public HashSet<String> getCategorySet() {
      return new HashSet<>();
    }

    @Override
    public Date getCreationDate() {
      return null;
    }

    @Override
    public String getIdInDataSource() {
      return null;
    }

    @Override
    public ISeekableInputStreamFactory getInputStreamFactory() {
      return null;
    }

    @Override
    public Object getExtraAttribute(String key) {
      return null;
    }

    @Override
    public Object getTempAttribute(String key) {
      return null;
    }

    @Override
    public Map<String, Object> getExtraAttributeMap() {
      return Map.of();
    }

    @Override
    public long getFileOffset() {
      return 0;
    }

    @Override
    public IHashValue getHashValue() {
      return null;
    }

    @Override
    public List<String> getLabels() {
      return List.of();
    }

    @Override
    public List<Integer> getParentIds() {
      return List.of();
    }

    @Override
    public String getParentIdsString() {
      return null;
    }

    @Override
    public String getParsedTextCache() {
      return null;
    }

    @Override
    public Reader getTextReader() {
      return null;
    }

    @Override
    public Date getChangeDate() {
      return null;
    }

    @Override
    public SeekableByteChannel getSeekableByteChannel() {
      return null;
    }

    @Override
    public SeekableInputStream getSeekableInputStream() {
      return null;
    }

    @Override
    public File getTempFile() {
      return null;
    }

    @Override
    public InputStream getTikaStream() {
      return null;
    }

    @Override
    public boolean hasTmpFile() {
      return false;
    }

    @Override
    public boolean isParsed() {
      return false;
    }

    @Override
    public boolean isQueueEnd() {
      return false;
    }

    @Override
    public boolean isToAddToCase() {
      return false;
    }

    @Override
    public boolean isToExtract() {
      return false;
    }

    @Override
    public boolean isToIgnore() {
      return false;
    }

    @Override
    public boolean isToSumVolume() {
      return false;
    }

    @Override
    public void removeCategory(String category) {}

    @Override
    public void setAccessDate(Date accessDate) {}

    @Override
    public void setAddToCase(boolean addToCase) {}

    @Override
    public void setCarved(boolean carved) {}

    @Override
    public void setCategory(String category) {}

    @Override
    public void setCreationDate(Date creationDate) {}

    @Override
    public void setDataSource(IDataSource evidence) {}

    @Override
    public void setDeleted(boolean deleted) {}

    @Override
    public void setExtension(String ext) {}

    @Override
    public void setExtraAttribute(String key, Object value) {}

    @Override
    public void setTempAttribute(String key, Object value) {}

    @Override
    public void setFileOffset(long fileOffset) {}

    @Override
    public void setHasChildren(boolean hasChildren) {}

    @Override
    public void setHash(String hash) {}

    @Override
    public void setId(int id) {}

    @Override
    public void setIsDir(boolean isDir) {}

    @Override
    public void setLabels(List<String> labels) {}

    @Override
    public void setLength(Long length) {}

    @Override
    public void setModificationDate(Date modificationDate) {}

    @Override
    public void setName(String name) {}

    @Override
    public void setParent(IItem parent) {}

    @Override
    public void setParentId(Integer parentId) {}

    @Override
    public void setParsed(boolean parsed) {}

    @Override
    public void setParsedTextCache(String parsedTextCache) {}

    @Override
    public void setPath(String path) {}

    @Override
    public void setQueueEnd(boolean isQueueEnd) {}

    @Override
    public void setChangeDate(Date changeDate) {}

    @Override
    public void setRoot(boolean isRoot) {}

    @Override
    public void setSubItem(boolean isSubItem) {}

    @Override
    public void setSumVolume(boolean sumVolume) {}

    @Override
    public void setTimeOut(boolean timeOut) {}

    @Override
    public void setToExtract(boolean isToExtract) {}

    @Override
    public void setToIgnore(boolean toIgnore) {}

    @Override
    public void setToIgnore(boolean toIgnore, boolean updateStats) {}

    @Override
    public void setType(String type) {}

    @Override
    public void setViewFile(File viewFile) {}

    @Override
    public void setHasPreview(boolean b) {}

    @Override
    public void setPreviewExt(String viewExt) {}

    @Override
    public void setInputStreamFactory(ISeekableInputStreamFactory inputStreamFactory) {}

    @Override
    public void setIdInDataSource(String string) {}

    @Override
    public void setThumb(byte[] thumb) {}

    @Override
    public IItem createChildItem() {
      return null;
    }

    @Override
    public int getId() {
      return 0;
    }

    @Override
    public Integer getParentId() {
      return null;
    }

    @Override
    public Integer getSubitemId() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getExt() {
      return null;
    }

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String getPath() {
      return null;
    }

    @Override
    public Long getLength() {
      return null;
    }

    @Override
    public String getHash() {
      return null;
    }

    @Override
    public boolean isDeleted() {
      return false;
    }

    @Override
    public boolean isCarved() {
      return false;
    }

    @Override
    public boolean isSubItem() {
      return false;
    }

    @Override
    public boolean isDir() {
      return false;
    }

    @Override
    public boolean isRoot() {
      return false;
    }

    @Override
    public boolean isTimedOut() {
      return false;
    }

    @Override
    public boolean hasChildren() {
      return false;
    }

    @Override
    public File getViewFile() {
      return null;
    }

    @Override
    public boolean hasPreview() {
      return false;
    }

    @Override
    public File getPreviewBaseFolder() {
      return null;
    }

    @Override
    public String getPreviewExt() {
      return null;
    }

    @Override
    public SeekableInputStream getPreviewSeekeableInputStream() {
      return null;
    }

    @Override
    public byte[] getThumb() {
      return null;
    }

    @Override
    public ImageInputStream getImageInputStream() {
      return null;
    }

    @Override
    public Date getModDate() {
      return null;
    }

    @Override
    public IDataSource getDataSource() {
      return null;
    }

    @Override
    public void setSubitemId(Integer id) {}

    @Override
    public void setOpenContainer(Object container) {}
  }
}
