package iped.engine.io;

import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Path;

public class UFDRInputStreamFactory implements ISeekableInputStreamFactory, Closeable {

    public static final String UFDR_PATH_PREFIX = "ufdr:///";
    private final Object delegate;
    private final Method getSeekableInputStreamMethod;
    private final Method entryExistsMethod;
    private final Method getEntrySizeMethod;
    private final Method closeMethod;
    private final Path dataSource;

    public UFDRInputStreamFactory(Path dataSource) throws IOException {
        this.dataSource = dataSource;
        try {
            Class<?> cls = Class.forName("iped.engine.io.ZIPInputStreamFactory");
            Constructor<?> ctor = cls.getConstructor(Path.class);
            delegate = ctor.newInstance(dataSource);
            getSeekableInputStreamMethod = cls.getMethod("getSeekableInputStream", String.class);
            entryExistsMethod = cls.getMethod("entryExists", String.class);
            getEntrySizeMethod = cls.getMethod("getEntrySize", String.class);
            closeMethod = cls.getMethod("close");
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new IOException("ZIPInputStreamFactory is not available", e);
        }
    }

    public boolean entryExists(String path) throws IOException {
        try {
            return (Boolean) entryExistsMethod.invoke(delegate, path);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Error checking UFDR entry", e);
        }
    }

    public long getEntrySize(String path) throws IOException {
        try {
            return (Long) getEntrySizeMethod.invoke(delegate, path);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Error getting UFDR entry size", e);
        }
    }

    @Override
    public SeekableInputStream getSeekableInputStream(String path) throws IOException {
        int idx = path.indexOf(UFDR_PATH_PREFIX);
        if (idx != -1) {
            path = path.substring(path.indexOf(UFDR_PATH_PREFIX) + UFDR_PATH_PREFIX.length());
        }
        try {
            return (SeekableInputStream) getSeekableInputStreamMethod.invoke(delegate, path);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Error reading UFDR stream", e);
        }
    }

    @Override
    public URI getDataSourceURI() {
        return dataSource.toUri();
    }

    @Override
    public void close() throws IOException {
        try {
            closeMethod.invoke(delegate);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IOException("Error closing UFDR factory", e);
        }
    }

}
