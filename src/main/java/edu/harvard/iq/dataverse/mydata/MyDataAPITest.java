/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBeanMyData;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.api.Access;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
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
public class MyDataAPITest extends AbstractApiBean {

    private static final Logger logger = Logger.getLogger(MyDataAPITest.class.getCanonicalName());

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
    
    private List<DataverseRole> roleList;
    private DataverseRolePermissionHelper rolePermissionHelper;
    private List<String> defaultDvObjectTypes = MyDataFilterParams.defaultDvObjectTypes;
    private MyDataFinder myDataFinder;
    private SolrQueryResponse solrQueryResponse;

    public static final String JSON_SUCCESS_FIELD_NAME = "success";
    public static final String JSON_ERROR_MSG_FIELD_NAME = "error_message";
    public static final String JSON_DATA_FIELD_NAME = "data";
    
    /**
     * Constructor
     * 
     */
    public MyDataAPITest(){
           
    }
    
    private int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }
    
    
    public Pager getRandomPagerPager(Integer selectedPage) throws JSONException{
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
    public String retrieveTestPager(@QueryParam("selectedPage") int selectedPage) throws JSONException{
        
        return this.getRandomPagerPager(selectedPage).asJSONString();
    }
    
    //private String getUserIdentifier()
    
    
    @Path("initial")
    @GET
    @Produces({"application/json"})
    public String retrieveMyDataInitialCall(@QueryParam("dvobject_types") List<String> dvobject_types, 
            @QueryParam("published_states") List<String> published_states, 
            @QueryParam("role_ids") List<Long> roleIds, 
            @QueryParam("userIdentifier") String userIdentifier) throws JSONException{ //String myDataParams) {

        //msgt("types: " + types.toString());
        AuthenticatedUser authUser;
        if ((session.getUser() != null)&&(session.getUser().isAuthenticated())){            
             authUser = (AuthenticatedUser)session.getUser();
        }else{
            authUser = authenticationService.getAuthenticatedUser(userIdentifier);
            if (authUser == null){
                throw new IllegalStateException("User not found!  Shouldn't be using this anyway");
            }
        }
             
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        
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
        MyDataFilterParams filterParams = new MyDataFilterParams(authUser.getIdentifier(), dtypes, pub_states, roleIds, null);
        if (filterParams.hasError()){
            jsonData.add(MyDataAPITest.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(MyDataAPITest.JSON_ERROR_MSG_FIELD_NAME, filterParams.getErrorMessage());
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
            jsonData.add(MyDataAPITest.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(MyDataAPITest.JSON_ERROR_MSG_FIELD_NAME, myDataFinder.getErrorMessage());
            return jsonData.build().toString();
        }

        // ---------------------------------
        // (3) Make Solr Query
        // ---------------------------------
        int paginationStart = 1;
        boolean dataRelatedToMe = true;
        try {
                solrQueryResponse = searchService.search(
                        null, // no user
                        null, // subtree, default it to Dataverse for now
                        "*", //this.filterParams.getSearchTerm(),
                        this.myDataFinder.getSolrFilterQueries(),//filterQueries,
                        SearchFields.NAME_SORT, SortBy.ASCENDING,
                        //SearchFields.RELEASE_OR_CREATE_DATE, SortBy.DESCENDING,
                        paginationStart,
                        dataRelatedToMe,
                        10 // SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE
                );
                msgt("getResultsStart: " + this.solrQueryResponse.getResultsStart());
                msgt("getNumResultsFound: " + this.solrQueryResponse.getNumResultsFound());
                msgt("getSolrSearchResults: " + this.solrQueryResponse.getSolrSearchResults().toString());
                
                //User user,
                //Dataverse dataverse,
                //String query, 
                //List<String> filterQueries, String sortField, String sortOrder, int paginationStart, boolean onlyDatatRelatedToMe, int numResultsPerPage) throws SearchException {
                
        } catch (SearchException ex) {
                solrQueryResponse = null;   
                this.logger.severe("Solr SearchException: " + ex.getMessage());
        }
        
        if (solrQueryResponse==null){
            jsonData.add(MyDataAPITest.JSON_SUCCESS_FIELD_NAME, false);
            jsonData.add(MyDataAPITest.JSON_ERROR_MSG_FIELD_NAME, "Sorry!  There was a SOLR Error");
            return jsonData.build().toString();
        }
        
        //jsonData.add(MyDataAPITest.JSON_DATA_FIELD_NAME, Json.createObjectBuilder()
        //jsonData.add(MyDataAPITest.JSON_DATA_FIELD_NAME, Json.createObjectBuilder()
        //jsonData.add("solr_docs", this.formatSolrDocs(solrQueryResponse));
        
         // ---------------------------------
        // (4) Add pagingation
        // ---------------------------------
        Pager pager = new Pager(solrQueryResponse.getNumResultsFound().intValue(), 
                                SearchFields.NUM_SOLR_DOCS_TO_RETRIEVE, 
                                paginationStart);
        
        jsonData.add(MyDataAPITest.JSON_SUCCESS_FIELD_NAME, true);
        jsonData.add(MyDataAPITest.JSON_DATA_FIELD_NAME,        
                        Json.createObjectBuilder()
                                .add("pagination", pager.asJsonObjectBuilder())
                                .add(SearchConstants.SEARCH_API_ITEMS, this.formatSolrDocs(solrQueryResponse))
                                .add(SearchConstants.SEARCH_API_TOTAL_COUNT, solrQueryResponse.getNumResultsFound())
                                .add(SearchConstants.SEARCH_API_START, solrQueryResponse.getResultsStart())
            );
                                
        return jsonData.build().toString();
    }
   
    
    //private String formatSolrDocs(
    public JsonObjectBuilder formatSolrDocs(SolrQueryResponse solrResponse){
        
        if (solrResponse == null){
            return null;
        }

        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        
        List<String> outputList = new ArrayList<>();

        JsonArrayBuilder jsonSolrDocsArrayBuilder = Json.createArrayBuilder();

        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()){
            jsonSolrDocsArrayBuilder.add(doc.toJsonObject(true, true, true));
            //jsonData.add(JSON_DATA_FIELD_NAME, BigDecimal.ZERO)
            //outputList.add(doc.toString());
            //String jsonDoc = doc.toJsonObject(true, true, true).toString();
            //if (true)return jsonDoc;
            //outputList.add(doc.toJsonObject(true, true, true).toString());
            //break;
        }
        jsonData.add("solr_docs", jsonSolrDocsArrayBuilder);
        
        return jsonData;
        //return jsonData.toString();
        //return "{ \"docs\" : [ " + StringUtils.join(outputList, ", ") + "] }";

    }
    
    //@Produces({"application/zip"})
    @Path("test-it")
    @GET
    public Response retrieveMyData(@QueryParam("key") String keyValue) throws JSONException{ //String myDataParams) {
        
        final JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add("name", keyValue);
        
        if (session == null){
            jsonData.add("has-session", false);
        } else{
            jsonData.add("has-session", true);
            if (session.getUser()==null){
                jsonData.add("has-user", false);
            }else{
                jsonData.add("has-user", true);
                if (session.getUser().isAuthenticated()){
                    jsonData.add("auth-status", "AUTHENTICATED");
                    AuthenticatedUser authUser = (AuthenticatedUser)session.getUser();
                    jsonData.add("username", authUser.getIdentifier());
                }else{
                    jsonData.add("auth-status", "GET OUT - NOT AUTHENTICATED");
                }
            }
            
        }
        JSONObject obj = new JSONObject();
        obj.put("name", "foo");
        obj.put("num", new Integer(100));
        obj.put("balance", new Double(1000.21));
        obj.put("is_vip", new Boolean(true));
        
        return okResponse(jsonData);
        
        
        //return okResponse(obj);
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