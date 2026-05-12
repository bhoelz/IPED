package iped.engine.webapi.json.v2;

import io.swagger.annotations.ApiModelProperty;

public class ViewerSearchRequestJSON {
    private String term;
    private String matchMode;
    private boolean caseSensitive;

    @ApiModelProperty(required = true)
    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    @ApiModelProperty(allowableValues = "contains,exact,regex")
    public String getMatchMode() {
        return matchMode;
    }

    public void setMatchMode(String matchMode) {
        this.matchMode = matchMode;
    }

    @ApiModelProperty()
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}

