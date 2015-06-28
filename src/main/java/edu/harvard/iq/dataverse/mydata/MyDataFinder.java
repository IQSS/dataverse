/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
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
    private DvObjectServiceBean dvObjectServiceBean;
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
    // ----------------------------
    // POPULATED IN STEP 1 (1st query)
    // ----------------------------
    public Map<Long, Boolean> idsWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithFilePermissions = new HashMap<>();  // { role id : true }

    private List<Long> directDvObjectIds = new ArrayList<Long>();

    // Lists later used to format Solr Queries
    // 
    // ----------------------------
    // POPULATED IN STEP 2 (2nd query)
    // ----------------------------
    private List<Long> directDataverseIds = new ArrayList<Long>();
    private List<Long> directDatasetIds = new ArrayList<Long>();
    private List<Long> directFileIds = new ArrayList<Long>();
    
    private List<Long> datasetParentIds = new ArrayList<Long>(); // dataverse has dataset permissions

    private List<Long> fileParentIds = new ArrayList<Long>();   // dataset has file permissions      
    private List<Long> fileGrandparentFileIds = new ArrayList<Long>();  // dataverse has file permissions

    
    public MyDataFinder(DataverseRolePermissionHelper rolePermissionHelper, RoleAssigneeServiceBean roleAssigneeService, DvObjectServiceBean dvObjectServiceBean) {
        this.msgt("MyDataFinder, constructor");
        this.rolePermissionHelper = rolePermissionHelper;
        this.roleAssigneeService = roleAssigneeService;
        this.dvObjectServiceBean = dvObjectServiceBean;
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
        if (!runStep2DirectAssignments()){
            return;
        }
        if (!fileGrandparentFileIds.isEmpty()){
            runStep3FilePermsAssignedAtDataverse();
        }        
    }

    
    public String getSolrDvObjectFilterQuery(){

        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyData object,first check 'hasError()'");
        }

        List<Long> entityIds = new ArrayList<Long>();
        List<Long> parentIds = new ArrayList<Long>();

        //List<Long> = entity_ids 
        //parent_ids = []
        
        /*if self.filter_form.are_dataverses_included():
            entity_ids += self.all_dataverse_ids

        if self.filter_form.are_datasets_included():
            entity_ids += self.initial_dataset_ids
            parent_ids += self.all_dataverse_ids

        if self.filter_form.are_files_included():
            entity_ids += self.initial_file_ids
            parent_ids += self.all_dataset_ids


        entity_ids = set(entity_ids)
        parent_ids = set(parent_ids)
        */
        return null;
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
        //msgt("runStep1RoleAssignments results: " + results.toString());

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
            directDvObjectIds.add(dvId);
        }      
        return true;
    }
    
    private boolean runStep2DirectAssignments(){
        
        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyData object,first check 'hasError()'");
        }
        msgt("runStep2DirectAssignments");
        
        List<Object[]> results = this.dvObjectServiceBean.getDvObjectInfoForMyData(directDvObjectIds);
        msgt("runStep2DirectAssignments results: " + results.toString());
//List<RoleAssignment> results = this.roleAssigneeService.getAssignmentsFor(this.userIdentifier);
        if (results == null){
            this.addErrorMessage("Sorry, there are no directly assigned Dataverses, Datasets, or Files.");
            return false;
        }else if (results.isEmpty()){
            this.addErrorMessage("Sorry, you have no assigned Dataverses, Datasets, or Files.");
            return false;
        }
    
         // Iterate through assigned objects
        //for (RoleAssignment ra : results) {
        for (Object[] ra : results) {
            Long dvId = (Long)ra[0];
            String dtype = (String)ra[1];
            Long parentId = (Long)ra[2];
            
            switch(dtype){
                case(DvObject.DATAVERSE_DTYPE_STRING):
                    if (this.idsWithDataversePermissions.containsKey(dvId)){
                        this.directDataverseIds.add(dvId);  // Direct dataverse
                    }
                    if (this.idsWithDatasetPermissions.containsKey(dvId)){
                        this.datasetParentIds.add(dvId);    // Parent to dataset
                    }
                    if (this.idsWithFilePermissions.containsKey(dvId)){
                        this.fileGrandparentFileIds.add(dvId); // Grandparent to file
                    }
                    break;
                case(DvObject.DATASET_DTYPE_STRING):
                    if (this.idsWithDatasetPermissions.containsKey(dvId)){
                        this.directDatasetIds.add(dvId); // Direct dataset
                    }
                    if (this.idsWithFilePermissions.containsKey(dvId)){
                        this.fileParentIds.add(dvId);   // Parent to file
                    }
                    break;
                case(DvObject.DATAFILE_DTYPE_STRING):
                    if (this.idsWithFilePermissions.containsKey(dvId)){
                        this.directFileIds.add(dvId); // Direct file
                    }
                    break;
            } // end switch
        }      
        
        // Direct ids no longer needed
        //
        this.directDvObjectIds = null;
        
        return true;
    }
    
    
    private boolean runStep3FilePermsAssignedAtDataverse(){
        msgt("runStep3FilePermsAssignedAtDataverse");
        if ((this.fileGrandparentFileIds == null)||(this.fileGrandparentFileIds.isEmpty())){
            return true;
        }
        
        List<Object[]> results = this.dvObjectServiceBean.getDvObjectInfoByParentIdForMyData(this.fileGrandparentFileIds);
        msgt("runStep3FilePermsAssignedAtDataverse results: " + results.toString());
        
        if (results == null){
            this.addErrorMessage("Sorry, no Dataset were found with those Ids");
            return false;
        }else if (results.isEmpty()){
            this.addErrorMessage("Sorry, no Dataset were found with those Ids");
            return false;
        }
    
        // Iterate through object list
        //
        for (Object[] ra : results) {
            Long dvId = (Long)ra[0];
            String dtype = (String)ra[1];
            Long parentId = (Long)ra[2];
            // Should ALWAYS be a Dataset!
            if (dtype.equals(DvObject.DATASET_DTYPE_STRING)){  
                this.fileParentIds.add(dvId);
            }
        }
        
        return true;
    }
    /*
    private void postStep2Cleanup(){
        // Clear step1 lookups
        idsWithDataversePermissions = null;
        idsWithDatasetPermissions = null;
        idsWithFilePermissions = null;
        directDvObjectIds = null;   // Direct ids no longer needed
    }*/
    
    
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
