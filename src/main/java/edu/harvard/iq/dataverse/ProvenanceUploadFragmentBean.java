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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.FacesMessage;
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
    String storedSelectedEntityName;
    
    private HashMap<String,ProvEntityFileData> provJsonParsedEntities = new HashMap<>();
   
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
        try {
            provJsonState = IOUtils.toString(jsonUploadedTempFile.getInputstream());
            generateAndReturnPJParsedNames();
        } catch (Exception e) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName())
                    .log(Level.SEVERE, "Exception occurred while handling upload of provenance json", e);
            removeJsonAndRelatedData();
            //MAD: We should throw a user-facing error when this fails
        }

        //provJsonState = null; //MAD why am I doing this? Hopefully removing it doesn't blow up my world...
                                //I get it now... I nulling this to track if remove needed to happen
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
        freeformTextState = popupDataFile.getFileMetadata().getProvFreeForm();
        storedSelectedEntityName = popupDataFile.getFileMetadata().getProvJsonObjName();
        
        if(jsonProvenanceUpdates.containsKey(popupDataFile.getStorageIdentifier())) { //If there is already staged provenance info 
            provJsonState = jsonProvenanceUpdates.get(popupDataFile.getStorageIdentifier()).provenanceJson;
            generateAndReturnPJParsedNames(); //MAD: using this inside this method is creating an unneeded data structure
            setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
            
        } else if(null != popupDataFile.getCreateDate()){//Is this file fully uploaded and already has prov data saved?     
            JsonObject provJsonObject = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), popupDataFile));
            if(null != provJsonObject) {
                provJsonState = provJsonObject.toString();
                generateAndReturnPJParsedNames();
                setDropdownSelectedEntity(provJsonParsedEntities.get(storedSelectedEntityName));
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
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //may need to specify encoding //MAD: May be able to use provJsonState instead
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

//MAD: When this case happens delete should be called anyways which will clean up
//        if(null == storedSelectedEntityName && null != dropdownSelectedEntity) {
//            freeformTextInput = "";
//        } 
        
        if(null != storedSelectedEntityName && null != dropdownSelectedEntity && !storedSelectedEntityName.equals(dropdownSelectedEntity.getEntityName())) {
            FileMetadata fileMetadata = popupDataFile.getFileMetadata();
            fileMetadata.setProvJsonObjName(dropdownSelectedEntity.getEntityName());
        }
        
        if(saveInPopup) {
            try {
                saveStagedProvJson(true);
                //We cannot just call the two prov saving commands back to back because of how context save functions
                //Instead we only trigger the command for saving freeform provenance if the prov json doesn't already trigger it.
                //We do not need to call the freeform command if json is already being saved because it is already in the metadata
                //and we are only saving one file so we can tell if that file will be saved. --MAD
                if(jsonProvenanceUpdates.entrySet().isEmpty()) {
                    //This is a little extra weird because I'm now leveraging the context save in the freeform command to also save the selected entity
                    FileMetadata fileMetadata = popupDataFile.getFileMetadata();
                    fileMetadata.setProvJsonObjName(dropdownSelectedEntity.getEntityName());
                    execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), popupDataFile, freeformTextInput));
                }
                
            } catch (AbstractApiBean.WrappedResponse ex) {
                filePage.showProvError();
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    }
    
    //Saves the staged provenance data, to be called by the pages launching the popup
    //Also called when saving happens in the popup on file page
    
    public void saveStagedProvJson() throws WrappedResponse {
        saveStagedProvJson(false);
    }
    
    public void saveStagedProvJson(boolean saveContext) throws AbstractApiBean.WrappedResponse {
        for (Map.Entry<String, UpdatesEntry> mapEntry : jsonProvenanceUpdates.entrySet()) {
            DataFile df = mapEntry.getValue().dataFile;
            String provString = mapEntry.getValue().provenanceJson;

            try {
                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, (saveContext && null == provString)));
            } catch (AbstractApiBean.WrappedResponse wr) {
                //do nothing, we always first try to delete the files in this list
            }
            if(null != provString ) {
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString, dropdownSelectedEntity.entityName, saveContext));
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
        provJsonParsedEntities = null;
    }
    //MAD: this method is called a ton on page load. Is that to be expected
    //god I wonder if its doing it per row...
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

    public ArrayList<ProvEntityFileData> generateAndReturnPJParsedNames() throws IOException {
        // String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //MAD: this only works I think if the file is first uploaded... Probably gotta pull it out of the staged changes as well for editing before publish...
        JsonParser parser = new JsonParser();
        com.google.gson.JsonObject jsonObject = parser.parse(provJsonState).getAsJsonObject(); //provJsonState is a weird variable and I shouldn't be using it at least without checking over logic again
        recurseNames(jsonObject);

        return getProvJsonParsedEntitiesArray();
    }
    
    public ArrayList<ProvEntityFileData> getProvJsonParsedEntitiesArray() throws IOException {
        return new ArrayList<>(provJsonParsedEntities.values());
    }
    
    public ProvEntityFileData getEntityByEntityName(String entityName) {
        return provJsonParsedEntities.get(entityName);
    }
    
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
                    
                    if("name".equals(key) || key.endsWith(":name")) {
                        ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                        e.fileName = s.getValue().getAsString();
                    } else if("type".equals(key) || key.endsWith(":type")) { //MAD: type is an object and should be treated as such... but is also a string sometimes
                        if(s.getValue().isJsonObject()) {
                            for ( Map.Entry tEntry : s.getValue().getAsJsonObject().entrySet()) {
                                String tKey = (String) tEntry.getKey();
                                if("type".equals(tKey) || tKey.endsWith(":type")) {
                                    ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                                    //Object tV = tEntry.getValue();
                                    //String value = (String) tEntry.getValue(); //MAD: This breaks but I want it to break so I can debug
                                    String value = tEntry.getValue().toString();
                                    e.fileType = value;
                                }
                            }                            
                        } else if(s.getValue().isJsonPrimitive()){ //this still isn't checking if its a string, just if its a primitive
                            ProvEntityFileData e = provJsonParsedEntities.get(outerKey);
                            String value = s.getValue().getAsString();
                            e.fileType = value;
                        }

                    }
                } 
                if(null != outerKey && outerKey.equals("entity")) { //collapse these?
                    provJsonParsedEntities.put(s.getKey(), new ProvEntityFileData(s.getKey(), null, null)); //we are storing the entity name both as the key and in the object, the former for access and the later for ease of use when converted to a list
                    recurseNames(s.getValue(),s.getKey(),true);
                } else {
                    recurseNames(s.getValue(),s.getKey(),false);
                }
                
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
