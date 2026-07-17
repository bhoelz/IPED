package iped.viewers.web;

public class ViewerCapabilities {

  public enum HitsMode {
    NONE,
    EXTERNAL,
    INTERNAL;

    public String label() {
      return name().toLowerCase();
    }
  }

  private final boolean search;
  private final HitsMode hitsMode;
  private final boolean toolbarSupported;
  private final boolean toolbarVisibleByDefault;

  public ViewerCapabilities(
      boolean search,
      HitsMode hitsMode,
      boolean toolbarSupported,
      boolean toolbarVisibleByDefault) {
    this.search = search;
    this.hitsMode = hitsMode;
    this.toolbarSupported = toolbarSupported;
    this.toolbarVisibleByDefault = toolbarVisibleByDefault;
  }

  public static ViewerCapabilities clientSearch() {
    return new ViewerCapabilities(true, HitsMode.INTERNAL, false, false);
  }

  public static ViewerCapabilities noSearch() {
    return new ViewerCapabilities(false, HitsMode.NONE, false, false);
  }

  public boolean isSearch() {
    return search;
  }

  public HitsMode getHitsMode() {
    return hitsMode;
  }

  public boolean isToolbarSupported() {
    return toolbarSupported;
  }

  public boolean isToolbarVisibleByDefault() {
    return toolbarVisibleByDefault;
  }
}
