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
@Named("SearchIncludeFragment")
public class SearchIncludeFragment {

    private static final Logger logger = Logger.getLogger(SearchIncludeFragment.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;

    private String query;
    private List<String> filterQueries = new ArrayList<>();
    private List<FacetCategory> facetCategoryList = new ArrayList<>();
    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private Long searchResultsCount;
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
    private Long dataverseId;
    private Dataverse dataverse;
    private boolean solrIsDown = false;
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    private List<DvObjectContainer> directChildDvObjectContainerList = new ArrayList<>();

    /**
     * @todo:
     *
     * style and icons for facets
     *
     * more/less on facets
     *
     * pagination (previous/next links)
     *
     * test dataset cards
     *
     * test files cards
     *
     * test dataset cards when Solr is down
     *
     * make results sortable: https://redmine.hmdc.harvard.edu/issues/3482
     *
     * always show all types, even if zero count:
     * https://redmine.hmdc.harvard.edu/issues/3488
     *
     * make subtree facet look like amazon widget (i.e. a tree)
     *
     * see also https://trello.com/c/jmry3BJR/28-browse-dataverses
     */
    public void search() {
        logger.info("search called");

        if (this.query == null) {
            this.query = "*";
        } else if (this.query.isEmpty()) {
            this.query = "*";
        }

        filterQueries = new ArrayList<>();
        for (String fq : Arrays.asList(fq0, fq1, fq2, fq3, fq4, fq5, fq6, fq7, fq8, fq9)) {
            if (fq != null) {
                filterQueries.add(fq);
            }
        }

        SolrQueryResponse solrQueryResponse = null;
        int paginationStart = 0;

        if (dataverseId != null) {
            this.dataverse = dataverseService.find(dataverseId);
            String dataversePath = dataverseService.determineDataversePath(this.dataverse);
            String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
            if (!this.dataverse.equals(dataverseService.findRootDataverse())) {
                filterQueries.add(filterDownToSubtree);
            }
        } else {
            this.dataverse = dataverseService.findRootDataverse();
        }

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
            this.solrIsDown = true;
        }
        if (!solrIsDown) {
            this.facetCategoryList = solrQueryResponse.getFacetCategoryList();
            this.searchResultsList = solrQueryResponse.getSolrSearchResults();
            this.searchResultsCount = solrQueryResponse.getNumResultsFound();
            List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();

            for (SolrSearchResult solrSearchResult : searchResults) {
                if (solrSearchResult.getType().equals("dataverses")) {
                    List<Dataset> datasets = datasetService.findByOwnerId(solrSearchResult.getEntityId());
                    solrSearchResult.setDatasets(datasets);
                } else if (solrSearchResult.getType().equals("datasets")) {
                    Dataset dataset = datasetService.find(solrSearchResult.getEntityId());
                    try {
                        if (dataset.getLatestVersion().getMetadata().getCitation() != null) {
                            solrSearchResult.setCitation(dataset.getLatestVersion().getMetadata().getCitation());
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
        } else {
            List contentsList = dataverseService.findByOwnerId(dataverse.getId());
            contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
            directChildDvObjectContainerList.addAll(contentsList);
        }

    }

    public int getNumberOfFacets(String name, int defaultValue) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numberOfFacets.put(name, defaultValue);
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

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public void setFacetCategoryList(List<FacetCategory> facetCategoryList) {
        this.facetCategoryList = facetCategoryList;
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

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public boolean isSolrIsDown() {
        return solrIsDown;
    }

    public void setSolrIsDown(boolean solrIsDown) {
        this.solrIsDown = solrIsDown;
    }

    public List<DvObjectContainer> getDirectChildDvObjectContainerList() {
        return directChildDvObjectContainerList;
    }

    public void setDirectChildDvObjectContainerList(List<DvObjectContainer> directChildDvObjectContainerList) {
        this.directChildDvObjectContainerList = directChildDvObjectContainerList;
    }

}
