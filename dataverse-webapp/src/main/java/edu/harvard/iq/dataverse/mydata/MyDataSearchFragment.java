package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SolrSearchResultsService;
import edu.harvard.iq.dataverse.ThumbnailServiceWrapper;
import edu.harvard.iq.dataverse.WidgetWrapper;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataTable;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.FacetCategory;
import edu.harvard.iq.dataverse.search.response.FacetLabel;
import edu.harvard.iq.dataverse.search.response.FilterQuery;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ViewScoped
@Named("MyDataSearchFragment")
public class MyDataSearchFragment implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(MyDataSearchFragment.class.getCanonicalName());

    @EJB
    SearchServiceBean searchService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    DatasetDao datasetDao;
    @EJB
    DvObjectServiceBean dvObjectService;
    @Inject
    DataverseSession session;
    @Inject
    SettingsServiceBean settingsService;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    @EJB
    private AuthenticationServiceBean authenticationService;
    @EJB
    private DataverseRoleServiceBean dataverseRoleService;
    @EJB
    private DvObjectServiceBean dvObjectServiceBean;
    @EJB
    private GroupServiceBean groupService;
    @Inject
    private WidgetWrapper widgetWrapper;
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseRequestServiceBean dataverseRequestService;
    @Inject
    private RoleAssigneeServiceBean roleAssigneeService;
    @Inject
    private SolrSearchResultsService solrSearchResultsService;

    private String browseModeString = "browse";
    private String searchModeString = "search";
    private String mode;
    private String query;
    private String searchUserId;
    private List<String> publicationStatusFilters = new ArrayList<>();
    private FacetCategory publicationStatusFacetCategory;
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
    private List<String> roleFilters = new ArrayList<>();
    private String rf0;
    private String rf1;
    private String rf2;
    private String rf3;
    private String rf4;
    private String rf5;
    private Dataverse dataverse;
    private String selectedTypesString;
    private SearchForTypes selectedTypes = SearchForTypes.all();

    private Map<String, Long> previewCountbyType = new HashMap<>();
    private int page = 1;
    private int paginationGuiStart = 1;
    private int paginationGuiEnd = 10;
    private int paginationGuiRows = 10;
    private boolean solrIsDown = false;
    private boolean debug = false;

    private List<FilterQuery> selectedFilterQueries = new ArrayList<>();
    private List<String> filterQueriesDebug = new ArrayList<>();

    private boolean rootDv = true;
    private boolean solrErrorEncountered = false;
    private AuthenticatedUser authUser = null;
    private RoleTagRetriever roleTagRetriever;
    private String errorFromSolr;
    private List<String> myRoles;

    // -------------------- GETTERS --------------------
    public String getBrowseModeString() {
        return browseModeString;
    }

    public String getSearchModeString() {
        return searchModeString;
    }

    public String getQuery() {
        return query;
    }

    public List<String> getMyRoles() {
        return myRoles;
    }

    public String getSearchUserId() {
        return searchUserId;
    }

    public String getMode() {
        return mode;
    }

    public List<String> getPublicationStatusFilters() {
        return publicationStatusFilters;
    }

    public List<SolrSearchResult> getSearchResultsList() {
        return searchResultsList;
    }

    public int getSearchResultsCount() {
        return searchResultsCount;
    }

    public List<String> getRoleFilters() {
        return roleFilters;
    }

    public String getRf0() {
        return rf0;
    }

    public String getRf1() {
        return rf1;
    }

    public String getRf2() {
        return rf2;
    }

    public String getRf3() {
        return rf3;
    }

    public String getRf4() {
        return rf4;
    }

    public String getRf5() {
        return rf5;
    }

    public String getFq0() {
        return fq0;
    }

    public String getFq1() {
        return fq1;
    }

    public String getFq2() {
        return fq2;
    }

    public String getFq3() {
        return fq3;
    }

    public String getFq4() {
        return fq4;
    }

    public String getFq5() {
        return fq5;
    }

    public String getFq6() {
        return fq6;
    }

    public String getFq7() {
        return fq7;
    }

    public String getFq8() {
        return fq8;
    }

    public String getFq9() {
        return fq9;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public String getSelectedTypesString() {
        return selectedTypesString;
    }

    public SearchForTypes getSelectedTypes() {
        return selectedTypes;
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

    // helper method
    public int getTotalPages() {
        return ((searchResultsCount - 1) / paginationGuiRows) + 1;
    }

    public int getPaginationGuiStart() {
        return paginationGuiStart;
    }

    public int getPaginationGuiEnd() {
        return paginationGuiEnd;
    }

    public int getPaginationGuiRows() {
        return paginationGuiRows;
    }

    public FacetCategory getPublicationStatusFacetCategory() {
        return publicationStatusFacetCategory;
    }

    public List<FilterQuery> getSelectedFilterQueries() {
        return selectedFilterQueries;
    }

    public List<String> getFilterQueriesDebug() {
        return filterQueriesDebug;
    }

    public String getErrorFromSolr() {
        return errorFromSolr;
    }

    // -------------------- LOGIC --------------------

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
            setSolrErrorEncountered(true);
            return true;
        }
        return solrErrorEncountered;
    }

    private Long findFacetCountByType(String type) {
        return previewCountbyType.get(type);
    }

    public boolean isSolrIsDown() {
        return solrIsDown;
    }

    public boolean isRootDv() {
        return rootDv;
    }

    public boolean isDebug() {
        return (debug && session.getUser().isSuperuser())
                || settingsService.isTrue(":Debug");
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
        if (this.publicationStatusFilters.isEmpty()) {
            return true;        // empty is valid!
        }

        for (FilterQuery fq : selectedFilterQueries) {
            if (!fq.hasFriendlyNameAndValue()) {
                return false;   // not parseable is bad!
            }
        }
        return true;
    }

    public String getNewSelectedTypes(SearchObjectType typeClicked) {
        SearchForTypes newTypesSelected = selectedTypes.toggleType(typeClicked);

        return newTypesSelected.getTypes().stream()
                .map(SearchObjectType::getSolrValue)
                .collect(Collectors.joining(":"));
    }

    public boolean isTabular(DataFile datafile) {
        return datafile != null && datafile.isTabularData();
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
        return datafile == null ? "" : datafile.getFriendlySize();
    }

    public String dataFileChecksumDisplay(DataFile datafile) {
        if (datafile == null) {
            return "";
        }

        if (StringUtils.isNotEmpty(datafile.getChecksumValue())) {
            if (datafile.getChecksumType() != null) {
                return " " + datafile.getChecksumType() + ": " + datafile.getChecksumValue() + " ";
            }
        }
        return "";
    }

    public void setDisplayCardValues() {
        for (SolrSearchResult result : searchResultsList) {
            if (result.getType() == SearchObjectType.DATAVERSES) {
                result.setImageUrl(thumbnailServiceWrapper.getDataverseCardImageAsBase64Url(result));
            } else if (result.getType() == SearchObjectType.DATASETS && result.getEntity() != null) {
                result.setImageUrl(thumbnailServiceWrapper.getDatasetCardImageAsBase64Url(result));
            } else if (result.getType() == SearchObjectType.FILES) {
                result.setImageUrl(thumbnailServiceWrapper.getFileCardImageAsBase64Url(result));
            }
        }
        thumbnailServiceWrapper.resetObjectMaps();
    }

    public String searchRedirect(String dataverseRedirectPage) {
        dataverseRedirectPage = StringUtils.isBlank(dataverseRedirectPage) ? "/dataverseuser.xhtml?selectTab=dataRelatedToMe" : dataverseRedirectPage;

        String qParam = "";
        if (query != null) {
            qParam = "&q=" + query;
        }
        if (searchUserId != null) {
            qParam += "&uId=" + searchUserId;
        }

        return widgetWrapper.wrapURL(dataverseRedirectPage + "?faces-redirect=true" + qParam);
    }

    public String getAuthUserIdentifier() {
        return this.authUser == null
                ? null : MyDataUtil.formatUserIdentifierForMyDataForm(this.authUser.getIdentifier());
    }

    public String retrieveMyData() {
        if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();
            if (StringUtils.isEmpty(searchUserId)) {
                searchUserId = getAuthUserIdentifier();
            }
            // If person is a superuser, see if a userIdentifier has been specified
            // and use that instead
            // For, superusers, the searchUser may differ from the authUser
            //
            AuthenticatedUser searchUser;
            if (authUser.isSuperuser()) {
                searchUser = getUserFromIdentifier(searchUserId);
                if (searchUser != null) {
                    authUser = searchUser;
                } else {
                    return "No user found for: \"" + searchUserId + "\"";
                }
            }
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        // wildcard/browse (*) unless user supplies a query
        mode = StringUtils.isEmpty(query)
                ? browseModeString : searchModeString;

        String searchTerm;
        if (mode.equals(browseModeString)) {
            searchTerm = "*";

            if (StringUtils.isEmpty(selectedTypesString)) {
                selectedTypesString = "dataverses:datasets";
            }
        } else {
            searchTerm = query;

            if (StringUtils.isEmpty(selectedTypesString)) {
                selectedTypesString = "dataverses:datasets:files";
            }
        }

        publicationStatusFilters = new ArrayList<>();
        for (String fq : Arrays.asList(fq0, fq1, fq2, fq3, fq4, fq5, fq6, fq7, fq8, fq9)) {
            if (MyDataFilterParams.allPublishedStates.contains(fq)) {
                publicationStatusFilters.add(fq);
            }
        }
        roleFilters = new ArrayList<>();
        for (String rf : Arrays.asList(rf0, rf1, rf2, rf3, rf4, rf5)) {
            if (rf != null) {
                roleFilters.add(rf);
            }
        }

        if (dataverse == null) {
            this.dataverse = dataverseDao.findRootDataverse();
        }

        List<DataverseRole> roleList = dataverseRoleService.findAll();
        DataverseRolePermissionHelper rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        List<String> pub_states = new ArrayList<>();
        for (String filter : publicationStatusFilters) {
            pub_states.add(filter.replace("\"",""));
        }
        if (pub_states.isEmpty()) {
            pub_states = MyDataFilterParams.defaultPublishedStates;
        }

        // ---------------------------------
        // (1) Initialize filterParams and check for Errors
        // ---------------------------------
        DataverseRequest originalRequest = dataverseRequestService.getDataverseRequest();
        DataverseRequest requestWithSearchedUser = new DataverseRequest(authUser, originalRequest.getSourceAddress());

        selectedTypes = SearchForTypes.byTypes(
                Arrays.stream(selectedTypesString.split(":"))
                    .map(SearchObjectType::fromSolrValue)
                    .collect(Collectors.toList()));

        List<Long> roleIdsForFilters = roleFilters.isEmpty()
                ? rolePermissionHelper.getRoleIdList() : rolePermissionHelper.findRolesIdsByNames(roleFilters);
        MyDataFilterParams filterParams = new MyDataFilterParams(requestWithSearchedUser, toMyDataFinderFormat(selectedTypes),
                pub_states, roleIdsForFilters, searchTerm);
        if (filterParams.hasError()) {
            return filterParams.getErrorMessage() + filterParams.getErrorMessage();
        }

        // ---------------------------------
        // (2) Initialize MyDataFinder and check for Errors
        // ---------------------------------
        MyDataFinder myDataFinder = new MyDataFinder(rolePermissionHelper, roleAssigneeService, dvObjectServiceBean, groupService);
        myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()) {
            return myDataFinder.getErrorMessage() + myDataFinder.getErrorMessage();
        }
        List<String> filterQueries = myDataFinder.getSolrFilterQueries();
        if (filterQueries == null) {
            logger.fine("No ids found for this search");
            return DataRetrieverAPI.MSG_NO_RESULTS_FOUND;
        }
        for (String filter : filterQueries) {
            if (filter.contains(SearchFields.PUBLICATION_STATUS) && pub_states.size() != MyDataFilterParams.defaultPublishedStates.size()) {
                filterQueries.add(filter.replace("OR", "AND"));
                filterQueries.remove(filter);
            }
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = (page - 1) * paginationGuiRows;

        SolrQueryResponse solrQueryResponse;
        try {
            solrQueryResponse = searchService.search(
                    requestWithSearchedUser,
                    null,
                    searchTerm,
                    selectedTypes,
                    filterQueries,
                    SearchFields.RELEASE_OR_CREATE_DATE,
                    SortOrder.desc,
                    paginationStart,
                    SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE,
                    false);

            if (solrQueryResponse.getNumResultsFound() == 0) {
                this.solrIsDown = true;
                return DataRetrieverAPI.MSG_NO_RESULTS_FOUND;
            }

        } catch (SearchException ex) {
            solrQueryResponse = null;
            errorFromSolr = ex.getMessage();
            logger.severe("Solr SearchException: " + ex.getMessage());
        }

        if (solrQueryResponse == null) {
            return "Sorry!  There was an error with the search service. Sorry! There was a SOLR Error";
        }
        if (!solrIsDown) {
            // we need to populate roleTagRetriver object with roles for all objects and not only for those from current page (int.max_value)
            try {
                SolrQueryResponse fullSolrQueryResponse = searchService.search(
                        requestWithSearchedUser,
                        null,
                        searchTerm,
                        selectedTypes,
                        filterQueries,
                        SearchFields.RELEASE_OR_CREATE_DATE,
                        SortOrder.desc,
                        0,
                        1000,
                        false);
                roleTagRetriever = new RoleTagRetriever(rolePermissionHelper, roleAssigneeService, this.dvObjectServiceBean);
                roleTagRetriever.loadRoles(requestWithSearchedUser, fullSolrQueryResponse);

                myRoles = fullSolrQueryResponse.getSolrSearchResults().stream()
                        .flatMap(r -> roleTagRetriever.getRolesForCard(r.getEntityId()).stream())
                        .distinct()
                        .collect(Collectors.toList());
            } catch (SearchException ex) {
                logger.severe("Solr SearchException: " + ex.getMessage());
            }

            roleTagRetriever.loadRoles(requestWithSearchedUser, solrQueryResponse);
            for (FacetCategory facetCat : solrQueryResponse.getFacetCategoryList()) {
                if (facetCat.getName().equals("publicationStatus")) {
                    publicationStatusFacetCategory = new FacetCategory();
                    publicationStatusFacetCategory.setName(facetCat.getName());
                    publicationStatusFacetCategory.setFriendlyName(facetCat.getFriendlyName());
                    for (FacetLabel facetLabel : facetCat.getFacetLabels()) {
                        FacetLabel convertedFacetLabel = new FacetLabel(
                                facetLabel.getName(), facetLabel.getDisplayName(), facetLabel.getCount());
                        convertedFacetLabel.setFilterQuery(facetLabel.getName());
                        publicationStatusFacetCategory.addFacetLabel(convertedFacetLabel);
                    }
                    break;
                }
            }
            this.searchResultsList = solrQueryResponse.getSolrSearchResults();
            this.searchResultsCount = solrQueryResponse.getNumResultsFound().intValue();
            this.filterQueriesDebug = solrQueryResponse.getFilterQueriesActual();
            paginationGuiStart = paginationStart + 1;
            paginationGuiEnd = Math.min(page * paginationGuiRows, searchResultsCount);
            List<SolrSearchResult> searchResults = solrQueryResponse.getSolrSearchResults();

            // populate preview counts: https://redmine.hmdc.harvard.edu/issues/3560
            previewCountbyType.put("dataverses", solrQueryResponse.getDvObjectCounts().getDataversesCount());
            previewCountbyType.put("datasets", solrQueryResponse.getDvObjectCounts().getDatasetsCount());
            previewCountbyType.put("files", solrQueryResponse.getDvObjectCounts().getDatafilesCount());

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
            }
            Map<SearchObjectType, List<SolrSearchResult>> results = searchResults.stream()
                    .collect(Collectors.groupingBy(SolrSearchResult::getType));
            solrSearchResultsService.populateDataverseSearchCard(
                    results.getOrDefault(SearchObjectType.DATAVERSES, Collections.emptyList()));
            solrSearchResultsService.populateDatasetSearchCard(
                    results.getOrDefault(SearchObjectType.DATASETS, Collections.emptyList()));
            solrSearchResultsService.populateDatafileSearchCard(
                    results.getOrDefault(SearchObjectType.FILES, Collections.emptyList()));

            for (String publicationStatusFilter: publicationStatusFilters) {
                String key = format(SearchServiceBean.FACETBUNDLE_MASK_VALUE, "publicationStatus");
                String value = format(SearchServiceBean.FACETBUNDLE_MASK_GROUP_AND_VALUE, "publicationStatus", publicationStatusFilter.toLowerCase().replace(" ", "_"));

                selectedFilterQueries.add(
                        new FilterQuery(publicationStatusFilter,
                                BundleUtil.getStringFromBundle(key), BundleUtil.getStringFromBundle(value)));
            }
        }
        setDisplayCardValues();
        return StringUtils.EMPTY;
    }

    public boolean isSuperuser() {
        return (session.getUser() != null) && session.getUser().isSuperuser();
    }

    public List<String> getRolesForEntity(long id) {
        return roleTagRetriever.getFinalIdToRolesHash().get(id);
    }

    // -------------------- PRIVATE ---------------------

    private List<String> toMyDataFinderFormat(SearchForTypes selectedTypes) {
        List<String> myDataFinderTypes = new ArrayList<>();
        if (selectedTypes.contains(SearchObjectType.DATAVERSES)) {
            myDataFinderTypes.add(DvObject.DATAVERSE_DTYPE_STRING);
        }
        if (selectedTypes.contains(SearchObjectType.DATASETS)) {
            myDataFinderTypes.add(DvObject.DATASET_DTYPE_STRING);
        }
        if (selectedTypes.contains(SearchObjectType.FILES)) {
            myDataFinderTypes.add(DvObject.DATAFILE_DTYPE_STRING);
        }
        return myDataFinderTypes;
    }

    private AuthenticatedUser getUserFromIdentifier(String userIdentifier) {
        return StringUtils.isEmpty(userIdentifier) ? null : authenticationService.getAuthenticatedUser(userIdentifier);
    }

    // -------------------- SETTERS --------------------

    public void setSolrErrorEncountered(boolean val) {
        this.solrErrorEncountered = val;
    }

    public void setSearchUserId(String searchUserId) {
        this.searchUserId = searchUserId;
    }

    public void setPublicationStatusFilters(List<String> filterQueries) {
        this.publicationStatusFilters = filterQueries;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setSearchResultsList(List<SolrSearchResult> searchResultsList) {
        this.searchResultsList = searchResultsList;
    }

    public void setSearchResultsCount(int searchResultsCount) {
        this.searchResultsCount = searchResultsCount;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setRf0(String rf0) {
        this.rf0 = rf0;
    }

    public void setRf1(String rf1) {
        this.rf1 = rf1;
    }

    public void setRf2(String rf2) {
        this.rf2 = rf2;
    }

    public void setRf3(String rf3) {
        this.rf3 = rf3;
    }

    public void setRf4(String rf4) {
        this.rf4 = rf4;
    }

    public void setRf5(String rf5) {
        this.rf5 = rf5;
    }

    public void setFq0(String fq0) {
        this.fq0 = fq0;
    }

    public void setFq1(String fq1) {
        this.fq1 = fq1;
    }

    public void setFq2(String fq2) {
        this.fq2 = fq2;
    }

    public void setFq3(String fq3) {
        this.fq3 = fq3;
    }

    public void setFq4(String fq4) {
        this.fq4 = fq4;
    }

    public void setFq5(String fq5) {
        this.fq5 = fq5;
    }

    public void setFq6(String fq6) {
        this.fq6 = fq6;
    }

    public void setFq7(String fq7) {
        this.fq7 = fq7;
    }

    public void setFq8(String fq8) {
        this.fq8 = fq8;
    }

    public void setFq9(String fq9) {
        this.fq9 = fq9;
    }

    public void setSelectedTypesString(String selectedTypesString) {
        this.selectedTypesString = selectedTypesString;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setPaginationGuiStart(int paginationGuiStart) {
        this.paginationGuiStart = paginationGuiStart;
    }

    public void setPaginationGuiEnd(int paginationGuiEnd) {
        this.paginationGuiEnd = paginationGuiEnd;
    }

    public void setPaginationGuiRows(int paginationGuiRows) {
        this.paginationGuiRows = paginationGuiRows;
    }

    public void setSolrIsDown(boolean solrIsDown) {
        this.solrIsDown = solrIsDown;
    }

    public void setRootDv(boolean rootDv) {
        this.rootDv = rootDv;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
