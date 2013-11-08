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

    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DataverseServiceBean dataverseService;

    private Dataset dataset = new Dataset();
    private boolean editMode = false;
    private Long ownerId;
    private Map<DataFile,UploadedFile> newFiles = new HashMap();

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
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
            editMode = true;
            dataset.setOwner(dataverseService.find(ownerId));

        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }

    public void edit(ActionEvent e) {
        editMode = true;
    }

    public void save(ActionEvent e) {
        // first save any new files
        for (DataFile file : newFiles.keySet()) {
            try {
                Files.createDirectory(Paths.get("files",dataset.getTitle()));
                Files.copy(newFiles.get(file).getInputstream(), Paths.get("files",dataset.getTitle(), file.getName()));
                newFiles.remove(file);
            } catch (IOException ex) {
                Logger.getLogger(DatasetPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        dataset.setOwner(dataverseService.find(ownerId));
        dataset = datasetService.save(dataset);
        editMode = false;
    }

    public void cancel(ActionEvent e) {
        // reset values
        dataset = datasetService.find(dataset.getId());
        ownerId = dataset.getOwner().getId();
        editMode = false;
    }

    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        DataFile dFile = new DataFile( uFile.getFileName(), uFile.getContentType()); 
        dataset.getFiles().add( dFile );
        newFiles.put(dFile, uFile);
    }

}
