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
    private Boolean isSuperuserLoggedIn = null;
    
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
    
        
    public String getRetrieveDataFullAPIPath(){
        return DataRetrieverAPI.retrieveDataFullAPIPath;
    }
    
    public boolean isSuperuser(){
        
        if (this.isSuperuserLoggedIn == null){
            this.setIsSuperUserLoggedIn();
        }
        return this.isSuperuserLoggedIn;
        
    }
    
    private void setIsSuperUserLoggedIn(){
        
        // Is this an authenticated user?
        //
        if ((session.getUser() == null)||(!session.getUser().isAuthenticated())){ 
             this.isSuperuserLoggedIn = false;             
             return;
        }
         
        // Is this a user?
        //
        authUser =  (AuthenticatedUser)session.getUser();
        if (authUser==null){
            this.isSuperuserLoggedIn = false;
            return;
        }
        
        // Is this a superuser?
        //
        this.isSuperuserLoggedIn = authUser.isSuperuser();
    }
    
    
    public String init() {
        msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        msgt("----------- init() -------------");

        if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();
        } else {
            return "/loginpage.xhtml";
	// redirect to login OR give some type ‘you must be logged in message'
        }
        List<DataverseRole> roleList = new ArrayList();

        List<String> dtypes = MyDataFilterParams.defaultDvObjectTypes;
        this.filterParams = new MyDataFilterParams(authUser.getIdentifier(), dtypes, null, null, null);
        roleList = roleAssigneeService.getAssigneeDataverseRoleFor(this.filterParams.getUserIdentifier());
        if (roleList.isEmpty()) {

            roleList = dataverseRoleService.findAll();
        }

        msgt("roles: " + roleList.toString());
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);
        
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

        

        //List<String> dtypes = Arrays.asList(DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING);
        //DvObject.DATAFILE_DTYPE_STRING, DvObject.DATASET_DTYPE_STRING, DvObject.DATAVERSE_DTYPE_STRING
        
        //List<String> dtypes = new ArrayList<>();

        
        this.myDataFinder = new MyDataFinder(rolePermissionHelper,
                                        roleAssigneeService,
                                        dvObjectServiceBean);
        //myDataFinder.runFindDataSteps(userIdentifier);
        this.myDataFinder.runFindDataSteps(filterParams);
      
        
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
                doc.setUserRole(myDataQueryHelperServiceBean.getRolesOnDVO(authUser, doc.getEntityId(), null)); 
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
