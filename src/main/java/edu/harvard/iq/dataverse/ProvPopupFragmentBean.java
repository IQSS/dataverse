package edu.harvard.iq.dataverse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.io.IOException;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
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
    private FileMetadata fileMetadata;
    
    private String freeformTextInput;
    private boolean deleteStoredJson = false;
    private DataFile popupDataFile;
    
    ProvEntityFileData dropdownSelectedEntity;
    String storedSelectedEntityName;
    
    HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
    
    //JsonParser parser = new JsonParser();
   
    //This map uses storageIdentifier as the key.
    //UpdatesEntry contains the prov json, prov freeform and whether we will delete json
    //Originally there was a Hashmap<DataFile,String> to store this data 
    //but equality is "broken" for entities like DataFile --mad 4.8.5    
    HashMap<String,UpdatesEntry> provenanceUpdates = new HashMap<>();
    
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
    
    public void updatePopupState(FileMetadata fm, Dataset dSet) throws AbstractApiBean.WrappedResponse, IOException {
        dataset = dSet;
        updatePopupState(fm);
    }
     
    public void updatePopupState(FileMetadata fm) {       
//MAD: This should exception better
        if(null == fm) {
            throw new NullPointerException("FileMetadata initialized to null");
        }
        fileMetadata = fm;
    }
    
    //This updates the popup for the selected file each time its open
    //You should call the one that takes fileMetadata if you are calling this fragment from another page.
    public void updatePopupState() throws AbstractApiBean.WrappedResponse, IOException {
        if(null == fileMetadata) {
            throw new NullPointerException("FileMetadata cannot be null when calling updatePopupState");
        }
        if(null == dataset ) {
            dataset = fileMetadata.getDatasetVersion().getDataset(); //DatasetVersion is null here on file upload page...
        }
        
        popupDataFile = fileMetadata.getDataFile();
        deleteStoredJson = false;
        provJsonState = null;
        provJsonParsedEntities = new HashMap<>();
        setDropdownSelectedEntity(null);
        freeformTextState = fileMetadata.getProvFreeForm();
        storedSelectedEntityName = popupDataFile.getProvEntityName();
        
        if(provenanceUpdates.containsKey(popupDataFile.getStorageIdentifier())) { //If there is already staged provenance info 
            provJsonState = provenanceUpdates.get(popupDataFile.getStorageIdentifier()).provJson;
            if(null != provenanceUpdates.get(popupDataFile.getStorageIdentifier()).provFreeform) {
                freeformTextState = provenanceUpdates.get(popupDataFile.getStorageIdentifier()).provFreeform;
            }
            generateProvJsonParsedEntities(); //calling this each time is somewhat inefficient, but storing the state is a lot of lifting.
            setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
            
        } else if(null != popupDataFile.getCreateDate()){ //Is this file fully uploaded and already has prov data saved?   
            JsonObject provJsonObject = execCommand(new GetProvJsonCommand(dvRequestService.getDataverseRequest(), popupDataFile));
            if(null != provJsonObject) {
                provJsonState = provUtil.getPrettyJsonString(provJsonObject);
                
                generateProvJsonParsedEntities();
                setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
            }

        } else { //clear the listed uploaded file
            jsonUploadedTempFile = null;
        }
        freeformTextInput = freeformTextState;
    }
    
    //Stores the provenance changes decided upon in the popup to be saved when all edits across files are done.
    public String stagePopupChanges(boolean saveInPopup) throws IOException{
        String freeformToSave = null; //different variable to ensure we only update freeform when needed
        if(null != freeformTextInput && !freeformTextInput.equals(freeformTextState)) {
            freeformToSave = freeformTextInput;
        } 
        
        UpdatesEntry stagingEntry = null;// = new UpdatesEntry(popupDataFile, null, null);
        
        if(deleteStoredJson) {
            stagingEntry = new UpdatesEntry(popupDataFile, null, true ,null);
//            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, null, null));
            
            popupDataFile.setProvEntityName(null); //MAD: This is probably ok but now that things are immutable I should doublecheck
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) { //delete and create again can both happen at once
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());
            stagingEntry = new UpdatesEntry(popupDataFile, jsonString, false, null);
//            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, jsonString, null));
            jsonUploadedTempFile = null;
            
            //storing the entity name associated with the DataFile. This is required data to get this far.
            popupDataFile.setProvEntityName(dropdownSelectedEntity.getEntityName());
        } 
        
        if(null != storedSelectedEntityName && null != dropdownSelectedEntity && !storedSelectedEntityName.equals(dropdownSelectedEntity.getEntityName())) {
            popupDataFile.setProvEntityName(dropdownSelectedEntity.getEntityName());
        }
        
        if(null == freeformTextInput && null != freeformTextState) {
            freeformTextInput = "";
        }            

        if(null != freeformTextInput && !freeformTextInput.equals(freeformTextState)) {
            if(null == stagingEntry) { //if file creation or deletion did not trigger creation
                stagingEntry = new UpdatesEntry(popupDataFile, null, false, null);
            }
            stagingEntry.provFreeform = freeformToSave;
            //FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            //fileMetadata.setProvFreeForm(freeformTextInput);
        } 
        if(null != stagingEntry) {
            provenanceUpdates.put(popupDataFile.getStorageIdentifier(), stagingEntry);
        }
        
        if(saveInPopup) {
            
            try {
                saveStagedProvJson(true);
                if(null != stagingEntry && null != stagingEntry.provFreeform) {
                    return filePage.saveProvFreeform(freeformTextInput);
                }  
            } catch (AbstractApiBean.WrappedResponse|CommandException ex) {
                filePage.showProvError();
                Logger.getLogger(ProvPopupFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return null;

    }
    
    public void saveStagedProvJson(boolean saveContext) throws AbstractApiBean.WrappedResponse {
        for (Map.Entry<String, UpdatesEntry> mapEntry : provenanceUpdates.entrySet()) {
            DataFile df = mapEntry.getValue().dataFile;
            String provString = mapEntry.getValue().provJson;

            if(mapEntry.getValue().deleteJson) {
                df = execCommand(new DeleteProvJsonCommand(dvRequestService.getDataverseRequest(), df, saveContext));
            } else if(null != provString) {
                df = execCommand(new PersistProvJsonCommand(dvRequestService.getDataverseRequest(), df, provString, dropdownSelectedEntity.entityName, saveContext));
            } 
        }
    }
    
    public void saveStageProvFreeformToLatestVersion() {
        for (Map.Entry<String, UpdatesEntry> mapEntry : provenanceUpdates.entrySet()) {
            String freeformText = mapEntry.getValue().provFreeform;
            FileMetadata fm = mapEntry.getValue().dataFile.getFileMetadata();
            fm.setProvFreeForm(freeformText);

        }
    }
    
//MAD: I'm unsure if a new file will match correctly...
    //Called by editFilesPage to update its metadata with stored prov freeform values for multiple DataFiles
    Boolean updatePageMetadatasWithProvFreeform(List<FileMetadata> fileMetadatas) {
        Boolean changes = false;
        for(FileMetadata fm : fileMetadatas) {
            UpdatesEntry ue = provenanceUpdates.get(fm.getDataFile().getStorageIdentifier());
            if(null != ue) {
                fm.setProvFreeForm(ue.provFreeform);
                changes = true;
            }
        }
        return changes;
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
    
//MAD: This doesn't catch a case where the json was created and then deleted before publish.
    // The deleted time wouldn't show the correct block, but in that case we are reverting to our original state
    public boolean isJsonUpdated() {
        return (null != jsonUploadedTempFile);
    }
    
    public boolean isFreeformUpdated() {
       return (null != freeformTextInput && !(freeformTextInput.equals(freeformTextState)))
                || (null == freeformTextInput && null != freeformTextState) ;
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
    
    //These two checks are for the render logic so don't strictly just check published state
    public boolean isDatasetPublishedRendering() {
        return null != fileMetadata && fileMetadata.getDatasetVersion().isPublished();
    }
    public boolean isDatasetInDraftRendering() {
        return null != fileMetadata && fileMetadata.getDatasetVersion().isDraft();
    }
    
//MAD: REDO now that cpl isn't in and system is updating
    public boolean provExistsInPreviousPublishedVersion() {
        return false;
//return (null != popupDataFile 
//                && null != popupDataFile.getFileMetadata()); 
                //&& popupDataFile.getProvCplId() != 0); //add when we integrate with provCPL
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
        String provJson;
        DataFile dataFile;
        String provFreeform;
        Boolean deleteJson;
        
        UpdatesEntry(DataFile dataFile, String provJson, Boolean deleteJson, String provFreeform) {
            this.provJson = provJson;
            this.dataFile = dataFile;
            this.provFreeform = provFreeform;
            this.deleteJson = deleteJson;
        }
    }
    
    public void showJsonPreviewNewWindow() throws IOException, WrappedResponse {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        
        ec.responseReset(); 
        ec.setResponseContentType("application/json;charset=UTF-8"); 
        //ec.setResponseContentLength(contentLength);
        String fileName = "prov-json.json";
        ec.setResponseHeader("Content-Disposition", "inline; filename=\"" + fileName + "\""); 

        OutputStream output = ec.getResponseOutputStream();
        
        OutputStreamWriter osw = new OutputStreamWriter(output, "UTF-8");
        osw.write(provJsonState); //the button calling this will only be rendered if provJsonState exists (e.g. a file is uploaded)
        osw.close();
        fc.responseComplete();

    }
}
