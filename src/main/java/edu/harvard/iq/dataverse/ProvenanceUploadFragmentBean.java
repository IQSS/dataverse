/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

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
import javax.ejb.EJB;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * This bean exists to ease the reuse of provenance upload code across pages
 * 
 * @author madunlap
 */
public class ProvenanceUploadFragmentBean implements java.io.Serializable{
    
    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());
    
    /**
     * Handle native file replace
     * @param event 
     * @throws java.io.IOException 
     */
    
    private Long fileId = null;
    
    @EJB
    DataFileServiceBean datafileService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    
    public void handleFileUpload(FileUploadEvent event) throws IOException {
        
        //copy pasted code from EditDatafilesPage.handleFileUpload ...  FileUtil.createDataFiles ... not actually...
        
        //... the normal upload code uses oncomplete
        //... there is one other fileupload for themes/widgets, searched p:fileUpload
        
        //this is an aux file tho...
        //do I need a size check here as well? If someone uses the api do we reject a huge json file?
        
        //uploadFinished then addFilesToDataset I think gets all the files ready
        //then on buttonclick, EditDatafilesPage.save saves them to the filesystem (calling injestService.addFiles) in the background
        //... These do metadata and standard files... I need an aux file
        
        //DatasetWidgetsPage.handleImageFileUpload is actually pretty close
        
        //.. I think with the way the command is written, you have to get the json out of the file as text and get the data file to attach it on
        //createDataverseRequest(findUserOrDie())
        //findDataFileOrDie(idSupplied) ... both in the prov api file and need to be broken out?
        
        
        //"text/plain" as well?
        UploadedFile uploadedFile = event.getFile(); //TOOD: try/catch this too?
        if(( uploadedFile != null) && "application/json".equalsIgnoreCase(uploadedFile.getContentType())) {
            String jsonString = IOUtils.toString(uploadedFile.getInputstream()); //may need to specify encoding
            new PersistProvJsonProvCommand(dvRequestService.getDataverseRequest(), datafileService.find(fileId), jsonString); //DataverseRequest aRequest, DataFile dataFile, String userInput)       
        }
        //DataFile dataFile = fileSvc.find(idSuppliedAsLong);
        
        
    }
    
    public void setFileId(Long setFileId) {
        fileId = setFileId;
    }

}
