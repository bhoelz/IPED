package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerNavigateHitRequestJSON {
    private String direction;
    private boolean wrap;

    @ApiModelProperty(allowableValues = "next,prev", required = true)
    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    @ApiModelProperty()
    public boolean isWrap() {
        return wrap;
    }

    public void setWrap(boolean wrap) {
        this.wrap = wrap;
    }
}

