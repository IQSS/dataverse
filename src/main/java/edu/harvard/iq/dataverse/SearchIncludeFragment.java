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
    private int searchResultsCount;
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
    private String dataverseSubtreeContext;
    private String[] selectedTypesArray = {"dataverses", "datasets", "files"};
    private String selectedTypesHumanReadable;
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
    private String searchFieldHostDataverse = SearchFields.HOST_DATAVERSE;
    private String typeFilterQuery;
    private Long facetCountDataverses = 0L;
    private Long facetCountDatasets = 0L;
    private Long facetCountFiles = 0L;
    private int page = 1;
    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private int paginationGuiRows = 10;
    private boolean solrIsDown = false;
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    private List<DvObjectContainer> directChildDvObjectContainerList = new ArrayList<>();
//    private Map<String, String> friendlyName = new HashMap<>();

    /**
     * @todo:
     *
     * facets with checkboxes: make bookmarkable? don't require select button
     * (just check box to call search method)?
     *
     * better style and icons for facets
     *
     * replace * with watermark saying "Search this Dataverse"
     *
     * get rid of "_s" et al. (human eyeball friendly)
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

        // wildcard/browse (*) unless user supplies a query
        String queryToPassToSolr = "*";
        if (this.query == null) {
            queryToPassToSolr = "*";
        } else if (this.query.isEmpty()) {
            queryToPassToSolr = "*";
        } else {
            queryToPassToSolr = query;
        }

        filterQueries = new ArrayList<>();
        for (String fq : Arrays.asList(fq0, fq1, fq2, fq3, fq4, fq5, fq6, fq7, fq8, fq9)) {
            if (fq != null) {
                filterQueries.add(fq);
            }
        }

        SolrQueryResponse solrQueryResponse = null;

        List<String> filterQueriesFinal = new ArrayList<>();
        if (dataverseId != null) {
            this.dataverse = dataverseService.find(dataverseId);
            String dataversePath = dataverseService.determineDataversePath(this.dataverse);
            String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
            if (!this.dataverse.equals(dataverseService.findRootDataverse())) {
                filterQueriesFinal.add(filterDownToSubtree);
                this.dataverseSubtreeContext = dataversePath;
            } else {
                this.dataverseSubtreeContext = "all";
            }
        } else {
            this.dataverse = dataverseService.findRootDataverse();
            this.dataverseSubtreeContext = "all";
        }

        selectedTypesHumanReadable = combine(selectedTypesArray, " OR ");
        typeFilterQuery = SearchFields.TYPE + ":(" + selectedTypesHumanReadable + ")";
        filterQueriesFinal.addAll(filterQueries);
        filterQueriesFinal.add(typeFilterQuery);

        int paginationStart = (page - 1) * paginationGuiRows;
        /**
         * @todo
         * bug: showing all pages, even if there are hundreds of pages
         * 
         * bug: previous and next buttons don't work
         * 
         * bug: first page (<<) and last page (>>) buttons don't work
         *
         * design/make room for sort widget drop down: https://redmine.hmdc.harvard.edu/issues/3482
         *
         */

        try {
            logger.info("query from user:   " + query);
            logger.info("queryToPassToSolr: " + queryToPassToSolr);
            solrQueryResponse = searchService.search(queryToPassToSolr, filterQueriesFinal, paginationStart, dataverse);
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(cause + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause + " ");
            }
            String message = "Exception running search for [" + queryToPassToSolr + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]: " + sb.toString();
            logger.info(message);
            this.solrIsDown = true;
        }
        if (!solrIsDown) {
            this.facetCategoryList = solrQueryResponse.getFacetCategoryList();
            this.searchResultsList = solrQueryResponse.getSolrSearchResults();
            this.searchResultsCount = solrQueryResponse.getNumResultsFound().intValue();
            paginationGuiStart = paginationStart + 1;
            paginationGuiEnd = Math.min(page * paginationGuiRows,searchResultsCount);            
            List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();

            for (SolrSearchResult solrSearchResult : searchResults) {
                if (solrSearchResult.getType().equals("dataverses")) {
                    List<Dataset> datasets = datasetService.findByOwnerId(solrSearchResult.getEntityId());
                    solrSearchResult.setDatasets(datasets);
                } else if (solrSearchResult.getType().equals("datasets")) {
                    Dataset dataset = datasetService.find(solrSearchResult.getEntityId());
                    if (dataset != null) {
                        try {
                            if (dataset.getLatestVersion().getCitation() != null) {
                                solrSearchResult.setCitation(dataset.getLatestVersion().getCitation());
                            }
                        } catch (NullPointerException npe) {
                            logger.info("caught NullPointerException trying to get citation for dataset " + dataset);
                        }
                    } else {
                        logger.info("couldn't find dataset id " + solrSearchResult.getEntityId() + ". Stale Solr data? Time to re-index?");
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
        /**
         * @todo: pull values from datasetField.getTitle() rather than hard
         * coding them here
         */
//        friendlyName.put(SearchFields.SUBTREE, "Dataverse Subtree");
//        friendlyName.put(SearchFields.HOST_DATAVERSE, "Original Dataverse");
//        friendlyName.put(SearchFields.AUTHOR_STRING, "Author");
//        friendlyName.put(SearchFields.AFFILIATION, "Affiliation");
//        friendlyName.put(SearchFields.KEYWORD, "Keyword");
//        friendlyName.put(SearchFields.DISTRIBUTOR, "Distributor");
//        friendlyName.put(SearchFields.FILE_TYPE_GROUP, "File Type");
//        friendlyName.put(SearchFields.PRODUCTION_DATE_YEAR_ONLY, "Production Date");
//        friendlyName.put(SearchFields.DISTRIBUTION_DATE_YEAR_ONLY, "Distribution Date");
    }

    public int getNumberOfFacets(String name, int defaultValue) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numberOfFacets.put(name, defaultValue);
            numFacets = defaultValue;
        }
        return numFacets;
    }

    /**
     * @todo why do we have to click "More" twice?
     */
    public void incrementFacets(String name, int incrementNum) {
        Integer numFacets = numberOfFacets.get(name);
        if (numFacets == null) {
            numFacets = incrementNum;
        }
        numberOfFacets.put(name, numFacets + incrementNum);
    }

    // http://stackoverflow.com/questions/1515437/java-function-for-arrays-like-phps-join/1515548#1515548
    String combine(String[] s, String glue) {
        int k = s.length;
        if (k == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        out.append(s[0]);
        for (int x = 1; x < k; ++x) {
            out.append(glue).append(s[x]);
        }
        return out.toString();
    }

    private Long findFacetCountByType(String type) {
        for (FacetCategory facetCategory : facetCategoryList) {
            if (facetCategory.getName().equals(SearchFields.TYPE)) {
                for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                    String facetLabelName = facetLabel.getName();
                    if (facetLabelName.equals(type)) {
                        return facetLabel.getCount();
                    }
                }
            }
        }
        return 0L;
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

    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public void setSearchResultsCount(int searchResultsCount) {
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

    public String getDataverseSubtreeContext() {
        return dataverseSubtreeContext;
    }

    public void setDataverseSubtreeContext(String dataverseSubtreeContext) {
        this.dataverseSubtreeContext = dataverseSubtreeContext;
    }

    public String[] getSelectedTypes() {
        return selectedTypesArray;
    }

    public void setSelectedTypes(String[] selectedTypesArray) {
        this.selectedTypesArray = selectedTypesArray;
    }

    public String getSelectedTypesHumanReadable() {
        return selectedTypesHumanReadable;
    }

    public void setSelectedTypesHumanReadable(String selectedTypesHumanReadable) {
        this.selectedTypesHumanReadable = selectedTypesHumanReadable;
    }

    public String getSearchFieldType() {
        return searchFieldType;
    }

    public void setSearchFieldType(String searchFieldType) {
        this.searchFieldType = searchFieldType;
    }

    public String getSearchFieldSubtree() {
        return searchFieldSubtree;
    }

    public void setSearchFieldSubtree(String searchFieldSubtree) {
        this.searchFieldSubtree = searchFieldSubtree;
    }

    public String getSearchFieldHostDataverse() {
        return searchFieldHostDataverse;
    }

    public void setSearchFieldHostDataverse(String searchFieldHostDataverse) {
        this.searchFieldHostDataverse = searchFieldHostDataverse;
    }

    public String getTypeFilterQuery() {
        return typeFilterQuery;
    }

    public void setTypeFilterQuery(String typeFilterQuery) {
        this.typeFilterQuery = typeFilterQuery;
    }

    public Long getFacetCountDatasets() {
        return findFacetCountByType("datasets");
    }

    public Long getFacetCountDataverses() {
        return findFacetCountByType("dataverses");
    }

    public Long getFacetCountFiles() {
        return findFacetCountByType("files");
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    // helper method
    public int getTotalPages() {
        return ( (searchResultsCount - 1) / paginationGuiRows) + 1;
    } 


    public int getPaginationGuiStart() {
        return paginationGuiStart;
    }

    public void setPaginationGuiStart(int paginationGuiStart) {
        this.paginationGuiStart = paginationGuiStart;
    }

    public int getPaginationGuiEnd() {
        return paginationGuiEnd;
    }

    public void setPaginationGuiEnd(int paginationGuiEnd) {
        this.paginationGuiEnd = paginationGuiEnd;
    }

    public int getPaginationGuiRows() {
        return paginationGuiRows;
    }

    public void setPaginationGuiRows(int paginationGuiRows) {
        this.paginationGuiRows = paginationGuiRows;
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

//    public Map<String, String> getFriendlyName() {
//        return friendlyName;
//        return null;
//    }

//    public void setFriendlyName(Map<String, String> friendlyName) {
//        this.friendlyName = friendlyName;
//    }

}
