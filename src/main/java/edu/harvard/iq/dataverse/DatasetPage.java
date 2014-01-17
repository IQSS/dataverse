/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    public enum EditMode {CREATE, INFO, FILE, METADATA};
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private int selectedTabIndex;
    private Map<UploadedFile,DataFile> newFiles = new HashMap();

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public EditMode getEditMode() {
        return editMode;
    }

    public void setEditMode(EditMode editMode) {
        this.editMode = editMode;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
    public void init() {
        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            ownerId = dataset.getOwner().getId();

        } else if (ownerId != null) { // create mode for a new child dataset
            editMode = EditMode.CREATE;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Create New Dataset", " - Fill in the required fields and add your data files."));
            FacesContext.getCurrentInstance().addMessage("datasetForm:tabView:fileUpload", new FacesMessage(FacesMessage.SEVERITY_INFO,"Add Files - ", "You can drag and drop your files from your desktop, directly into the upload widget."));
            dataset.setOwner(dataverseService.find(ownerId));

        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }
    
    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Info", " - Edit your dataset info."));
        } else if (editMode == EditMode.FILE) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Files", " - Edit the metadata for your files."));
            FacesContext.getCurrentInstance().addMessage("datasetForm:tabView:fileUpload", new FacesMessage(FacesMessage.SEVERITY_INFO,"Add Files - ", "You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode == EditMode.METADATA) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Metadata", " - Edit your dataset metadata."));
        }
    }
       
    public void save() {
        dataset.setOwner(dataverseService.find(ownerId));
        dataset = datasetService.save(dataset);

        // save any new files
        for (UploadedFile uFile : newFiles.keySet()) {
            DataFile dFile = newFiles.get(uFile);
            try {
                if (!Files.exists(Paths.get("files",dataset.getTitle()))) {
                    Files.createDirectory(Paths.get("files",dataset.getTitle()));
                }
                Files.copy(uFile.getInputstream(), Paths.get("files",dataset.getTitle(), dFile.getName()));
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }        
        newFiles.clear();
        editMode = null;
    }

    public void cancel() {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        newFiles.clear();
        editMode = null;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile( uFile.getFileName(), uFile.getContentType()); 
        dFile.setOwner(dataset);
        dataset.getFiles().add( dFile );
        newFiles.put(uFile, dFile);
    }

}
