/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersion.VersionState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlDataTable;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
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
   @EJB
    TemplateServiceBean templateService;

    private Dataset dataset = new Dataset();
    private EditMode editMode;
    private Long ownerId;
    private int selectedTabIndex;
    private Map<UploadedFile,DataFile> newFiles = new HashMap();
    private DatasetVersion editVersion = new DatasetVersion();
    private Metadata editMetadata = new Metadata();

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

    public Metadata getEditMetadata() {
        return editMetadata;
    }

    public void setEditMetadata(Metadata editMetadata) {
        this.editMetadata = editMetadata;
    }

    public DatasetVersion getEditVersion() {
        return editVersion;
    }

    public void setEditVersion(DatasetVersion editVersion) {
        this.editVersion = editVersion;
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
            dataset.setOwner(dataverseService.find(ownerId));
            dataset.setVersions(new ArrayList());
            editVersion.setFileMetadatas(new ArrayList());
            Template template = templateService.find(new Long (1));
            dataset.setTemplate(template);
            Metadata createMetadata = new Metadata(template.getMetadata());
            createMetadata.setTemplate(template);
            createMetadata.initCollections();
            editVersion.setVersionState(VersionState.DRAFT);
            createMetadata.setDatasetVersion(editVersion);
            editVersion.setMetadata(createMetadata);  
            editVersion.setVersionNumber(new Long(1));
            dataset.getVersions().add(editVersion);
            editVersion.setDataset(dataset);
        } else {
            throw new RuntimeException("On Dataset page without id or ownerid."); // improve error handling
        }
    }
    
    public void edit(EditMode editMode) {
        this.editMode = editMode;
        if (editMode == EditMode.INFO) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Info", " - Edit your dataset info."));
        } else if (editMode == EditMode.FILE) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Files", " - Edit your dataset files. Tip: You can drag and drop your files from your desktop, directly into the upload widget."));
        } else if (editMode == EditMode.METADATA) {
            editVersion = dataset.getEditVersion();
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,"Edit Dataset Metadata", " - Edit your dataset metadata."));
        }
    }
       
    public void save() {
        dataset.setOwner(dataverseService.find(ownerId));
        //TODO get real application-wide protocol/authority
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("5555");
        
        if (!(dataset.getVersions().get(0).getFileMetadatas() == null) && !dataset.getVersions().get(0).getFileMetadatas().isEmpty()) {
            int fmdIndex = 0;
            for (FileMetadata fmd : dataset.getVersions().get(0).getFileMetadatas()) {
                for (FileMetadata fmdTest : editVersion.getFileMetadatas()) {
                    if (fmd.equals(fmdTest)) {
                        dataset.getVersions().get(0).getFileMetadatas().get(fmdIndex).setDataFile(fmdTest.getDataFile());
                    }
                }
                fmdIndex++;
            }
        }
        
        dataset = datasetService.save(dataset);

        // save any new files
        for (UploadedFile uFile : newFiles.keySet()) {
            DataFile dFile = newFiles.get(uFile);
            try {
                if (!Files.exists(Paths.get("files",dataset.getLatestVersion().getMetadata().getTitle()))) {
                    Files.createDirectory(Paths.get("files",dataset.getLatestVersion().getMetadata().getTitle()));
                }
                Files.copy(uFile.getInputstream(), Paths.get("files",dataset.getLatestVersion().getMetadata().getTitle(), dFile.getName()));
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
        FileMetadata fmd = new FileMetadata();       
        dFile.setOwner(dataset);
        fmd.setDataFile(dFile);
        fmd.setLabel(dFile.getName());
        fmd.setCategory(dFile.getContentType());
        if (editVersion.getFileMetadatas() == null){
            editVersion.setFileMetadatas(new ArrayList() );
        }
        editVersion.getFileMetadatas().add(fmd);
        fmd.setDatasetVersion(editVersion);       
        dataset.getFiles().add( dFile );
        newFiles.put(uFile, dFile);
    }
    
    public void addRow(ActionEvent ae) {       
        //      UIComponent dataTable = ae.getComponent().getParent().getParent().getParent();
        HtmlDataTable dataTable = (HtmlDataTable)ae.getComponent().getParent().getParent();
            DatasetAuthor newElem = new DatasetAuthor();
            newElem.setMetadata(editVersion.getMetadata());
            editVersion.getMetadata().getDatasetAuthors().add(dataTable.getRowIndex()+1,newElem);
            //JavascriptContext.addJavascriptCall(getFacesContext(),"initAddAuthorSync();");
                         
    }

    private UIComponent dataTableAuthors;
    public UIComponent getDataTableAuthors() {
        return this.dataTableAuthors;
    }
    public void setDataTableAuthors(UIComponent dataTableAuthors) {
        this.dataTableAuthors = dataTableAuthors;
    }
    
    public DataModel getCustomFieldsDataModel() {
        List values = new ArrayList();        
        for (int i = 0; i < dataset.getTemplate().getTemplateFields().size(); i++) {
            TemplateField templateField = dataset.getTemplate().getTemplateFields().get(i);
            DatasetField datasetField = null;
            if (templateField.getDatasetField().isCustomField()) {
                for (DatasetField dsf : dataset.getEditVersion().getMetadata().getDatasetFields()) {
                    if (dsf.getName().equals(templateField.getDatasetField().getName())) { 
                        datasetField = dsf;
                        break;
                    }                
                }                       
                Object[] row = new Object[4];
                row[0] = templateField;
                row[1] = getCustomValuesDataModel(datasetField);
                row[2] = new Integer(i);
                row[3] = datasetField;                
                values.add(row);
            }            
        }               
        return new ListDataModel(values);
    }
    
    private DataModel getCustomValuesDataModel(DatasetField datasetField) {
        List values = new ArrayList();
        if (!datasetField.getDatasetFieldValues().isEmpty()) {
            for (DatasetFieldValue dsfv : datasetField.getDatasetFieldValues()) {
                Object[] row = new Object[2];
                row[0] = dsfv;
                row[1] = datasetField.getDatasetFieldValues(); // used by the remove method
                values.add(row);
            }
        } else {
            // Add a dummy row to the values table
            Object[] row = new Object[2];
            DatasetFieldValue dsfv = new DatasetFieldValue();
            dsfv.setDatasetField(datasetField);
            dsfv.setMetadata(editMetadata);
            dsfv.setStrValue("");
            row[0] = dsfv;
            row[1] = datasetField.getDatasetFieldValues(); // used by the remove method
            values.add(row);
        }
        return new ListDataModel(values);
    }

}
