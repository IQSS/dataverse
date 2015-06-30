package edu.harvard.iq.dataverse.mydata;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import static edu.harvard.iq.dataverse.DvObject.DATAFILE_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATASET_DTYPE_STRING;
import static edu.harvard.iq.dataverse.DvObject.DATAVERSE_DTYPE_STRING;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SearchServiceBeanMyData;
import edu.harvard.iq.dataverse.SolrQueryResponse;
import edu.harvard.iq.dataverse.SolrSearchResult;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.MyDataQueryHelperServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.SearchException;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.SortBy;
import java.io.IOException;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("MyDataPage")
public class MyDataPage implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    @Inject DataverseSession session;    

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    DvObjectServiceBean dvObjectServiceBean;
    @EJB
    SearchServiceBeanMyData searchService;
    @EJB
    MyDataQueryHelperServiceBean myDataQueryHelperServiceBean;
    
    private String testName = "blah";
    private DataverseRolePermissionHelper rolePermissionHelper;// = new DataverseRolePermissionHelper();
    private MyDataFinder myDataFinder;
    private Pager pager;
    private MyDataFilterParams filterParams;
    private SolrQueryResponse solrQueryResponse;
    private AuthenticatedUser authUser = null;
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
    public boolean hasFilterParams(){
        if (this.filterParams == null){
            return false;
        }
        return true;
    }
    
    public List<String[]> getRoleInfoForCheckboxes(){
        return this.rolePermissionHelper.getRoleInfoForCheckboxes();
    }
    
    public String init() {
        msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        msgt("----------- init() -------------");
        List<DataverseRole> roleList = dataverseRoleService.findAll();
        msgt("roles: " + roleList.toString());
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);

       

        if (session != null){
            if (session.getUser()==null){
                authUser = (AuthenticatedUser)session.getUser();
            }
        } 
        
        /*else{
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
            
        }*/
        //if (authUser )
        String userIdentifier = "dataverseAdmin";
        
        List<String> dtypes = MyDataFilterParams.defaultDvObjectTypes;
        //List<String> dtypes = Arrays.asList(DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
        //DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING, DvObject.DATAVERSE_DTYPE_STRING
        
        //List<String> dtypes = new ArrayList<>();
        this.filterParams = new MyDataFilterParams(userIdentifier, dtypes, null, null, null);
        
        this.myDataFinder = new MyDataFinder(rolePermissionHelper,
                                        roleAssigneeService,
                                        dvObjectServiceBean);
        //myDataFinder.runFindDataSteps(userIdentifier);
        this.myDataFinder.runFindDataSteps(filterParams);
        /*
        if (!this.myDataFinder.hasError()){

            int paginationStart = 1;
            boolean dataRelatedToMe = true;
            int numResultsPerPage = 10;
            msgt("getSolrFilterQueries: " + this.myDataFinder.getSolrFilterQueries().toString());
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
                        10
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
                Logger.getLogger(RolePermissionHelperPage.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        this.pager = new Pager(111, 10, 3);
        */
        
        return null;
    }

    public String getSolrDocs() throws JSONException{
        
        if (solrQueryResponse == null){
            return "(solrQueryResponse is null)";
        }

        //JsonObject jsonData = new JsonObject();
        
        List<String> outputList = new ArrayList<>();

        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()){
            
            //outputList.add(doc.toString());
            //String jsonDoc = doc.toJsonObject(true, true, true).toString();
            //if (true)return jsonDoc;
            if( authUser!= null){
                doc.setUserRole(myDataQueryHelperServiceBean.getRolesOnDVO(authUser, doc.getEntityId())); 
            }
            outputList.add(doc.toJsonObject(true, true, true).toString());
            //break;
        }
        //jsonData.add("docs", (JsonElement) outputList);
        //return jsonData.toString();
        return "{ \"docs\" : [ " + StringUtils.join(outputList, ", ") + "] }";

    }

    public MyDataFilterParams getFilterParams() throws JSONException{
        return this.filterParams;
    }
    
    public MyDataFinder getMyDataFinder(){
        return this.myDataFinder;
    }

    public String getMyDataFinderInfo(){
        if (this.myDataFinder.hasError()){
            return this.myDataFinder.getErrorMessage();
        }else{
            return this.myDataFinder.getTestString();
        }
    }
    
    
    public DataverseRolePermissionHelper getRolePermissionHelper(){
        return this.rolePermissionHelper;
    }

    
    public String getTestName(){
        return this.testName;//"blah";
    }

    public void setTestName(String name){
        this.testName = name;
    }
    
    public List<String> getPublishedStates(){
        return MyDataFilterParams.defaultPublishedStates;
    }
    
    public String getSomeJSON() throws JSONException{
        
      JSONObject obj = new JSONObject();

      obj.put("name", "foo");
      obj.put("num", new Integer(100));
      obj.put("balance", new Double(1000.21));
      obj.put("is_vip", new Boolean(true));

      return obj.toString();
    }

    public String getSomeText(){
        //System.out.println(this.rolePermissionHelper.getRoleNameListString());;
        return "pigletz";
        //return this.rolePermissionHelper.getRoleNameListString();
    }
}
