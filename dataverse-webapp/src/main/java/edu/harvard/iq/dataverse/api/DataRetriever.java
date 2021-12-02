package edu.harvard.iq.dataverse.api;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.mydata.MyDataFinder;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.mydata.RoleTagRetriever;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.search.query.SearchForTypes;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;
import edu.harvard.iq.dataverse.search.response.DvObjectCounts;
import edu.harvard.iq.dataverse.search.response.PublicationStatusCounts;
import edu.harvard.iq.dataverse.search.response.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.response.SolrSearchResult;
import org.apache.commons.lang3.StringUtils;

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
 * The primary method here is this API endpoint: retrieveMyDataAsJsonString
 * - This method validates the current logged in user
 */
@Path("mydata")
public class DataRetriever extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DataRetriever.class.getCanonicalName());

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
    @Inject
    private RoleAssigneeServiceBean roleAssigneeSvc;

    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private MyDataFinder myDataFinder;
    private SolrQueryResponse solrQueryResponse;
    private AuthenticatedUser authUser = null;

    public static final String JSON_SUCCESS_FIELD_NAME = "success";
    public static final String JSON_ERROR_MSG_FIELD_NAME = "error_message";
    public static final String JSON_DATA_FIELD_NAME = "data";

    public static final String MSG_NO_RESULTS_FOUND = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

    // -------------------- CONSTRUCTORS --------------------

    public DataRetriever() { }

    // -------------------- LOGIC --------------------

    public boolean isSuperuser() {
        return (session.getUser() != null) && session.getUser().isSuperuser();
    }

    /**
     * @todo This should support the "X-Dataverse-key" header like the other APIs.
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
                                             @QueryParam("key") String apiToken) {
        boolean DEBUG_MODE = false;
        boolean OTHER_USER = false;

        // For superusers the searchUser may differ from the authUser
        AuthenticatedUser searchUser = null;


        if (DEBUG_MODE) { // DEBUG: use userIdentifier
            authUser = getUserFromIdentifier(userIdentifier);
            if (authUser == null) {
                return this.getJSONErrorString("Requires authentication", "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
            }
        } else if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();

            // If person is a superuser, see if a userIdentifier has been specified
            // and use that instead
            if (authUser.isSuperuser() && StringUtils.isNotEmpty(userIdentifier)) {
                searchUser = getUserFromIdentifier(userIdentifier);
                if (searchUser == null) {
                    return this.getJSONErrorString("No user found for: \"" + userIdentifier + "\"", null);
                }
                authUser = searchUser;
                OTHER_USER = true;
            }
        } else if (apiToken != null) { // Is this being accessed by an API Token?
            authUser = findUserByApiToken(apiToken);
            if (authUser == null) {
                return getJSONErrorString("Requires authentication.  Please login.",
                        "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
            }
            // If person is a superuser, see if a userIdentifier has been specified
            // and use that instead
            if (authUser.isSuperuser() && StringUtils.isNotEmpty(userIdentifier)) {
                searchUser = getUserFromIdentifier(userIdentifier);
                if (searchUser == null) {
                    return getJSONErrorString("No user found for: \"" + userIdentifier + "\"", null);
                }
                authUser = searchUser;
                OTHER_USER = true;
            }
        } else {
            return this.getJSONErrorString("Requires authentication.  Please login.",
                    "retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
        }

        roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        List<String> dtypes = dvobject_types != null ? dvobject_types : MyDataFilterParams.defaultDvObjectTypes;

        // ---------------------------------
        // (1) Initialize filterParams and check for Errors
        // ---------------------------------
        DataverseRequest dataverseRequest = createDataverseRequest(authUser);

        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, dtypes, published_states, roleIds, searchTerm);
        if (filterParams.hasError()) {
            return this.getJSONErrorString(filterParams.getErrorMessage(), filterParams.getErrorMessage());
        }

        // ---------------------------------
        // (2) Initialize MyDataFinder and check for Errors
        // ---------------------------------
        myDataFinder = new MyDataFinder(rolePermissionHelper, roleAssigneeService, dvObjectServiceBean);
        this.myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()) {
            return this.getJSONErrorString(myDataFinder.getErrorMessage(), myDataFinder.getErrorMessage());
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = selectedPage != null ? selectedPage : 1;
        int solrCardStart = (paginationStart - 1) * SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;

        // Default the searchUser to the authUser.
        // The exception: for logged-in superusers, the searchUser may differ from the authUser
        if (searchUser == null) {
            searchUser = authUser;
        }
        SearchForTypes typesToSearch = filterParams.getSolrFragmentForDvObjectType();

        List<String> filterQueries = myDataFinder.getSolrFilterQueries();
        if (filterQueries == null) {
            logger.fine("No ids found for this search");
            return this.getJSONErrorString(DataRetriever.MSG_NO_RESULTS_FOUND, null);
        }

        try {
            solrQueryResponse = searchService.search(
                    dataverseRequest,
                    null, // subtree, default it to Dataverse for now
                    filterParams.getSearchTerm(),
                    typesToSearch,
                    filterQueries,
                    SearchFields.RELEASE_OR_CREATE_DATE, SortOrder.desc,
                    solrCardStart,
                    SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE,
                    false
            );

            if (this.solrQueryResponse.getNumResultsFound() == 0) {
                return this.getJSONErrorString(DataRetriever.MSG_NO_RESULTS_FOUND, null);
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

        Pager pager = new Pager(
                solrQueryResponse.getNumResultsFound().intValue(), SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE, paginationStart);

        RoleTagRetriever roleTagRetriever = new RoleTagRetriever(this.rolePermissionHelper, this.roleAssigneeSvc, this.dvObjectServiceBean);
        roleTagRetriever.loadRoles(dataverseRequest, solrQueryResponse);

        jsonData.add(DataRetriever.JSON_SUCCESS_FIELD_NAME, true)
                .add(DataRetriever.JSON_DATA_FIELD_NAME,
                     Json.createObjectBuilder()
                             .add("pagination", pager.asJsonObjectBuilderUsingCardTerms())
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

        if (OTHER_USER) {
            jsonData.add("other_user", searchUser.getIdentifier());
        }

        return jsonData.build().toString();
    }

    // -------------------- PRIVATE --------------------

    private AuthenticatedUser getUserFromIdentifier(String userIdentifier) {
        return userIdentifier == null || userIdentifier.isEmpty()
                ? null : authenticationService.getAuthenticatedUser(userIdentifier);
    }

    private String getJSONErrorString(String jsonMsg, String optionalLoggerMsg) {
        if (jsonMsg == null) {
            throw new NullPointerException("jsonMsg cannot be null");
        }
        if (optionalLoggerMsg != null) {
            logger.severe(optionalLoggerMsg);
        }
        JsonObjectBuilder jsonData = Json.createObjectBuilder();

        jsonData.add(DataRetriever.JSON_SUCCESS_FIELD_NAME, false);
        jsonData.add(DataRetriever.JSON_ERROR_MSG_FIELD_NAME, jsonMsg);

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

            if (!SearchObjectType.FILES.equals(doc.getType())) {
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