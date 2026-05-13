package iped.data;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.stream.ImageInputStream;

import org.junit.jupiter.api.Test;

import iped.datasource.IDataSource;
import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;

class IItemReaderDefaultMethodsTest {

    @Test
    void mediaTypeDefaultAccessors_whenMediaTypePresent_thenReturnNeutralRepresentations() {
        StubItemReader reader = new StubItemReader();
        reader.mediaType = "application/test";

        assertNotNull(reader.getMediaTypeValue());
        assertEquals("application/test", reader.getMediaTypeValue().value());
        assertEquals("application/test", reader.getMediaTypeString());
    }

    @Test
    void mediaTypeDefaultAccessors_whenMediaTypeNull_thenReturnNulls() {
        StubItemReader reader = new StubItemReader();

        assertNull(reader.getMediaTypeValue());
        assertNull(reader.getMediaTypeString());
    }

    @Test
    void metadataDefaults_whenMetadataPresent_thenReadValuesAndMap() {
        StubItemReader reader = new StubItemReader();
        FakeMetadata metadata = new FakeMetadata();
        metadata.set("key", "value1");
        metadata.add("key", "value2");
        reader.metadata = metadata;

        assertEquals("value1", reader.getMetadataValue("key"));
        assertArrayEquals(new String[] { "value1", "value2" }, reader.getMetadataValues("key"));
        assertEquals(List.of("value1", "value2"), reader.getMetadataMap().get("key"));
    }

    @Test
    void metadataDefaults_whenMetadataNull_thenReturnNullOrEmpty() {
        StubItemReader reader = new StubItemReader();

        assertNull(reader.getMetadataValue("x"));
        assertNull(reader.getMetadataValues("x"));
        assertEquals(0, reader.getMetadataMap().size());
    }

    @Test
    void metadataDefaults_whenMetadataDoesNotExposeExpectedMethods_thenThrow() {
        StubItemReader reader = new StubItemReader();
        reader.metadata = new Object();

        assertThrows(IllegalStateException.class, () -> reader.getMetadataValue("x"));
        assertThrows(IllegalStateException.class, () -> reader.getMetadataValues("x"));
        assertThrows(IllegalStateException.class, reader::getMetadataMap);
    }

    static class FakeMetadata {
        private final Map<String, List<String>> values = new HashMap<>();

        public String[] names() {
            return values.keySet().toArray(new String[0]);
        }

        public String get(String key) {
            List<String> list = values.get(key);
            return list == null || list.isEmpty() ? null : list.get(0);
        }

        public String[] getValues(String key) {
            List<String> list = values.get(key);
            return list == null ? new String[0] : list.toArray(new String[0]);
        }

        public void set(String key, String value) {
            values.put(key, new java.util.ArrayList<>(List.of(value)));
        }

        public void add(String key, String value) {
            values.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(value);
        }
    }

    static class StubItemReader implements IItemReader {
        private Object mediaType;
        private Object metadata;

        @Override
        public Object getMediaType() {
            return mediaType;
        }

        @Override
        public Object getMetadata() {
            return metadata;
        }

        @Override public int getId() { return 0; }
        @Override public Integer getParentId() { return null; }
        @Override public Integer getSubitemId() { return null; }
        @Override public String getName() { return null; }
        @Override public String getExt() { return null; }
        @Override public String getType() { return null; }
        @Override public HashSet<String> getCategorySet() { return new HashSet<>(); }
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
        @Override public File getTempFile() { return null; }
        @Override public String getIdInDataSource() { return null; }
        @Override public ISeekableInputStreamFactory getInputStreamFactory() { return null; }
        @Override public File getViewFile() { return null; }
        @Override public boolean hasPreview() { return false; }
        @Override public File getPreviewBaseFolder() { return null; }
        @Override public String getPreviewExt() { return null; }
        @Override public SeekableInputStream getPreviewSeekeableInputStream() throws SQLException, IOException { return null; }
        @Override public byte[] getThumb() { return null; }
        @Override public BufferedInputStream getBufferedInputStream() throws IOException { return null; }
        @Override public ImageInputStream getImageInputStream() throws IOException { return null; }
        @Override public Date getModDate() { return null; }
        @Override public Date getCreationDate() { return null; }
        @Override public Date getAccessDate() { return null; }
        @Override public Date getChangeDate() { return null; }
        @Override public Object getExtraAttribute(String key) { return null; }
        @Override public Map<String, Object> getExtraAttributeMap() { return Map.of(); }
        @Override public IDataSource getDataSource() { return null; }
        @Override public SeekableInputStream getSeekableInputStream() throws IOException { return null; }
        @Override public SeekableByteChannel getSeekableByteChannel() throws IOException { return null; }
    }
}
