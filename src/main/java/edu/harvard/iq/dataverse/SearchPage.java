package edu.harvard.iq.dataverse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named("SearchPage")
public class SearchPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SearchPage.class.getCanonicalName());

    private String query;
    private String facetQuery;
    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private List<String> facets = new ArrayList<>();
    private List<String> spelling_alternatives = new ArrayList<>();

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

    public SearchPage() {
        logger.info("SearchPage initialized. Query: " + query);
    }

    public void search() {
        logger.info("Search button clicked. Query: " + query);
        /**
         * @todo remove this? What about pagination for many, many results?
         */
        facets = new ArrayList<>();

        query = query == null ? "*" : query;
        SolrQueryResponse solrQueryResponse = searchService.search(query, facetQuery);
        searchResultsList = solrQueryResponse.getSolrSearchResults();
        List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();
        for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
            spelling_alternatives.add(entry.getValue().toString());
        }
        for (String facet : solrQueryResponse.getFacets()) {
            facets.add(facet);
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFacetQuery() {
        return facetQuery;
    }

    public void setFacetQuery(String facetQuery) {
        this.facetQuery = facetQuery;
    }

    public List<SolrSearchResult> getSearchResultsList() {
        return searchResultsList;
    }

    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public List<String> getFacets() {
        return facets;
    }

    public void setFacets(List<String> facets) {
        this.facets = facets;
    }

    public List<String> getSpelling_alternatives() {
        return spelling_alternatives;
    }

    public void setSpelling_alternatives(List<String> spelling_alternatives) {
        this.spelling_alternatives = spelling_alternatives;
    }

}
