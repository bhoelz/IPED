package iped.engine.webapi.spi;

public class RenditionDescriptor {

  private final String kind;
  private final String url;

  public RenditionDescriptor(String kind, String url) {
    this.kind = kind;
    this.url = url;
  }

  public String getKind() {
    return kind;
  }

  public String getUrl() {
    return url;
  }
}
