/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import java.io.IOException;
import java.util.logging.Logger;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonProvCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import org.apache.commons.io.IOUtils;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.Objects;
import javax.faces.application.FacesMessage;

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
    private String provJsonState; //This string is used in a few places mainly to check if things are null
    private String freeformTextState; 
    private Dataset dataset;
    
    private String freeformTextInput;
    private boolean deleteStoredJson = false;
    private DataFile popupDataFile;
    private ArrayList<DataFile> jsonProvUpdateFiles = new ArrayList<>();
    private ArrayList<String> jsonProvUpdateStrings = new ArrayList<>();
    
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

    public void updatePopupState(DataFile file, Dataset dSet) throws WrappedResponse {
        dataset = dSet;
        updatePopupState(file);
    }
    
    //This updates the popup for the selected file each time its open
    public void updatePopupState(DataFile file) throws WrappedResponse {
        if(null == dataset ) {
            dataset = file.getFileMetadata().getDatasetVersion().getDataset(); //DatasetVersion is null here on file upload page...
        }
        popupDataFile = file;
        deleteStoredJson = false;
        provJsonState = null;
        freeformTextState = popupDataFile.getFileMetadata().getProvFreeForm();
        
        /* We are unable to use the built in Map containsKey method as DataFile equality only checks id.
         * Id is always null before the dataset is crteated. If we updated it to also check checksum this would work.
         * Leaving the simpler code for here if we change it
         */

        int index = getDataFileUpdateIndex(popupDataFile);
        if(index >= 0) {
            provJsonState = jsonProvUpdateStrings.get(index);
        }
        
        else if(null != popupDataFile.getCreateDate()){//Is this file fully uploaded and already has prov data saved?     
            JsonObject provJsonObject = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), popupDataFile));
            if(null != provJsonObject) {
                provJsonState = provJsonObject.toString(); //This may not return quite what we want, this json object gets flipped around a lot --MAD
            }

        } else { //clear the listed uploaded file
            //freeformTextState = null;
            jsonUploadedTempFile = null;
        }
        freeformTextInput = freeformTextState;
    }
    
    public void stagePopupChanges() throws IOException, WrappedResponse {
        stagePopupChanges(false);
    }
    
    private int getDataFileUpdateIndex(DataFile file) {
        for(int i = 0; i < jsonProvUpdateFiles.size(); i++) {
            DataFile listFile = jsonProvUpdateFiles.get(i);
            if(dataFileGoodEquals(file,listFile))
            {
                return i;
            }
        }
        return -1;
    }
    
    private boolean dataFileGoodEquals(DataFile f1, DataFile f2) {
        if (null == f1 || null == f1.getChecksumType() || null == f1.getChecksumValue()
            || null == f2 || null == f2.getChecksumType() || null == f2.getChecksumValue()) {
            return false;
        } 
        
        return Objects.equals(f1.getChecksumType(),f2.getChecksumType()) && Objects.equals(f1.getChecksumValue(),f2.getChecksumValue());

    }
    
    private void dataFileArraysAdd(DataFile file, String s) {
         int index = getDataFileUpdateIndex(file);
            if(index >= 0) {
                jsonProvUpdateStrings.set(index, s);
            } else {
                jsonProvUpdateFiles.add(file);
                jsonProvUpdateStrings.add(s); //add adds to the end, so as long as we are always adding/removing/setting at the same time its ok..
            }
    }
    
    //Stores the provenance changes decided upon in the popup to be saved when all edits across files are done.
    public void stagePopupChanges(boolean saveInPopup) throws IOException{
            
        if(deleteStoredJson) {
            dataFileArraysAdd(popupDataFile, null);
            
            deleteStoredJson = false;
        }
        if(null != jsonUploadedTempFile && "application/json".equalsIgnoreCase(jsonUploadedTempFile.getContentType())) {
            String jsonString = IOUtils.toString(jsonUploadedTempFile.getInputstream()); //may need to specify encoding
            
            dataFileArraysAdd(popupDataFile, jsonString);
            jsonUploadedTempFile = null;
        } 
        
        //This is probably only needed to massage freeform text for when we are using the commands from a non datafile saving page
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
            } catch (WrappedResponse ex) {
                filePage.showProvError();
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
           
    }
    
    //Saves the staged provenance data, to be called by the pages launching the popup
    public void saveStagedJsonProvenance() throws WrappedResponse {
        for(int i = 0; i < jsonProvUpdateFiles.size(); i++)
        {
            DataFile df = jsonProvUpdateFiles.get(i);
            String provString = jsonProvUpdateStrings.get(i);

            if(null == provString) {
                try {
                    execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), df));
                } catch (WrappedResponse wr) {
                    //do nothing, we always try to delete files set to null in the list, even if they were created and then deleted.
                }
            } else {
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), df, provString));
            }
        }
        
    }
    
    //This method is only needed when saving provenance from a page that does not also save changes to datafiles.
    public void saveStagedJsonFreeform() throws WrappedResponse {  
        execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), popupDataFile, freeformTextInput));
    }

    public void updateJsonRemoveState() throws WrappedResponse {
        if (null != jsonUploadedTempFile ) {
            jsonUploadedTempFile = null;
        } else if (null != provJsonState) {
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
