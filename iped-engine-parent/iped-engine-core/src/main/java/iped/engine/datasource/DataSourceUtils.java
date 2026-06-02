package iped.engine.datasource;

import iped.data.IItem;

import java.io.File;

/**
 * Lightweight utilities for datasource reader modules that do not require
 * engine internals.
 */
public final class DataSourceUtils {

    private DataSourceUtils() {}

    public static String getParentPath(IItem item) {
        String path = item.getPath();
        int end = path.length() - item.getName().length() - 1;
        if (end <= 0)
            return "";
        if (path.charAt(end) == '>' && path.charAt(end - 1) == '>')
            end--;
        return path.substring(0, end);
    }

    public static boolean isPhysicalDrive(File file) {
        return file.getName().toLowerCase().startsWith("physicaldrive")
                || file.getAbsolutePath().toLowerCase().startsWith("/dev/");
    }
}
