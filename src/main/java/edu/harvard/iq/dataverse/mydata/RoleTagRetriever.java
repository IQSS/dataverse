/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.RoleAssigneeServiceBean;
import edu.harvard.iq.dataverse.search.SolrQueryResponse;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.search.SearchConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;

/**
 * Input:  dvObject id, parent Id, and dvObject type (from Solr)
 * Output: For each dvObject id, a list of Role names
 * 
 * @author rmp553
 */
public class RoleTagRetriever {
    
    private static final Logger logger = Logger.getLogger(RoleTagRetriever.class.getCanonicalName());

    private final DataverseRolePermissionHelper rolePermissionHelper;
    private final RoleAssigneeServiceBean roleAssigneeService;
    private final DvObjectServiceBean dvObjectServiceBean;

    public boolean errorFound = false;
    public String errorMessage = null;
    
    //public Map<Long, String> roleNameLookup = new HashMap<>();    // { role id : role name }

    
    private Map<Long, List<Long>> idToRoleListHash;    // { dvobject id : [role id, role id] }
    
    private Map<Long, Long> childToParentIdHash;     // { dvobject id : parent id }
    
    private Map<Long, String> idToDvObjectType;     // { dvobject id : dvobject type }
    
    private List<Long> datasetIdsNeedingParentIds;
    
    private List<Long> finalCardIds;
    
    private Map<Long, List<String>> finalIdToRolesHash;  // { dvobject id : [role name, role name] }

    // ----------------------------------
    // Constructor
    // ----------------------------------
    public RoleTagRetriever(DataverseRolePermissionHelper rolePermissionHelper
                        , RoleAssigneeServiceBean roleAssigneeService
                        , DvObjectServiceBean dvObjectServiceBean){
       this.rolePermissionHelper = rolePermissionHelper;
       this.roleAssigneeService = roleAssigneeService;
       this.dvObjectServiceBean = dvObjectServiceBean;
    }
    
    public void loadRoles(String userIdentifier, SolrQueryResponse solrQueryResponse){

        if (userIdentifier == null){
            throw new NullPointerException("RoleTagRetriever.constructor. userIdentifier cannot be null");
        }
        
        if (solrQueryResponse == null){
            throw new NullPointerException("RoleTagRetriever.constructor. solrQueryResponse cannot be null");
        }
        
        // (1) Reset variables
        initLookups();
        
        // (2) Load roles from solr docs
        loadInfoFromSolrResponseDocs(solrQueryResponse);
        
        // (3) Load grandparent ids, if needed
        findDataverseIdsForFiles();
        
        // (4) Retrieve the role ids
        retrieveRoleIdsForDvObjects(userIdentifier);

        // (5) Prepare final role lists
        prepareFinalRoleLists();

        //showRoleListHash();

    }
    
    
    private void initLookups(){
        
        this.errorFound = false;
        this.errorMessage = null;
        
        this.idToRoleListHash = new HashMap();    // { dvobject id : [role id, role id] }

        this.childToParentIdHash = new HashMap();     // { dvobject id : parent id }

        this.idToDvObjectType = new HashMap();     // { dvobject id : dvobject type }

        this.finalIdToRolesHash = new HashMap();  

        this.datasetIdsNeedingParentIds = new ArrayList<>();

        this.finalCardIds = new ArrayList<>();
    }
        
    private void addIdNeedingRoleRetrieval(Long dvObjectId){
        if (dvObjectId == null){
            return;
        }
        
        // initialize with dvObject id and empty list of role ids
        //
        if (!this.idToRoleListHash.containsKey(dvObjectId)){
            this.idToRoleListHash.put(dvObjectId, new ArrayList<Long>());
        }
    }
    
    public void showRoleListHash(){
        
        msgt("showRoleListHash");
        for (Map.Entry<Long, List<Long>> entry : idToRoleListHash.entrySet()) {
            msg("id: " + entry.getKey() + " | values: " + entry.getValue().toString());            
        }
        
        msgt("show idToDvObjectType");
        for (Map.Entry<Long, String> entry : idToDvObjectType.entrySet()) {
            msg("dv id: " + entry.getKey() + " | type: " + entry.getValue());            
        }

        for (Map.Entry<Long, List<String>> entry : finalIdToRolesHash.entrySet()) {
            msg("id: " + entry.getKey() + " | values: " + entry.getValue().toString());            
        }
        
    }
    
    
    private void addRoleIdForHash(Long dvObjectId, Long roleId){
        if ((dvObjectId == null)||(roleId == null)){
            return;
        }
        
        if (!this.idToRoleListHash.containsKey(dvObjectId)){
            logger.warning("DvObject id not found in hash (shouldn't happen): " + dvObjectId);
            return;
        }
        List<Long> roldIdList = this.idToRoleListHash.get(dvObjectId);
        roldIdList.add(roleId);   
        
        this.idToRoleListHash.put(dvObjectId, roldIdList);
        
    }
    
    private void loadInfoFromSolrResponseDocs(SolrQueryResponse solrQueryResponse){

        if (solrQueryResponse == null){
            throw new NullPointerException("RoleTagRetriever.constructor. solrQueryResponse cannot be null");
        }

        // ----------------------------------
        // Load initial data
        // ----------------------------------
        msgt("load initial data");
        //  Iterate through Solr cards
        //
        for (SolrSearchResult doc : solrQueryResponse.getSolrSearchResults()){

            // -------------------------------------------------
            // (a) retrieve Card Id and DvObject type
            // -------------------------------------------------
            finalCardIds.add(doc.getEntityId());

            String dtype = doc.getType();
            if (dtype == null){
                throw new NullPointerException("The dvobject type cannot be null for SolrSearchResult");
            }
            logger.fine("\nid: " + doc.getEntityId() + " dtype: " + dtype);
            
            // -------------------------------------------------
            // (b) Populate dict of { dvObject id : dtype } 
            //      e.g. { 3 : 'Dataverse' }
            // -------------------------------------------------
            this.idToDvObjectType.put(doc.getEntityId(), doc.getType());

            // -------------------------------------------------
            // (c) initialize dict of { dvObject id : [ (empty list for role ids) ] } 
            // -------------------------------------------------
            addIdNeedingRoleRetrieval(doc.getEntityId());
            
            // -------------------------------------------------
            // For datasets and files, check parents
            // -------------------------------------------------
            if (!(dtype.equals(SearchConstants.DATAVERSES))){

                // -------------------------------------------------
                // (d) Add to the childToParentIdHash  { child id : parent id }
                // -------------------------------------------------
                String parentIdString = doc.getParent().get("id");
                if (parentIdString == null){
                    throw new NullPointerException("A dataset or file parent cannot be null for SolrSearchResult");
                }

                Long parentId = Long.parseLong(parentIdString);
                logger.fine("\nparentId: " + parentId);

                this.childToParentIdHash.put(doc.getEntityId(), parentId);

                // -------------------------------------------------
                // (e) For the parent, add to dict of 
                //      { dvObject id : [ (empty list for role ids) ] } 
                //          - similar to (c) above
                // -------------------------------------------------
                addIdNeedingRoleRetrieval(parentId);
                
                // -------------------------------------------------
                // (f) Add the parent to the DvObject type lookup { dvObject id : dtype } 
                //          - similar to (b) above
                // -------------------------------------------------
                if (doc.getType().equals(SearchConstants.FILES)){
                    logger.fine("It's a file");

                    // -------------------------------------------------
                    // (f1) This is a file, we know the parent is a Dataset
                    // -------------------------------------------------
                    this.idToDvObjectType.put(parentId, DvObject.DATASET_DTYPE_STRING);
                    
                    // -------------------------------------------------
                    // (g) For files, we'll need to get roles from the grandparent--e.g., the dataverse
                    // -------------------------------------------------
                    this.datasetIdsNeedingParentIds.add(parentId);
    
                }if (dtype.equals(SearchConstants.DATASETS)){
                    logger.fine("It's a dataset");

                    // -------------------------------------------------
                    // (f2) This is a Dataset, we know the parent is a Dataverse
                    // -------------------------------------------------
                    this.idToDvObjectType.put(parentId, DvObject.DATAVERSE_DTYPE_STRING);
                }
            }
                        
            // -------------------------------------------------
            // initialize final hash of dvObject id and empty list of role names
            //          { dvObject id : [ (empty list for role nams) ] } 
            // -------------------------------------------------
            this.finalIdToRolesHash.put(doc.getEntityId(), new ArrayList<String>());

        }
        
    }
    
    private void findDataverseIdsForFiles(){
        msgt("findDataverseIdsForFiles: " + datasetIdsNeedingParentIds.toString());
        
        if (this.datasetIdsNeedingParentIds == null){
            throw new NullPointerException("findDataverseIdsForFiles should not be null");
        }
        
        if (this.datasetIdsNeedingParentIds.isEmpty()){
            logger.fine("No ids found!");
            return;
        }
        
        List<Object[]> results = this.dvObjectServiceBean.getDvObjectInfoForMyData(this.datasetIdsNeedingParentIds);
        logger.fine("findDataverseIdsForFiles results count: " + results.size());
       
        if (results.isEmpty()){
            return;       
        }
        
        Integer dvIdAsInteger;
        Long dvId;
        String dtype;
        Long parentId;
        
        // Iterate through object list
        //
        for (Object[] ra : results) {
            dvIdAsInteger = (Integer)ra[0];     // ?? Why, should be a Long
            dvId = new Long(dvIdAsInteger);
            dtype = (String)ra[1];
            parentId = (Long)ra[2];
                       
            //msg("result: dvId: " + dvId + " |dtype: " + dtype + " |parentId: " + parentId);
            // Should ALWAYS be a Dataset!
            if (dtype.equals(DvObject.DATASET_DTYPE_STRING)){  
                this.childToParentIdHash.put(dvId, parentId); // dataset -> dataverse
                this.addIdNeedingRoleRetrieval(parentId); // dataset -> dataverse
                this.idToDvObjectType.put(parentId, DvObject.DATAVERSE_DTYPE_STRING);
            }
        }
    }
    
    
    private boolean retrieveRoleIdsForDvObjects(String userIdentifier){
                
        if (userIdentifier == null){
            throw new NullPointerException("RoleTagRetriever.constructor. userIdentifier cannot be null");
        }        
        
        if (this.idToRoleListHash.isEmpty()){
            return true;
        }

        List<Long> dvObjectIdList = new ArrayList<>(this.idToRoleListHash.keySet());
        if (dvObjectIdList.isEmpty()){
            return true;
        }
        //logger.fine("dvObjectIdList: " + dvObjectIdList.toString());
        String assigneeIdentifer = MyDataUtil.formatUserIdentifierAsAssigneeIdentifier(userIdentifier);
        List<Object[]> results = this.roleAssigneeService.getRoleIdsFor(assigneeIdentifer, dvObjectIdList);
        
        //msgt("runStep1RoleAssignments results: " + results.toString());

        if (results == null){
            this.addErrorMessage("Sorry, the roleAssigneeService isn't working.");
            return false;
        }else if (results.isEmpty()){
            logger.log(Level.WARNING, "No roles were found for user {0} with ids {1}", new Object[]{userIdentifier, dvObjectIdList.toString()});
            this.addErrorMessage("Sorry, no roles were found.");              
            return false;
        }
    
        // Iterate through assigned objects, a single object may end up in 
        // multiple "buckets"
        for (Object[] ra : results) {
            Long dvId = (Long)ra[0];
            Long roleId = (Long)ra[1];
            
            this.addRoleIdForHash(dvId, roleId);
            //logger.fine("dv id: " + dvId + " | roleId: " + roleId);
        }      
        return true;
    }
    
    private List<String> getFormattedRoleListForId(Long dvId){
        
        if (dvId==null){
            return null;
        }
        if (!this.idToRoleListHash.containsKey(dvId)){
            return null;
        }
        
        List<String> roleNames = new ArrayList();
        for (Long roleId : this.idToRoleListHash.get(dvId) ){
            String roleName = this.rolePermissionHelper.getRoleName(roleId);
            if (roleName != null){
                roleNames.add(roleName);
            }
        }
        return roleNames;
        
    }
    
    public boolean hasRolesForCard(Long dvObjectId){
        if (dvObjectId == null){
            return false;
        }
        return this.finalIdToRolesHash.containsKey(dvObjectId);
    }

    public List<String> getRolesForCard(Long dvObjectId){
        if (!this.hasRolesForCard(dvObjectId)){
            return null;
        }

        return this.finalIdToRolesHash.get(dvObjectId);
    }

    public JsonArrayBuilder getRolesForCardAsJSON(Long dvObjectId){
        if (!this.hasRolesForCard(dvObjectId)){
            return null;
        }
               
        JsonArrayBuilder jsonArray = Json.createArrayBuilder();
        
        for (String roleName : this.finalIdToRolesHash.get(dvObjectId)){            
            jsonArray.add(roleName);            
        }
        return jsonArray;             
    }
    
    
    
    /**
     * For the cards, make a dict of { dv object id : [role name, role name, etc ]}
     * 
     */
    public void prepareFinalRoleLists(){
        
        msgt("prepareFinalRoleLists");
        
        if (finalCardIds.isEmpty()){
            return;
        }
        
        List<String> formattedRoleNames;
        List<String> finalRoleNames;
        
        for (Long dvIdForCard : this.finalCardIds) {
           //msgt("dvIdForCard: " + dvIdForCard);

            // -------------------------------------------------
            // (a) Make a new array with the role names for the card
            // -------------------------------------------------
            finalRoleNames = new ArrayList();
            if (!this.idToDvObjectType.containsKey(dvIdForCard)){
                throw new IllegalStateException("All dvObject ids from solr should have their dvObject types in this hash");                 
            }

            // -------------------------------------------------
            // (b) Add direct role assignments -- may be empty
            // -------------------------------------------------
            formattedRoleNames = getFormattedRoleListForId(dvIdForCard);
            if (formattedRoleNames != null){
                finalRoleNames.addAll(formattedRoleNames);
            }
            //msg("Roles so far: " + finalRoleNames.toString());
            
            // -------------------------------------------------
            // (c) get parent id
            // -------------------------------------------------
            Long parentId = null;
            if (this.childToParentIdHash.containsKey(dvIdForCard)){
                parentId = this.childToParentIdHash.get(dvIdForCard);
            }else{
                // -------------------------------------------------
                // No parent!  Store roles and move to next id
                // -------------------------------------------------
                finalIdToRolesHash.put(dvIdForCard, this.formatRoleNames(finalRoleNames));
                //msg("No parent found for id");
                continue;
            }
            
            // -------------------------------------------------
            // (d) get parent id
            // -------------------------------------------------
            String dtype = this.idToDvObjectType.get(dvIdForCard);
            //msg("dtype: " + dtype);

            switch(dtype){                
                //case(DvObject.DATAVERSE_DTYPE_STRING): // No indirect assignments                   

                case(SearchConstants.DATASETS):
                    // -------------------------------------------------
                    // (d1) May have indirect assignments re: dataverse
                    // -------------------------------------------------
                    formattedRoleNames = getFormattedRoleListForId(parentId);
                    if (formattedRoleNames != null){
                        finalRoleNames.addAll(formattedRoleNames);
                        //msg("Roles from dataverse: " + finalRoleNames.toString());
                    }
                    break;
                case(SearchConstants.FILES):
                    // -------------------------------------------------
                    // (d2) May have indirect assignments re: dataset 
                    // -------------------------------------------------
                    formattedRoleNames = getFormattedRoleListForId(parentId);
                    if (formattedRoleNames != null){
                        finalRoleNames.addAll(formattedRoleNames);
                        //msg("Roles from dataset: " + formattedRoleNames.toString());

                    }
                    // May have indirect assignments re: dataverse
                    //
                    if (this.childToParentIdHash.containsKey(parentId)){
                        Long grandparentId = this.childToParentIdHash.get(parentId);
                        formattedRoleNames = getFormattedRoleListForId(grandparentId);
                        if (formattedRoleNames != null){
                            finalRoleNames.addAll(formattedRoleNames);
                            //msg("Roles from dataverse: " + formattedRoleNames.toString());

                        }
                    }  
                   
                    break;
            } // end switch
            //msg("Roles from dataverse: " + formattedRoleNames.toString());
            finalIdToRolesHash.put(dvIdForCard, formatRoleNames(finalRoleNames));
            //String key = 
            //Object value = entry.getValue();
            // ...
        }
        
    }
    
    private List<String> formatRoleNames(List<String> roleNames){
        if (roleNames==null){
            return null;
        }

        // remove duplicates
        Set<String> distinctRoleNames = new HashSet<>(roleNames);
            
        // back to list
        roleNames = new ArrayList<>(distinctRoleNames);

        // sort list
        Collections.sort(roleNames);
        
        return roleNames;
        
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
       // System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
}
