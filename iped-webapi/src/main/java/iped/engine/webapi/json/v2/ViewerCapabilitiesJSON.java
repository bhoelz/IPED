package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerCapabilitiesJSON {
  private boolean search;
  private String hitsMode;
  private ViewerToolbarStateJSON toolbar;

  @ApiModelProperty()
  public boolean isSearch() {
    return search;
  }

  public void setSearch(boolean search) {
    this.search = search;
  }

  @ApiModelProperty(allowableValues = "none,external,internal")
  public String getHitsMode() {
    return hitsMode;
  }

  public void setHitsMode(String hitsMode) {
    this.hitsMode = hitsMode;
  }

  @ApiModelProperty()
  public ViewerToolbarStateJSON getToolbar() {
    return toolbar;
  }

  public void setToolbar(ViewerToolbarStateJSON toolbar) {
    this.toolbar = toolbar;
  }
}
