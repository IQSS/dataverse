/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private Long datasetVersionId;
    private DataFile file;

    private Dataset editDataset;
    
    @EJB
    DataFileServiceBean datafileService;

    @EJB
    PermissionServiceBean permissionService;

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
            
            fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(datasetVersionId, fileId);

            if (fileMetadata == null){
               return permissionsWrapper.notFound();
            }
            
            

           // If this DatasetVersion is unpublished and permission is doesn't have permissions:
           //  > Go to the Login page
           //
            
           if ( !permissionService.on(file).has(Permission.DownloadFile)) {
               return permissionsWrapper.notAuthorized();            
           }
         

        } else {

            return permissionsWrapper.notFound();
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
    
    public String restrictFile(boolean restricted){     

            String fileNames = null;   
            
        editDataset = this.file.getOwner();

                
                for (FileMetadata fmw: editDataset.getEditVersion().getFileMetadatas()){
                    if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())){
                        
                        fmw.setRestricted(restricted);
                    }
                }

        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }        
        save();
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
            System.out.print(filesToBeDeleted.size());
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

           setDatasetVersionId(editDataset.getEditVersion().getId());
        return "";
    }
    
    private String returnToDatasetOnly(){
        
         return "/dataset.xhtml?persistentId=" + editDataset.getGlobalId()  + "&version=DRAFT" + "&faces-redirect=true";   
    }
    
    private String returnToDraftVersion(){ 

         return "/file.xhtml?fileId=" + fileId + "&datasetVersionId=" + editDataset.getEditVersion().getId() + "&faces-redirect=true";    
    }
    
}
