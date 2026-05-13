package iped.engine.webapi.spi;

public class DocRef {
    private final String source;
    private final int id;

    public DocRef(String source, int id) {
        this.source = source;
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public int getId() {
        return id;
    }
}
