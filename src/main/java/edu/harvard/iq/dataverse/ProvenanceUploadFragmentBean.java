/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.api.AbstractApiBean;
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
import java.util.logging.Level;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * This bean exists to ease the reuse of provenance upload code across pages
 * 
 * @author madunlap
 */
//MAD: Unsure if I should be extending abstractAPI and implementing...
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
    private String provFreeformText;
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    public void handleFileUpload(FileUploadEvent event) {
        jsonTempFile = event.getFile();
    }
    
    //MAD: Rename to just saveContent to reflect the free text as well
    public void saveUploadedContent() {
        try {
            //"text/plain" as well?
            if(null != jsonTempFile && "application/json".equalsIgnoreCase(jsonTempFile.getContentType())) { //MAD: This needs a check for whether its been saved already?
                String jsonString = IOUtils.toString(jsonTempFile.getInputstream()); //may need to specify encoding
                try {
                    execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), jsonString)); //DataverseRequest aRequest, DataFile dataFile, String userInput)       
                } catch (WrappedResponse ex) {
                    Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
                    //MAD: Catch error better
                }
            }
            
            
            
        } catch (IOException e) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, e);
            //what to do if file is not there when trying to save? Anything at all? if nothing probably should just say it throws
        } 
        try { //freeform. maybe collapse with above
            if(!(null == provFreeformText || "".equals(provFreeformText))) {
                execCommand(new PersistProvFreeFormCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), provFreeformText));
            }
        }        
        catch (WrappedResponse ex) {
            Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void setFileId(Long setFileId) {
        fileId = setFileId;
    }
    
    public boolean provJsonExists() {
        return false;
    }
    
    //What do I actually need to return for these??? Just the name of the file?
    public String getTempJsonFileName() {
        if(null == jsonTempFile) {
            return "";
        }
        return jsonTempFile.getFileName();
    }
    
//    public String getStoredJsonFile() {
//        return "";
//    }
        
    public String getFreeformText() {
        return provFreeformText;
    }
    
    public void setFreeformText(String freeformText) {
        provFreeformText = freeformText;
    }
}
