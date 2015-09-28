package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataTable;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;

@ViewScoped
@Named("SearchIncludeFragment")
public class SearchIncludeFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SearchIncludeFragment.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    SystemConfig systemConfig;
    @Inject
    DataverseSession session;

    private String browseModeString = "browse";
    private String searchModeString = "search";
    private String mode;
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
    private String dataverseAlias;
    private Dataverse dataverse;
    // commenting out dataverseSubtreeContext. it was not well-loved in the GUI
//    private String dataverseSubtreeContext;
    private String selectedTypesString;
    private List<String> selectedTypesList = new ArrayList<String>();
    private String selectedTypesHumanReadable;
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
//    private String searchFieldHostDataverse = SearchFields.HOST_DATAVERSE;
    private String searchFieldNameSort = SearchFields.NAME_SORT;
    private String searchFieldRelevance = SearchFields.RELEVANCE;
//    private String searchFieldReleaseDate = SearchFields.RELEASE_DATE_YYYY;
    private String searchFieldReleaseOrCreateDate = SearchFields.RELEASE_OR_CREATE_DATE;
    final private String ASCENDING = "asc";
    final private String DESCENDING = "desc";
    private String typeFilterQuery;
    private Long facetCountDataverses = 0L;
    private Long facetCountDatasets = 0L;
    private Long facetCountFiles = 0L;
    Map<String, Long> previewCountbyType = new HashMap<>();
    private SolrQueryResponse solrQueryResponseAllTypes;
    private String sortField;
    private String sortOrder;
    private String currentSort;
    private String currentSortFriendly;
    private int page = 1;
    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private int paginationGuiRows = 10;
    Map<String, String> datasetfieldFriendlyNamesBySolrField = new HashMap<>();
    Map<String, String> staticSolrFieldFriendlyNamesBySolrField = new HashMap<>();
    private boolean solrIsDown = false;
    private Map<String, Integer> numberOfFacets = new HashMap<>();
    private boolean debug = false;
//    private boolean showUnpublished;
    List<String> filterQueriesDebug = new ArrayList<>();
//    private Map<String, String> friendlyName = new HashMap<>();
    private String errorFromSolr;
    private SearchException searchException;

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
    public String searchRedirect(String dataverseRedirectPage) {
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
         * search results. Search terms should be preserved. Sorting should be
         * preserved.
         *
         * 4. After search terms have been entered and facets have been
         * selected, we expect users to (optionally) page through search results
         * and as they do so we will preserve the state of their search terms,
         * their facet selections, and their sorting.
         *
         * 5. Someday the default sort order for browse mode will be by "release
         * date" (newest first) but that functionality is not yet available in
         * the system ( see https://redmine.hmdc.harvard.edu/issues/3628 and
         * https://redmine.hmdc.harvard.edu/issues/3629 ) so for now the default
         * sort order for browse mode will by alphabetical (sort by name,
         * ascending). The default sort order for search mode will be by
         * relevance. (We only offer ascending ordering for relevance since
         * descending order is unlikely to be useful.) When you sort, facet
         * selections and what page you are on should be preserved.
         *
         */

        dataverseRedirectPage = StringUtils.isBlank(dataverseRedirectPage) ? "dataverse.xhtml" : dataverseRedirectPage;
        String optionalDataverseScope = "&alias=" + dataverse.getAlias();

        return dataverseRedirectPage + "?faces-redirect=true&q=" + query + optionalDataverseScope;

    }

    public void search() {
        search(false);
    }

    public void search(boolean onlyDataRelatedToMe) {
        logger.fine("search called");

        // wildcard/browse (*) unless user supplies a query
        String queryToPassToSolr = "*";
        if (this.query == null) {
            mode = browseModeString;
        } else if (this.query.isEmpty()) {
            mode = browseModeString;
        } else {
            mode = searchModeString;
        }

        if (mode.equals(browseModeString)) {
            queryToPassToSolr = "*";
            if (sortField == null) {
                sortField = searchFieldReleaseOrCreateDate;
            }
            if (sortOrder == null) {
                sortOrder = DESCENDING;
            }
            if (selectedTypesString == null || selectedTypesString.isEmpty()) {
                selectedTypesString = "dataverses:datasets";
            }
        } else if (mode.equals(searchModeString)) {
            queryToPassToSolr = query;
            if (sortField == null) {
                sortField = searchFieldRelevance;
            }
            if (sortOrder == null) {
                sortOrder = DESCENDING;
            }
            if (selectedTypesString == null || selectedTypesString.isEmpty()) {
                selectedTypesString = "dataverses:datasets:files";
            }
        }

        filterQueries = new ArrayList<>();
        for (String fq : Arrays.asList(fq0, fq1, fq2, fq3, fq4, fq5, fq6, fq7, fq8, fq9)) {
            if (fq != null) {
                filterQueries.add(fq);
            }
        }

        SolrQueryResponse solrQueryResponse = null;

        List<String> filterQueriesFinal = new ArrayList<>();
        if (dataverseAlias != null) {
            this.dataverse = dataverseService.findByAlias(dataverseAlias);
        }
        if (this.dataverse != null) {
            String dataversePath = dataverseService.determineDataversePath(this.dataverse);
            String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
            if (!this.dataverse.equals(dataverseService.findRootDataverse())) {
                /**
                 * @todo centralize this into SearchServiceBean
                 */
                filterQueriesFinal.add(filterDownToSubtree);
//                this.dataverseSubtreeContext = dataversePath;
            } else {
//                this.dataverseSubtreeContext = "all";
            }
        } else {
            this.dataverse = dataverseService.findRootDataverse();
//            this.dataverseSubtreeContext = "all";
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
         * design/make room for sort widget drop down:
         * https://redmine.hmdc.harvard.edu/issues/3482
         *
         */

        try {
            logger.fine("query from user:   " + query);
            logger.fine("queryToPassToSolr: " + queryToPassToSolr);
            logger.fine("sort by: " + sortField);

            /**
             * @todo Number of search results per page should be configurable -
             * https://github.com/IQSS/dataverse/issues/84
             */
            int numRows = 10;
            solrQueryResponse = searchService.search(session.getUser(), dataverse, queryToPassToSolr, filterQueriesFinal, sortField, sortOrder, paginationStart, onlyDataRelatedToMe, numRows);
            solrQueryResponseAllTypes = searchService.search(session.getUser(), dataverse, queryToPassToSolr, filterQueriesFinalAllTypes, sortField, sortOrder, paginationStart, onlyDataRelatedToMe, numRows);
        } catch (SearchException ex) {
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
            this.searchException = ex;
        }
        if (!solrIsDown) {
            this.facetCategoryList = solrQueryResponse.getFacetCategoryList();
            this.searchResultsList = solrQueryResponse.getSolrSearchResults();
            this.searchResultsCount = solrQueryResponse.getNumResultsFound().intValue();
            this.datasetfieldFriendlyNamesBySolrField = solrQueryResponse.getDatasetfieldFriendlyNamesBySolrField();
            this.staticSolrFieldFriendlyNamesBySolrField = solrQueryResponse.getStaticSolrFieldFriendlyNamesBySolrField();
            this.filterQueriesDebug = solrQueryResponse.getFilterQueriesActual();
            this.errorFromSolr = solrQueryResponse.getError();
            paginationGuiStart = paginationStart + 1;
            paginationGuiEnd = Math.min(page * paginationGuiRows, searchResultsCount);
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
                if (solrSearchResult.getEntityId() == null) {
                    // avoiding EJBException a la https://redmine.hmdc.harvard.edu/issues/3809
                    logger.warning(SearchFields.ENTITY_ID + " was null for Solr document id:" + solrSearchResult.getId() + ", skipping. Bad Solr data?");
                    break;
                }
                if (solrSearchResult.getType().equals("dataverses")) {
                    Dataverse dataverseInCard = dataverseService.find(solrSearchResult.getEntityId());
                    String parentId = solrSearchResult.getParent().get("id");
                    solrSearchResult.setIsInTree(false);
                    if (parentId != null) {
                        Dataverse parentDataverseInCard = dataverseService.find(Long.parseLong(parentId));
                        solrSearchResult.setDataverseParentAlias(parentDataverseInCard.getAlias());
                        List<Dataverse> dvTree = new ArrayList();
                        Dataverse testDV = parentDataverseInCard;
                        dvTree.add(testDV);
                        while (testDV.getOwner() != null) {
                            dvTree.add(testDV.getOwner());
                            testDV = testDV.getOwner();
                        }
                        if (dvTree.contains(dataverse)) {
                            solrSearchResult.setIsInTree(true);
                        }
                    }

                    if (dataverseInCard != null) {
                        solrSearchResult.setDataverseAffiliation(dataverseInCard.getAffiliation());
                        solrSearchResult.setStatus(getCreatedOrReleasedDate(dataverseInCard, solrSearchResult.getReleaseOrCreateDate()));
                        solrSearchResult.setDataverseAlias(dataverseInCard.getAlias());
                    }
                } else if (solrSearchResult.getType().equals("datasets")) {
                    Long dataverseId = Long.parseLong(solrSearchResult.getParent().get("id"));
                    Dataverse parentDataverse = dataverseService.find(dataverseId);
                    solrSearchResult.setIsInTree(false);
                    List<Dataverse> dvTree = new ArrayList();
                    Dataverse testDV = parentDataverse;
                    dvTree.add(testDV);
                    /**
                     * @todo Why is a NPE being thrown at this `while
                     * (testDV.getOwner() != null){` line? An NPE was being
                     * thrown while browsing the site *after* issuing the
                     * DestroyDatasetCommand but before a fix was put in to
                     * remove the dataset "card" from Solr. The ticket tracking
                     * this that fix is
                     * https://github.com/IQSS/dataverse/issues/1316 but it's
                     * unclear why the NPE was thrown. The users see a nasty
                     * "Internal Server Error - An unexpected error was
                     * encountered, no more information is available." and
                     * server.log has a stacktrace with the NPE.
                     */
                    while (testDV.getOwner() != null) {
                        dvTree.add(testDV.getOwner());
                        testDV = testDV.getOwner();
                    }
                    if (dvTree.contains(dataverse)) {
                        solrSearchResult.setIsInTree(true);
                    }
                    /**
                     * @todo can a dataverse alias ever be null?
                     */
                    solrSearchResult.setDataverseAlias(parentDataverse.getAlias());
                    Long datasetVersionId = solrSearchResult.getDatasetVersionId();
                    if (datasetVersionId != null) {
                        DatasetVersion datasetVersion = datasetVersionService.find(datasetVersionId);
                        if (datasetVersion != null) {
                            if (datasetVersion.isDeaccessioned()) {
                                solrSearchResult.setDeaccessionedState(true);
                            }

                        }
                    }
                    String deaccesssionReason = solrSearchResult.getDeaccessionReason();
                    if (deaccesssionReason != null) {
                        solrSearchResult.setDescriptionNoSnippet(deaccesssionReason);
                    }
                } else if (solrSearchResult.getType().equals("files")) {
                    DataFile dataFile = dataFileService.find(solrSearchResult.getEntityId());
                    if (dataFile != null) {
                        solrSearchResult.setStatus(getCreatedOrReleasedDate(dataFile, solrSearchResult.getReleaseOrCreateDate()));
                    }
                    Long datasetId = Long.parseLong(solrSearchResult.getParent().get("id"));
                    Dataset parentDS = datasetService.find(datasetId);
                    /**
                     * I didn't write this code below about setIsInTree
                     * (whatever that is) but I did just add a null check.
                     * --pdurbin
                     */
                    if (parentDS != null) {
                        Dataverse parentDataverse = parentDS.getOwner();
                        solrSearchResult.setIsInTree(false);
                        List<Dataverse> dvTree = new ArrayList();
                        Dataverse testDV = parentDataverse;
                        dvTree.add(testDV);
                        /**
                         * @todo Why is a NPE being thrown at this `while
                         * (testDV.getOwner() != null){` line? An NPE was being
                         * thrown while browsing the site *after* issuing the
                         * DestroyDatasetCommand but before a fix was put in to
                         * remove the dataset "card" from Solr. The ticket
                         * tracking this that fix is
                         * https://github.com/IQSS/dataverse/issues/1316 but
                         * it's unclear why the NPE was thrown. The users see a
                         * nasty "Internal Server Error - An unexpected error
                         * was encountered, no more information is available."
                         * and server.log has a stacktrace with the NPE.
                         */
                        if (testDV != null) {
                            while (testDV.getOwner() != null) {
                                dvTree.add(testDV.getOwner());
                                testDV = testDV.getOwner();
                            }
                        }

                        if (dvTree.contains(dataverse)) {
                            solrSearchResult.setIsInTree(true);
                        }
                    }
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
                for (FacetCategory facetCategory : solrQueryResponseAllTypes.getTypeFacetCategories()) {
                    for (FacetLabel facetLabel : facetCategory.getFacetLabel()) {
                        previewCountbyType.put(facetLabel.getName(), facetLabel.getCount());
                    }
                }
            }

        } else {
            List contentsList = dataverseService.findByOwnerId(dataverse.getId());
            contentsList.addAll(datasetService.findByOwnerId(dataverse.getId()));
//            directChildDvObjectContainerList.addAll(contentsList);
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

//    public boolean isShowUnpublished() {
//        return showUnpublished;
//    }
//
//    public void setShowUnpublished(boolean showUnpublished) {
//        this.showUnpublished = showUnpublished;
//    }
    public String getBrowseModeString() {
        return browseModeString;
    }

    public String getSearchModeString() {
        return searchModeString;
    }

    public String getMode() {
        // enum would be prefered but we can't reference enums from JSF:
        // http://stackoverflow.com/questions/2524420/how-to-testing-for-enum-equality-in-jsf/2524901#2524901
        return mode;
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

        // TODO: decide on rules for this button and check actual permissions
        return session.getUser() != null && session.getUser().isAuthenticated();
        //return permissionService.userOn(session.getUser(), dataverse).has(Permission.UndoableEdit);
    }

    private String getCreatedOrReleasedDate(DvObject dvObject, Date date) {
        // the hedge is for https://redmine.hmdc.harvard.edu/issues/3806
        String hedge = "";
        if (dvObject instanceof Dataverse) {
            hedge = "";
        } else if (dvObject instanceof Dataset) {
            hedge = " maybe";
        } else if (dvObject instanceof DataFile) {
            hedge = " maybe";
        } else {
            hedge = " what object is this?";
        }
        if (dvObject.isReleased()) {
            return date + " released" + hedge;
        } else {
            return date + " created" + hedge;
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

//    public String getSearchFieldHostDataverse() {
//        return searchFieldHostDataverse;
//    }
//
//    public void setSearchFieldHostDataverse(String searchFieldHostDataverse) {
//        this.searchFieldHostDataverse = searchFieldHostDataverse;
//    }
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

    public String getSearchFieldRelevance() {
        return searchFieldRelevance;
    }

    public void setSearchFieldRelevance(String searchFieldRelevance) {
        this.searchFieldRelevance = searchFieldRelevance;
    }

    public String getSearchFieldNameSort() {
        return searchFieldNameSort;
    }

    public void setSearchFieldNameSort(String searchFieldNameSort) {
        this.searchFieldNameSort = searchFieldNameSort;
    }

    public String getSearchFieldReleaseOrCreateDate() {
        return searchFieldReleaseOrCreateDate;
    }

    public String getASCENDING() {
        return ASCENDING;
    }

    public String getDESCENDING() {
        return DESCENDING;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCurrentSortFriendly() {
        String friendlySortField = sortField;
        String friendlySortOrder = sortOrder;
        if (sortField.equals(SearchFields.NAME_SORT)) {
            friendlySortField = "Name";
            if (sortOrder.equals(ASCENDING)) {
                friendlySortOrder = " (A-Z)";
            } else if (sortOrder.equals(DESCENDING)) {
                friendlySortOrder = " (Z-A)";
            }
        } else if (sortField.equals(SearchFields.RELEVANCE)) {
            friendlySortField = "Relevance";
            friendlySortOrder = "";
        }
        return friendlySortField + friendlySortOrder;
    }

    public String getCurrentSort() {
        return sortField + ":" + sortOrder;
    }

    public boolean isSortedByNameAsc() {
        return getCurrentSort().equals(searchFieldNameSort + ":" + ASCENDING) ? true : false;
    }

    public boolean isSortedByNameDesc() {
        return getCurrentSort().equals(searchFieldNameSort + ":" + DESCENDING) ? true : false;
    }

    public boolean isSortedByReleaseDateAsc() {
        return getCurrentSort().equals(searchFieldReleaseOrCreateDate + ":" + ASCENDING) ? true : false;
    }

    public boolean isSortedByReleaseDateDesc() {
        return getCurrentSort().equals(searchFieldReleaseOrCreateDate + ":" + DESCENDING) ? true : false;
    }

    public boolean isSortedByRelevance() {
        return getCurrentSort().equals(searchFieldRelevance + ":" + DESCENDING) ? true : false;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    // helper method
    public int getTotalPages() {
        return ((searchResultsCount - 1) / paginationGuiRows) + 1;
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

    public boolean isDebug() {
        return (debug && session.getUser().isSuperuser())
                || systemConfig.isDebugEnabled();
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<String> getFilterQueriesDebug() {
        return filterQueriesDebug;
    }

    public boolean userLoggedIn() {
        return session.getUser().isAuthenticated();
    }

    public boolean publishedSelected() {
        String expected = SearchFields.PUBLICATION_STATUS + ":\"" + getPUBLISHED() + "\"";
        logger.info("published expected: " + expected + " actual: " + selectedTypesList);
        return filterQueries.contains(SearchFields.PUBLICATION_STATUS + ":\"" + getPUBLISHED() + "\"");
    }

    public boolean unpublishedSelected() {
        String expected = SearchFields.PUBLICATION_STATUS + ":\"" + getUNPUBLISHED() + "\"";
        logger.info("unpublished expected: " + expected + " actual: " + selectedTypesList);
        return filterQueries.contains(SearchFields.PUBLICATION_STATUS + ":\"" + getUNPUBLISHED() + "\"");
    }

    public String getPUBLISHED() {
        return IndexServiceBean.getPUBLISHED_STRING();
    }

    public String getUNPUBLISHED() {
        return IndexServiceBean.getUNPUBLISHED_STRING();
    }

    public String getDRAFT() {
        return IndexServiceBean.getDRAFT_STRING();
    }

    public String getIN_REVIEW() {
        return IndexServiceBean.getIN_REVIEW_STRING();
    }

    public String getDEACCESSIONED() {
        return IndexServiceBean.getDEACCESSIONED_STRING();
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
            } else if (key.equals(SearchFields.PUBLICATION_STATUS)) {
                /**
                 * @todo Refactor this quick fix for
                 * https://github.com/IQSS/dataverse/issues/618 . We really need
                 * to get rid of all the reflection that's happening with
                 * solrQueryResponse.getStaticSolrFieldFriendlyNamesBySolrField()
                 * and
                 */
                friendlyNames.add("Publication Status");
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

    public String getErrorFromSolr() {
        return errorFromSolr;
    }

    /**
     * @return the dataverseAlias
     */
    public String getDataverseAlias() {
        return dataverseAlias;
    }

    /**
     * @param dataverseAlias the dataverseAlias to set
     */
    public void setDataverseAlias(String dataverseAlias) {
        this.dataverseAlias = dataverseAlias;
    }

    public boolean isTabular(Long fileId) {
        if (fileId == null) {
            return false;
        }

        DataFile datafile = dataFileService.find(fileId);

        if (datafile == null) {
            logger.warning("isTabular: datafile service could not locate a DataFile object for id " + fileId + "!");
            return false;
        }

        return datafile.isTabularData();
    }

    public SearchException getSearchException() {
        return searchException;
    }

    public String tabularDataDisplayInfo(Long fileId) {
        String ret = "";

        if (fileId == null) {
            return "";
        }

        DataFile datafile = dataFileService.find(fileId);

        if (datafile == null) {
            logger.warning("isTabular: datafile service could not locate a DataFile object for id " + fileId + "!");
            return "";
        }

        if (datafile.isTabularData() && datafile.getDataTable() != null) {
            DataTable datatable = datafile.getDataTable();
            String unf = datatable.getUnf();
            Long varNumber = datatable.getVarQuantity();
            Long obsNumber = datatable.getCaseQuantity();
            if (varNumber != null && varNumber.intValue() != 0) {
                ret = ret.concat(varNumber + " Variables");
                if (obsNumber != null && obsNumber.intValue() != 0) {
                    ret = ret.concat(", " + obsNumber + " Observations");
                }
                ret = ret.concat(" - ");
            }
            if (unf != null && !unf.equals("")) {
                ret = ret.concat("UNF: " + unf);
            }
        }

        return ret;
    }

    public String dataFileSizeDisplay(Long fileId) {
        DataFile datafile = dataFileService.find(fileId);
        if (datafile == null) {
            logger.warning("isTabular: datafile service could not locate a DataFile object for id " + fileId + "!");
            return "";
        }

        return datafile.getFriendlySize();

    }

    public String dataFileMD5Display(Long fileId) {
        DataFile datafile = dataFileService.find(fileId);
        if (datafile == null) {
            logger.warning("isTabular: datafile service could not locate a DataFile object for id " + fileId + "!");
            return "";
        }

        if (datafile.getmd5() != null && datafile.getmd5() != "") {
            return " MD5: " + datafile.getmd5() + " ";
        }

        return "";
    }

    public void setDisplayCardValues() {
        for (SolrSearchResult result : searchResultsList) {
            boolean valueSet = false;
            if (result.getType().equals("dataverses") && result.getEntity() instanceof Dataverse) {
                result.setDisplayImage(dataverseService.isDataverseCardImageAvailable((Dataverse) result.getEntity(), session.getUser()));
                valueSet = true;
            } else if (result.getType().equals("datasets") && result.getEntity() instanceof Dataset) {
                result.setDisplayImage(datasetService.isDatasetCardImageAvailable(datasetVersionService.find(result.getDatasetVersionId()), session.getUser()));
                valueSet = true;
            } else if (result.getType().equals("files") && result.getEntity() instanceof DataFile) {
                result.setDisplayImage(dataFileService.isThumbnailAvailable((DataFile) result.getEntity(), session.getUser()));
                valueSet = true;
            }

            if (!valueSet) {
                logger.warning("Index result / entity mismatch (id:resultType) - " + result.getId() + ":" + result.getType());
            }
        }
    }
}
