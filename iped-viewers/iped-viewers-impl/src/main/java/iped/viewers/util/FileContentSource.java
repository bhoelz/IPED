package iped.viewers.util;

import iped.io.IStreamSource;
import iped.io.SeekableInputStream;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

public class FileContentSource implements IStreamSource {

    private File file;

    public FileContentSource(File file) {
        this.file = file;
    }

    @Override
    public SeekableInputStream getSeekableInputStream() throws IOException {
        return new SeekableFileInputStream(file);
    }

    @Override
    public SeekableByteChannel getSeekableByteChannel() throws IOException {
        return new SeekableByteChannelImpl(getSeekableInputStream());
    }

    @Override
    public File getTempFile() {
        return file;
    }

}
