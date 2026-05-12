package iped.engine.webapi.json.v2;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

public class ViewerSearchResponseJSON {
    private int totalHits;
    private int currentHit;
    private List<HitRangeJSON> hitRanges;

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

    @ApiModelProperty()
    public List<HitRangeJSON> getHitRanges() {
        return hitRanges;
    }

    public void setHitRanges(List<HitRangeJSON> hitRanges) {
        this.hitRanges = hitRanges;
    }
}

