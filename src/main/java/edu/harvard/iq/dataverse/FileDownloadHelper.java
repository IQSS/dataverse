/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.PrivateUrlUser;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;
//import org.primefaces.context.RequestContext;

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

    private final Map<Long, Boolean> fileDownloadPermissionMap = new HashMap<>(); // { FileMetadata.id : Boolean } 

    public FileDownloadHelper() {
        this.filesForRequestAccess = new ArrayList<>();
    }

    // See also @Size(max = 255) in GuestbookResponse
     private boolean testResponseLength(String value) {
        return !(value != null && value.length() > 255);
     }

    // This helper method is called from the Download terms/guestbook/etc. popup,
    // when the user clicks the "ok" button. We use it, instead of calling
    // downloadServiceBean directly, in order to differentiate between single
    // file downloads and multiple (batch) downloads - sice both use the same
    // terms/etc. popup.
    public void writeGuestbookAndStartDownload(GuestbookResponse guestbookResponse) {
        PrimeFaces.current().executeScript("PF('downloadPopup').hide()");
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

     public void writeGuestbookAndOpenSubset(GuestbookResponse guestbookResponse) {

             PrimeFaces.current().executeScript("PF('downloadPopup').hide()");
             PrimeFaces.current().executeScript("PF('downloadDataSubsetPopup').show()");
             guestbookResponse.setDownloadtype("Subset");
             fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);

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
         if(fmd== null) {
             DatasetVersion dv = guestbookResponse.getDatasetVersion();
             for(FileMetadata fm: dv.getFileMetadatas()) {
                 if(fm.getDataFile()==guestbookResponse.getDataFile()) {
                     fmd=fm;
                     break;
                 }
             }
         }

         fileDownloadService.explore(guestbookResponse, fmd, externalTool);
         //requestContext.execute("PF('downloadPopup').hide()");
         PrimeFaces.current().executeScript("PF('downloadPopup').hide()");
    }
     
    public void writeGuestbookAndLaunchPackagePopup(GuestbookResponse guestbookResponse) {

            PrimeFaces.current().executeScript("PF('downloadPopup').hide()");
            PrimeFaces.current().executeScript("PF('downloadPackagePopup').show()");
            PrimeFaces.current().executeScript("handleResizeDialog('downloadPackagePopup')");
            fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
    }

     /**
      * Writes a guestbook entry for either popup scenario: guestbook or terms.
      */
     public boolean writeGuestbookAndShowPreview(GuestbookResponse guestbookResponse) {
         guestbookResponse.setDownloadtype("Explore");
         fileDownloadService.writeGuestbookResponseRecord(guestbookResponse);
         return true;
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
     * @param  fileMetadata
     * @return boolean
     */
   public boolean canDownloadFile(FileMetadata fileMetadata){
        if (fileMetadata == null){
            return false;
        }
       
        if ((fileMetadata.getId() == null) || (fileMetadata.getDataFile().getId() == null)){
            return false;
        }
        
        if (session.getUser() instanceof PrivateUrlUser) {
             // Always allow download for PrivateUrlUser
             return true;
         }
        
        Long fid = fileMetadata.getId();
        //logger.info("calling candownloadfile on filemetadata "+fid);
        // Note that `isRestricted` at the FileMetadata level is for expressing intent by version. Enforcement is done with `isRestricted` at the DataFile level.
        boolean isRestrictedFile = fileMetadata.isRestricted() || fileMetadata.getDataFile().isRestricted();
        
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

        if (!isRestrictedFile && !FileUtil.isActivelyEmbargoed(fileMetadata)){
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

    public boolean isRestrictedOrEmbargoed(FileMetadata fileMetadata) {
        return fileMetadata.isRestricted() || FileUtil.isActivelyEmbargoed(fileMetadata);
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
    
    public void handleCommandLinkClick(FileMetadata fmd){
        
        if (FileUtil.isRequestAccessPopupRequired(fmd.getDatasetVersion())){
            addFileForRequestAccess(fmd.getDataFile());
            PrimeFaces.current().executeScript("PF('requestAccessPopup').show()");
        } else {
            requestAccess(fmd.getDataFile());
        }

    }

     public void requestAccessMultiple(List<DataFile> files) {
         //need to verify that a valid request was made before 
         //sending the notification - if at least one is valid send the notification
         boolean succeeded = false;
         boolean test = false;
         DataFile notificationFile = null;
         for (DataFile file : files) {
             //Not sending notification via request method so that
             // we can bundle them up into one nofication at dataset level
             test = processRequestAccess(file, false);
             succeeded |= test;
             if (notificationFile == null) {
                 notificationFile = file;
             }
         }
         if (notificationFile != null && succeeded) {
             fileDownloadService.sendRequestFileAccessNotification(notificationFile, (AuthenticatedUser) session.getUser());
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
    
    
     private boolean processRequestAccess(DataFile file, Boolean sendNotification) {
         if (fileDownloadService.requestAccess(file.getId())) {
             // update the local file object so that the page properly updates
             AuthenticatedUser user = (AuthenticatedUser) session.getUser();
             file.addFileAccessRequester(user);

             // create notification if necessary
             if (sendNotification) {
                 fileDownloadService.sendRequestFileAccessNotification(file, user);
             }           
             JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("file.accessRequested.success"));
             return true;
         }
         JsfHelper.addWarningMessage(BundleUtil.getStringFromBundle("file.accessRequested.alreadyRequested", Arrays.asList(file.getDisplayName())));
         return false;
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
