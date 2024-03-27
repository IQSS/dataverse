package edu.harvard.iq.dataverse.authorization;

import com.google.common.collect.Lists;
import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Originally created for the MyData page.
 * Helps facilitate "en masse" checking of whether Roles 
 * pertain to specific DvObject types
 * 
 * @author rmp553
 */
public class DataverseRolePermissionHelper implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DataverseRolePermissionHelper.class.getCanonicalName());
    
    //@EJB
    //DataverseRoleServiceBean roleService;

    public Map<Long, Boolean> rolesWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> rolesWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> rolesWithFilePermissions = new HashMap<>();  // { role id : true }

    public Map<Long, String> roleNameLookup = new HashMap<>();    // { role id : role name }

    public List<List<String>> rolesByDvObjectTable = Lists.newArrayList();
    
    /**
     * Initialize Map objects by iterating over role objects
     * 
     */
    public DataverseRolePermissionHelper(List<DataverseRole> roleList){
         
        // Load Role Information
        //
        for(DataverseRole role : roleList){
        
            // Does this role have Dataverse permissions?
            //
            if (role.doesDvObjectClassHavePermissionForObject(Dataverse.class)){
                this.rolesWithDataversePermissions.put(role.getId(), true);
            }

            // Does this role have Dataset permissions?
            //
            if (role.doesDvObjectClassHavePermissionForObject(Dataset.class)){
                this.rolesWithDatasetPermissions.put(role.getId(), true);
            }

            // Does this role have File permissions?
            //
            if (role.doesDvObjectClassHavePermissionForObject(DataFile.class)){
                this.rolesWithFilePermissions.put(role.getId(), true);
            }

            // Store role name in lookup
            //
            this.roleNameLookup.put(role.getId(), role.getName());      
        }
        this.loadRolesByDvObjectTable();
    }
   
    /**
     * top row: role names
     *
     */
    private void loadRolesByDvObjectTable(){
        
        List<String> row = new ArrayList<>();
        
        Set<Entry<Long,String>> roleNameSet = roleNameLookup.entrySet();
                
        // Row 1: Row Names
        row.add("");
        for (Map.Entry<Long,String> pair : roleNameSet) {
            row.add("<b>" + pair.getValue() +  "</b> (" + pair.getKey() + ")");
        }
        this.rolesByDvObjectTable.add(row);
        
        // Row 2: Dataverse role settings
        //
        row = new ArrayList<>();
        row.add("<b>Dataverse</b>");
        for (Map.Entry<Long,String> pair : roleNameSet) {
            Long role_id = pair.getKey();
            if (this.hasDataversePermissions(role_id)){
                row.add("YES");
            }else{
                row.add("--");
            }
        }
        this.rolesByDvObjectTable.add(row);

        
        // Row 3: Dataset role settings
        //
        row = new ArrayList<>();
                row.add("<b>Dataset</b>");

        for (Map.Entry<Long,String> pair : roleNameSet) {
            Long role_id = pair.getKey();
            if (this.hasDatasetPermissions(role_id)){
                row.add("YES");
            }else{
                row.add("--");
            }
        }
        this.rolesByDvObjectTable.add(row);

        // Row 4: File role settings
        //
        row = new ArrayList<>();
        row.add("<b>File</b>");
        for (Map.Entry<Long,String> pair : roleNameSet) {
            Long role_id = pair.getKey();
            if (this.hasFilePermissions(role_id)){
                row.add("YES");
            }else{
                row.add("--");
            }
        }
        this.rolesByDvObjectTable.add(row);
        
    }
  
    /**
     * Check if role contains a permission related to Files (DataFile)
     * 
     * @param role
     * @return 
     */
    public boolean hasFilePermissions(DataverseRole role){
        if (role == null){
            return false;
        }
        return this.hasFilePermissions(role.getId());
    }
    
    public boolean hasFilePermissions(Long role_id){
        if (role_id == null){
            return false;
        }
        return this.rolesWithFilePermissions.containsKey(role_id);
    }

    /**
     * Check if role contains a permission related to Datasets
     * 
     * @param role
     * @return 
     */
    public boolean hasDatasetPermissions(DataverseRole role){
        if (role == null){
            return false;
        }
        return this.hasDatasetPermissions(role.getId());
    }
    
    public boolean hasDatasetPermissions(Long role_id){
        if (role_id == null){
            return false;
        }
        return this.rolesWithDatasetPermissions.containsKey(role_id);
    }

    /**
     * Check if role contains a permission related to Dataverses
     * 
     * @param role
     * @return 
     */
    public boolean hasDataversePermissions(DataverseRole role){
        if (role == null){
            return false;
        }
        return this.hasDataversePermissions(role.getId());
    }
    
    public boolean hasDataversePermissions(Long role_id){
        if (role_id == null){
            return false;
        }
        return this.rolesWithDataversePermissions.containsKey(role_id);
    }
    
    /***
     * Get role name from lookup
     * 
     * @param role
     * @return 
     */
    public String getRoleName(DataverseRole role){
        if (role == null){
            return null;
        }
        return this.getRoleName(role.getId());
    }
    
    public String getRoleName(Long role_id){
        if (role_id == null){
            return null;
        }
        if (this.roleNameLookup.containsKey(role_id)){
            return this.roleNameLookup.get(role_id);
        }
        return null;
    }

    /*
    entries = new ArrayList<Entry<Integer, String>>(map.entrySet());
    */
    private List<Entry<Long, String>> roleNamesAsArrayList;
    
    public List<Entry<Long, String>> getRoleNamesAsArrayList(){
        return new ArrayList<>(roleNameLookup.entrySet()); 
    }
    
    public List<String[]> getRoleInfoForCheckboxes(){
        
        List<String[]> roleInfoList = new ArrayList<>();
        
        for (Entry<Long,String> entry : roleNameLookup.entrySet()){
            String idName = entry.getValue().toLowerCase().replace(" + ", "").replace(" ", "");
            // triplet:   key, name, id_name
            // Examples:  { 1, Admin, admin }, {2, File Downloader, filedownloader}
            String[] singleRole = { entry.getKey().toString(), entry.getValue(), idName };
            roleInfoList.add(singleRole);
        }
        return roleInfoList;
    }
    
    private void msg(String s){
        logger.info(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
    
    
    public List<Long> getRoleIdList(){
        
        List<Long> outputList = new ArrayList<>();
        for (Map.Entry<Long,String> pair : roleNameLookup.entrySet()) {
            outputList.add(pair.getKey());
        }
        return outputList;
    }
    
    
    public String getRoleNameListString(){
        List<String> outputList = new ArrayList<>();
        
        for (Map.Entry<Long,String> pair : roleNameLookup.entrySet()) {
            outputList.add(pair.getKey() + " --> " + pair.getValue());
        }
        return StringUtils.join(outputList, "<br />");
        
    }
    
    public String getRolesWithDataversePermissionsAsHTML(){
        
        List<String> outputList = new ArrayList<>();
        for (Map.Entry<Long,Boolean> pair : rolesWithDataversePermissions.entrySet()) {
            String roleName = this.roleNameLookup.get(pair.getKey());
            outputList.add(roleName);
        }
        return StringUtils.join(outputList, "<br />");
    }

    public String getDatasetRolesAsHTML(){
        
        List<String> outputList = new ArrayList<>();
        for (Map.Entry<Long,Boolean> pair : this.rolesWithDatasetPermissions.entrySet()) {
            String roleName = this.roleNameLookup.get(pair.getKey());
            outputList.add(roleName);
        }
        return StringUtils.join(outputList, "<br />");
    }
    
     public String getRolesWithFilePermissionsAsHTML(){
        
        List<String> outputList = new ArrayList<>();
        for (Map.Entry<Long,Boolean> pair : this.rolesWithFilePermissions.entrySet()) {
            String roleName = this.roleNameLookup.get(pair.getKey());
            outputList.add(roleName);
        }
        return StringUtils.join(outputList, "<br />");
    }
    
    public Map<Long, Boolean> getRolesWithDataversePermissions(){
        return this.rolesWithDataversePermissions;
    }
    
    public Map<Long, Boolean> getRolesWithDatasetPermissions(){
        return this.rolesWithDatasetPermissions;
    }
    
    public Map<Long, Boolean> getRolesWithFilePermissions(){
        return this.rolesWithFilePermissions;
    }
    
    public List<List<String>> getRolesByDvObjectTable(){
         return this.rolesByDvObjectTable;
    }
    
    public List<String> getRoleNamesByIdList(List<Long> idList){
        if ((idList==null)||(idList.isEmpty())){
            return null;
        }
        List<String> roleNameList = new ArrayList<>();
        for (Long roleId : idList){
            if (this.roleNameLookup.containsKey(roleId)){
                roleNameList.add(this.roleNameLookup.get(roleId));
            }
        }
        return roleNameList;
    }
}
