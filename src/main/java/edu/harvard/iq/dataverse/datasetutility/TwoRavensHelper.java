/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObject;
import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.Map;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author rmp553

 */
@ViewScoped
@Named
public class TwoRavensHelper implements java.io.Serializable {
    
    @Inject SettingsServiceBean settingsService;
    @Inject PermissionServiceBean permissionService;
    @Inject AuthenticationServiceBean authService;
    
    @Inject
    DataverseSession session;
            
    private final Map<Long, Boolean> fileMetadataTwoRavensExploreMap = new HashMap<>(); // { FileMetadata.id : Boolean } 

    public TwoRavensHelper(){
        
    }
   
    
     /**
     * Call this from a Dataset or File page
     *   - calls private method canSeeTwoRavensExploreButton
     * 
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     *   
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButtonFromAPI(FileMetadata fm, User user){
        
        if (fm == null){
            return false;
        }
        
        if (user == null){
            return false;
        }
                       
        if (!this.permissionService.userOn(user, fm.getDataFile()).has(Permission.DownloadFile)){
            return false;
        }
        
        return this.canSeeTwoRavensExploreButton(fm, true);
    }
    
    /**
     * Call this from a Dataset or File page
     *   - calls private method canSeeTwoRavensExploreButton
     * 
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     *   
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButtonFromPage(FileMetadata fm){
        
        if (fm == null){
            return false;
        }
        
        return this.canSeeTwoRavensExploreButton(fm, true);
    }
    
    /**
     * Used to check whether a tabular file 
     * may be viewed via TwoRavens
     * 
     * @param fm
     * @return 
     */
    public boolean canSeeTwoRavensExploreButton(FileMetadata fm, boolean permissionsChecked){
        if (fm == null){
            return false;
        }
        
        // This is only here as a reminder to the public method users 
        if (!permissionsChecked){
            return false;
        }
        
        if (!fm.getDataFile().isTabularData()){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        
        // Has this already been checked?
        if (this.fileMetadataTwoRavensExploreMap.containsKey(fm.getId())){
            // Yes, return previous answer
            //logger.info("using cached result for candownloadfile on filemetadata "+fid);
            return this.fileMetadataTwoRavensExploreMap.get(fm.getId());
        }
        
        
        // (1) Is TwoRavens active via the "setting" table?
        //      Nope: get out
        //      
        if (!settingsService.isTrueForKey(SettingsServiceBean.Key.TwoRavensTabularView, false)){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        
        //----------------------------------------------------------------------
        //(1a) Before we do any testing - if version is deaccessioned and user
        // does not have edit dataset permission then may download
        //--- 
        
        // (2) Is the DataFile object there and persisted?
        //      Nope: scat
        //
        if ((fm.getDataFile() == null)||(fm.getDataFile().getId()==null)){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        
       if (fm.getDatasetVersion().isDeaccessioned()) {
           if (this.doesSessionUserHavePermission( Permission.EditDataset, fm)) {
               // Yes, save answer and return true
               this.fileMetadataTwoRavensExploreMap.put(fm.getId(), true);
               return true;
           } else {
               this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
               return false;
           }
       }
        
        

        
        //Check for restrictions
        
        boolean isRestrictedFile = fm.isRestricted();
        
        
        // --------------------------------------------------------------------
        // Conditions (2) through (4) are for Restricted files
        // --------------------------------------------------------------------


        if (isRestrictedFile && session.getUser() instanceof GuestUser){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }

        
        // --------------------------------------------------------------------
        // (3) Does the User have DownloadFile Permission at the **Dataset** level 
        // --------------------------------------------------------------------
        

        if (isRestrictedFile && !this.doesSessionUserHavePermission(Permission.DownloadFile, fm)){
            // Yes, save answer and return true
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);
            return false;
        }
        
        // (3) Is there tabular data or is the ingest in progress?
        //      Yes: great
        //
        if ((fm.getDataFile().isTabularData())||(fm.getDataFile().isIngestInProgress())){
            this.fileMetadataTwoRavensExploreMap.put(fm.getId(), true);
            return true;
        }
        
        // Nope
        this.fileMetadataTwoRavensExploreMap.put(fm.getId(), false);            
        return false;
        
        //       (empty fileMetadata.dataFile.id) and (fileMetadata.dataFile.tabularData or fileMetadata.dataFile.ingestInProgress)
        //                                        and DatasetPage.canDownloadFile(fileMetadata) 
    }
    
   
    /**
     * Copied over from the dataset page - 9/21/2016
     * 
     * @return 
     */
    public String getDataExploreURL() {
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            return TwoRavensUrl;
        }

        return "";
    }


    /**
     * Copied over from the dataset page - 9/21/2016
     * 
     * @param fileid
     * @param apiTokenKey
     * @return 
     */
    public String getDataExploreURLComplete(Long fileid) {
        if (fileid == null){
            throw new NullPointerException("fileid cannot be null");
        }
            
        
        String TwoRavensUrl = settingsService.getValueForKey(SettingsServiceBean.Key.TwoRavensUrl);
        String TwoRavensDefaultLocal = "/dataexplore/gui.html?dfId=";

        if (TwoRavensUrl != null && !TwoRavensUrl.equals("")) {
            // If we have TwoRavensUrl set up as, as an optional 
            // configuration service, it must mean that TwoRavens is sitting 
            // on some remote server. And that in turn means that we must use 
            // full URLs to pass data and metadata to it. 
            // update: actually, no we don't want to use this "dataurl" notation.
            // switching back to the dfId=:
            // -- L.A. 4.1
            /*
            String tabularDataURL = getTabularDataFileURL(fileid);
            String tabularMetaURL = getVariableMetadataURL(fileid);
            return TwoRavensUrl + "?ddiurl=" + tabularMetaURL + "&dataurl=" + tabularDataURL + "&" + getApiTokenKey();
            */
            System.out.print("TwoRavensUrl Set up " + TwoRavensUrl + "?dfId=" + fileid + "&" + getApiTokenKey());
            
            return TwoRavensUrl + "?dfId=" + fileid + "&" + getApiTokenKey();
        }

        // For a local TwoRavens setup it's enough to call it with just 
        // the file id:     
        return TwoRavensDefaultLocal + fileid + "&" + getApiTokenKey();
    }
    
    private String getApiTokenKey() {
        ApiToken apiToken;
        if (session.getUser() == null) {
            return null;
        }
        if (isSessionUserAuthenticated()) {
            AuthenticatedUser au = (AuthenticatedUser) session.getUser();
            apiToken = authService.findApiTokenByUser(au);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
            // Generate if not available?
            // Or should it just be generated inside the authService
            // automatically? 
            apiToken = authService.generateApiTokenForUser(au);
            if (apiToken != null) {
                return "key=" + apiToken.getTokenString();
            }
        }
        return "";

    }
    
    public boolean isSessionUserAuthenticated() {

        if (session == null) {
            return false;
        }
        
        if (session.getUser() == null) {
            return false;
        }
        
        return session.getUser().isAuthenticated();

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
              
        
        // Check the permission
        //
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), objectToCheck).has(permissionToCheck);

        
        // return true/false
        return hasPermission;
    }
}
