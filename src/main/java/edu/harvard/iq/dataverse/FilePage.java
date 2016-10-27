/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.GuestUser;
import edu.harvard.iq.dataverse.datasetutility.TwoRavensHelper;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;

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
    private String version;
    private Long datasetVersionId;

    private DataFile file;
    
    private FileDownloadHelper fileDownloadHelper;
    private GuestbookResponse guestbookResponse;
        // Used to help with displaying buttons related to the WorldMap
    private WorldMapPermissionHelper worldMapPermissionHelper;

    private int selectedTabIndex;
    
    // Used to help with displaying buttons related to TwoRavens
    private TwoRavensHelper twoRavensHelper;

    private Dataset editDataset;
    
    @EJB
    DataFileServiceBean datafileService;
    
    @EJB
    DatasetVersionServiceBean datasetVersionService;

    @EJB
    PermissionServiceBean permissionService;
    
    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    AuthenticationServiceBean authService;


    @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;

    public String init() {
     
        
        if ( fileId != null ) { 
           
            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
               file = datafileService.find(fileId);   

            }  else if (fileId == null){
               return permissionsWrapper.notFound();
            }

            if (file == null){
               return permissionsWrapper.notFound();
            }

                RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
                Long getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
                fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);

          
            if (fileMetadata == null){
               return permissionsWrapper.notFound();
            }

           // If this DatasetVersion is unpublished and permission is doesn't have permissions:
           //  > Go to the Login page
           //
            // Check permisisons           
            if (!(fileMetadata.getDatasetVersion().isReleased()) && !this.canViewUnpublishedDataset()) {
                return permissionsWrapper.notAuthorized();
            }
          
           
           this.guestbookResponse = this.guestbookResponseService.initGuestbookResponseForFragment(fileMetadata, session);
           this.loadFileDownloadHelper();
           this.loadMapLayerMetadata();
           this.loadTwoRavensHelper();
           this.loadWorldMapPermissionHelper();
        } else {

            return permissionsWrapper.notFound();
        }

        return null;
    }
    
    private boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }
    
    private MapLayerMetadata mapLayerMetadata = null;
    
    private void loadMapLayerMetadata() {
        if (this.fileMetadata.getDatasetVersion().getDataset() == null) {
            return;
        }
        if (this.fileMetadata.getDatasetVersion().getDataset().getId() == null) {
            return;
        }
        mapLayerMetadata = mapLayerMetadataService.findMetadataByDatafile(file);
    }


 
    
    public boolean hasMapLayerMetadata() {
        return mapLayerMetadata != null;
    }

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }
    

    public boolean isDownloadPopupRequired() {  
        if(fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null ){
            return false;
        }
        return fileDownloadService.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
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
     
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
  
    public String restrictFile(boolean restricted){     
            String fileNames = null;   
            
        editDataset = this.file.getOwner();

                
                for (FileMetadata fmw: editDataset.getEditVersion().getFileMetadatas()){
                    if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())){
                        
                        fileNames += fmw.getLabel();
                        fmw.setRestricted(restricted);
                    }
                }

        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }        
        save();
        init();
        return returnToDraftVersion();
    }
    
    private List<FileMetadata> filesToBeDeleted = new ArrayList();

    public String deleteFile() {

        String fileNames = this.getFileMetadata().getLabel();

        editDataset = this.getFileMetadata().getDataFile().getOwner();
        
        FileMetadata markedForDelete = null;
        
        for (FileMetadata fmd : editDataset.getEditVersion().getFileMetadatas() ){
            
            if (fmd.getDataFile().getId().equals(this.getFile().getId())){
                markedForDelete = fmd;
            }
        }

            if (markedForDelete.getId() != null) {
                // the file already exists as part of this dataset
                // so all we remove is the file from the fileMetadatas (for display)
                // and let the delete be handled in the command (by adding it to the filesToBeDeleted list
                editDataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                filesToBeDeleted.add(markedForDelete);
                
            } else {
                 List<FileMetadata> filesToKeep = new ArrayList();
                 for (FileMetadata fmo: editDataset.getEditVersion().getFileMetadatas()){
                      if (!fmo.getDataFile().getId().equals(this.getFile().getId())){
                          filesToKeep.add(fmo);
                      }
                 }
                 editDataset.getEditVersion().setFileMetadatas(filesToKeep);
            }

    

     
        if (fileNames != null) {
            String successMessage = JH.localize("file.deleted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
        
        save();
        return returnToDatasetOnly();
        
    }
    
    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = this.fileMetadata.getDatasetVersion().validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
             JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.message.validationError"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
               

        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetCommand(editDataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
            commandEngine.submit(cmd);

        } catch (EJBException ex) {
            
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            
            
            Throwable cause = ex;
            while (cause.getCause()!= null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            return null;
        }


        JsfHelper.addSuccessMessage(JH.localize("dataset.message.filesSuccess"));
        setVersion("DRAFT");
        return "";
    }
    
    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.  
        
        if (!this.fileDownloadHelper.canDownloadFile(fileMetadata)) {
            return false;
        }
     
        return datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
    }
    
    private String returnToDatasetOnly(){
        
         return "/dataset.xhtml?persistentId=" + editDataset.getGlobalId()  + "&version=DRAFT" + "&faces-redirect=true";   
    }
    
    private String returnToDraftVersion(){ 
        
         return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";    
    }
    
    public FileDownloadHelper getFileDownloadHelper() {
        return fileDownloadHelper;
    }

    public void setFileDownloadHelper(FileDownloadHelper fileDownloadHelper) {
        this.fileDownloadHelper = fileDownloadHelper;
    }
    
    
    public FileDownloadServiceBean getFileDownloadService() {
        return fileDownloadService;
    }

    public void setFileDownloadService(FileDownloadServiceBean fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    
    public GuestbookResponseServiceBean getGuestbookResponseService() {
        return guestbookResponseService;
    }

    public void setGuestbookResponseService(GuestbookResponseServiceBean guestbookResponseService) {
        this.guestbookResponseService = guestbookResponseService;
    }
    
    
    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }
    
    private void loadFileDownloadHelper() {
       
        fileDownloadHelper = new FileDownloadHelper( this.file.getOwner(), permissionService, session);
        
    }
    
    
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.file.getOwner());
    }
    
    public WorldMapPermissionHelper getWorldMapPermissionHelper() {
        return worldMapPermissionHelper;
    }

    public void setWorldMapPermissionHelper(WorldMapPermissionHelper worldMapPermissionHelper) {
        this.worldMapPermissionHelper = worldMapPermissionHelper;
    }

    public TwoRavensHelper getTwoRavensHelper() {
        return twoRavensHelper;
    }

    public void setTwoRavensHelper(TwoRavensHelper twoRavensHelper) {
        this.twoRavensHelper = twoRavensHelper;
    }
    
    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
    /**
     * This object wraps methods used for hiding/displaying WorldMap related messages
     *
     */
    private void loadTwoRavensHelper() {
       
        twoRavensHelper = new TwoRavensHelper(settingsService, permissionService, session, authService);
        
    }
    

    
    /**
     * This object wraps methods used for hiding/displaying WorldMap related messages
     *
     */
    private void loadWorldMapPermissionHelper() {
       
        worldMapPermissionHelper = WorldMapPermissionHelper.getPermissionHelperForDatasetPage(settingsService, mapLayerMetadataService, fileMetadata.getDataFile().getOwner(), session);
        
    }
    
}
