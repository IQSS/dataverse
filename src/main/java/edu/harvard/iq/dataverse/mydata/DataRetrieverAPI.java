/**
 * @todo Shouldn't this be in the "edu.harvard.iq.dataverse.api" package? Is the only one that isn't.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.impl.GetUserPermittedCollectionsCommand;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceFactory;
import edu.harvard.iq.dataverse.search.SortBy;

import java.util.*;
import java.util.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.json.*;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import edu.harvard.iq.dataverse.util.BundleUtil;
import jakarta.ws.rs.core.Response;

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
public class DataRetrieverAPI extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DataRetrieverAPI.class.getCanonicalName());

    public static final String retrieveDataFullAPIPath = "/api/v1/mydata/retrieve";
    private static final String retrieveDataPartialAPIPath = "retrieve";

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

    private void verifyAuth (ContainerRequestContext crc, String userIdentifier) throws WrappedResponse {
        searchUser = authUser = getRequestAuthenticatedUserOrDie(crc);

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
    public Response retrieveMyDataAsJsonString(
            @Context ContainerRequestContext crc,
            @QueryParam("dvobject_types") List<DvObject.DType> dvobject_types,
            @QueryParam("published_states") List<String> published_states, 
            @QueryParam("selected_page") Integer selectedPage, 
            @QueryParam("mydata_search_term") String searchTerm,             
            @QueryParam("role_ids") List<Long> roleIds, 
            @QueryParam("userIdentifier") String userIdentifier,
            @QueryParam("filter_validities") Boolean filterValidities,
            @QueryParam("dataset_valid") List<Boolean> datasetValidities) {

        String noMsgResultsFound = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

        try {
            verifyAuth(crc, userIdentifier);
        } catch (WrappedResponse wr) {
            return wr.getResponse();
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
        // (1) Initialize filterParams and check for Errors 
        // ---------------------------------
        DataverseRequest dataverseRequest = createDataverseRequest(authUser);

        
        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, dtypes, pub_states, roleIds, searchTerm, validities);
        if (filterParams.hasError()) {
            return error(Response.Status.BAD_REQUEST, filterParams.getErrorMessage());
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
            return error(Response.Status.BAD_REQUEST, myDataFinder.getErrorMessage());
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

        List<String> filterQueries = this.myDataFinder.getSolrFilterQueries();
        if (filterQueries==null){
            logger.fine("No ids found for this search");
            return error(Response.Status.NOT_FOUND, noMsgResultsFound);
        }

        try {
                solrQueryResponse = searchService.getDefaultSearchService().search(
                        dataverseRequest,
                        null, // subtree, default it to Dataverse for now
                        filterParams.getSearchTerm(),  //"*", //
                        filterQueries,//filterQueries,
                        //SearchFields.NAME_SORT, SortBy.ASCENDING,
                        SearchFields.RELEASE_OR_CREATE_DATE, SortBy.DESCENDING,
                        solrCardStart, //paginationStart,
                        true, // dataRelatedToMe
                        SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE //10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
                );

                if (this.solrQueryResponse.getNumResultsFound()==0) {
                    return error(Response.Status.NOT_FOUND, noMsgResultsFound);
                }                
                         
        } catch (SearchException ex) {
            solrQueryResponse = null;   
            logger.severe("Solr SearchException: " + ex.getMessage());
        }
        
        if (solrQueryResponse == null) {
            return error(Response.Status.BAD_REQUEST, BundleUtil.getStringFromBundle("dataretrieverAPI.solr.error"));
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

                
        jsonData.add("pagination", pager.asJsonObjectBuilderUsingCardTerms())
                .add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse, roleTagRetriever))
                .add(SearchConstants.SEARCH_API_TOTAL_COUNT, solrQueryResponse.getNumResultsFound())
                .add(SearchConstants.SEARCH_API_START, solrQueryResponse.getResultsStart())
                .add("search_term",  filterParams.getSearchTerm())
                .add("dvobject_counts", this.getDvObjectTypeCounts(solrQueryResponse))
                .add("pubstatus_counts", this.getPublicationStatusCounts(solrQueryResponse))
                .add("selected_filters", this.myDataFinder.getSelectedFilterParamsAsJSON())
            ;

        // ---------------------------------------------------------
        // We're doing ~another~ solr query here
        // NOTE!  Do not reuse this.myDataFinder after this step!! It is being passed new filterParams
        // ---------------------------------------------------------
        //jsonData.add("total_dvobject_counts", getTotalCountsFromSolrAsJSON(searchUser, this.myDataFinder));

        
        if (searchUser != null && !authUser.equals(searchUser)) {
            jsonData.add("other_user", searchUser.getIdentifier());
        }
                                
        return ok(jsonData);
    }

    @GET
    @AuthRequired
    @Path(retrieveDataPartialAPIPath + "/collectionList")
    @Produces("application/json")
    public Response retrieveMyCollectionList(@Context ContainerRequestContext crc, @QueryParam("userIdentifier") String userIdentifier) {
        try {
            verifyAuth(crc, userIdentifier);
            JsonObjectBuilder jsonObjBuilder = execCommand(new GetUserPermittedCollectionsCommand(createDataverseRequest(getRequestUser(crc)), searchUser, Permission.AddDataset.name(), true));
            return ok(jsonObjBuilder);
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
    private JsonArrayBuilder formatSolrDocs(SolrQueryResponse solrResponse, RoleTagRetriever roleTagRetriever ){
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
            
            myDataCardInfo = doc.getJsonForMyData(isValid(doc));
            
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
