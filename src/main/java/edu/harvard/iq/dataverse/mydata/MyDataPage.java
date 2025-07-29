package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

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
    @Inject SettingsWrapper settingsWrapper;

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    @EJB
    RoleAssigneeServiceBean roleAssigneeService;
    @EJB
    DvObjectServiceBean dvObjectServiceBean;
    @Inject
    PermissionsWrapper permissionsWrapper;
    
    private DataverseRolePermissionHelper rolePermissionHelper;// = new DataverseRolePermissionHelper();
    private MyDataFilterParams filterParams;
    private AuthenticatedUser authUser = null;
    private Boolean isSuperuserLoggedIn = null;
    
    private long totalUserFileCount = 0;
    private long totalUserDataverseCount = 0;
    private long totalUserDatasetCount = 0;
    
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
        return this.filterParams != null;
    }
    
    public List<String[]> getRoleInfoForCheckboxes(){
        if (this.rolePermissionHelper == null){
            init();
        }
        List<String[]> retVal = this.rolePermissionHelper.getRoleInfoForCheckboxes();
        if (retVal != null){
            return retVal;
        } else {
            return new ArrayList<>();
        }
    }
    
    public List<String[]> getValidityInfoForCheckboxes(){
        return Arrays.asList(
            new String[] {"true", "valid", BundleUtil.getStringFromBundle("valid")},
            new String[] {"false", "incomplete", BundleUtil.getStringFromBundle("incomplete")}
        );
    }

    public boolean showValidityFilter() {
        return JvmSettings.UI_SHOW_VALIDITY_FILTER.lookupOptional(Boolean.class).orElse(false);
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
            return permissionsWrapper.notAuthorized();
	// redirect to login OR give some type ‘you must be logged in message'
        }

        // Initialize a filterParams object to buid the Publication Status checkboxes
        //
        HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        DataverseRequest dataverseRequest = new DataverseRequest(authUser, httpServletRequest);
        this.filterParams = new MyDataFilterParams(dataverseRequest,  MyDataFilterParams.defaultDvObjectTypes, null, null, null, null);
        
        
        // Temp DataverseRolePermissionHelper -- not in its normal role but for creating initial checkboxes
        //
        rolePermissionHelper = new DataverseRolePermissionHelper(getRolesUsedToCreateCheckboxes(dataverseRequest));
       
        //this.setUserCountTotals(authUser, rolePermissionHelper);
        return null;
    }
    
    public String getAuthUserIdentifier(){
        if (this.authUser==null){
            return null;
        }
        return MyDataUtil.formatUserIdentifierForMyDataForm(this.authUser.getIdentifier());
    }
    
    private List<DataverseRole> getRolesUsedToCreateCheckboxes(DataverseRequest dataverseRequest){

        if (dataverseRequest==null){
            throw new NullPointerException("dataverseRequest cannot be null");
        }
        // Initialize the role checboxes
        //
        List<DataverseRole> roleList = new ArrayList<>();
        
        // (1) For a superuser, show all the roles--in case they want to
        //    see another user's "My Data"
        if (authUser.isSuperuser()){
            roleList = dataverseRoleService.findAll();
        }else{
            // (2) For a regular users
            roleList = roleAssigneeService.getAssigneeDataverseRoleFor(dataverseRequest);
        
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
