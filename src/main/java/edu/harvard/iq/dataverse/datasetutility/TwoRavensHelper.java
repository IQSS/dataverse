/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.FileMetadata;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author rmp553
 */
public class TwoRavensHelper {
    
    private final SettingsServiceBean settingsService;
    private PermissionServiceBean permissionService;
            
    private final Map<Long, Boolean> fileMetadataTwoRavensExploreMap = new HashMap<>(); // { FileMetadata.id : Boolean } 

    public TwoRavensHelper(SettingsServiceBean settingsService, PermissionServiceBean permissionService){
        if (settingsService == null){
            throw new NullPointerException("settingsService cannot be null");
        }
        if (permissionService == null){
            throw new NullPointerException("permissionService cannot be null");
        }
        this.permissionService = permissionService;
        this.settingsService = settingsService;
        
        
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
        

        // (2) Is the DataFile object there and persisted?
        //      Nope: scat
        //
        if ((fm.getDataFile() == null)||(fm.getDataFile().getId()==null)){
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
    public String getDataExploreURLComplete(Long fileid, String apiTokenKey) {
        
        if (fileid == null){
            throw new NullPointerException("fileid cannot be null");
        }
        if (apiTokenKey == null){
            throw new NullPointerException("apiTokenKey cannot be null (at least adding this check)");
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
            return TwoRavensUrl + "?dfId=" + fileid + "&" + apiTokenKey;
        }

        // For a local TwoRavens setup it's enough to call it with just 
        // the file id:
        return TwoRavensDefaultLocal + fileid + "&" + apiTokenKey;
    }
}
