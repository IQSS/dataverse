/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.RoleAssignment;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.authorization.MyDataQueryHelperServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;

/**
 * Given a user and a set of filters (dvobject type, roles, publication status):
 *  - Use postgres to identify DvObject types
 *  - Format a solr query string
 * 
 * @author rmp553
 */
//@Stateless
public class MyDataFinder {
        
    private static final Logger logger = Logger.getLogger(MyDataFinder.class.getCanonicalName());

    private String userIdentifier;
    private ArrayList<DataverseRole> roles;
    private ArrayList<String> dvObjectTypes;
    private ArrayList<String> publicationStatuses;
    private String searchTerm = "*";
    
    // --------------------
    private DataverseRolePermissionHelper rolePermissionHelper;
    private RoleAssigneeServiceBean roleAssigneeService;
    //private RoleAssigneeServiceBean roleService = new RoleAssigneeServiceBean();
    //private MyDataQueryHelperServiceBean myDataQueryHelperService;
    // --------------------
    public boolean errorFound = false;
    public String errorMessage = null;
    // --------------------

    // Populated in initial query.  DvObject ids -- regardless of Dtype,
    // are sorted into respective buckets in regard to permissions.
    // The same id may appear in multiple lists--and more than once
    //
    public Map<Long, Boolean> idsWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithFilePermissions = new HashMap<>();  // { role id : true }

    public MyDataFinder(DataverseRolePermissionHelper rolePermissionHelper, RoleAssigneeServiceBean roleAssigneeService) {
        this.msgt("MyDataFinder, constructor");
        this.rolePermissionHelper = rolePermissionHelper;
        this.roleAssigneeService = roleAssigneeService;
    }

    /*
    private ArrayList<Long> dataverseIds;
    private ArrayList<Long> primaryDatasetIds;
    private ArrayList<Long> primaryFileIds;
    private ArrayList<Long> parentIds;
    */
    
    public void runFindDataSteps(String userIdentifier){
        this.userIdentifier = userIdentifier;
        msgt("runFindDataSteps: " + userIdentifier);
        if (!runStep1RoleAssignments()){
            return;
        }
        
    }

    
    

    /*
    public MyDataFinder(String userIdentifier, ArrayList<DataverseRole> roles, 
            ArrayList<String> dvObjectTypes, ArrayList<String> publicationStatuses, 
            String searchTerm, MyDataQueryHelperServiceBean injectedBean,  DataverseRoleServiceBean roleService) {
        
        this.userIdentifier = userIdentifier;
        this.roles = roles;
        this.dvObjectTypes = dvObjectTypes;
        this.publicationStatuses = publicationStatuses;
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            this.searchTerm = searchTerm;
        }
        initServices(injectedBean, roleService);        
        runSteps();

        //initializeLists();
    }
    */
    /*
    private void initServices(MyDataQueryHelperServiceBean injectedBean, DataverseRoleServiceBean roleService){
        myDataQueryHelperService = injectedBean;
        List<DataverseRole> roleList = roleService.findAll();
        msgt("roles: " + roleList.toString());
        rolePermissionHelper = new DataverseRolePermissionHelper(roleList);
        
    }
    */
    
    public String getTestString(){
        
        
        if (this.rolePermissionHelper == null){
            return "rolePermissionHelper is null";
        }
        
        //if (true)return this.rolePermissionHelper.getDatasetRolesAsHTML();
        
        List<String> outputList = new ArrayList<>();
        List<String> idList = new ArrayList<>();

        outputList.add("<h2>dataset ids: " + this.idsWithDatasetPermissions.size() + "</h2>");
        for (Map.Entry pair : this.idsWithDatasetPermissions.entrySet()) {          
            idList.add(pair.getKey().toString());
        }
        outputList.add("<pre>" + StringUtils.join(idList, ", ") + "</pre>");
        
        return StringUtils.join(outputList, "<br />");
        
    }
    

    public String formatUserIdentifierAsAssigneeIdentifier(String userIdentifier){
        if (userIdentifier == null){
            return null;
        }
        if (userIdentifier.startsWith("@")){
            return userIdentifier;
        }
        return "@" + userIdentifier;
    }
    
    private boolean runStep1RoleAssignments(){
        msgt("runStep1RoleAssignments");
        
        List<Object[]> results = this.roleAssigneeService.getAssigneeAndRoleIdListFor(formatUserIdentifierAsAssigneeIdentifier(this.userIdentifier), null);
        msgt("runStep1RoleAssignments results: " + results.toString());
//List<RoleAssignment> results = this.roleAssigneeService.getAssignmentsFor(this.userIdentifier);
        if (results == null){
            this.addErrorMessage("Sorry, the EntityManager isn't working (still).");
            return false;
        }else if (results.isEmpty()){
            this.addErrorMessage("Sorry, you have no assigned roles.");
            return false;
        }
    
        // Iterate through assigned objects
        //for (RoleAssignment ra : results) {
        for (Object[] ra : results) {
            Long dvId = (Long)ra[0];
            Long roleId = (Long)ra[1];
            if (this.rolePermissionHelper.hasDataversePermissions(roleId)){
                this.idsWithDataversePermissions.put(dvId, true);
            }
            if (this.rolePermissionHelper.hasDatasetPermissions(roleId)){
                this.idsWithDatasetPermissions.put(dvId, true);
            }
            if (this.rolePermissionHelper.hasFilePermissions(roleId)){
                this.idsWithFilePermissions.put(dvId, true);
            }
        }      
        return true;
    }
    
    public boolean hasError(){
        return this.errorFound;
    }
    public String getErrorMessage(){
        return this.errorMessage;
    }
    private void addErrorMessage(String s){
        this.errorFound = true;
        this.errorMessage = s;
    }
   
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }

}   // end: MyDataFinder
