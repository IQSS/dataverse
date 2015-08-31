/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author skraffmi
 * 
 */

@ViewScoped
@Named("FilePage")
public class FilePage implements java.io.Serializable {
    
    private FileMetadata fileMetadata;
    private Long fileId;  
    private Long datasetVersionId;
    private DataFile file;

    @EJB
    DataFileServiceBean datafileService;

    @EJB
    PermissionServiceBean permissionService;

    @Inject
    DataverseSession session;

    public String init() {
        // logger.fine("_YE_OLDE_QUERY_COUNTER_");  // for debug purposes        
        
        if ( fileId != null ) { 
           
            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
               file = datafileService.find(fileId);   

            }  else if (fileId == null){
               return "/404.xhtml";
            }

            if (file == null){
               return "/404.xhtml";
            }
            
            fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(datasetVersionId, fileId);

            if (fileMetadata == null){
               return "/404.xhtml";
            }
            
            

           // If this DatasetVersion is unpublished and permission is doesn't have permissions:
           //  > Go to the Login page
           //
           if ( !permissionService.on(file).has(Permission.DownloadFile)) {
               if(!session.getUser().isAuthenticated()){
                   return "/loginpage.xhtml" + DataverseHeaderFragment.getRedirectPage();
               } else {
                   return "/403.xhtml"; //SEK need a new landing page if user is already logged in but lacks permission
               }               
           }
         

        } else {

            return "/404.xhtml";
        }

        return null;
    }
  

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public DataFile getFile() {
        return file;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }
    
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        this.datasetVersionId = datasetVersionId;
    }
    
}
