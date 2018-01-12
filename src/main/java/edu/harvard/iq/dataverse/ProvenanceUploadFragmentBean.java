/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PersistProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteProvJsonProvCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvFreeFormCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetProvJsonProvCommand;
import java.io.File;
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * This bean exists to ease the reuse of provenance upload code across pages
 * 
 * @author madunlap
 */
//MAD: Unsure if I should be extending abstractAPI and implementing...//MAD: Should probably use commands instead
//        StorageIO<DataFile> dataAccess = DataAccess.getStorageIO(datafileService.find(fileId));
//        dataAccess.getAuxFileAsInputStream

@ViewScoped
@Named
public class ProvenanceUploadFragmentBean extends AbstractApiBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());
    
    /**
     * Handle native file replace
     * @param event 
     * @throws java.io.IOException 
     */
    
    private Long fileId = null;
    private UploadedFile jsonTempFile; //this isn't that useful to track state as the command does not return a file...
                                        // we will want preview to use the current file when it comes into being..
    private JsonObject provJsonStored;
    private String freeformTextInput;
    private String freeformTextStored;
    private boolean deleteStoredJson = false;
    //private boolean provFreeformStaged = false;
    //private boolean provJsonStaged = false;
    
    //MAD: This is hardcoded in the commands and here. We always name the prov file the same thing and the commands currently
    //      don't return the file itself, just the contents, but we should display the name as that's how the UI is set up...
    final String provJsonName = "prov-json.json"; 
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
        
    public void init(Long setFileId) throws WrappedResponse {
        fileId = setFileId;
        provJsonStored = execCommand(new GetProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId)));
        freeformTextStored = execCommand(new GetProvFreeFormCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId)));
        freeformTextInput = freeformTextStored;
    }
    
    public void handleFileUpload(FileUploadEvent event) {
        jsonTempFile = event.getFile();
        provJsonStored = null;
        //provJsonStaged = true;
    }
    
    //MAD: If failure, what do I do? User should know the prov upload failed, maybe just close popup?
    //MAD: Save should check if new content is there
    public void saveContent() {
        try {
            //"text/plain" as well?
            if(null != jsonTempFile && "application/json".equalsIgnoreCase(jsonTempFile.getContentType())) { //MAD: This needs a check for whether its been saved already?
                String jsonString = IOUtils.toString(jsonTempFile.getInputstream()); //may need to specify encoding
                try {
                    execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), jsonString)); //DataverseRequest aRequest, DataFile dataFile, String userInput)       
                    jsonTempFile = null;
                } catch (WrappedResponse ex) {
                    Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
                    //MAD: Catch error better
                }
            } else if(deleteStoredJson) {
                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId)));
                deleteStoredJson = false;
            }
        } catch (IOException e) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, e);
            //what to do if file is not there when trying to save? Anything at all? if nothing probably should just say it throws
        } catch (WrappedResponse ex) { 
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        try { //freeform. maybe collapse with above
            if(!(null == freeformTextInput || freeformTextInput.equals(freeformTextStored))) {
                execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), freeformTextInput));
            }
        }        
        catch (WrappedResponse ex) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }


    //MAD: Called by UI. This needs to smartly detect whether its removing a temp file or a permanent one
    //Nothing should be saved to db
    public void updateJsonRemoveState() throws WrappedResponse {
        if (jsonTempFile != null) {
            jsonTempFile = null;
        } else if (provJsonStored != null) {
            provJsonStored = null;
            deleteStoredJson = true;
        }        
    }
    
    //What do I actually need to return for these??? Just the name of the file?
    //MAD: Logic is a bit wonky but leaving it incase we need to distinguish the temp file from the stored file in the UI
    public String getJsonFileName() {
        if(null != jsonTempFile) {
            return provJsonName;
        } else if (null != provJsonStored) {
            return provJsonName;
        }
        return null;
    }
        
    public String getFreeformTextInput() {
        return freeformTextInput;
    }
    
    public void setFreeformTextInput(String freeformText) {
        freeformTextInput = freeformText;
    }
    
    public String getFreeformTextStored() {
        return freeformTextStored;
    }
    
    public void setFreeformTextStored(String freeformText) {
        freeformTextStored = freeformText;
    }
}
