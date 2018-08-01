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
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.search.SearchFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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
    
    // !! RMP - Excluded by default; don't have cases yet to make this true
    private boolean excludeHarvestedData = true;    
    //private String searchTerm = "*";
    
    // --------------------
    private DataverseRolePermissionHelper rolePermissionHelper;
    private RoleAssigneeServiceBean roleAssigneeService;
    private DvObjectServiceBean dvObjectServiceBean;
    private GroupServiceBean groupService; 
    //private RoleAssigneeServiceBean roleService = new RoleAssigneeServiceBean();
    //private MyDataQueryHelperServiceBean myDataQueryHelperService;
    // --------------------
    public boolean errorFound = false;
    public String errorMessage = null;
    // --------------------

    public Map<Long, Boolean> harvestedDataverseIds = new HashMap<>();

    // Populated in initial query.  DvObject ids -- regardless of Dtype,
    // are sorted into respective buckets in regard to permissions.
    // The same id may appear in multiple lists--and more than once
    //
    // ----------------------------
    // POPULATED IN STEP 1 (1st query)
    // ----------------------------
    public Map<Long, Long> childToParentIds = new HashMap<>();
    public Map<Long, Boolean> idsWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> idsWithFilePermissions = new HashMap<>();  // { role id : true }

    private List<Long> directDvObjectIds = new ArrayList<>();

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

    
    public MyDataFinder(DataverseRolePermissionHelper rolePermissionHelper, RoleAssigneeServiceBean roleAssigneeService, DvObjectServiceBean dvObjectServiceBean, GroupServiceBean groupService) {
        this.rolePermissionHelper = rolePermissionHelper;
        this.roleAssigneeService = roleAssigneeService;
        this.dvObjectServiceBean = dvObjectServiceBean;
        this.groupService = groupService;
        this.loadHarvestedDataverseIds();
    }

    private void loadHarvestedDataverseIds(){
        
        for (Long id : dvObjectServiceBean.getAllHarvestedDataverseIds()){
            harvestedDataverseIds.put(id, true);
        }
        
    }
    
    public void setExcludeHarvestedData(boolean val){
        
        this.excludeHarvestedData = val;
    }
            
    public boolean isHarvestedDataExcluded(){
        return excludeHarvestedData;
    }
            
    /**
     * Check if a dvobject id is in the Harvested Id dict
     * @param id
     * @return 
     */
    private boolean isHarvesteDataverseId(Long id){
        
        if (id == null){
            return false;
        }
    
        if (this.harvestedDataverseIds.containsKey(id)){
            return true;
        }
        return false;
    }
    
    public void initFields(){
        // ----------------------------
        // POPULATED IN STEP 1 (1st query)
        // ----------------------------
        this.childToParentIds = new HashMap<>();
        this.idsWithDataversePermissions = new HashMap<>();  // { role id : true }
        this.idsWithDatasetPermissions = new HashMap<>();  // { role id : true }
        this.idsWithFilePermissions = new HashMap<>();  // { role id : true }

        this.directDvObjectIds = new ArrayList<>();

        // Lists later used to format Solr Queries
        // 
        // ----------------------------
        // POPULATED IN STEP 2 (2nd query)
        // ----------------------------
        this.directDataverseIds = new ArrayList<>();
        this.directDatasetIds = new ArrayList<>();
        this.directFileIds = new ArrayList<>();

        this.datasetParentIds = new ArrayList<>(); // dataverse has dataset permissions

        this.fileParentIds = new ArrayList<>();   // dataset has file permissions      
        this.fileGrandparentFileIds = new ArrayList<>();  // dataverse has file permissions
    
    }
   
    public DataverseRolePermissionHelper getRolePermissionHelper(){
        return this.rolePermissionHelper;
    }

    public void runFindDataSteps(MyDataFilterParams filterParams){
        
      
        this.filterParams = filterParams;
        this.userIdentifier = this.filterParams.getUserIdentifier();
        
        if (this.filterParams.hasError()){
            this.addErrorMessage(filterParams.getErrorMessage());
            return;
        }
        
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
    
    public List<String> getSolrFilterQueriesForTotalCounts(){
        
        return this.getSolrFilterQueries(true);
    }
    
    
    public List<String> getSolrFilterQueries(){
        
        return this.getSolrFilterQueries(false);
    }
    
    /**
     * Get the final queries for the Solr Search object
     * 
     * @return 
     */
    private List<String> getSolrFilterQueries(boolean totalCountsOnly){
        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object, first check 'hasError()'");
        }
        
        // init filterQueries list
        List<String> filterQueries = new ArrayList<>();

        // -----------------------------------------------------------------
        // (1) Add entityId/parentId FQ 
        //  - by entityId (dvObject id) and parentId (dvObject ownerId)
        // -----------------------------------------------------------------
        String dvObjectFQ = this.getSolrDvObjectFilterQuery();
        if (dvObjectFQ ==null){
            this.addErrorMessage(DataRetrieverAPI.MSG_NO_RESULTS_FOUND);
            return null;
        }
        filterQueries.add(dvObjectFQ);
        // -----------------------------------------------------------------
        // For total counts, don't filter by publicationStatus or DvObjectType
        // -----------------------------------------------------------------
        if (totalCountsOnly == true){
            return filterQueries;
        }

        // -----------------------------------------------------------------
        // (2) FQ by dvObjectType
        // -----------------------------------------------------------------
        filterQueries.add(this.filterParams.getSolrFragmentForDvObjectType());
        //fq=dvObjectType:(dataverses+OR+datasets+OR+files)
        //fq=(dvObjectType:Dataset)
        //filterQueries.add("dvObjectType:(dataverses OR datasets OR files)");

        // -----------------------------------------------------------------
        // (3) FQ by Publication Status
        // -----------------------------------------------------------------
        filterQueries.add(this.filterParams.getSolrFragmentForPublicationStatus());
        //fq=publicationStatus:"Unpublished"&fq=publicationStatus:"Draft"
        
        return filterQueries;
    }
    

   
    
    
    
    public String getSolrDvObjectFilterQuery(){

        if (this.hasError()){
            throw new IllegalStateException("Error encountered earlier.  Before calling this method on a MyDataFinder object,first check 'hasError()'");
        }

        // Build lists of Ids
        List<Long> entityIds = new ArrayList<>();
        List<Long> parentIds = new ArrayList<>();
        List<Long> datasetParentIdsForFQ = new ArrayList<>();
        List<Long> fileParentIdsForFQ = new ArrayList<>();

        if (this.filterParams.areDataversesIncluded()){
            entityIds.addAll(this.directDataverseIds); // dv ids
        }        
        if (this.filterParams.areDatasetsIncluded()){
            entityIds.addAll(this.directDatasetIds);  // dataset ids
            parentIds.addAll(this.datasetParentIds);  // dv ids that are dataset parents
            datasetParentIdsForFQ.addAll(this.datasetParentIds);
        }
        
         if (this.filterParams.areFilesIncluded()){
            entityIds.addAll(this.directFileIds); // file ids
            parentIds.addAll(this.fileParentIds); // dataset ids that are file parents
            fileParentIdsForFQ.addAll(this.fileParentIds);
        }
        
        // Remove duplicates by Creating a Set
        //
        Set<Long> distinctEntityIds = new HashSet<>(entityIds);
        Set<Long> distinctParentIds = new HashSet<>(parentIds);


        if ((distinctEntityIds.isEmpty()) && (distinctParentIds.isEmpty())) {
            this.addErrorMessage(DataRetrieverAPI.MSG_NO_RESULTS_FOUND);
            return null;
        }
        
        // See if we can trim down the list of distinctEntityIds
        //  If we have the parent of a distinctEntityId in distinctParentIds,
        //  then we query it via the parent
        //        
        List<Long> finalDirectEntityIds = new ArrayList<>();
        for (Long idToCheck : distinctEntityIds){
            if (this.childToParentIds.containsKey(idToCheck)){  // Do we have the parent in our map?

                // we are not checking the parent of dataverses, so add this explicitly
                // Similar to SEK 7/015 - all direct dataverse ids are used because child dataverses with direct assignments are being lost.
                //
                if (this.directDataverseIds.contains(idToCheck)){
                    // Add all dataverse ids explicitly
                    finalDirectEntityIds.add(idToCheck);
                    
                } else if (!distinctParentIds.contains(this.childToParentIds.get(idToCheck))){
                    // Is the parent also in our list of Ids to query?
                    // No, then let's check this id directly
                    //
                    finalDirectEntityIds.add(idToCheck);
                } 
            }
        }
        // Set the distinctEntityIds to the finalDirectEntityIds
        //distinctEntityIds = new HashSet<>(distinctEntityIds);
        distinctEntityIds = new HashSet<>(finalDirectEntityIds);

        // Start up a SolrQueryFormatter for building clauses
        //
        SolrQueryFormatter sqf = new SolrQueryFormatter();

        // Build clauses
        String entityIdClause = null;
        if (distinctEntityIds.size() > 0){
            entityIdClause = sqf.buildIdQuery(distinctEntityIds, SearchFields.ENTITY_ID, null);
        }
        
        String parentIdClause = null;
        if (distinctParentIds.size() > 0){
            parentIdClause = sqf.buildIdQuery(distinctParentIds, SearchFields.PARENT_ID, "datasets OR files");  
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
                
        List<Object[]> results = this.roleAssigneeService.getAssigneeAndRoleIdListFor(filterParams);
        
        //logger.info("runStep1RoleAssignments results: " + results.toString());

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
    
        // Iterate through assigned objects, a single object may end up in 
        // multiple "buckets"
        for (Object[] ra : results) {
            Long dvId = (Long)ra[0];
            Long roleId = (Long)ra[1];
            
            
            
            //----------------------------------
            // Is this is a harvested Dataverse?
            // If so, skip it.
            //----------------------------------
            if ((this.isHarvestedDataExcluded())&&(this.isHarvesteDataverseId(dvId))){
                continue;
            }
            
            //----------------------------------
            // Put dvId in 1 or more buckets, depending pn if role
            // applies to a Dataverse, Dataset, and/or File
            //----------------------------------
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
//List<RoleAssignment> results = this.roleAssigneeService.getAssignmentsFor(this.userIdentifier);
        if (results.isEmpty()){
            this.addErrorMessage("Sorry, you have no assigned Dataverses, Datasets, or Files.");
            return false;
        }
    
        Integer dvIdAsInteger;
        Long dvId;
        String dtype;
        Long parentId;
        
        // -----------------------------------------------
        // Iterate through assigned objects
        // -----------------------------------------------
        for (Object[] ra : results) {
            dvIdAsInteger = (Integer)ra[0];     // ?? Why?
            dvId = new Long(dvIdAsInteger);
            dtype = (String)ra[1];
            parentId = (Long)ra[2];
            
            
            // -----------------------------------------------
            // If this object is harvested, then skip it...
            // -----------------------------------------------
            if (this.isHarvestedDataExcluded()){
                if ((this.isHarvesteDataverseId(dvId))||(this.isHarvesteDataverseId(parentId))){
                    continue;
                }
            }
            
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
                        // Also show the Dataset--even though the permissions don't apply directly
                        //  e.g. The Permissions flows:
                        //      from the DV -> through the DS -> to the file
                        this.datasetParentIds.add(dvId);    // Parent to dataset
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
        if ((this.fileGrandparentFileIds == null)||(this.fileGrandparentFileIds.isEmpty())){
            return true;
        }
        
        List<Object[]> results = this.dvObjectServiceBean.getDvObjectInfoByParentIdForMyData(this.fileGrandparentFileIds);
        /*  SEK 07/09 Ticket 2329
        Removed failure for empty results - if there are none let it go
        */
        if (results.isEmpty()){
            return true;        // RMP, shouldn't throw an error if no results           
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
}   // end: MyDataFinder
