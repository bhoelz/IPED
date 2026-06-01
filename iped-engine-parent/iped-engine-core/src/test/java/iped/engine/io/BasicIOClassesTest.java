package iped.engine.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class BasicIOClassesTest {

    @Test
    void filterOutputStreamShouldRouteByPrefix() throws Exception {
        ByteArrayOutputStream def = new ByteArrayOutputStream();
        ByteArrayOutputStream red = new ByteArrayOutputStream();
        FilterOutputStream out = new FilterOutputStream(def, red, "R:");

        out.write("R:abc".getBytes(), 0, 5);
        out.write("DEF".getBytes(), 0, 3);
        out.write('X');
        out.close();

        assertEquals("R:abc", red.toString());
        assertEquals("DEFX", def.toString());
    }

    @Test
    void closeFilterReaderShouldIgnoreCloseUntilReallyClose() throws Exception {
        StringReader base = new StringReader("abc");
        CloseFilterReader reader = new CloseFilterReader(base);
        reader.close();
        assertEquals('a', reader.read());
        reader.reallyClose();
        assertThrows(IOException.class, reader::read);
    }

    @Test
    void timeoutExceptionConstructorsShouldPreserveMessage() {
        assertEquals("m", new TimeoutException("m").getMessage());
    }

    @Test
    void bufferedRandomAccessFileShouldReadAndSeekWithUnsignedBytes() throws Exception {
        Path tmp = Files.createTempFile("braf", ".bin");
        byte[] bytes = new byte[] {0, 1, (byte) 255, 3, 4, 5};
        Files.write(tmp, bytes);
        try (BufferedRandomAccessFile file = new BufferedRandomAccessFile(tmp.toFile(), "r", 2)) {
            assertEquals(0, file.read());
            assertEquals(1, file.read());
            assertEquals(255, file.read());
            file.seek(1);
            assertEquals(1, file.read());
            byte[] read = new byte[3];
            int n = file.read(read, 0, 3);
            assertEquals(3, n);
            assertArrayEquals(new byte[] {(byte) 255, 3, 4}, read);
        }
    }

    @Test
    void referencedFileShouldDeleteWhenReferenceCountReachesZero() throws Exception {
        Path tmp = Files.createTempFile("ref-file", ".tmp");
        File file = tmp.toFile();
        ReferencedFile rf = new ReferencedFile(file);
        rf.increment();
        rf.close();
        assertTrue(file.exists());
        rf.close();
        assertFalse(file.exists());
    }

    @Test
    void sequenceSeekableByteChannelShouldReadAcrossChannelsAndTrackPosition() throws Exception {
        SeekableByteChannel c1 = new InMemorySeekableByteChannel(new byte[] {1, 2});
        SeekableByteChannel c2 = new InMemorySeekableByteChannel(new byte[] {3, 4, 5});
        SequenceSeekableByteChannel seq = new SequenceSeekableByteChannel(c1, c2);

        ByteBuffer bb = ByteBuffer.allocate(2);
        assertEquals(2, seq.read(bb));
        assertEquals(2, seq.position());
        bb.clear();
        assertEquals(0, seq.read(bb)); // transition to next channel
        bb.clear();
        assertEquals(2, seq.read(bb));
        assertEquals(4, seq.position());
        assertEquals(5, seq.size());
        assertThrows(IOException.class, () -> seq.write(ByteBuffer.wrap(new byte[] {1})));
        assertThrows(IOException.class, () -> seq.truncate(1));
        seq.close();
        assertTrue(seq.isOpen());
    }

    @Test
    void ufedXmlWrapperShouldReplaceInvalidCodesAndWrapPasswordDataWithCData() throws Exception {
        String xml = "<x>&#x1;&#x2;</x>\n"
                + "<model type=\"Password\" >\n"
                + "<field name=\"Data\" type=\"String\">\n"
                + "<value type=\"String\">secret</value>\n"
                + "</field>\n"
                + "</model>\n";
        UFEDXMLWrapper wrapper = new UFEDXMLWrapper(new ByteArrayInputStream(xml.getBytes()));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[64];
        int n;
        while ((n = wrapper.read(buf, 0, buf.length)) != -1) {
            sb.append(buf, 0, n);
        }
        String out = sb.toString();
        assertTrue(out.contains("??"));
        assertTrue(out.contains("<![CDATA[secret]]>"));
        wrapper.close();
    }

    private static final class InMemorySeekableByteChannel implements SeekableByteChannel {
        private final byte[] data;
        private int pos;
        private boolean open = true;

        private InMemorySeekableByteChannel(byte[] data) {
            this.data = Arrays.copyOf(data, data.length);
        }

        @Override
        public int read(ByteBuffer dst) {
            if (pos >= data.length) {
                return -1;
            }
            int len = Math.min(dst.remaining(), data.length - pos);
            dst.put(data, pos, len);
            pos += len;
            return len;
        }

        @Override
        public int write(ByteBuffer src) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long position() {
            return pos;
        }

        @Override
        public SeekableByteChannel position(long newPosition) {
            pos = (int) newPosition;
            return this;
        }

        @Override
        public long size() {
            return data.length;
        }

        @Override
        public SeekableByteChannel truncate(long size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
