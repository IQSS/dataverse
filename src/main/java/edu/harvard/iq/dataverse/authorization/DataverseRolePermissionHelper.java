package edu.harvard.iq.dataverse.authorization;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.DataverseRoleServiceBean;
import edu.harvard.iq.dataverse.authorization.DataverseRole;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;

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
public class DataverseRolePermissionHelper {
    
    @EJB
    DataverseRoleServiceBean roleService;

    public Map<Long, Boolean> rolesWithDataversePermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> rolesWithDatasetPermissions = new HashMap<>();  // { role id : true }
    public Map<Long, Boolean> rolesWithFilePermissions = new HashMap<>();  // { role id : true }

    public Map<Long, String> roleNameLookup = new HashMap<>();    // { role id : role name }
     
     
    /**
     * Initialize Map objects by iterating over role objects
     * 
     */
    public void DataverseRolePermissionHelper(){
         
        // Load Role Information
        //
        for(DataverseRole role : roleService.findAll()){
        
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
    
}
