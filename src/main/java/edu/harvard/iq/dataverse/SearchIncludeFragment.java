package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.SearchFields;
import edu.harvard.iq.dataverse.engine.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
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
    @EJB
    PermissionServiceBean permissionService;
    @Inject
    DataverseSession session;

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
    // commenting out dataverseSubtreeContext. it was not well-loved in the GUI
//    private String dataverseSubtreeContext;
    private String selectedTypesString;
    private List<String> selectedTypesList = new ArrayList<String>();
    private String selectedTypesHumanReadable;
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
    private String searchFieldHostDataverse = SearchFields.HOST_DATAVERSE;
    private String typeFilterQuery;
    private Long facetCountDataverses = 0L;
    private Long facetCountDatasets = 0L;
    private Long facetCountFiles = 0L;
    Map<String,Long> previewCountbyType = new HashMap<>();
    private SolrQueryResponse solrQueryResponseAllTypes;
    private int page = 1;
    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private int paginationGuiRows = 10;
    Map<String, String> datasetfieldFriendlyNamesBySolrField = new HashMap<>();
    Map<String, String> staticSolrFieldFriendlyNamesBySolrField = new HashMap<>();
    private boolean solrIsDown = false;
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    private List<DvObjectContainer> directChildDvObjectContainerList = new ArrayList<>();
    private boolean debug = false;
    List<String> filterQueriesDebug = new ArrayList<>();
//    private Map<String, String> friendlyName = new HashMap<>();

    /**
     * @todo:
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
    public String searchRedirect(String stayOnDataversePage) {
        /**
         * These are our decided-upon search/browse rules, the way we expect
         * users to search/browse and how we want the app behave:
         *
         * 1. When a user is browsing (i.e. hasn't entered a search term) we
         * only show dataverses and datasets. Files are hidden. See
         * https://redmine.hmdc.harvard.edu/issues/3573
         *
         * 2. A search is always brand new. Don't keep around old facets that
         * were selected. Show page 1 of results. Make the results bookmarkable:
         * https://redmine.hmdc.harvard.edu/issues/3664
         *
         * 3. When you add or remove a facet, you should always go to page 1 of
         * search results. Search terms should be preserved.
         *
         * 4. After search terms have been entered and facets have been
         * selected, we expect users to (optionally) page through search results
         * and as they do so we will preserve the state of both their search
         * terms and their facet selections.
         */
        if (stayOnDataversePage.equals("true")) {
            return "dataverse.xhtml?faces-redirect=true&q=" + query + "&amp;types=dataverses:datasets:files" ;
        } else {
            return "FIXME";
        }
    }

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
//                this.dataverseSubtreeContext = dataversePath;
            } else {
//                this.dataverseSubtreeContext = "all";
            }
        } else {
            this.dataverse = dataverseService.findRootDataverse();
//            this.dataverseSubtreeContext = "all";
        }

        if (selectedTypesString == null || selectedTypesString.isEmpty()) {
            /**
             *
             * "When you browse to a dataverse, we show dataverses OR datasets.
             * The moment you type a term and search, we show all types
             * (dataverses, datasets, files)."
             *
             * -- https://redmine.hmdc.harvard.edu/issues/3573
             */
            selectedTypesString = "dataverses:datasets";
        }
        selectedTypesList = new ArrayList<>();
        String[] parts = selectedTypesString.split(":");
//        int count = 0;
        for (String string : parts) {
            selectedTypesList.add(string);
        }

        List<String> filterQueriesFinalAllTypes = new ArrayList<>();
        String[] arr = selectedTypesList.toArray(new String[selectedTypesList.size()]);
        selectedTypesHumanReadable = combine(arr, " OR ");
        if (!selectedTypesHumanReadable.isEmpty()) {
            typeFilterQuery = SearchFields.TYPE + ":(" + selectedTypesHumanReadable + ")";
        }
        filterQueriesFinal.addAll(filterQueries);
        filterQueriesFinalAllTypes.addAll(filterQueriesFinal);
        filterQueriesFinal.add(typeFilterQuery);
        String allTypesFilterQuery = SearchFields.TYPE + ":(dataverses OR datasets OR files)";
        filterQueriesFinalAllTypes.add(allTypesFilterQuery);

        int paginationStart = (page - 1) * paginationGuiRows;
        /**
         * @todo
         *
         * design/make room for sort widget drop down: https://redmine.hmdc.harvard.edu/issues/3482
         *
         */

        try {
            logger.info("query from user:   " + query);
            logger.info("queryToPassToSolr: " + queryToPassToSolr);
            filterQueriesDebug = filterQueriesFinal;
            solrQueryResponse = searchService.search(queryToPassToSolr, filterQueriesFinal, paginationStart, dataverse);
            solrQueryResponseAllTypes = searchService.search(queryToPassToSolr, filterQueriesFinalAllTypes, paginationStart, dataverse);
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
            this.datasetfieldFriendlyNamesBySolrField = solrQueryResponse.getDatasetfieldFriendlyNamesBySolrField();
            this.staticSolrFieldFriendlyNamesBySolrField = solrQueryResponse.getStaticSolrFieldFriendlyNamesBySolrField();
            paginationGuiStart = paginationStart + 1;
            paginationGuiEnd = Math.min(page * paginationGuiRows,searchResultsCount);            
            List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();

            /**
             * @todo consider creating Java objects called DatasetCard,
             * DatasetCart, and FileCard since that's what we call them in the
             * UI. These objects' fields (affiliation, citation, etc.) would be
             * populated from Solr if possible (for performance, to avoid extra
             * database calls) or by a database call (if it's tricky or doesn't
             * make sense to get the data in and out of Solr). We would continue
             * to iterate through all the SolrSearchResult objects as we build
             * up the new card objects. Think about how we have a
             * solrSearchResult.setCitation method but only the dataset card in
             * the UI (currently) shows this "citation" field.
             */
            for (SolrSearchResult solrSearchResult : searchResults) {
                if (solrSearchResult.getType().equals("dataverses")) {
                    Dataverse dataverseInCard = dataverseService.find(solrSearchResult.getEntityId());
                    if (dataverseInCard != null) {
                        List<Dataset> datasets = datasetService.findByOwnerId(dataverseInCard.getId());
                        solrSearchResult.setDatasets(datasets);
                        solrSearchResult.setDataverseAffiliation(dataverseInCard.getAffiliation());
                    }
                } else if (solrSearchResult.getType().equals("datasets")) {
                    Dataset dataset = datasetService.find(solrSearchResult.getEntityId());
                    if (dataset != null) {
                        DatasetVersion datasetVersion = dataset.getLatestVersion();
                        if (datasetVersion != null) {
                            DatasetVersionUI datasetVersionUI = null;
                            try {
                                datasetVersionUI = new DatasetVersionUI(datasetVersion);
                            } catch (NullPointerException ex) {
                                logger.info("Caught exception trying to instantiate DatasetVersionUI for dataset " + dataset.getId() + ". : " + ex);
                            }
                            if (datasetVersionUI != null) {
                                String citation = null;
                                try {
                                    citation = datasetVersionUI.getCitation();
                                } catch (NullPointerException ex) {
                                    logger.info("Caught exception trying to get citation for dataset " + dataset.getId() + ". : " + ex);
                                }
                                solrSearchResult.setCitation(citation);
                            }
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

            // populate preview counts: https://redmine.hmdc.harvard.edu/issues/3560
            previewCountbyType.put("dataverses", 0L);
            previewCountbyType.put("datasets", 0L);
            previewCountbyType.put("files", 0L);
            if (solrQueryResponseAllTypes != null) {
                for (FacetCategory facetCategory : solrQueryResponseAllTypes.getFacetCategoryList()) {
                    if (facetCategory.getName().equals(SearchFields.TYPE)) {
                        for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                            previewCountbyType.put(facetLabel.getName(), facetLabel.getCount());
                        }
                    }
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
//        friendlyName.put(SearchFields.FILE_TYPE, "File Type");
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
        return previewCountbyType.get(type);
    }

    public boolean isAllowedToClickAddData() {
        /**
         * @todo is this the right permission to check?
         */
        // being explicit about the user, could just call permissionService.on(dataverse)
        return permissionService.userOn(session.getUser(), dataverse).has(Permission.UndoableEdit);
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

//    public String getDataverseSubtreeContext() {
//        return dataverseSubtreeContext;
//    }
//
//    public void setDataverseSubtreeContext(String dataverseSubtreeContext) {
//        this.dataverseSubtreeContext = dataverseSubtreeContext;
//    }

    public String getSelectedTypesString() {
        return selectedTypesString;
    }

    public void setSelectedTypesString(String selectedTypesString) {
        this.selectedTypesString = selectedTypesString;
    }

    public List<String> getSelectedTypesList() {
        return selectedTypesList;
    }

    public void setSelectedTypesList(List<String> selectedTypesList) {
        this.selectedTypesList = selectedTypesList;
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

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<String> getFilterQueriesDebug() {
        return filterQueriesDebug;
    }

    public List<String> getFriendlyNamesFromFilterQuery(String filterQuery) {
        String[] parts = filterQuery.split(":");
        String key = parts[0];
        String value = parts[1];
        List<String> friendlyNames = new ArrayList<>();
        String datasetfieldFriendyName = datasetfieldFriendlyNamesBySolrField.get(key);
        if (datasetfieldFriendyName != null) {
            friendlyNames.add(datasetfieldFriendyName);
        } else {
            String nonDatasetSolrField = staticSolrFieldFriendlyNamesBySolrField.get(key);
            if (nonDatasetSolrField != null) {
                friendlyNames.add(nonDatasetSolrField);
            } else {
                // meh. better than nuthin'
                friendlyNames.add(key);
            }
        }
        String noLeadingQuote = value.replaceAll("^\"", "");
        String noTrailingQuote = noLeadingQuote.replaceAll("\"$", "");
        String valueWithoutQuotes = noTrailingQuote;
        friendlyNames.add(valueWithoutQuotes);
        return friendlyNames;
    }

    public String getNewSelectedTypes(String typeClicked) {
        List<String> newTypesSelected = new ArrayList<>();
        for (String selectedType : selectedTypesList) {
            if (selectedType.equals(typeClicked)) {

            } else {
                newTypesSelected.add(selectedType);
            }

        }
        if (selectedTypesList.contains(typeClicked)) {

        } else {
            newTypesSelected.add(typeClicked);
        }

        String[] arr = newTypesSelected.toArray(new String[newTypesSelected.size()]);
        return combine(arr, ":");

    }
}
