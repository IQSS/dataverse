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
    private String filterQuery;
    private List<String> filterQueries = new ArrayList<>();
    /**
     * @todo: support at least 10 facet clicks
     */
    private String fq0;
    private String fq1;
    private String fq2;
    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private List<FacetCategory> facetCategoryList = new ArrayList<FacetCategory>();
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

        query = query == null ? "*" : query;
        /**
         * @todo: Will JSF allow us to put more than one filter query (fq) in
         * the URL?
         *
         * The answer (sadly) is "no" according to this:
         *
         * the <f:viewParam> does not support nor handle a single parameter with
         * multiple values" --
         * http://stackoverflow.com/questions/17275130/is-there-a-way-to-make-fviewparam-handle-lists-of-values/17276832#17276832
         */
        List<String> tmpFilterQueries = new ArrayList<>();
        tmpFilterQueries.add(filterQuery); // one-click only version
        tmpFilterQueries.add(fq0);
        tmpFilterQueries.add(fq1);
        tmpFilterQueries.add(fq2);
        SolrQueryResponse solrQueryResponse = searchService.search(query, tmpFilterQueries);
        searchResultsList = solrQueryResponse.getSolrSearchResults();
        List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();
        for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
            spelling_alternatives.add(entry.getValue().toString());
        }
        facetCategoryList = solrQueryResponse.getFacetCategoryList();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public List<String> getFilterQueries() {
        return filterQueries;
    }

    public void setFilterQueries(List<String> filterQueries) {
        this.filterQueries = filterQueries;
    }

    public String getFq0() {
        return fq0;
    }

    public void setFq0(String fq0) {
        this.fq0 = fq0;
    }

    public String getFq1() {
        return fq1;
    }

    public void setFq1(String fq1) {
        this.fq1 = fq1;
    }

    public String getFq2() {
        return fq2;
    }

    public void setFq2(String fq2) {
        this.fq2 = fq2;
    }

    public List<SolrSearchResult> getSearchResultsList() {
        return searchResultsList;
    }

    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public void setFacetCategoryList(List<FacetCategory> facetCategoryList) {
        this.facetCategoryList = facetCategoryList;
    }

    public List<String> getSpelling_alternatives() {
        return spelling_alternatives;
    }

    public void setSpelling_alternatives(List<String> spelling_alternatives) {
        this.spelling_alternatives = spelling_alternatives;
    }

}
