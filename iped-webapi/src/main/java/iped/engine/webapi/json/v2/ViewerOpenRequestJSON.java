package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;

public class ViewerOpenRequestJSON {
  private String itemId;
  private String mimeType;
  private List<String> highlightTerms;
  private String preferredViewerId;
  private ViewerOpenContextJSON context;

  @ApiModelProperty(required = true)
  public String getItemId() {
    return itemId;
  }

  public void setItemId(String itemId) {
    this.itemId = itemId;
  }

  @ApiModelProperty(required = true)
  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  @ApiModelProperty()
  public List<String> getHighlightTerms() {
    return highlightTerms;
  }

  public void setHighlightTerms(List<String> highlightTerms) {
    this.highlightTerms = highlightTerms;
  }

  @ApiModelProperty()
  public String getPreferredViewerId() {
    return preferredViewerId;
  }

  public void setPreferredViewerId(String preferredViewerId) {
    this.preferredViewerId = preferredViewerId;
  }

  @ApiModelProperty()
  public ViewerOpenContextJSON getContext() {
    return context;
  }

  public void setContext(ViewerOpenContextJSON context) {
    this.context = context;
  }
}
