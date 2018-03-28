package edu.harvard.iq.dataverse;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import java.io.IOException;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonProvCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import java.util.ArrayList;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.json.JsonObject;

/**
 * This bean contains functionality for the provenance json pop up
 * This pop up can be accessed from multiple pages (editDataFile, Dataset (create), File)
 * 
 * @author madunlap
 */

@ViewScoped
@Named
public class ProvPopupFragmentBean extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ProvPopupFragmentBean.class.getCanonicalName());
    
    private UploadedFile jsonUploadedTempFile; 
    
    //These two variables hold the state of the prov variables for the current open file before any changes would be applied by the editing "session"
    private String provJsonState;
    private String freeformTextState; 
    
    private Dataset dataset;
    
    private String freeformTextInput;
    private boolean deleteStoredJson = false;
    private DataFile popupDataFile;
    
    ProvEntityFileData dropdownSelectedEntity;
    String storedSelectedEntityName;
    
    HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
    
    //JsonParser parser = new JsonParser();
   
    //This map uses storageIdentifier as the key.
    //UpdatesEntry is an object containing the DataFile and the provJson string.
    //Originally there was a Hashmap<DataFile,String> to store this data 
    //but equality is "broken" for entities like DataFile --mad 4.8.5
    HashMap<String,UpdatesEntry> jsonProvenanceUpdates = new HashMap<>();
    
    @EJB
    DataFileServiceBean dataFileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    FilePage filePage;
    @Inject
    DatasetPage datasetPage;
    @Inject
    EditDatafilesPage datafilesPage;
    @Inject
    ProvUtilFragmentBean provUtil;
        
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        jsonUploadedTempFile = event.getFile();

        provJsonState = IOUtils.toString(jsonUploadedTempFile.getInputstream());
        try {
            generateProvJsonParsedEntities();

        } catch (Exception e) {
            Logger.getLogger(ProvPopupFragmentBean.class.getName())
                    .log(Level.SEVERE, BundleUtil.getStringFromBundle("file.editProvenanceDialog.uploadError"), e);
            removeJsonAndRelatedData();
            JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("file.editProvenanceDialog.uploadError")); 
        } 
        if(provJsonParsedEntities.isEmpty()) {
            removeJsonAndRelatedData();
            JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("file.editProvenanceDialog.noEntitiesError"));
        }

    }

    public void updatePopupState(DataFile file, Dataset dSet) throws AbstractApiBean.WrappedResponse, IOException {
        dataset = dSet;
        updatePopupState(file);
    }
    
    //This updates the popup for the selected file each time its open
    public void updatePopupState(DataFile file) throws AbstractApiBean.WrappedResponse, IOException {
        if(null == dataset ) {
            dataset = file.getFileMetadata().getDatasetVersion().getDataset(); //DatasetVersion is null here on file upload page...
        }
        
        popupDataFile = file;
        deleteStoredJson = false;
        provJsonState = null;
        provJsonParsedEntities = new HashMap<>();
        setDropdownSelectedEntity(null);
        freeformTextState = popupDataFile.getFileMetadata().getProvFreeForm();
        storedSelectedEntityName = popupDataFile.getFileMetadata().getProvEntityName();
        
        if(jsonProvenanceUpdates.containsKey(popupDataFile.getStorageIdentifier())) { //If there is already staged provenance info 
            provJsonState = jsonProvenanceUpdates.get(popupDataFile.getStorageIdentifier()).provenanceJson;
            generateProvJsonParsedEntities(); //calling this each time is somewhat inefficient, but storing the state is a lot of lifting.
            setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
            
        } else if(null != popupDataFile.getCreateDate()){ //Is this file fully uploaded and already has prov data saved?     
            JsonObject provJsonObject = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), popupDataFile));
            if(null != provJsonObject) {
                provJsonState = provJsonObject.toString();
                generateProvJsonParsedEntities();
                setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
            }

        } else { //clear the listed uploaded file
            jsonUploadedTempFile = null;
        }
        freeformTextInput = freeformTextState;
    }
    
    //Stores the provenance changes decided upon in the popup to be saved when all edits across files are done.
    public void stagePopupChanges(boolean saveInPopup) throws IOException{
        
        if(deleteStoredJson) {
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, null));
            
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvEntityName(null);
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, jsonString));
            jsonUploadedTempFile = null;
            
            //storing the entity name associated with the DataFile. This is required data to get this far.
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvEntityName(dropdownSelectedEntity.getEntityName());
        } 
        
        if(null == freeformTextInput && null != freeformTextState) {
            freeformTextInput = "";
        }
            
        if(null != freeformTextInput && !freeformTextInput.equals(freeformTextState)) {
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvFreeForm(freeformTextInput);
        } 
        
        if(null != storedSelectedEntityName && null != dropdownSelectedEntity && !storedSelectedEntityName.equals(dropdownSelectedEntity.getEntityName())) {
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvEntityName(dropdownSelectedEntity.getEntityName());
        }
        
        if(saveInPopup) {
            try {
                saveStagedProvJson(true);
                
                //This block is for when an update was only made to the freeform provenance, as we are saving in the popup itself on the file page, not as part of another flow.
                if(jsonProvenanceUpdates.entrySet().isEmpty()) {
                    popupDataFile = execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), popupDataFile, freeformTextInput));
                    //Below lines ensure if the user saves the popup on file page and then opens it again and saves the datafile is up to date.
                    filePage.setFile(popupDataFile); 
                    filePage.init();
                }
                
            } catch (AbstractApiBean.WrappedResponse ex) {
                filePage.showProvError();
                Logger.getLogger(ProvPopupFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void saveStagedProvJson(boolean saveContext) throws AbstractApiBean.WrappedResponse {
        for (Map.Entry<String, UpdatesEntry> mapEntry : jsonProvenanceUpdates.entrySet()) {
            DataFile df = mapEntry.getValue().dataFile;
            
            String provString = mapEntry.getValue().provenanceJson;

            DataverseRequest dvr = dvRequestService.getDataverseRequest();

            if(null != provString ) {
                df = execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString, dropdownSelectedEntity.entityName, saveContext));
            } else {
                df = execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, saveContext));
            }
            
        }
    }

    public void removeJsonAndRelatedData() {
        if (provJsonState != null) {
            deleteStoredJson = true;
        }
        jsonUploadedTempFile = null;
        provJsonState = null;      
        dropdownSelectedEntity = null;
        storedSelectedEntityName = null;
        provJsonParsedEntities = new HashMap<>();
    }
    
    public boolean getJsonUploadedState() {
        return null != jsonUploadedTempFile || null != provJsonState;   
    }
        
    public String getFreeformTextInput() {
        return freeformTextInput;
    }
    
    public void setFreeformTextInput(String freeformText) {
        freeformTextInput = freeformText;
    }
    
    public String getFreeformTextStored() {
        return freeformTextState;
    }
    
    public void setFreeformTextStored(String freeformText) {
        freeformTextState = freeformText;
    }
    
    public boolean provExistsInPreviousPublishedVersion() {
        return (null != popupDataFile 
                && null != popupDataFile.getFileMetadata() 
                && popupDataFile.getFileMetadata().getProvCplId() != 0);
    }

    public ProvEntityFileData getDropdownSelectedEntity() {
        return dropdownSelectedEntity;
    }

    public void setDropdownSelectedEntity(ProvEntityFileData entity) {
        this.dropdownSelectedEntity = entity;
    }
        
    public void generateProvJsonParsedEntities() throws IOException { 
        //
        provJsonParsedEntities = provUtil.startRecurseNames(provJsonState);
    }
        

        
    public ArrayList<ProvEntityFileData> getProvJsonParsedEntitiesArray() throws IOException {
        return new ArrayList<>(provJsonParsedEntities.values());
    }
        
    public ArrayList<ProvEntityFileData> searchParsedEntities(String query) throws IOException {
        ArrayList<ProvEntityFileData> fd = new ArrayList<>();
        
        for ( ProvEntityFileData s : getProvJsonParsedEntitiesArray()) {
            if(s.entityName.contains(query) || s.fileName.contains(query) || s.fileType.contains(query)) {
                fd.add(s);
            }
        }
        fd.sort(null);
        
        return fd;
    }
    
    public ProvEntityFileData getEntityByEntityName(String entityName) {
        return provJsonParsedEntities.get(entityName);
    }
    
     //for storing datafile and provjson in a map value
    class UpdatesEntry {
        String provenanceJson;
        DataFile dataFile;
        
        UpdatesEntry(DataFile dataFile, String provenanceJson) {
            this.provenanceJson = provenanceJson;
            this.dataFile = dataFile;
        }
    }
}
