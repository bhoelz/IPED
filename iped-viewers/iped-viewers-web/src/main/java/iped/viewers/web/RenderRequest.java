package iped.viewers.web;

import iped.data.IItem;
import java.util.Collections;
import java.util.List;

public class RenderRequest {

  private final IItem item;
  private final String mimeType;
  private final RenditionKind kind;
  private final List<String> highlightTerms;
  private final int page; // 0-based; -1 = all (for text/html)
  private final int widthHint; // pixels; 0 = renderer default

  private RenderRequest(Builder b) {
    this.item = b.item;
    this.mimeType = b.mimeType;
    this.kind = b.kind;
    this.highlightTerms = Collections.unmodifiableList(b.highlightTerms);
    this.page = b.page;
    this.widthHint = b.widthHint;
  }

  public IItem getItem() {
    return item;
  }

  public String getMimeType() {
    return mimeType;
  }

  public RenditionKind getKind() {
    return kind;
  }

  public List<String> getHighlightTerms() {
    return highlightTerms;
  }

  public int getPage() {
    return page;
  }

  public int getWidthHint() {
    return widthHint;
  }

  public static Builder builder(IItem item, String mimeType, RenditionKind kind) {
    return new Builder(item, mimeType, kind);
  }

  public static final class Builder {
    private final IItem item;
    private final String mimeType;
    private final RenditionKind kind;
    private List<String> highlightTerms = Collections.emptyList();
    private int page = -1;
    private int widthHint = 0;

    private Builder(IItem item, String mimeType, RenditionKind kind) {
      this.item = item;
      this.mimeType = mimeType;
      this.kind = kind;
    }

    public Builder highlightTerms(List<String> terms) {
      this.highlightTerms = terms != null ? terms : Collections.emptyList();
      return this;
    }

    public Builder page(int page) {
      this.page = page;
      return this;
    }

    public Builder widthHint(int width) {
      this.widthHint = width;
      return this;
    }

    public RenderRequest build() {
      return new RenderRequest(this);
    }
  }
}
