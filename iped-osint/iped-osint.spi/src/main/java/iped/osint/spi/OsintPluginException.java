package iped.osint.spi;

public class OsintPluginException extends Exception {

    public OsintPluginException(String message) {
        super(message);
    }

    public OsintPluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
