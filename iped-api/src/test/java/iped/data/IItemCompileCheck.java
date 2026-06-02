package iped.data;

import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;

import javax.imageio.stream.ImageInputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.SeekableByteChannel;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Minimal stub to verify IItem can be implemented.
 */
class MinimalStub implements IItem {
    @Override public Object getMediaType() { return null; }
    @Override public Object getMetadata() { return null; }
    @Override public void addCategory(String c) {}
    @Override public void addParentId(int p) {}
    @Override public void addParentIds(List<Integer> p) {}
    @Override public void dispose() {}
    @Override public Date getAccessDate() { return null; }
    @Override public BufferedInputStream getBufferedInputStream() { return null; }
    @Override public String getCategories() { return null; }
    @Override public HashSet<String> getCategorySet() { return new HashSet<>(); }
    @Override public Date getCreationDate() { return null; }
    @Override public String getIdInDataSource() { return null; }
    @Override public ISeekableInputStreamFactory getInputStreamFactory() { return null; }
    @Override public Object getExtraAttribute(String k) { return null; }
    @Override public Object getTempAttribute(String k) { return null; }
    @Override public Map<String, Object> getExtraAttributeMap() { return Map.of(); }
    @Override public long getFileOffset() { return 0; }
    @Override public IHashValue getHashValue() { return null; }
    @Override public List<String> getLabels() { return List.of(); }
    @Override public List<Integer> getParentIds() { return List.of(); }
    @Override public String getParentIdsString() { return null; }
    @Override public String getParsedTextCache() { return null; }
    @Override public Reader getTextReader() { return null; }
    @Override public Date getChangeDate() { return null; }
    @Override public SeekableByteChannel getSeekableByteChannel() { return null; }
    @Override public SeekableInputStream getSeekableInputStream() { return null; }
    @Override public File getTempFile() { return null; }
    @Override public InputStream getTikaStream() { return null; }
    @Override public boolean hasTmpFile() { return false; }
    @Override public boolean isParsed() { return false; }
    @Override public boolean isQueueEnd() { return false; }
    @Override public boolean isToAddToCase() { return false; }
    @Override public boolean isToExtract() { return false; }
    @Override public boolean isToIgnore() { return false; }
    @Override public boolean isToSumVolume() { return false; }
    @Override public void removeCategory(String c) {}
    @Override public void setAccessDate(Date d) {}
    @Override public void setAddToCase(boolean b) {}
    @Override public void setCarved(boolean b) {}
    @Override public void setCategory(String c) {}
    @Override public void setCreationDate(Date d) {}
    @Override public void setDataSource(IDataSource ds) {}
    @Override public void setDeleted(boolean b) {}
    @Override public void setExtension(String e) {}
    @Override public void setExtraAttribute(String k, Object v) {}
    @Override public void setTempAttribute(String k, Object v) {}
    @Override public void setFileOffset(long o) {}
    @Override public void setHasChildren(boolean b) {}
    @Override public void setHash(String h) {}
    @Override public void setId(int id) {}
    @Override public void setIsDir(boolean b) {}
    @Override public void setLabels(List<String> l) {}
    @Override public void setLength(Long l) {}
    @Override public void setModificationDate(Date d) {}
    @Override public void setName(String n) {}
    @Override public void setParent(IItem p) {}
    @Override public void setParentId(Integer p) {}
    @Override public void setParsed(boolean b) {}
    @Override public void setParsedTextCache(String s) {}
    @Override public void setPath(String p) {}
    @Override public void setQueueEnd(boolean b) {}
    @Override public void setChangeDate(Date d) {}
    @Override public void setRoot(boolean b) {}
    @Override public void setSubItem(boolean b) {}
    @Override public void setSumVolume(boolean b) {}
    @Override public void setTimeOut(boolean b) {}
    @Override public void setToExtract(boolean b) {}
    @Override public void setToIgnore(boolean b) {}
    @Override public void setToIgnore(boolean b, boolean u) {}
    @Override public void setType(String t) {}
    @Override public void setViewFile(File f) {}
    @Override public void setHasPreview(boolean b) {}
    @Override public void setPreviewExt(String e) {}
    @Override public void setInputStreamFactory(ISeekableInputStreamFactory f) {}
    @Override public void setIdInDataSource(String s) {}
    @Override public void setThumb(byte[] t) {}
    @Override public IItem createChildItem() { return null; }
    @Override public int getId() { return 0; }
    @Override public Integer getParentId() { return null; }
    @Override public Integer getSubitemId() { return null; }
    @Override public String getName() { return null; }
    @Override public String getExt() { return null; }
    @Override public String getType() { return null; }
    @Override public String getPath() { return null; }
    @Override public Long getLength() { return null; }
    @Override public String getHash() { return null; }
    @Override public boolean isDeleted() { return false; }
    @Override public boolean isCarved() { return false; }
    @Override public boolean isSubItem() { return false; }
    @Override public boolean isDir() { return false; }
    @Override public boolean isRoot() { return false; }
    @Override public boolean isTimedOut() { return false; }
    @Override public boolean hasChildren() { return false; }
    @Override public File getViewFile() { return null; }
    @Override public boolean hasPreview() { return false; }
    @Override public File getPreviewBaseFolder() { return null; }
    @Override public String getPreviewExt() { return null; }
    @Override public SeekableInputStream getPreviewSeekeableInputStream() { return null; }
    @Override public byte[] getThumb() { return null; }
    @Override public ImageInputStream getImageInputStream() { return null; }
    @Override public Date getModDate() { return null; }
    @Override public IDataSource getDataSource() { return null; }
    @Override public void setSubitemId(Integer id) {}
    @Override public void setOpenContainer(Object container) {}
}
