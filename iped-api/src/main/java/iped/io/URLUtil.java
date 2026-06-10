package iped.io;

import java.net.URI;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * Helpers to obtain the code-source URL of a class, working around Windows
 * UNC path issues.
 */
public class URLUtil {
    /**
     * Return a URL from a Class. This method was created to handle Windows unmapped
     * network paths, like \\server\case. It adds another "//" before the actual
     * path name to avoid an IllegalArgumentException: URI has an authority
     * component. See issue #1336.
     *
     * @param clazz the class whose code-source location is wanted
     * @return the code-source URL, adjusted for UNC paths if needed
     */
    public static URL getURL(Class<?> clazz) {
        return getURL(clazz.getProtectionDomain());
    }

    /**
     * Return a URL from a ProtectionDomain.
     *
     * @param domain the protection domain whose code-source location is wanted
     * @return the code-source URL, adjusted for UNC paths if needed
     */
    public static URL getURL(ProtectionDomain domain) {
        URL url = domain.getCodeSource().getLocation();
        if (url != null && "file".equalsIgnoreCase(url.getProtocol()) && url.getPath() != null
            && url.getPath().startsWith("//")) {
            try {
                String newPath = "file:///" + url.getPath().substring(1);
                return URI.create(newPath).toURL();
            } catch (Exception e) {
                // If the fix doesn't work, just return the original URL.
            }
        }
        return url;
    }
}
