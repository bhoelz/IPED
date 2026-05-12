package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerOpenContextJSON {
    private String queryId;
    private Integer selectedRow;

    @ApiModelProperty()
    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    @ApiModelProperty()
    public Integer getSelectedRow() {
        return selectedRow;
    }

    public void setSelectedRow(Integer selectedRow) {
        this.selectedRow = selectedRow;
    }
}

