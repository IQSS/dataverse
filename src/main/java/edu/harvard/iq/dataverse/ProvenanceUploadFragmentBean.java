package edu.harvard.iq.dataverse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import java.io.IOException;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonProvCommand;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.IOUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.JsonObject;
import org.hibernate.validator.constraints.NotBlank;

/**
 * This bean exists to ease the use of provenance upload popup functionality`
 * 
 * @author madunlap
 */

@ViewScoped
@Named
public class ProvenanceUploadFragmentBean extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(ProvenanceUploadFragmentBean.class.getCanonicalName());
    
    private UploadedFile jsonUploadedTempFile; 
    
    //These two variables hold the state of the prov variables for the current open file before any changes would be applied by the editing "session"
    private String provJsonState; //The contents of this string should not be used outside of checking for contents period
    private String freeformTextState; 
    private Dataset dataset;
    
    private String freeformTextInput;
    private boolean deleteStoredJson = false;
    private DataFile popupDataFile;
    
    //MAD: Unsure if these annotations are needed
    //@NotBlank(message = "Please enter a valid email address.")
    //@ValidateEmail(message = "Please enter a valid email address.")
    ProvEntityFileData dropdownSelectedEntity;
   
    //This map uses storageIdentifier as the key.
    //UpdatesEntry is an object containing the DataFile and the provJson string.
    //Originally there was a Hashmap<DataFile,String> to store this data 
    //but equality is "broken"for entities like DataFile --MAD 4.8.5
    HashMap<String,UpdatesEntry> jsonProvenanceUpdates = new HashMap<>();
    
    @EJB
    DataFileServiceBean dataFileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    FilePage filePage;
        
    public void handleFileUpload(FileUploadEvent event) {
        jsonUploadedTempFile = event.getFile();
        provJsonState = null;
    }

    public void updatePopupState(DataFile file, Dataset dSet) throws AbstractApiBean.WrappedResponse {
        dataset = dSet;
        updatePopupState(file);
    }
    
    //This updates the popup for the selected file each time its open
    public void updatePopupState(DataFile file) throws AbstractApiBean.WrappedResponse {
        if(null == dataset ) {
            dataset = file.getFileMetadata().getDatasetVersion().getDataset(); //DatasetVersion is null here on file upload page...
        }
        
        popupDataFile = file;
        deleteStoredJson = false;
        provJsonState = null;
        freeformTextState = popupDataFile.getFileMetadata().getProvFreeForm();
        
        if(jsonProvenanceUpdates.containsKey(popupDataFile.getStorageIdentifier())) { //If there is already staged provenance info 
            provJsonState = jsonProvenanceUpdates.get(popupDataFile.getStorageIdentifier()).provenanceJson;
            
        } else if(null != popupDataFile.getCreateDate()){//Is this file fully uploaded and already has prov data saved?     
            JsonObject provJsonObject = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), popupDataFile));
            if(null != provJsonObject) {
                provJsonState = provJsonObject.toString();
            }

        } else { //clear the listed uploaded file
            jsonUploadedTempFile = null;
        }
        freeformTextInput = freeformTextState;
    }
    
    //Stores the provenance changes decided upon in the popup to be saved when all edits across files are done.
    public void stagePopupChanges(boolean saveInPopup) throws IOException{
        //MAD: In here I need to use the dropdownSelectedEntity to save the selected entity name to the filemetadata    
        
        if(deleteStoredJson) {
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, null));
            
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvJsonObjName(null);
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //may need to specify encoding
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, jsonString));
            jsonUploadedTempFile = null;
            
            //storing the entity name associated with the DataFile. This is required data to get this far.
            //MAD: Make sure this is done in the api call for prov json as well
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
        
        if(saveInPopup) {
            try {
                saveStagedProvJson();
                saveStagedProvFreeform();
            } catch (AbstractApiBean.WrappedResponse ex) {
                filePage.showProvError();
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    }
    
    //Saves the staged provenance data, to be called by the pages launching the popup
    //Also called when saving happens in the popup on file page
    public void saveStagedProvJson() throws AbstractApiBean.WrappedResponse {
        for (Map.Entry<String, UpdatesEntry> mapEntry : jsonProvenanceUpdates.entrySet()) {
            DataFile df = mapEntry.getValue().dataFile;
            String provString = mapEntry.getValue().provenanceJson;

            try {
                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df));
            } catch (AbstractApiBean.WrappedResponse wr) {
                //do nothing, we always first try to delete the files in this list
            }
            if(null != provString) {
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString, dropdownSelectedEntity.entityName));
            }
        }
    }
    
    //Only called when saving the provenance data in the popup on the file page
    public void saveStagedProvFreeform() throws AbstractApiBean.WrappedResponse {  
        //MAD: This is broken due to 
        //"Exception Description: The object [edu.harvard.iq.dvn.core.study.FileMetadata[id=96]] cannot be merged because it has changed or been deleted since it was last read. "
        //execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), popupDataFile, freeformTextInput));
    }

    public void updateJsonRemoveState() throws AbstractApiBean.WrappedResponse {
        if (jsonUploadedTempFile != null) {
            jsonUploadedTempFile = null;
        } else if (provJsonState != null) {
            provJsonState = null;
            deleteStoredJson = true;
        }        
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
    
    //for storing datafile and provjson in a map value
    class UpdatesEntry {
        String provenanceJson;
        DataFile dataFile;
        
        UpdatesEntry(DataFile dataFile, String provenanceJson) {
            this.provenanceJson = provenanceJson;
            this.dataFile = dataFile;
        }
    }
    
    public boolean provExistsInPreviousVersion() {
        return (null != popupDataFile 
                && null != popupDataFile.getFileMetadata() 
                && popupDataFile.getFileMetadata().getCplId() != 0);
    }
    
    private static final String nameRegex = "name";
    private HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
    
    
    public ArrayList<ProvEntityFileData> generateAndReturnPJParsedNames() throws IOException {
        String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //MAD: this only works I think if the file is first uploaded... Probably gotta pull it out of the staged changes as well for editing before publish...
        JsonParser parser = new JsonParser();
        com.google.gson.JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject(); //provJsonState is a weird variable and I shouldn't be using it at least without checking over logic again

        recurseNames(jsonObject);

        return getProvJsonParsedEntitiesArray();
    }
    
    //MAD: I don't know how to return this now that its a more complex data structure... what does the popup want
    public ArrayList<ProvEntityFileData> getProvJsonParsedEntitiesArray() throws IOException {
        //if(null == provJsonParsedNames) { //MAD: This may not be the right way to do this, and if so may need to do more sanitization as its before other sanitization
            //String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());
        //}
        return new ArrayList<>(provJsonParsedEntities.values());
    }
    
    public ProvEntityFileData getEntityByEntityName(String entityName) {
        return provJsonParsedEntities.get(entityName);
    }
    
//    //MAD: I don't know how to return this now that its a more complex data structure... what does the popup want
//    public HashMap<String,ProvEntityFileData> getProvJsonParsedEntitiesMap() throws IOException {
//        //if(null == provJsonParsedNames) { //MAD: This may not be the right way to do this, and if so may need to do more sanitization as its before other sanitization
//            //String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());
//        //}
//        return provJsonParsedEntities;
//    }
    
    protected JsonElement recurseNames(JsonElement element) {
        return recurseNames(element, null, false);
    }
    
    //MAD: first just try to get all names, later we'll do it conditionally and stuff
    //MAD: This StackOverflow had good info https://stackoverflow.com/questions/15658124/finding-deeply-nested-key-value-in-json
    protected JsonElement recurseNames(JsonElement element, String outerKey, boolean atEntity) {
        //changes needed:
        //only inside entity
        //need to temporarilly display name of each entity "p1", "p2", the file name stored in that and the type?
        //"rdt:name" : "test.R",
        //"rdt:type" : "Finish",
        //we only need the entity json name to send to prov CPL
        //
        //so we need to know when we are inside of entity 
        //we also need to know when we are inside of each entity so we correctly connect the values
        //each option will need to be stored in a list of arrays I think? 
        if(element.isJsonObject()) {
            com.google.gson.JsonObject jsonObject = element.getAsJsonObject();
            Set<Map.Entry<String,JsonElement>> entrySet = jsonObject.entrySet();
            entrySet.forEach((s) -> {
                if(atEntity) {
                    //MAD: this may need checks to ensure its a string, not a null, etc 
                    //https://stackoverflow.com/questions/9324760/gson-jsonobject-unsupported-operation-exception-null-getasstring
                    String key = s.getKey();
                    String value = s.getValue().getAsString();
                    
                    if(key.equals("name") || key.endsWith(":name")) {
                        ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                        e.fileName = value;
                    } else if(key.equals("type") || key.endsWith(":type")) {
                        ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                        e.fileType = value;
                    }
                } 
                if(null != outerKey && outerKey.equals("entity")) { //collapse these?
                    provJsonParsedEntities.put(s.getKey(), new ProvEntityFileData(s.getKey(), null, null)); //we are storing the entity name both as the key and in the object, the former for access and the later for ease of use when converted to a list
                    recurseNames(s.getValue(),s.getKey(),true);
                } else {
                    recurseNames(s.getValue(),s.getKey(),false);
                }
                
//                ///old
//                Matcher m = Pattern.compile(nameRegex).matcher(s.getKey());
//                if(m.find()) { 
//                    try { 
//                        provJsonParsedNames.add(s.getValue().getAsString());
//                    } catch (ClassCastException | IllegalStateException e) { 
//                        //if not a string don't care skip
//                    }
//                } else if(!s.getValue().isJsonPrimitive()){
//                    recurseNames(s.getValue());
//                }
            });
          
        } //MAD: todo address this
//        else if(element.isJsonArray()) { //MAD: I'm unsure if this happens at all in cpl...
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
    
    public ProvEntityFileData getDropdownSelectedEntity() {
        return dropdownSelectedEntity;
    }

    public void setDropdownSelectedEntity(ProvEntityFileData entity) {
        this.dropdownSelectedEntity = entity;
    }
}
