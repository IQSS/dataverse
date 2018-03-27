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
public class ProvenanceUploadFragmentBean extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ProvenanceUploadFragmentBean.class.getCanonicalName());
    
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
    
    JsonParser parser = new JsonParser();
   
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
        
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        jsonUploadedTempFile = event.getFile();

        provJsonState = IOUtils.toString(jsonUploadedTempFile.getInputstream());
        try {
            generateProvJsonParsedEntities();

        } catch (Exception e) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName())
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
        storedSelectedEntityName = popupDataFile.getFileMetadata().getProvJsonObjName();
        
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
            fileMetadata.setProvJsonObjName(null);
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, jsonString));
            jsonUploadedTempFile = null;
            
            //storing the entity name associated with the DataFile. This is required data to get this far.
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvJsonObjName(dropdownSelectedEntity.getEntityName());
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
            fileMetadata.setProvJsonObjName(dropdownSelectedEntity.getEntityName());
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
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
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
                && popupDataFile.getFileMetadata().getCplId() != 0);
    }

    public ProvEntityFileData getDropdownSelectedEntity() {
        return dropdownSelectedEntity;
    }

    public void setDropdownSelectedEntity(ProvEntityFileData entity) {
        this.dropdownSelectedEntity = entity;
    }
        
    public void generateProvJsonParsedEntities() throws IOException { 
        provJsonParsedEntities = new HashMap<>();
        com.google.gson.JsonObject jsonObject = parser.parse(provJsonState).getAsJsonObject();
        recurseNames(jsonObject);
    }
        
    protected JsonElement recurseNames(JsonElement element) {
        return recurseNames(element, null, false);
    }
    
    /** Parsing recurser for prov json. Pulls out all names/types inside entity, including the name of each entry inside entity
     * Note that if a later entity is found with the same entity name (not name tag) its parsed contents will replace values that are stored
     * Current parsing code does not parse json arrays. My understanding of the schema is that these do not take place
     * Schema: https://www.w3.org/Submission/2013/SUBM-prov-json-20130424/schema
     */
    protected JsonElement recurseNames(JsonElement element, String outerKey, boolean atEntity) {
        //we need to know when we are inside of entity 
        //we also need to know when we are inside of each entity so we correctly connect the values
        if(element.isJsonObject()) {
            com.google.gson.JsonObject jsonObject = element.getAsJsonObject();
            Set<Map.Entry<String,JsonElement>> entrySet = jsonObject.entrySet();
            entrySet.forEach((s) -> {
                if(atEntity) {
                    String key = s.getKey();
                    
                    if("name".equals(key) || key.endsWith(":name")) {
                        ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                        e.fileName = s.getValue().getAsString();
                    } else if("type".equals(key) || key.endsWith(":type")) {
                        if(s.getValue().isJsonObject()) {
                            for ( Map.Entry tEntry : s.getValue().getAsJsonObject().entrySet()) {
                                String tKey = (String) tEntry.getKey();
                                if("type".equals(tKey) || tKey.endsWith(":type")) {
                                    ProvEntityFileData e = provJsonParsedEntities.get(outerKey);

                                    String value = tEntry.getValue().toString();
                                    e.fileType = value;
                                }
                            }                            
                        } else if(s.getValue().isJsonPrimitive()){
                            ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                            String value = s.getValue().getAsString();
                            e.fileType = value;
                        }

                    }
                } 
                if(null != outerKey && (outerKey.equals("entity") || outerKey.endsWith(":entity"))) {
                    provJsonParsedEntities.put(s.getKey(), new ProvEntityFileData(s.getKey(), null, null)); //we are storing the entity name both as the key and in the object, the former for access and the later for ease of use when converted to a list
                    recurseNames(s.getValue(),s.getKey(),true);
                } else {
                    recurseNames(s.getValue(),s.getKey(),false);
                }
                
            });
          
        } 
//        else if(element.isJsonArray()) {
//            JsonArray jsonArray = element.getAsJsonArray();
//            for (JsonElement childElement : jsonArray) {
//                JsonElement result = recurseNames(childElement);
//                if(result != null) {
//                    return result;
//                }
//            }
//        }
        
        return null;
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
