package iped.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LimitedInputStreamTest {

    @Test
    void read_whenLimitNotReached_thenReadsNormally() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 5);
        for (byte b : data) {
            assertEquals(b & 0xFF, lis.read());
        }
    }

    @Test
    void read_whenLimitReached_thenReturnsMinusOne() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 3);
        lis.read();
        lis.read();
        lis.read();
        assertEquals(-1, lis.read());
    }

    @Test
    void readArray_whenLimitSmaller_thenOnlyLimitBytesRead() throws IOException {
        byte[] data = {10, 20, 30, 40, 50};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 3);
        byte[] buf = new byte[10];
        int n = lis.read(buf);
        assertEquals(3, n);
        assertEquals(10, buf[0]);
        assertEquals(20, buf[1]);
        assertEquals(30, buf[2]);
    }

    @Test
    void readArray_afterLimit_thenMinusOne() throws IOException {
        byte[] data = {1, 2, 3};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 3);
        byte[] buf = new byte[3];
        lis.read(buf);
        assertEquals(-1, lis.read(buf));
    }

    @Test
    void available_reflectsLimit() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 3);
        assertTrue(lis.available() <= 3);
    }

    @Test
    void skip_doesNotExceedLimit() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(data), 3);
        long skipped = lis.skip(10);
        assertTrue(skipped <= 3);
        assertEquals(-1, lis.read());
    }

    @Test
    void markAndReset_whenSupported_thenResetsCorrectly() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        LimitedInputStream lis = new LimitedInputStream(bais, 5);
        lis.read(); // reads 1
        lis.mark(10);
        lis.read(); // reads 2
        lis.reset();
        assertEquals(2, lis.read()); // should re-read 2
    }

    @Test
    void limitZero_thenImmediatelyEof() throws IOException {
        LimitedInputStream lis = new LimitedInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3}), 0);
        assertEquals(-1, lis.read());
    }
}
