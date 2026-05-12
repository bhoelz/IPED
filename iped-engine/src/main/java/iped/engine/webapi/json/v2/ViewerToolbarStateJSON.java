package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerToolbarStateJSON {
    private boolean supported;
    private boolean visible;

    @ApiModelProperty()
    public boolean isSupported() {
        return supported;
    }

    public void setSupported(boolean supported) {
        this.supported = supported;
    }

    @ApiModelProperty()
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}

