package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class RenditionLinkJSON {
    private String kind;
    private String url;

    @ApiModelProperty(allowableValues = "text,html,image,pdf,bytes")
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @ApiModelProperty()
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}

