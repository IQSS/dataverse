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
    @NotBlank(message = "Please enter a valid email address.")
    @ValidateEmail(message = "Please enter a valid email address.")
    String dropdownSelectedJsonName;
   
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
            
        if(deleteStoredJson) {
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, null));
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //may need to specify encoding
            jsonProvenanceUpdates.put(popupDataFile.getStorageIdentifier(), new UpdatesEntry(popupDataFile, jsonString));
            jsonUploadedTempFile = null;
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
                saveStagedJsonProvenance();
                saveStagedJsonFreeform();
            } catch (AbstractApiBean.WrappedResponse ex) {
                filePage.showProvError();
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
        } 
    }
    
    //Saves the staged provenance data, to be called by the pages launching the popup
    public void saveStagedJsonProvenance() throws AbstractApiBean.WrappedResponse {
        for (Map.Entry<String, UpdatesEntry> mapEntry : jsonProvenanceUpdates.entrySet()) {
            DataFile df = mapEntry.getValue().dataFile;
            String provString = mapEntry.getValue().provenanceJson;

            try {
                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df));
            } catch (AbstractApiBean.WrappedResponse wr) {
                //do nothing, we always first try to delete the files in this list
            }
            if(null != provString) {
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString));
            }
        }
    }
    
    public void saveStagedJsonFreeform() throws AbstractApiBean.WrappedResponse {  
            execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), popupDataFile, freeformTextInput));
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
    
    //MAD: Incomplete, I may have to take the actual json parser approach afterall..
    //This method takes a simple regex-based approach to finding all "name" tags. 
    //Because we want them no matter where in the JSON and because we need regex to match,
    //this was easier and more efficient than a recursive json parsing approach.
    
    
    
//    public Set<String> parseStringForNames(String jsonString) throws IOException {
//        //List <String> theNames = new ArrayList<>();
//        Matcher m = Pattern.compile(nameRegex).matcher(jsonString);
//        while (m.find()) {
//            theNames.add(m.group());
//        }
//        
//        return theNames;
//    }

    private static final String nameRegex = "name";
    private HashSet<String> provJsonParsedNames = new HashSet<>();
    
    
    public ArrayList<String> generateAndReturnPJParsedNames() throws IOException {
        String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //MAD: this only works I think if the file is first uploaded... Probably gotta pull it out of the staged changes as well for editing before publish...
        JsonParser parser = new JsonParser();
        com.google.gson.JsonObject jsonObject = parser.parse(jsonString).getAsJsonObject(); //provJsonState is a weird variable and I shouldn't be using it at least without checking over logic again

        recurseNames(jsonObject);

        return getProvJsonParsedNames();
    }
    
    public ArrayList<String> getProvJsonParsedNames() throws IOException {
        //if(null == provJsonParsedNames) { //MAD: This may not be the right way to do this, and if so may need to do more sanitization as its before other sanitization
            //String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream());

        //}
        return new ArrayList<String>(provJsonParsedNames);
    }
    
    //MAD: first just try to get all names, later we'll do it conditionally and stuff
    //MAD: This StackOverflow had good info https://stackoverflow.com/questions/15658124/finding-deeply-nested-key-value-in-json
    protected JsonElement recurseNames(JsonElement element) {
        if(element.isJsonObject()) {
            com.google.gson.JsonObject jsonObject = element.getAsJsonObject();
            Set<Map.Entry<String,JsonElement>> entrySet = jsonObject.entrySet();
            entrySet.forEach((s) -> {
                Matcher m = Pattern.compile(nameRegex).matcher(s.getKey());
                if(m.find()) { 
                    try { 
                        provJsonParsedNames.add(s.getValue().getAsString());
                    } catch (ClassCastException | IllegalStateException e) { 
                        //if not a string don't care skip
                    }
                } else if(!s.getValue().isJsonPrimitive()){
                    recurseNames(s.getValue());
                }
            });
          
        } else if(element.isJsonArray()) {
            JsonArray jsonArray = element.getAsJsonArray();
            for (JsonElement childElement : jsonArray) {
                JsonElement result = recurseNames(childElement);
                if(result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    public String getDropdownSelectedJsonName() {
        return dropdownSelectedJsonName;
    }

    public void setDropdownSelectedJsonName(String dropdownSelectedJsonName) {
        this.dropdownSelectedJsonName = dropdownSelectedJsonName;
    }
}
