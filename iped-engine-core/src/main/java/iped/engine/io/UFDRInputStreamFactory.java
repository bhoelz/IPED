package iped.engine.io;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;

import iped.io.SeekableInputStream;

public class UFDRInputStreamFactory {

    public static final String UFDR_PATH_PREFIX = "ufdr:///";
    private final Object delegate;
    private final Method getSeekableInputStreamMethod;

    public UFDRInputStreamFactory(Path dataSource) throws IOException {
        try {
            Class<?> cls = Class.forName("iped.engine.io.ZIPInputStreamFactory");
            Constructor<?> ctor = cls.getConstructor(Path.class);
            delegate = ctor.newInstance(dataSource);
            getSeekableInputStreamMethod = cls.getMethod("getSeekableInputStream", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new IOException("ZIPInputStreamFactory is not available", e);
        }
    }

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

}
