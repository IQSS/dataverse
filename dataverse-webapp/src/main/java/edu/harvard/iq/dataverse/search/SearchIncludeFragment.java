package edu.harvard.iq.dataverse.search;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.SolrSearchResultsService;
import edu.harvard.iq.dataverse.ThumbnailServiceWrapper;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FilterQuery;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.Param;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@RequestScoped
@Named("SearchIncludeFragment")
public class SearchIncludeFragment {

    private static final Logger logger = Logger.getLogger(SearchIncludeFragment.class.getCanonicalName());

    /**
     * @todo Number of search results per page should be configurable -
     * https://github.com/IQSS/dataverse/issues/84
     */
    private static final int RESULTS_PER_PAGE = 10;

    public static final String SEARCH_FIELD_TYPE = SearchFields.TYPE;
    public static final String SEARCH_FIELD_SUBTREE = SearchFields.SUBTREE;
    public static final String SEARCH_FIELD_NAME_SORT = SearchFields.NAME_SORT;
    public static final String SEARCH_FIELD_RELEVANCE = SearchFields.RELEVANCE;
    public static final String SEARCH_FIELD_RELEASE_OR_CREATE_DATE = SearchFields.RELEASE_OR_CREATE_DATE;
    public static final String ASCENDING = SortOrder.asc.toString();
    public static final String DESCENDING = SortOrder.desc.toString();

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    DatasetDao datasetDao;
    @EJB
    DvObjectServiceBean dvObjectService;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    @Inject
    WidgetWrapper widgetWrapper;
    @Inject
    private DataverseRequestServiceBean dataverseRequestService;
    @Inject
    private PermissionServiceBean permissionService;
    @Inject
    private SolrSearchResultsService solrSearchResultsService;

    @Inject @Param(name = "q")
    private String query;
    @Inject @Param
    private String fq0;
    @Inject @Param
    private String fq1;
    @Inject @Param
    private String fq2;
    @Inject @Param
    private String fq3;
    @Inject @Param
    private String fq4;
    @Inject @Param
    private String fq5;
    @Inject @Param
    private String fq6;
    @Inject @Param
    private String fq7;
    @Inject @Param
    private String fq8;
    @Inject @Param
    private String fq9;
    @Inject @Param(name = "types")
    private String selectedTypesString;
    @Inject @Param(name = "sort")
    private String sortField;
    @Inject @Param(name = "order")
    private SortOrder sortOrder;
    @Inject @Param
    private Integer page;

    private List<String> filterQueries = new ArrayList<>();
    private List<FacetCategory> facetCategoryList = new ArrayList<>();
    private List<SolrSearchResult> searchResultsList = new ArrayList<>();
    private int searchResultsCount;

    private String dataverseAlias;
    private Dataverse dataverse;


    private Map<SearchObjectType, Boolean> selectedTypesMap = new HashMap<>();
    private Map<SearchObjectType, Long> previewCountbyType = new HashMap<>();


    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private boolean solrIsDown = false;

    private List<FilterQuery> responseFilterQueries = new ArrayList<>();
    private List<String> filterQueriesDebug = new ArrayList<>();
    private String errorFromSolr;
    private SearchException searchException;
    private boolean solrErrorEncountered = false;

    @PostConstruct
    public void postConstruct() {

        if (page == null) {
            page = 1;
        }

        if (StringUtils.isEmpty(query)) {
            if (sortField == null) {
                sortField = SEARCH_FIELD_RELEASE_OR_CREATE_DATE;
            }
            if (sortOrder == null) {
                sortOrder = SortOrder.desc;
            }
            if (selectedTypesString == null || selectedTypesString.isEmpty()) {
                selectedTypesString = "dataverses:datasets";
            }
        } else {
            if (sortField == null) {
                sortField = SEARCH_FIELD_RELEVANCE;
            }
            if (sortOrder == null) {
                sortOrder = SortOrder.desc;
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

        selectedTypesMap.put(SearchObjectType.DATAVERSES, selectedTypesString.contains(SearchObjectType.DATAVERSES.getSolrValue()));
        selectedTypesMap.put(SearchObjectType.DATASETS, selectedTypesString.contains(SearchObjectType.DATASETS.getSolrValue()));
        selectedTypesMap.put(SearchObjectType.FILES, selectedTypesString.contains(SearchObjectType.FILES.getSolrValue()));
    }

    /**
     * @todo: better style and icons for facets
     * <p>
     * replace * with watermark saying "Search this Dataverse"
     * <p>
     * get rid of "_s" et al. (human eyeball friendly)
     * <p>
     * pagination (previous/next links)
     * <p>
     * test dataset cards
     * <p>
     * test files cards
     * <p>
     * test dataset cards when Solr is down
     * <p>
     * make results sortable: https://redmine.hmdc.harvard.edu/issues/3482
     * <p>
     * always show all types, even if zero count:
     * https://redmine.hmdc.harvard.edu/issues/3488
     * <p>
     * make subtree facet look like amazon widget (i.e. a tree)
     * <p>
     * see also https://trello.com/c/jmry3BJR/28-browse-dataverses
     */
    public String searchRedirect() {
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

        String optionalDataverseScope = "&alias=" + dataverseAlias;

        String qParam = "";
        if (query != null) {
            qParam = "&q=" + query;
        }

        return widgetWrapper.wrapURL("dataverse.xhtml?faces-redirect=true&q=" + qParam + optionalDataverseScope);

    }

    public void search(Dataverse dataverse) {
        logger.fine("search called");

        // wildcard/browse (*) unless user supplies a query
        String queryToPassToSolr = StringUtils.isEmpty(query) ? "*" : query;

        this.dataverse = dataverse;
        this.dataverseAlias = dataverse.getAlias();

        List<String> filterQueriesFinal = new ArrayList<>();
        String dataversePath = null;

        /**
         * @todo centralize this into SearchServiceBean
         */
        Optional<String> filterDownToSubtree;
        if (!dataverse.isRoot()) {
            dataversePath = dataverseDao.determineDataversePath(dataverse);
            filterDownToSubtree = Optional.of(SearchFields.SUBTREE + ":\"" + dataversePath + "\"");
        } else {
            filterDownToSubtree = Optional.empty();
        }

        filterDownToSubtree.ifPresent(filterQueriesFinal::add);

        filterQueriesFinal.addAll(filterQueries);

        SearchForTypes searchForTypes = SearchForTypes.byTypes(
                selectedTypesMap.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList()));

        int paginationStart = (page - 1) * RESULTS_PER_PAGE;

        // reset the solr error flag
        solrErrorEncountered = false;

        SolrQueryResponse solrQueryResponse = null;
        try {
            logger.fine("query from user:   " + query
                    + "\tqueryToPassToSolr: " + queryToPassToSolr
                    + "\tsort by: " + sortField);


            solrQueryResponse = searchService.search(dataverseRequestService.getDataverseRequest(), Collections.singletonList(dataverse),
                    queryToPassToSolr, searchForTypes, filterQueriesFinal, sortField, sortOrder,
                    paginationStart, RESULTS_PER_PAGE, false);

            // This 2nd search() is for populating the facets: -- L.A.
            // We only query for types that were not already queried in previous solr request
            SearchForTypes typesNotSearchedFor = searchForTypes.takeInverse();
            SolrQueryResponse notSearchedForTypesResponse = !typesNotSearchedFor.equals(SearchForTypes.EMPTY)
                    ? searchService.search(dataverseRequestService.getDataverseRequest(), Collections.singletonList(dataverse),
                    queryToPassToSolr, typesNotSearchedFor, filterQueriesFinal, sortField, sortOrder,
                    paginationStart, RESULTS_PER_PAGE, true)
                    : solrQueryResponse;
            DvObjectCounts countsFromSearch = solrQueryResponse.getDvObjectCounts();
            DvObjectCounts countOfNotSearchedForTypes = notSearchedForTypesResponse.getDvObjectCounts();

            // populate preview counts: https://redmine.hmdc.harvard.edu/issues/3560
            previewCountbyType.put(SearchObjectType.DATAVERSES, searchForTypes.isContainsDataverse()
                    ? countsFromSearch.getDataversesCount() : countOfNotSearchedForTypes.getDataversesCount());
            previewCountbyType.put(SearchObjectType.DATASETS, searchForTypes.isContainsDataset()
                    ? countsFromSearch.getDatasetsCount() : countOfNotSearchedForTypes.getDatasetsCount());
            previewCountbyType.put(SearchObjectType.FILES, searchForTypes.isContainsFiles()
                    ? countsFromSearch.getDatafilesCount() : countOfNotSearchedForTypes.getDatafilesCount());

        } catch (SearchException ex) {
            String message = "Exception running search for [" + queryToPassToSolr + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]";
            logger.log(Level.INFO, message, ex);
            this.solrIsDown = true;
            this.searchException = ex;
            this.solrErrorEncountered = true;
            this.errorFromSolr = ex.getMessage();
            return;
        }


        for (FacetCategory facetCategory: solrQueryResponse.getFacetCategoryList()) {
            if (facetCategory.getName().equals(SearchFields.PUBLICATION_STATUS) && facetCategory.getFacetLabels().size() < 2) {
                continue;
            }
            facetCategoryList.add(facetCategory);
        }
        this.searchResultsList = solrQueryResponse.getSolrSearchResults();
        this.searchResultsCount = solrQueryResponse.getNumResultsFound().intValue();
        if (filterDownToSubtree.isPresent()) {
            this.responseFilterQueries = solrQueryResponse.getFilterQueries().stream()
                    .filter(filter -> !filterDownToSubtree.get().equals(filter.getQuery()))
                    .collect(toList());
        } else {
            this.responseFilterQueries = solrQueryResponse.getFilterQueries();
        }
        this.filterQueriesDebug = solrQueryResponse.getFilterQueriesActual();

        paginationGuiStart = paginationStart + 1;
        paginationGuiEnd = Math.min(page * RESULTS_PER_PAGE, searchResultsCount);

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
        for (SolrSearchResult solrSearchResult : searchResultsList) {

            // going to assume that this is NOT a linked object, for now:
            solrSearchResult.setIsInTree(true);
            // (we'll review this later!)
        }

        Map<SearchObjectType, List<SolrSearchResult>> results = searchResultsList.stream()
                .collect(Collectors.groupingBy(SolrSearchResult::getType));
        solrSearchResultsService.populateDataverseSearchCard(
                results.getOrDefault(SearchObjectType.DATAVERSES, Collections.emptyList()));
        solrSearchResultsService.populateDatasetSearchCard(
                results.getOrDefault(SearchObjectType.DATASETS, Collections.emptyList()));
        solrSearchResultsService.populateDatafileSearchCard(
                results.getOrDefault(SearchObjectType.FILES, Collections.emptyList()));

        setDisplayCardValues(dataversePath);
    }

    public String searchWithSelectedTypesRedirect() {
        StringBuilder searchUrlBuilder = new StringBuilder()
                .append("dataverse.xhtml?alias=").append(dataverseAlias)
                .append("&q=").append((query == null) ? "" : query)
                .append("&types=").append(selectedTypesMap.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(entry -> entry.getKey().getSolrValue())
                        .collect(Collectors.joining(":")));

        for (int i=0; i< filterQueries.size(); i++) {
            searchUrlBuilder.append("&fq").append(i).append("=").append(filterQueries.get(i));
        }
        searchUrlBuilder.append("&sort=").append(sortField)
                        .append("&order=").append(sortOrder)
                        .append("&page=1&faces-redirect=true");

        return widgetWrapper.wrapURL(searchUrlBuilder.toString());
    }

    /**
     * Used for capturing errors that happen during solr query
     * Added to catch exceptions when parsing the solr query string
     *
     * @return
     */
    public boolean wasSolrErrorEncountered() {

        if (this.solrErrorEncountered) {
            return true;
        }
        if (!this.hasValidFilterQueries()) {
            solrErrorEncountered = true;
            return true;
        }
        return solrErrorEncountered;
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

    public List<FacetCategory> getFacetCategoryList() {
        return facetCategoryList;
    }

    public List<SolrSearchResult> getSearchResultsList() {
        return searchResultsList;
    }

    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public String getSelectedTypesString() {
        return selectedTypesString;
    }

    public void setSelectedTypesString(String selectedTypesString) {
        this.selectedTypesString = selectedTypesString;
    }

    public Long getFacetCountDatasets() {
        return previewCountbyType.get(SearchObjectType.DATASETS);
    }

    public Long getFacetCountDataverses() {
        return previewCountbyType.get(SearchObjectType.DATAVERSES);
    }

    public Long getFacetCountFiles() {
        return previewCountbyType.get(SearchObjectType.FILES);
    }

    public String getSortField() {
        return sortField;
    }

    public String getSortOrder() {
        if (sortOrder != null) {
            return sortOrder.toString();
        } else {
            return null;
        }
    }

    /**
     * Allow only valid values to be set.
     * <p>
     * Rather than passing in a String and converting it to an enum in this
     * method we could write a converter:
     * http://stackoverflow.com/questions/8609378/jsf-2-0-view-parameters-to-pass-objects
     */
    public void setSortOrder(String sortOrderSupplied) {
        if (sortOrderSupplied != null) {
            if (sortOrderSupplied.equals(SortOrder.asc.toString())) {
                this.sortOrder = SortOrder.asc;
            }
            if (sortOrderSupplied.equals(SortOrder.desc.toString())) {
                this.sortOrder = SortOrder.desc;
            }
        }
    }

    public boolean isSortedByNameAsc() {
        return sortField.equals(SEARCH_FIELD_NAME_SORT) && sortOrder == SortOrder.asc;
    }

    public boolean isSortedByNameDesc() {
        return sortField.equals(SEARCH_FIELD_NAME_SORT) && sortOrder == SortOrder.desc;
    }

    public boolean isSortedByReleaseDateAsc() {
        return sortField.equals(SEARCH_FIELD_RELEASE_OR_CREATE_DATE) && sortOrder == SortOrder.asc;
    }

    public boolean isSortedByReleaseDateDesc() {
        return sortField.equals(SEARCH_FIELD_RELEASE_OR_CREATE_DATE) && sortOrder == SortOrder.desc;
    }

    public boolean isSortedByRelevance() {
        return sortField.equals(SEARCH_FIELD_RELEVANCE) && sortOrder == SortOrder.desc;
    }

    public int getPage() {
        return page;
    }

    // helper method
    public int getTotalPages() {
        return ((searchResultsCount - 1) / RESULTS_PER_PAGE) + 1;
    }

    public int getPaginationGuiStart() {
        return paginationGuiStart;
    }

    public int getPaginationGuiEnd() {
        return paginationGuiEnd;
    }

    public boolean isSolrIsDown() {
        return solrIsDown;
    }

    public boolean isRootDv() {
        return dataverse.isRoot();
    }

    public List<FilterQuery> getResponseFilterQueries() {
        return responseFilterQueries;
    }

    public List<String> getFilterQueriesDebug() {
        return filterQueriesDebug;
    }


    /**
     * A bit of redundant effort for error checking in the .xhtml
     * <p>
     * Specifically for searches with bad facets in query string--
     * incorrect quoting.  These searches don't always throw an explicit
     * solr error.
     * <p>
     * Note: An empty or null filterQuery array is OK
     * Values within the array that can't be split are NOT ok
     * (This is quick "downstream" fix--not necessarily efficient)
     *
     * @return
     */
    private boolean hasValidFilterQueries() {

        if (this.filterQueries.isEmpty()) {
            return true;        // empty is valid!
        }

        for (FilterQuery fq : responseFilterQueries) {
            if (!fq.hasFriendlyNameAndValue()) {
                return false;   // not parseable is bad!
            }
        }
        return true;
    }

    public Map<SearchObjectType, Boolean> getSelectedTypesMap() {
        return selectedTypesMap;
    }

    public void setSelectedTypesMap(Map<SearchObjectType, Boolean> selectedTypesMap) {
        this.selectedTypesMap = selectedTypesMap;
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

    public boolean isTabular(DataFile datafile) {

        if (datafile == null) {
            return false;
        }

        return datafile.isTabularData();
    }

    public SearchException getSearchException() {
        return searchException;
    }

    public String tabularDataDisplayInfo(DataFile datafile) {
        String ret = "";

        if (datafile == null) {
            return null;
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

    public String dataFileSizeDisplay(DataFile datafile) {
        if (datafile == null) {
            return "";
        }

        return datafile.getFriendlySize();

    }

    public String dataFileChecksumDisplay(DataFile datafile) {
        if (datafile == null) {
            return "";
        }

        if (datafile.getChecksumValue() != null && !StringUtils.isEmpty(datafile.getChecksumValue())) {
            if (datafile.getChecksumType() != null) {
                return " " + datafile.getChecksumType() + ": " + datafile.getChecksumValue() + " ";
            }
        }

        return "";
    }

    private void setDisplayCardValues(String dataversePath) {

        for (SolrSearchResult result : searchResultsList) {
            //logger.info("checking DisplayImage for the search result " + i++);
            if (result.getType() == SearchObjectType.DATAVERSES) {
                /**
                 * @todo Someday we should probably revert this setImageUrl to
                 * the original meaning "image_url" to address this issue:
                 * `image_url` from Search API results no longer yields a
                 * downloadable image -
                 * https://github.com/IQSS/dataverse/issues/3616
                 */
                result.setImageUrl(thumbnailServiceWrapper.getDataverseCardImageAsBase64Url(result));
            } else if (result.getType() == SearchObjectType.DATASETS && result.getEntity() != null) {
                result.setImageUrl(thumbnailServiceWrapper.getDatasetCardImageAsBase64Url(result));
            } else if (result.getType() == SearchObjectType.FILES) {
                result.setImageUrl(thumbnailServiceWrapper.getFileCardImageAsBase64Url(result));
            }
        }

        thumbnailServiceWrapper.resetObjectMaps();

        // determine which of the objects are linked:
        if (dataverse.isRoot()) {
            // (nothing is "linked" if it's the root DV!)
            return;
        }
        Set<Long> dvObjectParentIds = new HashSet<>();
        for (SolrSearchResult result : searchResultsList) {
            if (dataverse.getId().equals(result.getParentIdAsLong())) {
                // definitely NOT linked:
                result.setIsInTree(true);
            } else if (result.getParentIdAsLong().equals(dataverseDao.findRootDataverse().getId())) {
                // the object's parent is the root Dv; and the current
                // Dv is NOT root... definitely linked:
                result.setIsInTree(false);
            } else {
                dvObjectParentIds.add(result.getParentIdAsLong());
            }
        }

        if (dvObjectParentIds.size() > 0) {
            Map<Long, String> treePathMap = dvObjectService.getObjectPathsByIds(dvObjectParentIds);
            if (treePathMap != null) {
                for (SolrSearchResult result : searchResultsList) {
                    Long objectId = result.getParentIdAsLong();
                    if (treePathMap.containsKey(objectId)) {
                        String objectPath = treePathMap.get(objectId);
                        if (!StringUtils.startsWith(objectPath, dataversePath)) {
                            result.setIsInTree(false);
                        }
                    }
                }
            }
        }
    }

    public boolean couldCreateDatasetOrDataverseIfWasAuthenticated() throws ClassNotFoundException {
        return permissionService.userOn(AuthenticatedUsers.get(), dataverse).has(Permission.AddDataverse)
                || permissionService.userOn(AuthenticatedUsers.get(), dataverse).has(Permission.AddDataset);
    }

}
