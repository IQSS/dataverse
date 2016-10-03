/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datasetutility;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DatasetServiceBean;
import edu.harvard.iq.dataverse.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.DataverseLinkingServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserNotificationServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author rmp553
 */
@ViewScoped
@Named("FileUploadTestPage")
public class FileUploadTestPage implements java.io.Serializable {
    
    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());

    @EJB
    IngestServiceBean ingestService;
    @Inject DataverseSession session;    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    UserNotificationServiceBean userNotificationService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    AuthenticationServiceBean authService;
    @EJB
    SystemConfig systemConfig;
    @EJB
    DataverseLinkingServiceBean dvLinkingService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    EjbDataverseEngine commandEngine;
    
    public String init() {
        
        return null;
    }

    public String yesYes(){
        return "yes yes";
    }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }


    public void handleFileUpload(FileUploadEvent event) {
        msgt("handleFileUpload");
        
        //FacesMessage message = new FacesMessage("Succesful", event.getFile().getFileName() + " is uploaded.");
        //FacesContext.getCurrentInstance().addMessage(null, message);
            
        
        UploadedFile uFile = event.getFile();

        msg("getFileName: " + uFile.getFileName());
        msg("getContentType: " + uFile.getContentType());
        
        addFile(uFile);
        //msg("file name: " + event.getFileName());
    //        dFileList = ingestService.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
      
    }
    
    
    public void addFile(UploadedFile laFile){
        
        
        //DataverseRequest dvRequest2 = createDataverseRequest(authUser);
        AddReplaceFileHelper addFileHelper = new AddReplaceFileHelper(dvRequestService.getDataverseRequest(),
                                                ingestService,
                                                datasetService,
                                                datafileService,
                                                permissionService,
                                                commandEngine);
    
        
        InputStream inputStream = null;
        try {
            inputStream = laFile.getInputstream();
        } catch (IOException ex) {
            msgt("file io exception");

            Logger.getLogger(FileUploadTestPage.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        
        addFileHelper.runAddFileByDatasetId(new Long(10),
                                laFile.getFileName(),
                                laFile.getContentType(),
                                inputStream);
        
        if (addFileHelper.hasError()){
            msgt("upload error");
            msg(addFileHelper.getErrorMessagesAsString("\n"));
        }else{
            msg("Look at that!  You added a file! (hey hey, it may have worked)");
        }
    }
}
