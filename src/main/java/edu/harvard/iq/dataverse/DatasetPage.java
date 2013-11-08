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

    public enum EditMode {CREATE, INFO, FILE};
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
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
    
    public void init() {
        if (dataset.getId() != null) { // view mode for a dataset           
            dataset = datasetService.find(dataset.getId());
            ownerId = dataset.getOwner().getId();

        } else if (ownerId != null) { // create mode for a new child dataset
            editMode = EditMode.CREATE;
            dataset.setOwner(dataverseService.find(ownerId));

        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }

    public void editInfo(ActionEvent e) {
        editMode = EditMode.INFO;
    }

    public void editFile(ActionEvent e) {
        editMode = EditMode.FILE;
    }  
    
    public void save(ActionEvent e) {
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

    public void cancel(ActionEvent e) {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        newFiles.clear();
        editMode = null;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile( uFile.getFileName(), uFile.getContentType()); 
        dFile.setDataset(dataset);
        dataset.getFiles().add( dFile );
        newFiles.put(uFile, dFile);
    }

}
