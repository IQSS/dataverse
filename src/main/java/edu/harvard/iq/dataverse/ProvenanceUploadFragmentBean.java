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
    
    /**
     * Handle native file replace
     * @param event 
     * @throws java.io.IOException 
     */
    
    private Long fileId = null;
    private UploadedFile jsonTempFile; 
                                        
    private JsonObject provJsonStored;
    private String freeformTextInput;
    private String freeformTextStored;
    private boolean deleteStoredJson = false;
    
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
    }
    
    public void saveContent() {
        try {
            
            if(deleteStoredJson) {
                execCommand(new DeleteProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId)));
                deleteStoredJson = false;
            }
            if(null != jsonTempFile && "application/json".equalsIgnoreCase(jsonTempFile.getContentType())) {
                String jsonString = IOUtils.toString(jsonTempFile.getInputstream()); //may need to specify encoding
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), jsonString));     
                jsonTempFile = null;
            } 
            if(null == freeformTextInput) {
                freeformTextInput = "";
            }
            if(!(freeformTextInput.equals(freeformTextStored))) {
                execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), freeformTextInput));
            }
        }        
        catch (WrappedResponse | IOException ex) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
        }
    }

    public void updateJsonRemoveState() throws WrappedResponse {
        if (jsonTempFile != null) {
            jsonTempFile = null;
        } else if (provJsonStored != null) {
            provJsonStored = null;
            deleteStoredJson = true;
        }        
    }
    public boolean getJsonUploadedState() {
        return null != jsonTempFile || null != provJsonStored;   
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
