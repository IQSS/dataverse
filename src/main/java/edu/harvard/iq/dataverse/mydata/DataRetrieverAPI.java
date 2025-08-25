/**
 * @todo Shouldn't this be in the "edu.harvard.iq.dataverse.api" package? Is the only one that isn't.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.api.auth.AuthRequired;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
//import edu.harvard.iq.dataverse.authorization.MyDataQueryHelperServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SearchServiceFactory;
import edu.harvard.iq.dataverse.search.SortBy;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
import org.apache.commons.lang3.StringUtils;

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
    //@EJB
    //MyDataQueryHelperServiceBean myDataQueryHelperServiceBean;
    @EJB
    GroupServiceBean groupService;
    @EJB
    DatasetServiceBean datasetService;
    
    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private MyDataFinder myDataFinder;
    private SolrQueryResponse solrQueryResponse;
    private AuthenticatedUser authUser = null;

    public static final String JSON_SUCCESS_FIELD_NAME = "success";
    public static final String JSON_ERROR_MSG_FIELD_NAME = "error_message";
    public static final String JSON_DATA_FIELD_NAME = "data";

    /**
     * Constructor
     * 
     */
    public DataRetrieverAPI(){
           
    }
    
    public String getRetrieveDataFullAPIPath(){
        return DataRetrieverAPI.retrieveDataFullAPIPath;
    }
    
    public Pager getRandomPagerPager(Integer selectedPage){
        if (selectedPage == null){
            selectedPage = 1;
        }
        
        int itemsPerPage = 10;
        int numResults = 108;//randInt(1,200);
        int numPages =  numResults / itemsPerPage;
        if ((numResults % itemsPerPage) > 0){
            numPages++;
        }
        int chosenPage = 1;
        if ((selectedPage > numPages)||(selectedPage < 1)){
            chosenPage = 1;
        }else{
            chosenPage = selectedPage;
        }
        //int chosenPage = max(randInt(0, numPages), 1);
        return new Pager(numResults, itemsPerPage, chosenPage);
                
    }
    
    /*
    @Path("test-it2")
    @GET
    @Produces({"application/json"})
    public String retrieveTestPager(@QueryParam("selectedPage") int selectedPage){
        
        return this.getRandomPagerPager(selectedPage).asJSONString();
    }
    */
    //private String getUserIdentifier()
    
    
    public boolean isSuperuser(){
        return (session.getUser() != null) && session.getUser().isSuperuser();
    }
    
    private AuthenticatedUser getUserFromIdentifier(String userIdentifier){
        
        if ((userIdentifier==null)||(userIdentifier.isEmpty())){
            return null;
        }
        return authenticationService.getAuthenticatedUser(userIdentifier);
    }
    
    public Map<String, Long> getTotalCountsFromSolrAsJavaMap(DataverseRequest dataverseRequest, MyDataFinder myDataFinder ){
        //msgt("getTotalCountsFromSolrAsJavaMap: " + searchUser.getIdentifier());
        SolrQueryResponse solrQueryResponseForCounts = getTotalCountsFromSolr(dataverseRequest, myDataFinder);
        if (solrQueryResponseForCounts == null){
            logger.severe("DataRetrieverAPI.getTotalCountsFromSolrAsJSON: solrQueryResponseForCounts should not be null");
            return null;
        }
        return solrQueryResponseForCounts.getDvObjectCounts();
    }
    
    public JsonObjectBuilder getTotalCountsFromSolrAsJSON(DataverseRequest dataverseRequest, MyDataFinder myDataFinder ){
    
        SolrQueryResponse solrQueryResponseForCounts = getTotalCountsFromSolr(dataverseRequest, myDataFinder);
        if (solrQueryResponseForCounts == null){
            logger.severe("DataRetrieverAPI.getTotalCountsFromSolrAsJSON: solrQueryResponseForCounts should not be null");
            return null;
        }
        return solrQueryResponseForCounts.getDvObjectCountsAsJSON();
    }
    
    
    private SolrQueryResponse getTotalCountsFromSolr(DataverseRequest dataverseRequest, MyDataFinder myDataFinder){
        //msgt("getTotalCountsFromSolr: " + searchUser.getIdentifier());

        if (myDataFinder == null){
            throw new NullPointerException("myDataFinder cannot be null");
        }
        if (dataverseRequest == null){
            throw new NullPointerException("dataverseRequest cannot be null");
        }
        
        // -------------------------------------------------------
        // Create new filter params that only check by the User 
        // -------------------------------------------------------
        MyDataFilterParams filterParams = new MyDataFilterParams(dataverseRequest, myDataFinder.getRolePermissionHelper());
        if (filterParams.hasError()){
            logger.severe("getTotalCountsFromSolr. filterParams error: " + filterParams.getErrorMessage());           
            return null;
        }
       
        // -------------------------------------------------------
        // Re-run all of the entity queries (sigh)
        // -------------------------------------------------------
        myDataFinder.initFields();
        myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()){
            logger.severe("getTotalCountsFromSolr. myDataFinder error: " + myDataFinder.getErrorMessage());           
            return null;            
        }
        
        // -------------------------------------------------------
        // Generate filterQueries for total counts
        // -------------------------------------------------------
        List<String> filterQueries = myDataFinder.getSolrFilterQueriesForTotalCounts();
        if (filterQueries==null){
            logger.severe("getTotalCountsFromSolr. filterQueries was null!");
            return null;
        }
        //msgt("getTotalCountsFromSolr");
        //msgt(StringUtils.join(filterQueries, " AND "));
        
        // -------------------------------------------------------
        // Run Solr
        // -------------------------------------------------------
        SolrQueryResponse solrQueryResponseForCounts;
        try {
            solrQueryResponseForCounts = searchService.getDefaultSearchService().search(
                    dataverseRequest,
                    null, // subtree, default it to Dataverse for now
                    "*",  //    Get everything--always
                    filterQueries,//filterQueries,
                    SearchFields.NAME_SORT, SortBy.ASCENDING,
                    //SearchFields.RELEASE_OR_CREATE_DATE, SortBy.DESCENDING,
                    0, //paginationStart,
                    true, // dataRelatedToMe
                    SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE, //10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
                    true, 
                    null,
                    null,
                    false, // no need to request facets here ...
                    false, // ... same for highlights
                    false  // ... same for collections
            );
        } catch (SearchException ex) {
            logger.severe("Search for total counts failed with filter query");
            logger.severe("filterQueries: " + StringUtils.join(filterQueries, "(separator)"));
            return null;
        }
        return solrQueryResponseForCounts;
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


    @GET
    @AuthRequired
    @Path(retrieveDataPartialAPIPath)
    @Produces({"application/json"})
    public String retrieveMyDataAsJsonString(
            @Context ContainerRequestContext crc,
            @QueryParam("dvobject_types") List<DvObject.DType> dvobject_types,
            @QueryParam("published_states") List<String> published_states, 
            @QueryParam("selected_page") Integer selectedPage, 
            @QueryParam("mydata_search_term") String searchTerm,             
            @QueryParam("role_ids") List<Long> roleIds, 
            @QueryParam("userIdentifier") String userIdentifier,
            @QueryParam("filter_validities") Boolean filterValidities,
            @QueryParam("dataset_valid") List<Boolean> datasetValidities) {
        boolean OTHER_USER = false;

        String noMsgResultsFound = BundleUtil.getStringFromBundle("dataretrieverAPI.noMsgResultsFound");

        if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();
        } else {
            try {
                authUser = getRequestAuthenticatedUserOrDie(crc);
            } catch (WrappedResponse e) {
                return this.getJSONErrorString(
                    BundleUtil.getStringFromBundle("dataretrieverAPI.authentication.required"),
                    BundleUtil.getStringFromBundle("dataretrieverAPI.authentication.required.opt")
                );
            }
        }

        // For superusers, the searchUser may differ from the authUser
        AuthenticatedUser searchUser = null;
        // If the user is a superuser, see if a userIdentifier has been specified and use that instead
        if ((authUser.isSuperuser()) && (userIdentifier != null) && (!userIdentifier.isEmpty())) {
            searchUser = getUserFromIdentifier(userIdentifier);
            if (searchUser != null) {
                authUser = searchUser;
                OTHER_USER = true;
            } else {
                return this.getJSONErrorString(
                        BundleUtil.getStringFromBundle("dataretrieverAPI.user.not.found", Arrays.asList(userIdentifier)),
                        null);
            }
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
        if (filterParams.hasError()){
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
        if (myDataFinder.hasError()){
            return this.getJSONErrorString(myDataFinder.getErrorMessage(), myDataFinder.getErrorMessage());
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = 1;
        if (selectedPage != null){
            paginationStart = selectedPage;
        }
        int solrCardStart = (paginationStart - 1) * SearchConstants.NUM_SOLR_DOCS_TO_RETRIEVE;
       
        // Default the searchUser to the authUser.
        // The exception: for logged-in superusers, the searchUser may differ from the authUser
        //
        if (searchUser == null){
            searchUser = authUser;
        }

        //msg("search with user: " + searchUser.getIdentifier());

        List<String> filterQueries = this.myDataFinder.getSolrFilterQueries();
        if (filterQueries==null){
            logger.fine("No ids found for this search");
            return this.getJSONErrorString(noMsgResultsFound, null);
        }
        //msgt("myDataFinder.getSolrFilterQueries(): " + myDataFinder.getSolrFilterQueries().toString());
        
        //msg("Selected paginationStart: " + paginationStart);

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
                
                //msgt("getResultsStart: " + this.solrQueryResponse.getResultsStart());
                //msgt("getNumResultsFound: " + this.solrQueryResponse.getNumResultsFound());
                //msgt("getSolrSearchResults: " + this.solrQueryResponse.getSolrSearchResults().toString());
                if (this.solrQueryResponse.getNumResultsFound()==0){
                    return this.getJSONErrorString(noMsgResultsFound, null);
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
                                .add("search_term",  filterParams.getSearchTerm())
                                .add("dvobject_counts", this.getDvObjectTypeCounts(solrQueryResponse))
                                .add("pubstatus_counts", this.getPublicationStatusCounts(solrQueryResponse))
                                .add("selected_filters", this.myDataFinder.getSelectedFilterParamsAsJSON())
            );

        // ---------------------------------------------------------
        // We're doing ~another~ solr query here
        // NOTE!  Do not reuse this.myDataFinder after this step!! It is being passed new filterParams
        // ---------------------------------------------------------
        //jsonData.add("total_dvobject_counts", getTotalCountsFromSolrAsJSON(searchUser, this.myDataFinder));

        
        if (OTHER_USER){
            jsonData.add("other_user", searchUser.getIdentifier());
        }
                                
        return jsonData.build().toString();
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