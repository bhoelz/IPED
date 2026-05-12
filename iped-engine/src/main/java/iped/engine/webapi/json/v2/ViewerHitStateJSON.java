package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerHitStateJSON {
    private int totalHits;
    private int currentHit;

    @ApiModelProperty()
    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    @ApiModelProperty()
    public int getCurrentHit() {
        return currentHit;
    }

    public void setCurrentHit(int currentHit) {
        this.currentHit = currentHit;
    }
}

