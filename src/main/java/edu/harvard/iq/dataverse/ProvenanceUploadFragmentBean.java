/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import java.io.IOException;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvFreeFormCommand;
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
import javax.json.JsonObject;
import org.apache.commons.io.IOUtils;

/**
 * This bean exists to ease the use of provenance upload functionality`
 * 
 * @author madunlap
 */

@ViewScoped
@Named
public class ProvenanceUploadFragmentBean extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());
    
    private UploadedFile jsonUploadedTempFile; 
    
    //These two variables hold the state of the prov variables for the current open file before any changes would be applied by the editing "session"
    private String provJsonState; 
    private String freeformTextState; 
    
    private String freeformTextInput;
    private boolean deleteStoredJson = false; //MAD: rename to reflect that this variable is temporal
    private DataFile activePopupDataFile;
    HashMap<String,HashMap<String,String>> fileProvenanceUpdates = new HashMap<>();
    
    public static final String PROV_FREEFORM = "Prov Freeform";
    public static final String PROV_JSON = "Prov Json";
    
    @EJB
    DataFileServiceBean dataFileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
        
    public void handleFileUpload(FileUploadEvent event) {
        jsonUploadedTempFile = event.getFile();
        provJsonState = null;
    }

    //This updates the popup for the selected file each time its open
    public void updatePopupState(DataFile file) throws WrappedResponse {
        activePopupDataFile = file;
        String storageId = file.getStorageIdentifier();
        deleteStoredJson = false; //MAD: Are there other variables like this I need to init?
        
        if(fileProvenanceUpdates.containsKey(storageId)) { //If there is already staged provenance info
            HashMap innerProvMap = fileProvenanceUpdates.get(storageId);
            freeformTextState = (String) innerProvMap.get(PROV_FREEFORM);
            provJsonState = (String) innerProvMap.get(PROV_JSON); //MAD: As also noted before, I'm throwing a bunch of different things into this string for the same tracking but its likely to get all screwed up
            freeformTextInput = freeformTextState;
            
        } else if(null != file.getOwner()){//Is this file fully uploaded and already has prov data saved?     
            JsonObject provJsonObject = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), activePopupDataFile));
            provJsonState = provJsonObject.toString(); //This may not return quite what we want, this json object gets flipped around a lot --MAD
            freeformTextState = execCommand(new GetProvFreeFormCommand(dvRequestService.getDataverseRequest(), activePopupDataFile));
            freeformTextInput = freeformTextState;

        } else { //Clear the popup
            freeformTextInput = null;
            jsonUploadedTempFile = null;
        }
    }

    //Stores the provenance changes decided upon in the popup to be saved when all edits across files are done.
    public void stagePopupChanges() throws IOException {
        HashMap innerProvMap = fileProvenanceUpdates.get(activePopupDataFile.getStorageIdentifier());
        if(null == innerProvMap) { 
            innerProvMap = new HashMap(); 
        }
            
        if(deleteStoredJson) {
            innerProvMap.put(PROV_JSON, null);
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //may need to specify encoding
            innerProvMap.put(PROV_JSON, jsonString);
            jsonUploadedTempFile = null;
        } 
        
        if(null == freeformTextInput && null != freeformTextState) {
            freeformTextInput = "";
        } 
            
        if(null != freeformTextInput && !freeformTextInput.equals(freeformTextState)) { //MAD: This is triggering even for blank, need to init the no value            
            innerProvMap.put(PROV_FREEFORM, freeformTextInput);
        }
        
        fileProvenanceUpdates.put(activePopupDataFile.getStorageIdentifier(), innerProvMap);       
    }
    
    //Saves the staged provenance data, to be called by the pages launching the popup
    public void saveStagedProvenance(Dataset dataset) throws WrappedResponse {
        for (Map.Entry<String, HashMap<String, String>> entry : fileProvenanceUpdates.entrySet()) {
            String storageId = entry.getKey();
            
            DataFile df = dataFileService.findByStorageIdandDatasetVersion(storageId, dataset.getLatestVersion());
            HashMap innerProvMap = entry.getValue();
            if(innerProvMap.containsKey(PROV_FREEFORM)) {
                String freeformString = (String) innerProvMap.get(PROV_FREEFORM);
                execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), df , freeformString));
            }
            if(innerProvMap.containsKey(PROV_JSON)) {
                String provString = (String) innerProvMap.get(PROV_JSON);
                if(null == provString) {
                    execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df));
                } else {
                    execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString));
                    //MAD: I'm not convinced persist will override if needed and I really don't want to keep track on this end so I should update the command if needed
                    //MAD: I could just always call delete...
                }
            }
        }

    }

    public void updateJsonRemoveState() throws WrappedResponse {
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
}
