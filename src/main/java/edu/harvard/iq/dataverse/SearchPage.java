package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

@ViewScoped
@Named("SearchPage")
public class SearchPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SearchPage.class.getCanonicalName());

    private String query;
    private List<String> filterQueries = new ArrayList<>();
    private String fq0;
    private String fq1;
    private String fq2;
    private String fq3;
    private String fq4;
    private String fq5;
    private String fq6;
    private String fq7;
    private String fq8;
    private String fq9;
    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private Long searchResultsCount;
    private int paginationStart;
    private List<FacetCategory> facetCategoryList = new ArrayList<FacetCategory>();
    private List<String> spelling_alternatives = new ArrayList<>();
    private Map<String, String> friendlyName = new HashMap<>();
    private Map<String,Integer> numberOfFacets = new HashMap<>();
    private Map<String,Integer> numberOfChildDatasets = new HashMap<>();
    private Long fromDataverseId;
    private Dataverse fromDataverse;

    

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

//        query = query.trim(); // buggy, threw an NPE
//        query = query.isEmpty() ? "*" : query; // also buggy
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
        filterQueries = new ArrayList<>();
        for (String fq : Arrays.asList(fq0, fq1, fq2, fq3, fq4, fq5, fq6, fq7, fq8, fq9)) {
            if (fq != null) {
                filterQueries.add(fq);
            }
        }
        SolrQueryResponse solrQueryResponse = null;
        try {
            solrQueryResponse = searchService.search(query, filterQueries, paginationStart);
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(cause + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause + " ");
            }
            String message = "Exception running search for [" + query + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]: " + sb.toString();
            logger.info(message);
            return;
        }
        searchResultsList = solrQueryResponse.getSolrSearchResults();
        List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();
        for (SolrSearchResult solrSearchResult : searchResults) {
            if (solrSearchResult.getType().equals("dataverses")) {
                List<Dataset> datasets = datasetService.findByOwnerId(solrSearchResult.getEntityId());
                solrSearchResult.setDatasets(datasets);
            } else if (solrSearchResult.getType().equals("datasets")) {
                Dataset dataset = datasetService.find(solrSearchResult.getEntityId());
                try {
                    if (dataset.getLatestVersion().getCitation() != null) {
                        solrSearchResult.setCitation(dataset.getLatestVersion().getCitation());
                    }
                } catch (NullPointerException npe) {
                    logger.info("caught NullPointerException trying to get citation for " + dataset.getId());
                }
            } else if (solrSearchResult.getType().equals("files")) {
                /**
                 * @todo: show DataTable variables
                 */
            }
        }
        searchResultsCount = solrQueryResponse.getNumResultsFound();
        for (Map.Entry<String, List<String>> entry : solrQueryResponse.getSpellingSuggestionsByToken().entrySet()) {
            spelling_alternatives.add(entry.getValue().toString());
        }
        facetCategoryList = solrQueryResponse.getFacetCategoryList();
        friendlyName.put(SearchFields.SUBTREE, "Dataverse Subtree");
        friendlyName.put(SearchFields.ORIGINAL_DATAVERSE, "Original Dataverse");
//        friendlyName.put(SearchFields.CATEGORY, "Category");
        friendlyName.put(SearchFields.AUTHOR_STRING, "Author");
        friendlyName.put(SearchFields.AFFILIATION, "Affiliation");
        friendlyName.put(SearchFields.CITATION_YEAR, "Citation Year");
//        friendlyName.put(SearchFields.FILE_TYPE, "File Type");
        friendlyName.put(SearchFields.FILE_TYPE_GROUP, "File Type");
        if (fromDataverseId != null) {
            fromDataverse = dataverseService.find(fromDataverseId);
        }
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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

    public String getFq3() {
        return fq3;
    }

    public void setFq3(String fq3) {
        this.fq3 = fq3;
    }

    public String getFq4() {
        return fq4;
    }

    public void setFq4(String fq4) {
        this.fq4 = fq4;
    }

    public String getFq5() {
        return fq5;
    }

    public void setFq5(String fq5) {
        this.fq5 = fq5;
    }

    public String getFq6() {
        return fq6;
    }

    public void setFq6(String fq6) {
        this.fq6 = fq6;
    }

    public String getFq7() {
        return fq7;
    }

    public void setFq7(String fq7) {
        this.fq7 = fq7;
    }

    public String getFq8() {
        return fq8;
    }

    public void setFq8(String fq8) {
        this.fq8 = fq8;
    }

    public String getFq9() {
        return fq9;
    }

    public void setFq9(String fq9) {
        this.fq9 = fq9;
    }

    public List<SolrSearchResult> getSearchResultsList() {
        return searchResultsList;
    }

    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public Long getSearchResultsCount() {
        return searchResultsCount;
    }

    public void setSearchResultsCount(Long searchResultsCount) {
        this.searchResultsCount = searchResultsCount;
    }

    public int getPaginationStart() {
        return paginationStart;
    }

    public void setPaginationStart(int paginationStart) {
        this.paginationStart = paginationStart;
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

    public Map<String, String> getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(Map<String, String> friendlyName) {
        this.friendlyName = friendlyName;
    }

    public  int getNumberOfFacets(String name, int defaultValue) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numberOfFacets.put(name,defaultValue);
            numFacets = defaultValue;
        }
        return numFacets;
    }
    
    public void incrementFacets(String name, int incrementNum) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numFacets = incrementNum;
        }
        numberOfFacets.put(name, numFacets + incrementNum);   
    }
    
    
    public  int getNumberOfChildDatasets(String name, int defaultValue) {
        Integer numChildDatasets = numberOfChildDatasets.get(name);
        if (numChildDatasets == null) {
            numberOfChildDatasets.put(name,defaultValue);
            numChildDatasets = defaultValue;
        }
        return numChildDatasets;
    }
    
    public void incrementChildDatasets(String name, int incrementNum) {
        Integer numChildDatasets = numberOfChildDatasets.get(name);
        if (numChildDatasets == null) {
            numChildDatasets = incrementNum;
        }
        numberOfChildDatasets.put(name, numChildDatasets + incrementNum);   
    }
    

    public Long getFromDataverseId() {
        return fromDataverseId;
    }

    public void setFromDataverseId(Long fromDataverseId) {
        this.fromDataverseId = fromDataverseId;
    }

    public Dataverse getFromDataverse() {
        return fromDataverse;
    }

    public void setFromDataverse(Dataverse fromDataverse) {
        this.fromDataverse = fromDataverse;
    }

}
