package iped.parsers.compress;

public class CompressionUtil {

    public static String getParentPath(String path) {
        int i = path.lastIndexOf('\\');
        if (i == -1)
            i = path.lastIndexOf('/');
        if (i <= 0)
            return null;
        if (i == path.length() - 1)
            return getParentPath(path.substring(0, path.length() - 1));
        else
            return path.substring(0, i);
    }
}
