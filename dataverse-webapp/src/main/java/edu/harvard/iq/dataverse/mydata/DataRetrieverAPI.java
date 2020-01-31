/**
 * @todo Shouldn't this be in the "edu.harvard.iq.dataverse.api" package? Is the only one that isn't.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.PublicationStatusCounts;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.logging.Logger;


/**
 * @author rmp553
 * <p>
 * The primary method here is this API endpoint: retrieveMyDataAsJsonString
 * <p>
 * - This method validates the current logged in user
 */
@Path("mydata")
public class DataRetrieverAPI extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DataRetrieverAPI.class.getCanonicalName());

    public static final String retrieveDataFullAPIPath = "/api/v1/mydata/retrieve";
    private static final String retrieveDataPartialAPIPath = "retrieve";

    @Inject
    DataverseSession session;

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    DvObjectServiceBean dvObjectServiceBean;
    @EJB
    SearchServiceBean searchService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    GroupServiceBean groupService;

    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private MyDataFinder myDataFinder;
    private SolrQueryResponse solrQueryResponse;
    private AuthenticatedUser authUser = null;

    public static final String JSON_SUCCESS_FIELD_NAME = "success";
    public static final String JSON_ERROR_MSG_FIELD_NAME = "error_message";
    public static final String JSON_DATA_FIELD_NAME = "data";

    public static final String MSG_NO_RESULTS_FOUND = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

    /**
     * Constructor
     */
    public DataRetrieverAPI() {

    }

    public String getRetrieveDataFullAPIPath() {
        return DataRetrieverAPI.retrieveDataFullAPIPath;
    }

    public Pager getRandomPagerPager(Integer selectedPage) {
        if (selectedPage == null) {
            selectedPage = 1;
        }

        int itemsPerPage = 10;
        int numResults = 108;//randInt(1,200);
        int numPages = numResults / itemsPerPage;
        if ((numResults % itemsPerPage) > 0) {
            numPages++;
        }
        int chosenPage = 1;
        if ((selectedPage > numPages) || (selectedPage < 1)) {
            chosenPage = 1;
        } else {
            chosenPage = selectedPage;
        }
        //int chosenPage = max(randInt(0, numPages), 1);
        return new Pager(numResults, itemsPerPage, chosenPage);

    }


    public boolean isSuperuser() {
        return (session.getUser() != null) && session.getUser().isSuperuser();
    }

    private AuthenticatedUser getUserFromIdentifier(String userIdentifier) {

        if ((userIdentifier == null) || (userIdentifier.isEmpty())) {
            return null;
        }
        return authenticationService.getAuthenticatedUser(userIdentifier);
    }

    private String getJSONErrorString(String jsonMsg, String optionalLoggerMsg) {

        if (jsonMsg == null) {
            throw new NullPointerException("jsonMsg cannot be null");
        }
        if (optionalLoggerMsg != null) {
            logger.severe(optionalLoggerMsg);
        }
        JsonObjectBuilder jsonData = Json.createObjectBuilder();

        jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
        jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, jsonMsg);

        return jsonData.build().toString();

    }


    /**
     * @todo This should support the "X-Dataverse-key" header like the other
     * APIs.
     */
    @Path(retrieveDataPartialAPIPath)
    @GET
    @Produces({"application/json"})
    public String retrieveMyDataAsJsonString(@QueryParam("dvobject_types") List<String> dvobject_types,
                                             @QueryParam("published_states") List<String> published_states,
                                             @QueryParam("selected_page") Integer selectedPage,
                                             @QueryParam("mydata_search_term") String searchTerm,
                                             @QueryParam("role_ids") List<Long> roleIds,
                                             @QueryParam("userIdentifier") String userIdentifier,
                                             @QueryParam("key") String apiToken) { //String myDataParams) {
        //System.out.println("_YE_OLDE_QUERY_COUNTER_");
        //msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes
        boolean DEBUG_MODE = false;
        boolean OTHER_USER = false;


        // For, superusers, the searchUser may differ from the authUser
        //
        AuthenticatedUser searchUser = null;

        if (DEBUG_MODE == true) {      // DEBUG: use userIdentifier
            authUser = getUserFromIdentifier(userIdentifier);
            if (authUser == null) {
                return this.getJSONErrorString("Requires authentication", "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
            }
        } else if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();

            // If person is a superuser, see if a userIdentifier has been specified
            // and use that instead
            if ((authUser.isSuperuser()) && (userIdentifier != null) && (!userIdentifier.isEmpty())) {
                searchUser = getUserFromIdentifier(userIdentifier);
                if (searchUser != null) {
                    authUser = searchUser;
                    OTHER_USER = true;
                } else {
                    return this.getJSONErrorString("No user found for: \"" + userIdentifier + "\"", null);
                }
            }
        } else if (apiToken != null) {      // Is this being accessed by an API Token?

            authUser = findUserByApiToken(apiToken);
            if (authUser == null) {
                return this.getJSONErrorString("Requires authentication.  Please login.", "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
            } else {
                // If person is a superuser, see if a userIdentifier has been specified 
                // and use that instead
                if ((authUser.isSuperuser()) && (userIdentifier != null) && (!userIdentifier.isEmpty())) {
                    searchUser = getUserFromIdentifier(userIdentifier);
                    if (searchUser != null) {
                        authUser = searchUser;
                        OTHER_USER = true;
                    } else {
                        return this.getJSONErrorString("No user found for: \"" + userIdentifier + "\"", null);
                    }
                }

            }

        } else {
            return this.getJSONErrorString("Requires authentication.  Please login.", "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
        }

        roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);


        List<String> dtypes;
        if (dvobject_types != null) {
            dtypes = dvobject_types;
        } else {
            dtypes = MyDataFilterParams.defaultDvObjectTypes;
        }
        List<String> pub_states = null;
        if (published_states != null) {
            pub_states = published_states;
        }

        // ---------------------------------
        // (1) Initialize filterParams and check for Errors 
        // ---------------------------------
        DataverseRequest dataverseRequest = createDataverseRequest(authUser);


        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, dtypes, pub_states, roleIds, searchTerm);
        if (filterParams.hasError()) {
            return this.getJSONErrorString(filterParams.getErrorMessage(), filterParams.getErrorMessage());
        }

        // ---------------------------------
        // (2) Initialize MyDataFinder and check for Errors 
        // ---------------------------------
        myDataFinder = new MyDataFinder(rolePermissionHelper,
                                        roleAssigneeService,
                                        dvObjectServiceBean,
                                        groupService);
        this.myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()) {
            return this.getJSONErrorString(myDataFinder.getErrorMessage(), myDataFinder.getErrorMessage());
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = 1;
        if (selectedPage != null) {
            paginationStart = selectedPage;
        }
        int solrCardStart = (paginationStart - 1) * SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;

        // Default the searchUser to the authUser.
        // The exception: for logged-in superusers, the searchUser may differ from the authUser
        //
        if (searchUser == null) {
            searchUser = authUser;
        }

        //msg("search with user: " + searchUser.getIdentifier());

        SearchForTypes typesToSearch = filterParams.getSolrFragmentForDvObjectType();
        
        List<String> filterQueries = this.myDataFinder.getSolrFilterQueries();
        if (filterQueries == null) {
            logger.fine("No ids found for this search");
            return this.getJSONErrorString(DataRetrieverAPI.MSG_NO_RESULTS_FOUND, null);
        }
        //msgt("myDataFinder.getSolrFilterQueries(): " + myDataFinder.getSolrFilterQueries().toString());

        //msg("Selected paginationStart: " + paginationStart);

        try {
            solrQueryResponse = searchService.search(
                    dataverseRequest,
                    null, // subtree, default it to Dataverse for now
                    filterParams.getSearchTerm(),  //"*", //
                    typesToSearch,
                    filterQueries,//filterQueries,
                    //SearchFields.NAME_SORT, SortBy.ASCENDING,
                    SearchFields.RELEASE_OR_CREATE_DATE, SortOrder.desc,
                    solrCardStart, //paginationStart,
                    true, // dataRelatedToMe
                    SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE //10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
            );

            //msgt("getResultsStart: " + this.solrQueryResponse.getResultsStart());
            //msgt("getNumResultsFound: " + this.solrQueryResponse.getNumResultsFound());
            //msgt("getSolrSearchResults: " + this.solrQueryResponse.getSolrSearchResults().toString());
            if (this.solrQueryResponse.getNumResultsFound() == 0) {
                return this.getJSONErrorString(DataRetrieverAPI.MSG_NO_RESULTS_FOUND, null);
            }

        } catch (SearchException ex) {
            solrQueryResponse = null;
            logger.severe("Solr SearchException: " + ex.getMessage());
        }

        if (solrQueryResponse == null) {
            return this.getJSONErrorString("Sorry!  There was an error with the search service.", "Sorry!  There was a SOLR Error");
        }

        // ---------------------------------
        // (4) Build JSON document including:
        //      - Pager
        //      - Formatted solr docs
        //      - Num results found
        //      - Search term
        //      - DvObject counts
        // ---------------------------------

        // Initialize JSON response
        JsonObjectBuilder jsonData = Json.createObjectBuilder();

        Pager pager = new Pager(solrQueryResponse.getNumResultsFound().intValue(),
                                SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE,
                                paginationStart);

        RoleTagRetriever roleTagRetriever = new RoleTagRetriever(this.rolePermissionHelper, this.roleAssigneeSvc, this.dvObjectServiceBean);
        roleTagRetriever.loadRoles(dataverseRequest, solrQueryResponse);


        jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, true)
                .add(DataRetrieverAPI.JSON_DATA_FIELD_NAME,
                     Json.createObjectBuilder()
                             .add("pagination", pager.asJsonObjectBuilderUsingCardTerms())
                             //.add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse, filterParams, this.myDataFinder))
                             .add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse, roleTagRetriever))
                             .add(SearchConstants.SEARCH_API_TOTAL_COUNT, solrQueryResponse.getNumResultsFound())
                             .add(SearchConstants.SEARCH_API_START, solrQueryResponse.getResultsStart())
                             .add("search_term", filterParams.getSearchTerm())
                             .add("dvobject_counts", this.getDvObjectTypeCounts(solrQueryResponse))
                             .add("pubstatus_counts", this.getPublicationStatusCounts(solrQueryResponse))
                             .add("selected_filters", this.myDataFinder.getSelectedFilterParamsAsJSON())
                );

        // ---------------------------------------------------------
        // We're doing ~another~ solr query here
        // NOTE!  Do not reuse this.myDataFinder after this step!! It is being passed new filterParams
        // ---------------------------------------------------------
        //jsonData.add("total_dvobject_counts", getTotalCountsFromSolrAsJSON(searchUser, this.myDataFinder));


        if (OTHER_USER == true) {
            jsonData.add("other_user", searchUser.getIdentifier());
        }

        return jsonData.build().toString();
    }

    private JsonObjectBuilder getDvObjectTypeCounts(SolrQueryResponse solrResponse) {

        if (solrQueryResponse == null) {
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: solrQueryResponse should not be null");
            return null;
        }
        DvObjectCounts dvObjectCounts = solrResponse.getDvObjectCounts();
        
        return Json.createObjectBuilder()
                .add("dataverses_count", dvObjectCounts.getDataversesCount())
                .add("datasets_count", dvObjectCounts.getDatasetsCount())
                .add("files_count", dvObjectCounts.getDatafilesCount());
    }

    private JsonObjectBuilder getPublicationStatusCounts(SolrQueryResponse solrResponse) {

        if (solrQueryResponse == null) {
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: solrQueryResponse should not be null");
            return null;
        }
        
        PublicationStatusCounts statusCounts = solrResponse.getPublicationStatusCounts();
        return Json.createObjectBuilder()
                .add("in_review_count", statusCounts.getInReviewCount())
                .add("unpublished_count", statusCounts.getUnpublishedCount())
                .add("published_count", statusCounts.getPublishedCount())
                .add("draft_count", statusCounts.getDraftCount())
                .add("deaccessioned_count", statusCounts.getDeaccessionedCount());
    }

    /**
     * Using RoleTagRetriever to find role names for each card
     * Trying to minimize extra queries
     *
     * @param solrResponse
     * @param roleTagRetriever
     * @return
     */
    private JsonArrayBuilder formatSolrDocs(SolrQueryResponse solrResponse, RoleTagRetriever roleTagRetriever) {
        if (solrResponse == null) {
            throw new NullPointerException("DataRetrieverAPI.formatSolrDocs:  solrResponse should not be null");
        }
        if (roleTagRetriever == null) {
            throw new NullPointerException("DataRetrieverAPI.formatSolrDocs:  roleTagRetriever should not be null");
        }

        JsonArrayBuilder jsonSolrDocsArrayBuilder = Json.createArrayBuilder();

        JsonObjectBuilder myDataCardInfo;
        JsonArrayBuilder rolesForCard;

        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()) {
            // -------------------------------------------
            // (a) Get core card data from solr
            // -------------------------------------------
            myDataCardInfo = doc.getJsonForMyData();

            if (!doc.getEntity().isInstanceofDataFile()) {
                String parentAlias = dataverseDao.getParentAliasString(doc);
                myDataCardInfo.add("parent_alias", parentAlias);
            }

            // -------------------------------------------
            // (b) Add role info
            // -------------------------------------------
            rolesForCard = roleTagRetriever.getRolesForCardAsJSON(doc.getEntityId());
            if (rolesForCard != null) {
                myDataCardInfo.add("user_roles", rolesForCard);
            }

            // -------------------------------------------
            // (c) Add final MyData JSON to array
            // -------------------------------------------
            jsonSolrDocsArrayBuilder.add(myDataCardInfo);
        }
        return jsonSolrDocsArrayBuilder;

    }
}        