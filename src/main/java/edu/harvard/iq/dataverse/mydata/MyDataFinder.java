/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.search.SearchConstants;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.apache.commons.io.FileUtils;
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
    MyDataFilterParams filterParams;
    
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
    public Map<Long, Long> childToParentIds = new HashMap();
    public Map<Long, Boolean> idsWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithFilePermissions = new HashMap<>();  // { role id : true }

    private List<Long> directDvObjectIds = new ArrayList<Long>();

    // Lists later used to format Solr Queries
    // 
    // ----------------------------
    // POPULATED IN STEP 2 (2nd query)
    // ----------------------------
    private List<Long> directDataverseIds = new ArrayList<>();
    private List<Long> directDatasetIds = new ArrayList<>();
    private List<Long> directFileIds = new ArrayList<>();
    
    private List<Long> datasetParentIds = new ArrayList<>(); // dataverse has dataset permissions

    private List<Long> fileParentIds = new ArrayList<>();   // dataset has file permissions      
    private List<Long> fileGrandparentFileIds = new ArrayList<>();  // dataverse has file permissions

    
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
    
    /*public void runFindDataSteps(String userIdentifier){
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
    }*/

    public void runFindDataSteps(MyDataFilterParams filterParams){
        
      
        this.filterParams = filterParams;
        this.userIdentifier = this.filterParams.getUserIdentifier();
        
        if (this.filterParams.hasError()){
            this.addErrorMessage(filterParams.getErrorMessage());
            return;
        }
        
        msgt("runFindDataSteps: " + this.userIdentifier);
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
    
    /**
     * Get the final queries for the Solr Search object
     * 
     * @return 
     */
    public List<String> getSolrFilterQueries(){
        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object, first check 'hasError()'");
        }
        
        List<String> filterQueries = new ArrayList<>();

        // FQ by dvObjectType
        //
        filterQueries.add(this.filterParams.getSolrFragmentForDvObjectType());
        //fq=dvObjectType:(dataverses+OR+datasets+OR+files)
        //fq=(dvObjectType:Dataset)
        //filterQueries.add("dvObjectType:(dataverses OR datasets OR files)");
        
        // FQ by Publication Status
        //
        filterQueries.add(this.filterParams.getSolrFragmentForPublicationStatus());
        //fq=publicationStatus:"Unpublished"&fq=publicationStatus:"Draft"

        // FQ by entityId (dvObject id) and parentId (dvObject ownerId)
        //
        String dvObjectFQ = this.getSolrDvObjectFilterQuery();
        if (dvObjectFQ ==null){
            this.addErrorMessage(DataRetrieverAPI.MSG_NO_RESULTS_FOUND);
            return null;
        }
        filterQueries.add(this.getSolrDvObjectFilterQuery());
        
        return filterQueries;
    }
    
    public String getSolrDvObjectFilterQuery(){

        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object,first check 'hasError()'");
        }

        // Build lists of Ids
        List<Long> entityIds = new ArrayList<>();
        List<Long> parentIds = new ArrayList<>();

        if (this.filterParams.areDataversesIncluded()){
            entityIds.addAll(this.directDataverseIds); // dv ids
        }        
        if (this.filterParams.areDatasetsIncluded()){
            entityIds.addAll(this.directDatasetIds);  // dataset ids
            parentIds.addAll(this.datasetParentIds);  // dv ids that are dataset parents
        }
        
         if (this.filterParams.areFilesIncluded()){
            entityIds.addAll(this.directFileIds); // file ids
            parentIds.addAll(this.fileParentIds); // dataset ids that are file parents
        }
        
        // Remove duplicates by Creating a Set
        //
        Set<Long> distinctEntityIds = new HashSet<>(entityIds);
        Set<Long> distinctParentIds = new HashSet<>(parentIds);
        
        if ((distinctEntityIds.size()==0)&&(distinctParentIds.size()==0)){
            this.addErrorMessage(DataRetrieverAPI.MSG_NO_RESULTS_FOUND);
            return null;
        }
        
        msg("distinctEntityIds (1): " + distinctEntityIds.size());
        msg("distinctParentIds: " + distinctEntityIds.size());
        
        // See if we can trim down the list of distinctEntityIds
        //  If we have the parent of a distinctEntityId in distinctParentIds,
        //  then we query it via the parent
        //        
        
        List<Long> finalDirectEntityIds = new ArrayList<>();
        for (Long idToCheck : distinctEntityIds){
            if (this.childToParentIds.containsKey(idToCheck)){  // Do we have the parent in our map?
                
                // Is the parent also in our list of Ids to query?
                // No, then let's check this id directly
                //
                if (!distinctParentIds.contains(this.childToParentIds.get(idToCheck))){
                    // we are not checking the parent, so add this explicitly
                    //
                    finalDirectEntityIds.add(idToCheck);
                }
            }
        }
        // Set the distinctEntityIds to the finalDirectEntityIds
        distinctEntityIds = new HashSet<>(finalDirectEntityIds);
        
        msg("distinctEntityIds (2): " + distinctEntityIds.size());

        // Start up a SolrQueryFormatter for building clauses
        //
        SolrQueryFormatter sqf = new SolrQueryFormatter();

        // Build clauses
        String entityIdClause = null;
        if (distinctEntityIds.size() > 0){
            entityIdClause = sqf.buildIdQuery(distinctEntityIds, SearchFields.ENTITY_ID);
        }
        
        String parentIdClause = null;
        if (distinctParentIds.size() > 0){
            parentIdClause = sqf.buildIdQuery(distinctParentIds, SearchFields.PARENT_ID);            
        }
        
        if ((entityIdClause != null) && (parentIdClause != null)){
            return "(" + entityIdClause + " OR " + parentIdClause + ")";
        
        } else if (entityIdClause != null){
            // only entityIdClause
            return entityIdClause;
        
        } else if (parentIdClause != null){
            // only parentIdClause
            return parentIdClause;
        }

        // Shouldn't get here...
        return null;       
    }
    
    
    
    public String getTestString(){
        
        if (this.hasError()){
            return this.getErrorMessage();
        }
                    
        List<String> outputList = new ArrayList<>();
        
        // ----------------------
        // idsWithDatasetPermissions
        // ----------------------
        List<String> idList = new ArrayList<>();
        outputList.add("<h4>dataset ids: " + this.idsWithDatasetPermissions.size() + "</h4>");
        for (Map.Entry pair : this.idsWithDatasetPermissions.entrySet()) {          
            idList.add(pair.getKey().toString());
        }
        outputList.add("<pre>" + StringUtils.join(idList, ", ") + "</pre>");        

        // ----------------------
        // datasetParentIds
        // ----------------------
        List<String> idList2 = new ArrayList<>();
        outputList.add("<h4>datasetParentIds ids: " + this.datasetParentIds.size() + "</h4>");
        for (Long dpId : this.datasetParentIds) {          
            idList2.add(dpId.toString());
        }
        outputList.add("<pre>" + StringUtils.join(idList2, ", ") + "</pre>");

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
    
    
    /**
     * "publication_statuses" : [ name 1, name 2, etc.]
     * 
     * @return 
     */
     public JsonObjectBuilder getSelectedFilterParamsAsJSON(){
                
        JsonObjectBuilder jsonData = Json.createObjectBuilder();
        jsonData.add("publication_statuses", this.filterParams.getListofSelectedPublicationStatuses())
                .add("role_names", this.getListofSelectedRoles());
        
        return jsonData;
    }
    
    
     
    /**
     * "publication_statuses" : [ name 1, name 2, etc.]
     * 
     * @return 
     */
    public JsonArrayBuilder getListofSelectedRoles(){
        
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        
        for (Long roleId : this.filterParams.getRoleIds()){
            jsonArray.add(this.rolePermissionHelper.getRoleName(roleId));            
        }
        return jsonArray;                
    }
    
    
    private boolean runStep1RoleAssignments(){
                
        List<Object[]> results = this.roleAssigneeService.getAssigneeAndRoleIdListFor(formatUserIdentifierAsAssigneeIdentifier(this.userIdentifier)
                                        , this.filterParams.getRoleIds());
        
        //msgt("runStep1RoleAssignments results: " + results.toString());

        if (results == null){
            this.addErrorMessage("Sorry, the EntityManager isn't working (still).");
            return false;
        }else if (results.isEmpty()){
            List<String> roleNames = this.rolePermissionHelper.getRoleNamesByIdList(this.filterParams.getRoleIds());
            if ((roleNames == null)||(roleNames.isEmpty())){
                this.addErrorMessage("Sorry, you have no assigned roles.");
            }else{
                if (roleNames.size()==1){
                    this.addErrorMessage("Sorry, nothing was found for this role: " + StringUtils.join(roleNames, ", "));                
                }else{
                    this.addErrorMessage("Sorry, nothing was found for these roles: " + StringUtils.join(roleNames, ", "));                
                }
            }
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
        //msgt("runStep2DirectAssignments");
        
        List<Object[]> results = this.dvObjectServiceBean.getDvObjectInfoForMyData(directDvObjectIds);
        msgt("runStep2DirectAssignments number of results: " + results.size());
//List<RoleAssignment> results = this.roleAssigneeService.getAssignmentsFor(this.userIdentifier);
        if (results.isEmpty()){
            this.addErrorMessage("Sorry, you have no assigned Dataverses, Datasets, or Files.");
            return false;
        }
    
        Integer dvIdAsInteger;
        Long dvId;
        String dtype;
        Long parentId;
        
         // Iterate through assigned objects
        //for (RoleAssignment ra : results) {
        for (Object[] ra : results) {
            dvIdAsInteger = (Integer)ra[0];     // ?? Why?
            dvId = new Long(dvIdAsInteger);
            dtype = (String)ra[1];
            parentId = (Long)ra[2];
            
            this.childToParentIds.put(dvId, parentId);
            
            switch(dtype){
                case(DvObject.DATAVERSE_DTYPE_STRING):
                    //if (this.idsWithDataversePermissions.containsKey(dvId)){
                        this.directDataverseIds.add(dvId);  // Direct dataverse (no indirect dataverses)
                    //}
                    if (this.idsWithDatasetPermissions.containsKey(dvId)){
                        this.datasetParentIds.add(dvId);    // Parent to dataset
                    }
                    if (this.idsWithFilePermissions.containsKey(dvId)){
                        this.fileGrandparentFileIds.add(dvId); // Grandparent to file
                    }
                    break;
                case(DvObject.DATASET_DTYPE_STRING):
                    //if (this.idsWithDatasetPermissions.containsKey(dvId)){
                        this.directDatasetIds.add(dvId); // Direct dataset
                    //}
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
        msg("runStep3FilePermsAssignedAtDataverse results count: " + results.size());
        
       if (results.isEmpty()){
            this.addErrorMessage("Sorry, no Dataset were found with those Ids");
            return false;
        }
    
        Integer dvIdAsInteger;
        Long dvId;
        String dtype;
        Long parentId;
        
        // Iterate through object list
        //
        for (Object[] ra : results) {
            dvIdAsInteger = (Integer)ra[0];     // ?? Why?
            dvId = new Long(dvIdAsInteger);
            dtype = (String)ra[1];
            parentId = (Long)ra[2];
            
            this.childToParentIds.put(dvId, parentId);
            
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
