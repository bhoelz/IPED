/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.search;

/**
 *
 * @author WERNECK
 */
public interface IIPEDSearcher {

    int MAX_SIZE_TO_SCORE = 1000000;

    void cancel();

    Object getQueryObject();

    SearchQueryDefinition getQueryDefinition();

    IMultiSearchResult multiSearch() throws Exception;

    SearchResult search() throws Exception;

    void setQueryObject(Object queryObject);

    void setQueryDefinition(SearchQueryDefinition queryDefinition);

    void setTreeQuery(boolean treeQuery);

}
