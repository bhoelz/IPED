package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

import java.util.List;

public class ViewerOpenResponseJSON {
    private String viewerSessionId;
    private String viewerId;
    private ViewerCapabilitiesJSON capabilities;
    private List<RenditionLinkJSON> renditions;

    @ApiModelProperty()
    public String getViewerSessionId() {
        return viewerSessionId;
    }

    public void setViewerSessionId(String viewerSessionId) {
        this.viewerSessionId = viewerSessionId;
    }

    @ApiModelProperty()
    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    @ApiModelProperty()
    public ViewerCapabilitiesJSON getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(ViewerCapabilitiesJSON capabilities) {
        this.capabilities = capabilities;
    }

    @ApiModelProperty()
    public List<RenditionLinkJSON> getRenditions() {
        return renditions;
    }

    public void setRenditions(List<RenditionLinkJSON> renditions) {
        this.renditions = renditions;
    }
}

