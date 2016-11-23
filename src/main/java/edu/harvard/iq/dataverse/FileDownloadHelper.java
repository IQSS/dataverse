/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author skraffmi
 * 
 *
 */
 @ViewScoped
 @Named
public class FileDownloadHelper implements java.io.Serializable {
    
    @Inject
    DataverseSession session;

    @EJB
    PermissionServiceBean  permissionService;

    
    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { FileMetadata.id : Boolean } 
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    
    
    /**
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     * 
     * Should there be a Explore WorldMap Button for this file?
     *   See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  (1) Does the file have MapLayerMetadata?
     *  (2) Are the proper settings in place
     * 
     * @param fm fileMetadata
     * @return boolean
     */
   public boolean canDownloadFile(FileMetadata fileMetadata){
        if (fileMetadata == null){
            return false;
        }
       
        if ((fileMetadata.getId() == null) || (fileMetadata.getDataFile().getId() == null)){
            return false;
        } 
        
        // --------------------------------------------------------------------        
        // Grab the fileMetadata.id and restriction flag                
        // --------------------------------------------------------------------
        Long fid = fileMetadata.getId();
        //logger.info("calling candownloadfile on filemetadata "+fid);
        boolean isRestrictedFile = fileMetadata.isRestricted();
        
        // --------------------------------------------------------------------
        // Has this file been checked? Look at the DatasetPage hash
        // --------------------------------------------------------------------
        if (this.fileDownloadPermissionMap.containsKey(fid)){
            // Yes, return previous answer
            //logger.info("using cached result for candownloadfile on filemetadata "+fid);
            return this.fileDownloadPermissionMap.get(fid);
        }
        //----------------------------------------------------------------------
        //(0) Before we do any testing - if version is deaccessioned and user
        // does not have edit dataset permission then may download
        //----------------------------------------------------------------------
        
       if (fileMetadata.getDatasetVersion().isDeaccessioned()) {
           if (this.doesSessionUserHavePermission(Permission.EditDataset, fileMetadata)) {
               // Yes, save answer and return true
               this.fileDownloadPermissionMap.put(fid, true);
               return true;
           } else {
               this.fileDownloadPermissionMap.put(fid, false);
               return false;
           }
       }

        // --------------------------------------------------------------------
        // (1) Is the file Unrestricted ?        
        // --------------------------------------------------------------------
        if (!isRestrictedFile){
            // Yes, save answer and return true
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
        
        // --------------------------------------------------------------------
        // Conditions (2) through (4) are for Restricted files
        // --------------------------------------------------------------------
        
        // --------------------------------------------------------------------
        // (2) In Dataverse 4.3 and earlier we required that users be authenticated
        // to download files, but in developing the Private URL feature, we have
        // added a new subclass of "User" called "PrivateUrlUser" that returns false
        // for isAuthenticated but that should be able to download restricted files
        // when given the Member role (which includes the DownloadFile permission).
        // This is consistent with how Builtin and Shib users (both are
        // AuthenticatedUsers) can download restricted files when they are granted
        // the Member role. For this reason condition 2 has been changed. Previously,
        // we required isSessionUserAuthenticated to return true. Now we require
        // that the User is not an instance of GuestUser, which is similar in
        // spirit to the previous check.
        // --------------------------------------------------------------------

        if (session.getUser() instanceof GuestUser){
            this.fileDownloadPermissionMap.put(fid, false);
            return false;
        }

        
        // --------------------------------------------------------------------
        // (3) Does the User have DownloadFile Permission at the **Dataset** level 
        // --------------------------------------------------------------------
        

        if (this.doesSessionUserHavePermission(Permission.DownloadFile, fileMetadata)){
            // Yes, save answer and return true
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }

  
        // --------------------------------------------------------------------
        // (4) Does the user has DownloadFile permission on the DataFile            
        // --------------------------------------------------------------------
        /*
        if (this.permissionService.on(fileMetadata.getDataFile()).has(Permission.DownloadFile)){
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
        */
        
        // --------------------------------------------------------------------
        // (6) No download....
        // --------------------------------------------------------------------
        this.fileDownloadPermissionMap.put(fid, false);
       
        return false;
    }
   
    public boolean doesSessionUserHavePermission(Permission permissionToCheck, FileMetadata fileMetadata){
        if (permissionToCheck == null){
            return false;
        }
        
        DvObject objectToCheck = null;
        
        if (permissionToCheck.equals(Permission.EditDataset)){
            objectToCheck = fileMetadata.getDatasetVersion().getDataset();
        } else if (permissionToCheck.equals(Permission.DownloadFile)){
            objectToCheck = fileMetadata.getDataFile();
        }
        
        if (objectToCheck == null){
            return false;
        }
        
        
               
        String permName = permissionToCheck.getHumanName();
       
        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)){
            // Yes, return previous answer
            return this.datasetPermissionMap.get(permName);
        }
        
        // Check the permission
        //
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), objectToCheck).has(permissionToCheck);

        // Save the permission
        this.datasetPermissionMap.put(permName, hasPermission);
        
        // return true/false
        return hasPermission;
    }

    public DataverseSession getSession() {
        return session;
    }

    public void setSession(DataverseSession session) {
        this.session = session;
    }
    
}
