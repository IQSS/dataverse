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
import edu.harvard.iq.dataverse.search.SearchServiceBean;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
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
    SearchServiceBean searchService;
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
    
    private long totalUserFileCount = (long) 0;
    private long totalUserDataverseCount = (long) 0;
    private long totalUserDatasetCount = (long) 0;
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }

    public void setTotalUserDatasetCount(long cnt){
        this.totalUserDatasetCount = cnt;
    }    
    public void setTotalUserDataverseCount(long cnt){
        this.totalUserDataverseCount = cnt;
    }    
    public void setTotalUserFileCount(long cnt){
        this.totalUserFileCount = cnt;
    }    

    public long getTotalUserDatasetCount(){
        return this.totalUserDatasetCount;
    }    
    public long getTotalUserDataverseCount(){
        return this.totalUserDataverseCount;
    }
    public long getTotalUserFileCount(){
        return this.totalUserFileCount;
    }
    
    public boolean hasFilterParams(){
        if (this.filterParams == null){
            return false;
        }
        return true;
    }
    
    public List<String[]> getRoleInfoForCheckboxes(){
        if (this.rolePermissionHelper == null){
            init();
        }
        List<String[]> retVal = this.rolePermissionHelper.getRoleInfoForCheckboxes();
        if (retVal != null){
            return retVal;
        } else {
            return new ArrayList();
        }
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
        //msgt("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes

        //msgt("----------- init() -------------");

        if ((session.getUser() != null) && (session.getUser().isAuthenticated())) {
            authUser = (AuthenticatedUser) session.getUser();
        } else {
            return "/loginpage.xhtml";
	// redirect to login OR give some type ‘you must be logged in message'
        }

        // Initialize a filterParams object to buid the Publication Status checkboxes
        //
        this.filterParams = new MyDataFilterParams(authUser.getIdentifier(),  MyDataFilterParams.defaultDvObjectTypes, null, null, null);
        
        
        // Temp DataverseRolePermissionHelper -- not in its normal role but for creating initial checkboxes
        //
        rolePermissionHelper = new DataverseRolePermissionHelper(getRolesUsedToCreateCheckboxes(authUser));
       
        //this.setUserCountTotals(authUser, rolePermissionHelper);
        return null;
    }
    
    public String getAuthUserIdentifier(){
        if (this.authUser==null){
            return null;
        }
        return MyDataUtil.formatUserIdentifierForMyDataForm(this.authUser.getIdentifier());
    }
    /*
    private void setUserCountTotals(AuthenticatedUser userForCounts, DataverseRolePermissionHelper rolePermissionHelper){
        if (userForCounts == null){
            throw new NullPointerException("userForCounts cannot be null");
        }
        if (rolePermissionHelper == null){
            throw new NullPointerException("rolePermissionHelper cannot be null");
        }
          
        MyDataFinder dataFinder = new MyDataFinder(rolePermissionHelper, this.roleAssigneeService, this.dvObjectServiceBean);
        
        DataRetrieverAPI dataRetriever = new DataRetrieverAPI();

        Map<String, Long> countMap = dataRetriever.getTotalCountsFromSolrAsJavaMap(userForCounts, dataFinder);

        if (countMap ==null){
            logger.severe("MyDataPage.setUserCountTotals() jsonCountData should not be null!!!");
            return;
        }
    
        // Set counts: this is alot of extra code...
        //
        Object cntVal = null;
        if (countMap.containsKey(SolrQueryResponse.DATAVERSES_COUNT_KEY)){
            cntVal = countMap.get(SolrQueryResponse.DATAVERSES_COUNT_KEY);
            if (cntVal != null){
                this.totalUserDataverseCount = (long)cntVal;
            }
        }
        if (countMap.containsKey(SolrQueryResponse.DATASETS_COUNT_KEY)){
            cntVal = countMap.get(SolrQueryResponse.DATAVERSES_COUNT_KEY);
            if (cntVal != null){
                this.totalUserDatasetCount =  (long)cntVal;
            }
        }
        if (countMap.containsKey(SolrQueryResponse.FILES_COUNT_KEY)){
            cntVal = countMap.get(SolrQueryResponse.FILES_COUNT_KEY);
            if (cntVal != null){
                this.totalUserFileCount = (long) cntVal;
            }
        }        
  
    }
    */
    
    private List<DataverseRole> getRolesUsedToCreateCheckboxes(AuthenticatedUser authUser){

        if (authUser==null){
            throw new NullPointerException("authUser cannot be null");
        }
        // Initialize the role checboxes
        //
        List<DataverseRole> roleList = new ArrayList();
        
        // (1) For a superuser, show all the roles--in case they want to
        //    see another user's "My Data"
        if (authUser.isSuperuser()){
            roleList = dataverseRoleService.findAll();
        }else{
            // (2) For a regular users
            roleList = roleAssigneeService.getAssigneeDataverseRoleFor(this.filterParams.getUserIdentifier());
        
            // If there are no assigned roles, show them all?
            // This may not make sense
            if (roleList.isEmpty()) {
                roleList = dataverseRoleService.findAll();
            }
        }
        return roleList;
    }
        
    public DataverseRolePermissionHelper getRolePermissionHelper(){
        return this.rolePermissionHelper;
    }

    
    
    public List<String[]> getPublishedStatesForMyDataPage(){
        return MyDataFilterParams.getPublishedStatesForMyDataPage();
    }
    
}
