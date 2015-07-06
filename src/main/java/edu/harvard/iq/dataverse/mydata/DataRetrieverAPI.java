/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.FacetCategory;
import edu.harvard.iq.dataverse.FacetLabel;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBeanMyData;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.Access;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.MyDataQueryHelperServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SortBy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/**
 *
 * @author rmp553
 */
@Path("mydata")
public class DataRetrieverAPI extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(DataRetrieverAPI.class.getCanonicalName());

    public static final String retrieveDataFullAPIPath = "/api/mydata/retrieve";
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
    SearchServiceBeanMyData searchService;
    @EJB
    AuthenticationServiceBean authenticationService;
        @EJB
    MyDataQueryHelperServiceBean myDataQueryHelperServiceBean;
    
    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private List<String> defaultDvObjectTypes = MyDataFilterParams.defaultDvObjectTypes;
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
    
    private int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
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
    
    @Path("test-it2")
    @GET
    @Produces({"application/json"})
    public String retrieveTestPager(@QueryParam("selectedPage") int selectedPage){
        
        return this.getRandomPagerPager(selectedPage).asJSONString();
    }
    
    //private String getUserIdentifier()
    
    
    public boolean isSuperuser(){
        
        // Is this an authenticated user?
        //
        if ((session.getUser() == null)||(!session.getUser().isAuthenticated())){ 
             return false;             
        }
         
        // Is this a user?
        //
        authUser =  (AuthenticatedUser)session.getUser();
        if (authUser==null){
            return false;
        }
        
        // Is this a superuser?
        //
        return authUser.isSuperuser();
    }
    
    @Path(retrieveDataPartialAPIPath)
    @GET
    @Produces({"application/json"})
    public String retrieveMyDataAsJsonString(@QueryParam("dvobject_types") List<String> dvobject_types, 
            @QueryParam("published_states") List<String> published_states, 
            @QueryParam("selected_page") Integer selectedPage, 
            @QueryParam("mydata_search_term") String searchTerm,             
            @QueryParam("role_ids") List<Long> roleIds, 
            @QueryParam("userIdentifier") String userIdentifier) { //String myDataParams) {

        msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        //msgt("types: " + types.toString());
        JsonObjectBuilder jsonData = Json.createObjectBuilder();

        if ((session.getUser() != null)&&(session.getUser().isAuthenticated())){            
             authUser = (AuthenticatedUser)session.getUser();
        }else{
            authUser = authenticationService.getAuthenticatedUser(userIdentifier);
            if (authUser == null){
                logger.severe("retrieveMyDataAsJsonString. User not found!  Shouldn't be using this anyway");
                jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
                jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, "Requires authentication");
                return jsonData.build().toString();
            }
        }
                     
        roleList = dataverseRoleService.findAll();
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);    
        
        // for testing
        //
        //if (userIdentifier==null){
        //    logger.warning("retrieveMyDataInitialCall: ADmin test - REMOVE REMOVE REMOVE  REMOVE REMOVE  REMOVE REMOVE  REMOVE REMOVE ");
        //    userIdentifier = "dataverseAdmin";  
        //}
        
        List<String> dtypes;
        if (dvobject_types != null){
            dtypes = dvobject_types;
        }else{
            dtypes = MyDataFilterParams.defaultDvObjectTypes;
        }
        List<String> pub_states = null;
        if (published_states != null){
            pub_states = published_states;
        }
        msgt(">>>roleIds: " + roleIds);
        
        // ---------------------------------
        // (1) Initialize filterParams and check for Errors 
        // ---------------------------------
        MyDataFilterParams filterParams = new MyDataFilterParams(authUser.getIdentifier(), dtypes, pub_states, roleIds, searchTerm);
        if (filterParams.hasError()){
            jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, filterParams.getErrorMessage());
            return jsonData.build().toString();
        }
       
        // ---------------------------------
        // (2) Initialize MyDataFinder and check for Errors 
        // ---------------------------------
        myDataFinder = new MyDataFinder(rolePermissionHelper,
                                        roleAssigneeService,
                                        dvObjectServiceBean);
        this.myDataFinder.runFindDataSteps(filterParams);
        if (myDataFinder.hasError()){
            jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, myDataFinder.getErrorMessage());
            return jsonData.build().toString();
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = 1;
        if (selectedPage != null){
            paginationStart = selectedPage;
        }
        boolean dataRelatedToMe = true;
        msgt("myDataFinder.getSolrFilterQueries(): " + myDataFinder.getSolrFilterQueries().toString());
        try {
                solrQueryResponse = searchService.search(
                        null, // no user
                        null, // subtree, default it to Dataverse for now
                        filterParams.getSearchTerm(),  //"*", //
                        this.myDataFinder.getSolrFilterQueries(),//filterQueries,
                        SearchFields.NAME_SORT, SortBy.ASCENDING,
                        //SearchFields.RELEASE_OR_CREATE_DATE, SortBy.DESCENDING,
                        paginationStart,
                        dataRelatedToMe,
                        10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
                );
                
                //msgt("getResultsStart: " + this.solrQueryResponse.getResultsStart());
                msgt("getNumResultsFound: " + this.solrQueryResponse.getNumResultsFound());
                msgt("getSolrSearchResults: " + this.solrQueryResponse.getSolrSearchResults().toString());
                if (this.solrQueryResponse.getNumResultsFound()==0){
                    jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
                    jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, "Sorry, no results were found.");
                    return jsonData.build().toString();
                }
                 msgt("getFilterQueriesActual: " + solrQueryResponse.getFilterQueriesActual());
                 msgt("getFacetCategoryList: " + solrQueryResponse.getFacetCategoryList());
                 msgt("getTypeFacetCategories: " + solrQueryResponse.getTypeFacetCategories().toString());
                 for (FacetCategory fc : solrQueryResponse.getTypeFacetCategories()){
                      for (FacetLabel fl : fc.getFacetLabel()) {
                          
                          msg("name: | " + fl.getName()+ " | count: " + fl.getCount());
                            //previewCountbyType.put(facetLabel.getName(), facetLabel.getCount());
                        }
                 }
                 //solrQueryResponse.get();
                         
        } catch (SearchException ex) {
                solrQueryResponse = null;   
                this.logger.severe("Solr SearchException: " + ex.getMessage());
        }
        
        if (solrQueryResponse==null){
            jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(DataRetrieverAPI.JSON_ERROR_MSG_FIELD_NAME, "Sorry!  There was a SOLR Error");
            return jsonData.build().toString();
        }
        
        //jsonData.add(DataRetrieverAPI.JSON_DATA_FIELD_NAME, Json.createObjectBuilder()
        //jsonData.add(DataRetrieverAPI.JSON_DATA_FIELD_NAME, Json.createObjectBuilder()
        //jsonData.add("solr_docs", this.formatSolrDocs(solrQueryResponse));
        
         // ---------------------------------
        // (4) Build JSON document including:
        //      - Pager
        //      - Formatted solr docs
        //      - Num results found
        //      - Search term
        //      - DvObject counts
        // ---------------------------------
        Pager pager = new Pager(solrQueryResponse.getNumResultsFound().intValue(), 
                                10, 
                                paginationStart);
        
        jsonData.add(DataRetrieverAPI.JSON_SUCCESS_FIELD_NAME, true);
        jsonData.add(DataRetrieverAPI.JSON_DATA_FIELD_NAME,        
                        Json.createObjectBuilder()
                                .add("pagination", pager.asJsonObjectBuilder())
                                .add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse))
                                .add(SearchConstants.SEARCH_API_TOTAL_COUNT, solrQueryResponse.getNumResultsFound())
                                .add(SearchConstants.SEARCH_API_START, solrQueryResponse.getResultsStart())
                                .add("search_term",  filterParams.getSearchTerm())
                                .add("dvobject_counts", this.getDvObjectTypeCounts(solrQueryResponse))
            );
                                
        return jsonData.build().toString();
    }
   
    private JsonObjectBuilder getDvObjectTypeCounts(SolrQueryResponse solrResponse){
        
        if (solrQueryResponse==null){
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: solrQueryResponse should not be null");
            return null;
        }
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        for (FacetCategory fc : solrResponse.getTypeFacetCategories()){
            for (FacetLabel fl : fc.getFacetLabel()) {  
                jsonData.add(fl.getName() + "_count", fl.getCount());
                //msg("name: | " + fl.getName()+ " | count: " + fl.getCount());
            }
        }
        return jsonData;
    }
    
    //private JsonObjectBuilder formatSolrDocs(SolrQueryResponse solrResponse){
    private JsonArrayBuilder formatSolrDocs(SolrQueryResponse solrResponse){
        
        if (solrResponse == null){
            logger.severe("DataRetrieverAPI.getDvObjectTypeCounts: formatSolrDocs should not be null");
            return null;
        }
        JsonArrayBuilder jsonSolrDocsArrayBuilder = Json.createArrayBuilder();

        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()){

            if( authUser!= null){
                doc.setUserRole(myDataQueryHelperServiceBean.getRolesOnDVO(authUser, doc.getEntityId())); 
            }
            jsonSolrDocsArrayBuilder.add(doc.getJsonForMyData());
        }
        return jsonSolrDocsArrayBuilder;
        
    }
    
    @Path("test-it")
    @Produces({"application/json"})
    @GET
    public String retrieveMyData(@QueryParam("key") String keyValue){ //String myDataParams) {
        
        final JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add("name", keyValue);
        return jsonData.build().toString();
    }
    
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
}        