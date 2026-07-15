/**
 * @todo Shouldn't this be in the "edu.harvard.iq.dataverse.api" package? Is the only one that isn't.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.engine.command.impl.GetUserPermittedCollectionsCommand;
import edu.harvard.iq.dataverse.search.*;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.settings.FeatureFlags;
import edu.harvard.iq.dataverse.util.json.JsonPrinter;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 *
 * @author rmp553
 * 
 *  The primary method here is this API endpoint: retrieveMyDataAsJsonString
 *
 *  - This method validates the current logged in user
 * 
 */
@Path("mydata")
@Tag(name = "Users", description = "User-specific data discovery and collection access operations.")
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
    SearchServiceFactory searchService;
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    GroupServiceBean groupService;
    
    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private MyDataFinder myDataFinder;
    private SolrQueryResponse solrQueryResponse;
    private AuthenticatedUser authUser = null;
    private AuthenticatedUser searchUser = null;

    public static final String JSON_SUCCESS_FIELD_NAME = "success";
    public static final String JSON_ERROR_MSG_FIELD_NAME = "error_message";
    public static final String JSON_MSG_FIELD_NAME = "message";
    public static final String JSON_DATA_FIELD_NAME = "data";

    /**
     * Constructor
     * 
     */
    public DataRetrieverAPI(){
           
    }

    private AuthenticatedUser getUserFromIdentifier(String userIdentifier){
        
        if ((userIdentifier==null)||(userIdentifier.isEmpty())){
            return null;
        }
        return authenticationService.getAuthenticatedUser(userIdentifier);
    }

    private String getJSONErrorString(String jsonMsg, String optionalLoggerMsg){

        if (jsonMsg == null){
            throw new NullPointerException("jsonMsg cannot be null");
        }
        if (optionalLoggerMsg != null){
            logger.severe(optionalLoggerMsg);
        }
        JsonObjectBuilder jsonData = Json.createObjectBuilder();

        jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
        jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, jsonMsg);

        return jsonData.build().toString();

    }

    private void verifyAuth (ContainerRequestContext crc, String userIdentifier) throws WrappedResponse {
        // Handle calls from JSF where the User is in the session
        User requestUser = getRequestUser(crc);
        boolean checkSession = !FeatureFlags.API_SESSION_AUTH.enabled() && (requestUser instanceof GuestUser);
        if (checkSession && session != null && session.getUser() != null) {
            searchUser = authUser = (AuthenticatedUser) session.getUser();
            if (!authUser.isAuthenticated()) {
                throw new WrappedResponse(authenticatedUserRequired());
            }
        } else {
            searchUser = authUser = getRequestAuthenticatedUserOrDie(crc);
        }

        // If the user is a superuser, see if a userIdentifier has been specified and use that instead
        if ((authUser.isSuperuser()) && (userIdentifier != null) && (!userIdentifier.isEmpty())) {
            searchUser = getUserFromIdentifier(userIdentifier);
            if (searchUser == null) {
                throw new WrappedResponse(error(Response.Status.NOT_FOUND, BundleUtil.getStringFromBundle("dataretrieverAPI.user.not.found", Arrays.asList(userIdentifier))));
            }
        }
    }

    @GET
    @AuthRequired
    @Path(retrieveDataPartialAPIPath)
    @Produces({"application/json"})
    @Operation(summary = "Retrieves My Data results",
            description = "Returns datasets, files, and collections visible to the requester with filters for object type, publication state, role, validity, search text, and pagination.")
    public String retrieveMyDataAsJsonString(
            @Context ContainerRequestContext crc,
            @Parameter(description = "Dataverse object type filters.") @QueryParam("dvobject_types") List<DvObject.DType> dvobject_types,
            @Parameter(description = "Publication state filters.") @QueryParam("published_states") List<String> published_states,
            @Parameter(description = "Metadata fields to include.") @QueryParam("metadata_fields") List<String> metadataFields,
            @Parameter(description = "Selected results page.") @QueryParam("selected_page") Integer selectedPage, 
            @Parameter(description = "Search term for My Data results.") @QueryParam("mydata_search_term") String searchTerm,             
            @Parameter(description = "Role id filters.") @QueryParam("role_ids") List<Long> roleIds, 
            @Parameter(description = "User identifier filter.") @QueryParam("userIdentifier") String userIdentifier,
            @Parameter(description = "Validity filters for My Data results.") @QueryParam("filter_validities") Boolean filterValidities,
            @Parameter(description = "Dataset validity filter.") @QueryParam("dataset_valid") List<Boolean> datasetValidities,
            @Parameter(description = "Whether collection results are included.") @QueryParam("show_collections") boolean showCollections,
            @Parameter(description = "Sort field.") @QueryParam("sort") String sortField,
            @Parameter(description = "Sort order.") @QueryParam("order") String sortOrder,
            @Parameter(description = "Filter query.") @QueryParam("fq") final List<String> filterQueries) {
        boolean otherUser;

        String noMsgResultsFound = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

        try {
            verifyAuth(crc, userIdentifier);
            otherUser = !authUser.equals(searchUser);
        } catch (WrappedResponse wr) {
            return this.getJSONErrorString((new JSONObject(wr.getResponse().getEntity().toString())).getString("message"), null);
        }

        roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

        List<DvObject.DType> dtypes;
        if (dvobject_types != null){
            dtypes = dvobject_types;
        }else{
            dtypes = MyDataFilterParams.defaultDvObjectTypes;
        }
        List<String> pub_states = null;
        if (published_states != null){
            pub_states = published_states;
        }
        List<Boolean> validities = null;
        if (filterValidities != null && filterValidities){
            validities = datasetValidities;
        }
        
        // ---------------------------------
        // (1) Initialize filterParams and MyDataFinder and check for Errors
        // ---------------------------------
        DataverseRequest dataverseRequest = createDataverseRequest(authUser);
        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, dtypes, pub_states, roleIds, searchTerm, validities);
        myDataFinder = new MyDataFinder(rolePermissionHelper, roleAssigneeService, dvObjectServiceBean, groupService);
        myDataFinder.runFindDataSteps(filterParams);

        if (filterParams.hasError()) {
            return myDataAsJson(filterParams.getErrorMessage()).build().toString();
        }
        if (myDataFinder.hasError()) {
            return myDataAsJson(myDataFinder.getErrorMessage()).build().toString();
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = 1;
        if (selectedPage != null){
            paginationStart = selectedPage;
        }
        int solrCardStart = (paginationStart - 1) * SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;

        //msg("search with user: " + searchUser.getIdentifier());

        List<String> defaultFilterQueries = this.myDataFinder.getSolrFilterQueries();
        if (defaultFilterQueries==null){
            logger.fine("No ids found for this search");
            return myDataAsJson(noMsgResultsFound).build().toString();
        }
        filterQueries.addAll(defaultFilterQueries);

        SortBy sortBy;
        try {
            sortBy = SearchUtil.getSortBy(sortField, sortOrder, SearchFields.RELEASE_OR_CREATE_DATE);
        } catch (Exception ex) {
            return this.getJSONErrorString(ex.getLocalizedMessage(), null);
        }

        try {
                solrQueryResponse = searchService.getDefaultSearchService().search(
                        dataverseRequest,
                        null, // subtree, default it to Dataverse for now
                        filterParams.getSearchTerm(),  //"*", //
                        filterQueries,//filterQueries,
                        sortBy.getField(),
                        sortBy.getOrder(),
                        solrCardStart, //paginationStart,
                        true, // dataRelatedToMe
                        SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE, //10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
                        true,
                        null,
                        null,
                        true,
                        true,
                        showCollections
                );

            if (this.solrQueryResponse.getNumResultsFound()==0){
                return myDataAsJson(noMsgResultsFound).build().toString();
            }

        } catch (SearchException ex) {
            solrQueryResponse = null;   
            logger.severe("Solr SearchException: " + ex.getMessage());
        }
        
        if (solrQueryResponse == null) {
            return this.getJSONErrorString(
                    BundleUtil.getStringFromBundle("dataretrieverAPI.solr.error"),
                    BundleUtil.getStringFromBundle("dataretrieverAPI.solr.error.opt")
            );
        }
                
         // ---------------------------------
        // (4) Build JSON document including:
        //      - Pager
        //      - Formatted solr docs
        //      - Num results found
        //      - Search term
        //      - DvObject counts
        // ---------------------------------

        Pager pager = new Pager(solrQueryResponse.getNumResultsFound().intValue(),
                SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE,
                paginationStart);

        RoleTagRetriever roleTagRetriever = new RoleTagRetriever(this.rolePermissionHelper, this.roleAssigneeSvc, this.dvObjectServiceBean);
        roleTagRetriever.loadRoles(dataverseRequest, solrQueryResponse);

        JsonObjectBuilder jsonData = myDataAsJson(null, pager, roleTagRetriever, metadataFields);

        // ---------------------------------------------------------
        // We're doing ~another~ solr query here
        // NOTE!  Do not reuse this.myDataFinder after this step!! It is being passed new filterParams
        // ---------------------------------------------------------
        //jsonData.add("total_dvobject_counts", getTotalCountsFromSolrAsJSON(searchUser, this.myDataFinder));


        if (otherUser){
            jsonData.add("other_user", searchUser.getIdentifier());
        }

        return jsonData.build().toString();
    }

    // For empty data to prevent null pointer exceptions in all the dependencies
    private JsonObjectBuilder myDataAsJson(String message) {
        solrQueryResponse = new SolrQueryResponse(null);
        return myDataAsJson(message, new Pager(0, SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE, 1),
                new RoleTagRetriever(this.rolePermissionHelper, this.roleAssigneeSvc, this.dvObjectServiceBean), List.of());
    }

    private JsonObjectBuilder myDataAsJson(String message, Pager pager, RoleTagRetriever roleTagRetriever, List<String> metadataFields) {
        JsonObjectBuilder jsonData = Json.createObjectBuilder().add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, true);
        if (message != null) {
            jsonData.add(DataRetrieverAPI.JSON_MSG_FIELD_NAME, message);
        }
        jsonData.add(DataRetrieverAPI.JSON_DATA_FIELD_NAME,
                Json.createObjectBuilder()
                        .add("pagination", pager.asJsonObjectBuilderUsingCardTerms())
                        .add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse, roleTagRetriever, metadataFields))
                        .add(SearchConstants.SEARCH_API_TOTAL_COUNT, solrQueryResponse.getNumResultsFound())
                        .add(SearchConstants.SEARCH_API_START, solrQueryResponse.getResultsStart())
                        .add("search_term",  myDataFinder.filterParams.getSearchTerm())
                        .add("dvobject_counts", this.getDvObjectTypeCounts(solrQueryResponse))
                        .add("pubstatus_counts", this.getPublicationStatusCounts(solrQueryResponse))
                        .add("selected_filters", this.myDataFinder.getSelectedFilterParamsAsJSON())
                );
        return jsonData;
    }

    @GET
    @AuthRequired
    @Path(retrieveDataPartialAPIPath + "/collectionList")
    @Produces("application/json")
    @Operation(summary = "Lists collections for My Data",
            description = "Returns collections where the requester or selected user may add datasets.")
    public Response retrieveMyCollectionList(@Context ContainerRequestContext crc, @Parameter(description = "User identifier filter.") @QueryParam("userIdentifier") String userIdentifier) {
        try {
            verifyAuth(crc, userIdentifier);
            List<Dataverse> collections = execCommand(new GetUserPermittedCollectionsCommand(createDataverseRequest(getRequestUser(crc)), searchUser, Permission.AddDataset.name()));
            return ok(JsonPrinter.jsonArray(collections));
        } catch (WrappedResponse wr) {
            return wr.getResponse();
        }
    }

    private JsonObjectBuilder getDvObjectTypeCounts(SolrQueryResponse solrResponse) {

        if (solrQueryResponse == null) {
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: solrQueryResponse should not be null");
            return null;
        }
        return solrResponse.getDvObjectCountsAsJSON();
    }
    
    private JsonObjectBuilder getPublicationStatusCounts(SolrQueryResponse solrResponse) {

        if (solrQueryResponse == null) {
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: solrQueryResponse should not be null");
            return null;
        }
        return solrResponse.getPublicationStatusCountsAsJSON();
    }
    
    /**
     * Using RoleTagRetriever to find role names for each card
     * Trying to minimize extra queries
     * 
     * @param solrResponse
     * @param roleTagRetriever
     * @return 
     */
    private JsonArrayBuilder formatSolrDocs(SolrQueryResponse solrResponse, RoleTagRetriever roleTagRetriever, List<String> metadataFields){
        if (solrResponse == null){
            throw new NullPointerException("DataRetrieverAPI.formatSolrDocs:  solrResponse should not be null");     
        }
        if(roleTagRetriever==null){
            throw new NullPointerException("DataRetrieverAPI.formatSolrDocs:  roleTagRetriever should not be null");     
        }

        JsonArrayBuilder jsonSolrDocsArrayBuilder = Json.createArrayBuilder();

        JsonObjectBuilder myDataCardInfo;
        JsonArrayBuilder rolesForCard;
        
        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()){
            // -------------------------------------------
            // (a) Get core card data from solr
            // -------------------------------------------
            
            myDataCardInfo = doc.getJsonForMyData(isValid(doc), metadataFields);
            
            if (doc.getEntity() != null && !doc.getEntity().isInstanceofDataFile()){
                String parentAlias = dataverseService.getParentAliasString(doc);
                myDataCardInfo.add("parent_alias",parentAlias);
            }
            
            // -------------------------------------------
            // (b) Add role info
            // -------------------------------------------
            rolesForCard = roleTagRetriever.getRolesForCardAsJSON(doc.getEntityId());
            if (rolesForCard!=null){
                myDataCardInfo.add("user_roles", rolesForCard);
            }
            
            // -------------------------------------------
            // (c) Add final MyData JSON to array
            // -------------------------------------------
            jsonSolrDocsArrayBuilder.add(myDataCardInfo);
        }
        return jsonSolrDocsArrayBuilder;
        
    }

    private boolean isValid(SolrSearchResult result) {
        return result.isValid(x -> true);
    }
}        
