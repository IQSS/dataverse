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
    UploadedFile uploadedJsonFile;
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    public void handleFileUpload(FileUploadEvent event) throws IOException {
    
        //this really needs to store the content to a temp location.... does it actually need to be a temp file?
        
        
        //"text/plain" as well?
        uploadedJsonFile = event.getFile(); //TOOD: try/catch this too?
        if(( uploadedJsonFile != null) && "application/json".equalsIgnoreCase(uploadedJsonFile.getContentType())) {
            String jsonString = IOUtils.toString(uploadedJsonFile.getInputstream()); //may need to specify encoding
            try {
                execCommand(new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), jsonString)); //DataverseRequest aRequest, DataFile dataFile, String userInput)       
            } catch (WrappedResponse ex) {
                Logger.getLogger(ProvenanceUploadFragmentBean.class.getName()).log(Level.SEVERE, null, ex);
                //MAD: Catch error better
            }
        }
        //DataFile dataFile = fileSvc.find(idSuppliedAsLong);
       
    }
    
    public void storeUploadedProvJson() {
        
    }
    
    public void setFileId(Long setFileId) {
        fileId = setFileId;
    }
    
    public boolean provJsonExists() {
        return false;
    }
    
    public String getTempProvJson() {
        return "";
    }
    
    public String getStoredProvJson() {
        return "";
    }
}
