/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.util.BundleUtil;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIInput;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

/**
 *
 * @author skraffmi
 * 
 *
 */
 @ViewScoped
 @Named
public class FileDownloadHelper implements java.io.Serializable {
     
    private static final Logger logger = Logger.getLogger(FileDownloadHelper.class.getCanonicalName());
    @Inject
    DataverseSession session;
        
    @Inject
    DataverseRequestServiceBean dvRequestService;

    @EJB
    PermissionServiceBean  permissionService;
    
    @EJB
    FileDownloadServiceBean  fileDownloadService;
    
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    
    @EJB
    DataFileServiceBean datafileService;
    
    UIInput nameField;

    public UIInput getNameField() {
        return nameField;
    }

    public void setNameField(UIInput nameField) {
        this.nameField = nameField;
    }

    public UIInput getEmailField() {
        return emailField;
    }

    public void setEmailField(UIInput emailField) {
        this.emailField = emailField;
    }

    public UIInput getInstitutionField() {
        return institutionField;
    }

    public void setInstitutionField(UIInput institutionField) {
        this.institutionField = institutionField;
    }

    public UIInput getPositionField() {
        return positionField;
    }

    public void setPositionField(UIInput positionField) {
        this.positionField = positionField;
    }
    UIInput emailField;
    UIInput institutionField;
    UIInput positionField;
   
    


    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { FileMetadata.id : Boolean } 

     public void nameValueChangeListener(AjaxBehaviorEvent e) {
         String name= (String) ((UIOutput) e.getSource()).getValue();
         this.guestbookResponse.setName(name);
     }
     
    public void emailValueChangeListener(AjaxBehaviorEvent e) {
         String email= (String) ((UIOutput) e.getSource()).getValue();
         this.guestbookResponse.setEmail(email);
     }
    
    public void institutionValueChangeListener(AjaxBehaviorEvent e) {        
         String institution= (String) ((UIOutput) e.getSource()).getValue();
         this.guestbookResponse.setInstitution(institution);
     }
    
    public void positionValueChangeListener(AjaxBehaviorEvent e) {
         String position= (String) ((UIOutput) e.getSource()).getValue();
         this.guestbookResponse.setPosition(position);
     }
    
    public void customQuestionValueChangeListener(AjaxBehaviorEvent e) {
        String questionNo = (String) ((UIOutput) e.getSource()).getId();
         String position= (String) ((UIOutput) e.getSource()).getValue();
     }
    
    public FileDownloadHelper() {
        this.filesForRequestAccess = new ArrayList<>();
    }

    
     private boolean testResponseLength(String value) {
        return !(value != null && value.length() > 255);
     }
     
     private boolean validateGuestbookResponse(GuestbookResponse guestbookResponse){
                Dataset dataset = guestbookResponse.getDataset();
                boolean valid = true;
         if (dataset.getGuestbook() != null) {
             if (dataset.getGuestbook().isNameRequired()) {
                 boolean nameValid = (guestbookResponse.getName() != null && !guestbookResponse.getName().isEmpty());
                 if (!nameValid) {
                     nameField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(nameField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("requiredField"), null));
                 }
                 valid &= nameValid;
             }
                valid &= testResponseLength(guestbookResponse.getName());
                 if (! testResponseLength(guestbookResponse.getName())){
                    nameField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(nameField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.responseTooLong"), null));
                 }
             if (dataset.getGuestbook().isEmailRequired()) {
                 boolean emailValid = (guestbookResponse.getEmail() != null && !guestbookResponse.getEmail().isEmpty());
                 if (!emailValid) {
                     emailField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(emailField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("requiredField"), null));
                 }
                 valid &= emailValid;
             }
                valid &= testResponseLength(guestbookResponse.getEmail());
                 if (! testResponseLength(guestbookResponse.getEmail())){
                    emailField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(emailField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.responseTooLong"), null));
                 }
            if (dataset.getGuestbook().isInstitutionRequired()) {
                 boolean institutionValid = (guestbookResponse.getInstitution()!= null && !guestbookResponse.getInstitution().isEmpty());
                 if (!institutionValid) {
                     institutionField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(institutionField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("requiredField"), null));
                 }
                valid &= institutionValid;
            }
                valid &= testResponseLength(guestbookResponse.getInstitution());
                 if (! testResponseLength(guestbookResponse.getInstitution())){
                    institutionField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(institutionField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.responseTooLong"), null));
                 }
            if (dataset.getGuestbook().isPositionRequired()) {
                 boolean positionValid = (guestbookResponse.getPosition()!= null && !guestbookResponse.getPosition().isEmpty());
                 if (!positionValid) {
                     positionField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(positionField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("requiredField"), null));
                 }
                valid &= positionValid;
            }
                valid &= testResponseLength(guestbookResponse.getPosition());
                 if (! testResponseLength(guestbookResponse.getPosition())){
                    positionField.setValid(false);
                     FacesContext.getCurrentInstance().addMessage(positionField.getClientId(),
                             new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.guestbookResponse.guestbook.responseTooLong"), null));
                 }
        }

        if (dataset.getGuestbook() != null && !dataset.getGuestbook().getCustomQuestions().isEmpty()) {
            for (CustomQuestion cq : dataset.getGuestbook().getCustomQuestions()) {
                if (cq.isRequired()) {
                    for (CustomQuestionResponse cqr : guestbookResponse.getCustomQuestionResponses()) {
                        if (cqr.getCustomQuestion().equals(cq)) {
                            valid &= (cqr.getResponse() != null && !cqr.getResponse().isEmpty());
                            if (cqr.getResponse() == null ||  cqr.getResponse().isEmpty()){
                                cqr.setValidationMessage(BundleUtil.getStringFromBundle("requiredField"));                               
                            } else{
                                cqr.setValidationMessage(""); 
                            }                          
                        }
                    }
                }
            }
        }       

        return valid;
         
     }
    
     // This helper method is called from the Download terms/guestbook/etc. popup, 
     // when the user clicks the "ok" button. We use it, instead of calling 
     // downloadServiceBean directly, in order to differentiate between single
     // file downloads and multiple (batch) downloads - sice both use the same 
     // terms/etc. popup. 
     public void writeGuestbookAndStartDownload(GuestbookResponse guestbookResponse) {
         RequestContext requestContext = RequestContext.getCurrentInstance();
         boolean valid = validateGuestbookResponse(guestbookResponse);

         if (!valid) {
             JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.validationError"));
         } else {
             requestContext.execute("PF('downloadPopup').hide()");
             guestbookResponse.setDownloadtype("Download");

             // Note that this method is only ever called from the file-download-popup - 
             // meaning we know for the fact that we DO want to save this 
             // guestbookResponse permanently in the database.
             if (guestbookResponse.getSelectedFileIds() != null) {
                 // this is a batch (multiple file) download.
                 // Although here's a chance that this is not really a batch download - i.e., 
                 // there may only be one file on the file list. But the fileDownloadService 
                 // method below will check for that, and will redirect to the single download, if
                 // that's the case. -- L.A.
                 fileDownloadService.writeGuestbookAndStartBatchDownload(guestbookResponse);
             } else if (guestbookResponse.getDataFile() != null) {
                 // this a single file download: 
                 fileDownloadService.writeGuestbookAndStartFileDownload(guestbookResponse);
             }
         }

     }
     
     public void writeGuestbookAndOpenSubset(GuestbookResponse guestbookResponse) {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        boolean valid = validateGuestbookResponse(guestbookResponse);
                  
         if (!valid) {
             
         } else {
             requestContext.execute("PF('downloadPopup').hide()"); 
             requestContext.execute("PF('downloadDataSubsetPopup').show()");
             guestbookResponse.setDownloadtype("Subset");
             fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
         }

     }

     /**
      * This method is only invoked from a popup. A popup appears when the
      * user might have to accept terms of use, fill in a guestbook, etc.
      */
     public void writeGuestbookAndLaunchExploreTool(GuestbookResponse guestbookResponse, FileMetadata fmd, ExternalTool externalTool) {

         /**
          * We need externalTool to be non-null when calling "explore" below (so
          * that we can instantiate an ExternalToolHandler) so we retrieve
          * externalTool from a transient variable in guestbookResponse if
          * externalTool is null. The current observation is that externalTool
          * is null from the dataset page and non-null from the file page. See
          * file-download-button-fragment.xhtml where the popup is launched (as
          * needed) and file-download-popup-fragment.xhtml for the popup itself.
          *
          * TODO: If we could figure out a way for externalTool to always be
          * non-null, we could remove this if statement and the transient
          * "externalTool" variable on guestbookResponse.
          */
         if (externalTool == null) {
             externalTool = guestbookResponse.getExternalTool();
         }

         RequestContext requestContext = RequestContext.getCurrentInstance();
         boolean valid = validateGuestbookResponse(guestbookResponse);

         if (!valid) {
             return;
         }
         fileDownloadService.explore(guestbookResponse, fmd, externalTool);
         requestContext.execute("PF('downloadPopup').hide()");
    }
     
    public void writeGuestbookAndLaunchPackagePopup(GuestbookResponse guestbookResponse) {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        boolean valid = validateGuestbookResponse(guestbookResponse);

        if (!valid) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.validationError"));
        } else {
            requestContext.execute("PF('downloadPopup').hide()");
            requestContext.execute("PF('downloadPackagePopup').show()");
            requestContext.execute("handleResizeDialog('downloadPackagePopup')");

            fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
        }
    }

    public String startWorldMapDownloadLink(GuestbookResponse guestbookResponse, FileMetadata fmd){
        
        RequestContext requestContext = RequestContext.getCurrentInstance();
        boolean valid = validateGuestbookResponse(guestbookResponse);
                  
         if (!valid) {
             return "";
         } 
        guestbookResponse.setDownloadtype("WorldMap");
        String retVal = fileDownloadService.startWorldMapDownloadLink(guestbookResponse, fmd);
        requestContext.execute("PF('downloadPopup').hide()"); 
        return retVal;
    }

        
    private List<DataFile> filesForRequestAccess;

    public List<DataFile> getFilesForRequestAccess() {
        return filesForRequestAccess;
    }

    public void setFilesForRequestAccess(List<DataFile> filesForRequestAccess) {
        this.filesForRequestAccess = filesForRequestAccess;
    }
    
    public void addFileForRequestAccess(DataFile dataFile){
        this.filesForRequestAccess.clear();
        this.filesForRequestAccess.add(dataFile);
    }
    
    public void clearRequestAccessFiles(){
        this.filesForRequestAccess.clear();
    }
    
    public void addMultipleFilesForRequestAccess(DataFile dataFile) {
        this.filesForRequestAccess.add(dataFile);

    }
        
    private String selectedFileId = null;

    public String getSelectedFileId() {
        return selectedFileId;
    }

    public void setSelectedFileId(String selectedFileId) {
        this.selectedFileId = selectedFileId;
    }
   
    /**
     *  WARNING: Before calling this, make sure the user has download
     *  permission for the file!!  (See DatasetPage.canDownloadFile())
     * 
     * Should there be a Explore WorldMap Button for this file?
     *   See table in: https://github.com/IQSS/dataverse/issues/1618
     * 
     *  (1) Does the file have MapLayerMetadata?
     *  (2) Are the proper settings in place
     * 
     * @param fm fileMetadata
     * @return boolean
     */
   public boolean canDownloadFile(FileMetadata fileMetadata){
        if (fileMetadata == null){
            return false;
        }
       
        if ((fileMetadata.getId() == null) || (fileMetadata.getDataFile().getId() == null)){
            return false;
        } 
        
        Long fid = fileMetadata.getId();
        //logger.info("calling candownloadfile on filemetadata "+fid);
        // Note that `isRestricted` at the FileMetadata level is for expressing intent by version. Enforcement is done with `isRestricted` at the DataFile level.
        boolean isRestrictedFile = fileMetadata.isRestricted();
        
        // Has this file been checked? Look at the DatasetPage hash
        if (this.fileDownloadPermissionMap.containsKey(fid)){
            // Yes, return previous answer
            //logger.info("using cached result for candownloadfile on filemetadata "+fid);
            return this.fileDownloadPermissionMap.get(fid);
        }
       if (fileMetadata.getDatasetVersion().isDeaccessioned()) {
           if (this.doesSessionUserHavePermission(Permission.EditDataset, fileMetadata)) {
               // Yes, save answer and return true
               this.fileDownloadPermissionMap.put(fid, true);
               return true;
           } else {
               this.fileDownloadPermissionMap.put(fid, false);
               return false;
           }
       }

        if (!isRestrictedFile){
            // Yes, save answer and return true
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }
        
        // See if the DataverseRequest, which contains IP Groups, has permission to download the file.
        if (permissionService.requestOn(dvRequestService.getDataverseRequest(), fileMetadata.getDataFile()).has(Permission.DownloadFile)) {
            logger.fine("The DataverseRequest (User plus IP address) has access to download the file.");
            this.fileDownloadPermissionMap.put(fid, true);
            return true;
        }

        this.fileDownloadPermissionMap.put(fid, false);
        return false;
    }
   
    public boolean doesSessionUserHavePermission(Permission permissionToCheck, FileMetadata fileMetadata){
        if (permissionToCheck == null){
            return false;
        }
        
        DvObject objectToCheck = null;
        
        if (permissionToCheck.equals(Permission.EditDataset)){
            objectToCheck = fileMetadata.getDatasetVersion().getDataset();
        } else if (permissionToCheck.equals(Permission.DownloadFile)){
            objectToCheck = fileMetadata.getDataFile();
        }
        
        if (objectToCheck == null){
            return false;
        }
        
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), objectToCheck).has(permissionToCheck);
       
        // return true/false
        return hasPermission;
    }
    
    
    public void requestAccess(DataFile file){   
        //Called from download button fragment via either dataset page or file page
        // when there's only one file for the access request and there's no pop-up
        processRequestAccess(file, true);        
    }
    
    public void requestAccessMultiple(List<DataFile> files) {

         DataFile notificationFile = null;
         for (DataFile file : files) {
             //Not sending notification via request method so that
             // we can bundle them up into one nofication at dataset level
             processRequestAccess(file, false);
             if (notificationFile == null){
                 notificationFile = file;
             }
         }
         if ( notificationFile != null){
             fileDownloadService.sendRequestFileAccessNotification(notificationFile.getOwner(), notificationFile.getId(), (AuthenticatedUser) session.getUser()); 
         }
     }
    
     public void requestAccessIndirect() {
         //Called when there are multiple files and no popup
         // or there's a popup with sigular or multiple files
         // The list of files for Request Access is set in the Dataset Page when
         // user clicks the request access button in the files fragment
         // (and has selected one or more files)
         requestAccessMultiple(this.filesForRequestAccess);
     }    
    
    
     private void processRequestAccess(DataFile file, Boolean sendNotification) {
         if (fileDownloadService.requestAccess(file.getId())) {
             // update the local file object so that the page properly updates
             file.getFileAccessRequesters().add((AuthenticatedUser) session.getUser());
             // create notification if necessary
             if (sendNotification) {
                 fileDownloadService.sendRequestFileAccessNotification(file.getOwner(), file.getId(), (AuthenticatedUser) session.getUser());
             }
         }
     } 
     
    private GuestbookResponse guestbookResponse;

    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }    
    
    public GuestbookResponseServiceBean getGuestbookResponseService(){
        return this.guestbookResponseService;
    }
    
    
    //todo: potential cleanup - are these methods needed?
    public DataverseSession getSession() {
        return session;
    }

    public void setSession(DataverseSession session) {
        this.session = session;
    }
    
}
