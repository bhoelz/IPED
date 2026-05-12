package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class HitRangeJSON {
    private int start;
    private int end;
    private Integer page;

    @ApiModelProperty()
    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    @ApiModelProperty()
    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @ApiModelProperty()
    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}

