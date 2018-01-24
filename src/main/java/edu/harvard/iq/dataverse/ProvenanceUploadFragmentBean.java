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
    
    //private Long fileId = null;
    private UploadedFile jsonUploadedTempFile; 
                                        
    //I need to store:
    //DataFile checksum, uploadedJsonString, uploadedFreeformText
    //// freeform text may just be able to be added to the metadata.
    //Do I need to save anything else? Most of the flow should be the same as before, we hopefully won't need more temp files as much of the popup workings should remain the same.
    
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

   
    
    //MAD: NEXT: Actually try using the uploaded unsaved files and debug through to find the correct identifier for the k,v
    //              may have to write a bit more code first to get tehre.
    
    //This updates the popup when it is opened for a different file
    public void updatePopupState(DataFile file) throws WrappedResponse {
        activePopupDataFile = file;
        String storageId = file.getStorageIdentifier();
        deleteStoredJson = false; //MAD: Are there other variables like this I need to init?
        
        
        
        if(fileProvenanceUpdates.containsKey(storageId)) { //If there is already staged provenance info
            HashMap innerProvMap = fileProvenanceUpdates.get(storageId);
            freeformTextState = (String) innerProvMap.get(PROV_FREEFORM);
            provJsonState = (String) innerProvMap.get(PROV_JSON); //MAD: As also noted before, I'm throwing a bunch of different things into this string for the same tracking but its likely to get all screwed up
            freeformTextInput = freeformTextState;
            
            //MAD: Still need to handle the json. Also I'm unsure about the below chunk...
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
    
    //maybe this should be updatePopupState, but just take the dataFile instead
    //there is an order we have to go through things every time we open this prov popup for a datafile
    //1: is there data in the k,v(s)
    //2: is the file already attached to the dataset. If so, try to query the data with commands and add it to the k,v(s)
//    public void init(Long setFileId) throws WrappedResponse {
//
//    }
    
    
    
    
    //MAD: I need a new method to stage the content of the popup when the popup "Save Changes" is clicked (well actually the continue button)
    //          This will need to store the two provenance strings to whatever data structure I end up with.
    //          When I do this I need to be sure whatever edge cases I thought about with the old flow don't get lost.
    
    
        //MAD: This only works from the edit files page. It does not work from create dataset or upload file.
        //          In those cases I need to stage the changes and call this method when the outer save is called.
        //          This may need to be done from ALL pages depending on feedback from github.
        //          Something also needs to happen to ensure that save changes doesn't trigger saving provenance data not wanted and discarded by closing the popup.
        //          I will also need to check on the status of the alert thrown across the 3 pages and ensure the text makes sense.
    
    //MAD NEW: This will need to be rewritten to go through a k,v of provenance info and save the correct json string as a new file
    //              For the freeform content, I can either add it into the FileMetadata for the files already uploaded or process it as a k,v as well
    //              I get the feeling the k,v may be easier as this is to run both on non-saved and saved files and k,v will be a more similar flow and use the same commands.
    //
    //              Maybe rename it as well.
//    public void saveContent() {
//        try {
//            
//            if(deleteStoredJson) {
//                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), currentDataFile));
//                deleteStoredJson = false;
//            }
//            if(null != jsonTempFile && "application/json".equalsIgnoreCase(jsonTempFile.getContentType())) {
//                String jsonString = IOUtils.toString(jsonTempFile.getInputstream()); //may need to specify encoding
//                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), currentDataFile, jsonString));     
//                jsonTempFile = null;
//            } 
//            if(null == freeformTextInput) {
//                freeformTextInput = "";
//            }
//            if(!(freeformTextInput.equals(freeformTextStored))) {
//                execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), currentDataFile , freeformTextInput));
//            }
//        }        
//        catch (WrappedResponse | IOException ex) {
//            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
//            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
//        }
//    }
    
    //MAD: I need a new method to stage the content of the popup when the popup "Save Changes" is clicked (well actually the continue button)
    //          This will need to store the two provenance strings to whatever data structure I end up with.
    //          When I do this I need to be sure whatever edge cases I thought about with the old flow don't get lost.
    //          
    //          How do I keep track of wanting to delete? Just make the value null?
    public void stagePopupChanges() throws IOException {
        //fileProvenanceStore
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
    
    //MAD: I should pass in the info I need to process my list. I need to know all the files in the dataset
    //      I'm not sure passing the whole dataset in is the way but ok.
    //      There is probably a less intensive way?
    //This is to be called only after our DataFiles have been saved.
    public void saveStagedProvenance(Dataset dataset) throws WrappedResponse {
        //fileProvenanceUpdates
        for (Map.Entry<String, HashMap<String, String>> entry : fileProvenanceUpdates.entrySet()) {
            String storageId = entry.getKey();
            
            //MAD: this command may just not work? I got this error: Error finding datafile by storageID and DataSetVersion: java.lang.Integer cannot be cast to java.lang.Long
            DataFile df = dataFileService.findByStorageIdandDatasetVersion(storageId, dataset.getLatestVersion()); //MAD: How do I get DatasetVersion
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
                    //MAD: Persist. I'm not convinced persist will override if needed and I really don't want to keep track on this end so I should update the command if needed
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
