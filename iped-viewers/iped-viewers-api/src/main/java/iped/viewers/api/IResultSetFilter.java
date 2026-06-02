package iped.viewers.api;

import iped.exception.ParseException;
import iped.exception.QueryNodeException;
import iped.search.IMultiSearchResult;

import java.io.IOException;

public interface IResultSetFilter extends IFilter {
    IMultiSearchResult filterResult(IMultiSearchResult src) throws ParseException, QueryNodeException, IOException;
}
