package iped.engine.webapi.spi;

public class SourceDescriptor {
    private final String id;
    private final String path;

    public SourceDescriptor(String id, String path) {
        this.id = id;
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }
}
