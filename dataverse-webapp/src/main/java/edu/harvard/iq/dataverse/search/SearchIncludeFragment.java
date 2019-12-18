package edu.harvard.iq.dataverse.search;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.ThumbnailServiceWrapper;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import org.apache.commons.lang.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("SearchIncludeFragment")
public class SearchIncludeFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(SearchIncludeFragment.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    DatasetDao datasetDao;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean dataFileService;
    @EJB
    DvObjectServiceBean dvObjectService;
    @Inject
    DataverseSession session;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    @Inject
    WidgetWrapper widgetWrapper;
    @EJB
    private DatasetFieldServiceBean datasetFieldService;
    @Inject
    private DataverseRequestServiceBean dataverseRequestService;

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
    private String dataversePath = null;

    private String selectedTypesString;
    private List<String> selectedTypesList = new ArrayList<>();
    private String searchFieldType = SearchFields.TYPE;
    private String searchFieldSubtree = SearchFields.SUBTREE;
    private String searchFieldNameSort = SearchFields.NAME_SORT;
    private String searchFieldRelevance = SearchFields.RELEVANCE;
    private String searchFieldReleaseOrCreateDate = SearchFields.RELEASE_OR_CREATE_DATE;
    final private String ASCENDING = SortOrder.asc.toString();
    final private String DESCENDING = SortOrder.desc.toString();
    private String typeFilterQuery;
    Map<String, Long> previewCountbyType = new HashMap<>();
    private String sortField;
    private SortOrder sortOrder;
    private int page = 1;
    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private int paginationGuiRows = 10;
    private boolean solrIsDown = false;
    private Map<String, Integer> numberOfFacets = new HashMap<>();

    List<String> filterQueriesDebug = new ArrayList<>();
    private String errorFromSolr;
    private SearchException searchException;
    private boolean solrErrorEncountered = false;

    /**
     * @todo Number of search results per page should be configurable -
     * https://github.com/IQSS/dataverse/issues/84
     */
    private final static int numRows = 10;
    
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

        String optionalDataverseScope = "&alias=" + dataverse.getAlias();

        String qParam = "";
        if (query != null) {
            qParam = "&q=" + query;
        }

        return widgetWrapper.wrapURL("dataverse.xhtml?faces-redirect=true&q=" + qParam + optionalDataverseScope);

    }

    public void search() {
        logger.fine("search called");

        if (dataverseAlias != null) {
            dataverse = dataverseDao.findByAlias(dataverseAlias);
        }
        if (dataverse == null) {
            dataverse = dataverseDao.findRootDataverse();
        }

        // wildcard/browse (*) unless user supplies a query
        String queryToPassToSolr = "*";

        if (StringUtils.isEmpty(query)) {
            queryToPassToSolr = "*";
            if (sortField == null) {
                sortField = searchFieldReleaseOrCreateDate;
            }
            if (sortOrder == null) {
                sortOrder = SortOrder.desc;
            }
            if (selectedTypesString == null || selectedTypesString.isEmpty()) {
                selectedTypesString = "dataverses:datasets";
            }
        } else {
            queryToPassToSolr = query;
            if (sortField == null) {
                sortField = searchFieldRelevance;
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


        List<String> filterQueriesWithoutType = new ArrayList<>();

        if (!dataverse.isRoot()) {
            /**
             * @todo centralize this into SearchServiceBean
             */
            dataversePath = dataverseDao.determineDataversePath(this.dataverse);
            String filterDownToSubtree = SearchFields.SUBTREE + ":\"" + dataversePath + "\"";
            filterQueriesWithoutType.add(filterDownToSubtree);
        }

        filterQueriesWithoutType.addAll(filterQueries);
        
        
        
        if (StringUtils.isNotEmpty(selectedTypesString)) {
            
            String selectedTypesHumanReadable = StringUtils.join(selectedTypesString.split(":"), " OR ");
            typeFilterQuery = SearchFields.TYPE + ":(" + selectedTypesHumanReadable + ")";
        }
        selectedTypesList = Lists.newArrayList(selectedTypesString.split(":"));
        

        List<String> filterQueriesFinal = new ArrayList<>(filterQueriesWithoutType);
        filterQueriesFinal.add(typeFilterQuery);
        
        List<String> filterQueriesFinalAllTypes = new ArrayList<>(filterQueriesWithoutType);
        filterQueriesFinalAllTypes.add(SearchFields.TYPE + ":(dataverses OR datasets OR files)");

        int paginationStart = (page - 1) * paginationGuiRows;

        // reset the solr error flag
        solrErrorEncountered = false;

        SolrQueryResponse solrQueryResponse = null;
        try {
            logger.fine("query from user:   " + query);
            logger.fine("queryToPassToSolr: " + queryToPassToSolr);
            logger.fine("sort by: " + sortField);

            
            solrQueryResponse = searchService.search(dataverseRequestService.getDataverseRequest(), Collections.singletonList(dataverse), 
                    queryToPassToSolr, filterQueriesFinal, sortField, sortOrder, 
                    paginationStart, false, numRows, false);
            
            // This 2nd search() is for populating the facets: -- L.A. 
            // TODO: ...
            SolrQueryResponse solrQueryResponseAllTypes = searchService.search(dataverseRequestService.getDataverseRequest(), Collections.singletonList(dataverse),
                    queryToPassToSolr, filterQueriesFinalAllTypes, sortField, sortOrder, 
                    paginationStart, false, numRows, false);
            
            // populate preview counts: https://redmine.hmdc.harvard.edu/issues/3560
            previewCountbyType.put("dataverses", solrQueryResponseAllTypes.getDvObjectCounts().get("dataverses_count"));
            previewCountbyType.put("datasets", solrQueryResponseAllTypes.getDvObjectCounts().get("datasets_count"));
            previewCountbyType.put("files", solrQueryResponseAllTypes.getDvObjectCounts().get("files_count"));

        } catch (SearchException ex) {
            String message = "Exception running search for [" + queryToPassToSolr + "] with filterQueries " + filterQueries + " and paginationStart [" + paginationStart + "]";
            logger.log(Level.INFO, message, ex);
            this.solrIsDown = true;
            this.searchException = ex;
            this.solrErrorEncountered = true;
            this.errorFromSolr = ex.getMessage();
            return;
        }
        
        
        this.facetCategoryList = solrQueryResponse.getFacetCategoryList();
        this.searchResultsList = solrQueryResponse.getSolrSearchResults();
        this.searchResultsCount = solrQueryResponse.getNumResultsFound().intValue();
        this.filterQueriesDebug = solrQueryResponse.getFilterQueriesActual();
        
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

            // going to assume that this is NOT a linked object, for now:
            solrSearchResult.setIsInTree(true);
            // (we'll review this later!)

            if (solrSearchResult.getType().equals("dataverses")) {
                dataverseDao.populateDvSearchCard(solrSearchResult);
                
                /*
                Dataverses cannot be harvested yet.
                if (isHarvestedDataverse(solrSearchResult.getEntityId())) {
                    solrSearchResult.setHarvested(true);
                }*/

            } else if (solrSearchResult.getType().equals("datasets")) {
                datasetVersionService.populateDatasetSearchCard(solrSearchResult);

                // @todo - the 3 lines below, should they be moved inside
                // searchServiceBean.search()?
                String deaccesssionReason = solrSearchResult.getDeaccessionReason();
                if (deaccesssionReason != null) {
                    solrSearchResult.setDescriptionNoSnippet(deaccesssionReason);
                }

            } else if (solrSearchResult.getType().equals("files")) {
                dataFileService.populateFileSearchCard(solrSearchResult);

                /**
                 * @todo: show DataTable variables
                 */
            }
        }
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

    private Long findFacetCountByType(String type) {
        return previewCountbyType.get(type);
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
        return sortField.equals(searchFieldNameSort) && sortOrder == SortOrder.asc;
    }

    public boolean isSortedByNameDesc() {
        return sortField.equals(searchFieldNameSort) && sortOrder == SortOrder.desc;
    }

    public boolean isSortedByReleaseDateAsc() {
        return sortField.equals(searchFieldReleaseOrCreateDate) && sortOrder == SortOrder.asc;
    }

    public boolean isSortedByReleaseDateDesc() {
        return sortField.equals(searchFieldReleaseOrCreateDate) && sortOrder == SortOrder.desc;
    }

    public boolean isSortedByRelevance() {
        return sortField.equals(searchFieldRelevance) && sortOrder == SortOrder.desc;
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

    public boolean isRootDv() {
        return dataverse.isRoot();
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
    public boolean hasValidFilterQueries() {

        if (this.filterQueries.isEmpty()) {
            return true;        // empty is valid!
        }

        for (String fq : this.filterQueries) {
            if (this.getFriendlyNamesFromFilterQuery(fq) == null) {
                return false;   // not parseable is bad!
            }
        }
        return true;
    }

    public List<String> getFriendlyNamesFromFilterQuery(String filterQuery) {


        if (filterQuery == null) {
            return null;
        }

        String[] parts = filterQuery.split(":");
        if (parts.length != 2) {
            return null;
        }
        String key = parts[0];
        String value = parts[1];

        List<String> friendlyNames = new ArrayList<>();

        friendlyNames.add(searchService.getLocaleFacetName(key));

        String localizedFacetName = searchService.getLocaleFacetName(value.replaceAll("^\"", "").replaceAll("\"$", ""));
        friendlyNames.add(localizedFacetName);
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
        return StringUtils.join(newTypesSelected, ":");

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

    public void setDisplayCardValues() {

        Set<Long> harvestedDatasetIds = null;
        for (SolrSearchResult result : searchResultsList) {
            //logger.info("checking DisplayImage for the search result " + i++);
            if (result.getType().equals("dataverses")) {
                /**
                 * @todo Someday we should probably revert this setImageUrl to
                 * the original meaning "image_url" to address this issue:
                 * `image_url` from Search API results no longer yields a
                 * downloadable image -
                 * https://github.com/IQSS/dataverse/issues/3616
                 */
                result.setImageUrl(thumbnailServiceWrapper.getDataverseCardImageAsBase64Url(result));
            } else if (result.getType().equals("datasets")) {
                if (result.getEntity() != null) {
                    result.setImageUrl(thumbnailServiceWrapper.getDatasetCardImageAsBase64Url(result));
                }

                if (result.isHarvested()) {
                    if (harvestedDatasetIds == null) {
                        harvestedDatasetIds = new HashSet<>();
                    }
                    harvestedDatasetIds.add(result.getEntityId());
                }
            } else if (result.getType().equals("files")) {
                result.setImageUrl(thumbnailServiceWrapper.getFileCardImageAsBase64Url(result));
                if (result.isHarvested()) {
                    if (harvestedDatasetIds == null) {
                        harvestedDatasetIds = new HashSet<>();
                    }
                    harvestedDatasetIds.add(result.getParentIdAsLong());
                }
            }
        }

        thumbnailServiceWrapper.resetObjectMaps();

        // Now, make another pass, and add the remote archive descriptions to the 
        // harvested dataset and datafile cards (at the expense of one extra 
        // SQL query:

        if (harvestedDatasetIds != null) {
            Map<Long, String> descriptionsForHarvestedDatasets = datasetDao.getArchiveDescriptionsForHarvestedDatasets(harvestedDatasetIds);
            if (descriptionsForHarvestedDatasets != null && descriptionsForHarvestedDatasets.size() > 0) {
                for (SolrSearchResult result : searchResultsList) {
                    if (result.isHarvested()) {
                        if (result.getType().equals("files")) {
                            if (descriptionsForHarvestedDatasets.containsKey(result.getParentIdAsLong())) {
                                result.setHarvestingDescription(descriptionsForHarvestedDatasets.get(result.getParentIdAsLong()));
                            }
                        } else if (result.getType().equals("datasets")) {
                            if (descriptionsForHarvestedDatasets.containsKey(result.getEntityId())) {
                                result.setHarvestingDescription(descriptionsForHarvestedDatasets.get(result.getEntityId()));
                            }
                        }
                    }
                }
            }
            descriptionsForHarvestedDatasets = null;
            harvestedDatasetIds = null;
        }

        // determine which of the objects are linked:

        if (!dataverse.isRoot()) {
            // (nothing is "linked" if it's the root DV!)
            Set<Long> dvObjectParentIds = new HashSet<>();
            for (SolrSearchResult result : searchResultsList) {
                if (dataverse.getId().equals(result.getParentIdAsLong())) {
                    // definitely NOT linked:
                    result.setIsInTree(true);
                } else if (result.getParentIdAsLong() == dataverseDao.findRootDataverse().getId()) {
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
                            if (!objectPath.startsWith(dataversePath)) {
                                result.setIsInTree(false);
                            }
                        }
                    }
                }
            }

            dvObjectParentIds = null;
        }

    }

}
