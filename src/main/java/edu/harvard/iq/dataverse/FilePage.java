/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.datasetutility.TwoRavensHelper;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.export.ExportException;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ConstraintViolation;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author skraffmi
 * 
 */

@ViewScoped
@Named("FilePage")
public class FilePage implements java.io.Serializable {
    
    private FileMetadata fileMetadata;
    private Long fileId;  
    private String version;
    private DataFile file;   
    private GuestbookResponse guestbookResponse;
    private int selectedTabIndex;
    private Dataset editDataset;
    private Dataset dataset;
    private List<DatasetVersion> datasetVersionsForTab;
    private List<FileMetadata> fileMetadatasForTab;


    @EJB
    DataFileServiceBean datafileService;
    
    @EJB
    DatasetVersionServiceBean datasetVersionService;

    @EJB
    PermissionServiceBean permissionService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    AuthenticationServiceBean authService;
    
    @EJB
    SystemConfig systemConfig;


    @Inject
    DataverseSession session;
    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject
    TwoRavensHelper twoRavensHelper;
    @Inject WorldMapPermissionHelper worldMapPermissionHelper;

    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    public String init() {
     
        
        if ( fileId != null ) { 
           
            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
               file = datafileService.find(fileId);   

            }  else if (fileId == null){
               return permissionsWrapper.notFound();
            }

            if (file == null){
               return permissionsWrapper.notFound();
            }

                RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
                Long getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
                fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);

          
            if (fileMetadata == null){
               return permissionsWrapper.notFound();
            }

           // If this DatasetVersion is unpublished and permission is doesn't have permissions:
           //  > Go to the Login page
           //
            // Check permisisons       

            
            Boolean authorized = (fileMetadata.getDatasetVersion().isReleased()) ||
                    (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());
            
            if (!authorized ) {
                return permissionsWrapper.notAuthorized();
            }         
           
           this.guestbookResponse = this.guestbookResponseService.initGuestbookResponseForFragment(fileMetadata, session);
           
        } else {

            return permissionsWrapper.notFound();
        }

        return null;
    }
    
    private boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset( dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }
   

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }
    

    public boolean isDownloadPopupRequired() {  
        if(fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null ){
            return false;
        }
        return FileUtil.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
    }


    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public DataFile getFile() {
        return file;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }
    
    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
     
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
    
    public List< String[]> getExporters(){
        List<String[]> retList = new ArrayList();
        String myHostURL = systemConfig.getDataverseSiteUrl();
        for (String [] provider : ExportService.getInstance().getExportersLabels() ){
            String formatName = provider[1];
            String formatDisplayName = provider[0];
            
            Exporter exporter = null; 
            try {
                exporter = ExportService.getInstance().getExporter(formatName);
            } catch (ExportException ex) {
                exporter = null;
            }
            if (exporter != null && exporter.isAvailableToUsers()) {
                // Not all metadata exports should be presented to the web users!
                // Some are only for harvesting clients.
                
                String[] temp = new String[2];            
                temp[0] = formatDisplayName;
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + formatName + "&persistentId=" + fileMetadata.getDatasetVersion().getDataset().getGlobalId();
                retList.add(temp);
            }
        }
        return retList;  
    }
  
    public String restrictFile(boolean restricted) {
        String fileNames = null;
        String termsOfAccess = this.fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().getTermsOfAccess();        
        Boolean allowRequest = this.fileMetadata.getDatasetVersion().getTermsOfUseAndAccess().isFileAccessRequest();
        editDataset = this.file.getOwner();

        for (FileMetadata fmw : editDataset.getEditVersion().getFileMetadatas()) {
            if (fmw.getDataFile().equals(this.fileMetadata.getDataFile())) {
                fileNames += fmw.getLabel();
                fmw.setRestricted(restricted);
            }
        }
        
        editDataset.getEditVersion().getTermsOfUseAndAccess().setTermsOfAccess(termsOfAccess);
        editDataset.getEditVersion().getTermsOfUseAndAccess().setFileAccessRequest(allowRequest);
        
        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
        save();
        init();
        return returnToDraftVersion();
    }
    
    private List<FileMetadata> filesToBeDeleted = new ArrayList();

    public String deleteFile() {

        String fileNames = this.getFileMetadata().getLabel();

        editDataset = this.getFileMetadata().getDataFile().getOwner();
        
        FileMetadata markedForDelete = null;
        
        for (FileMetadata fmd : editDataset.getEditVersion().getFileMetadatas() ){
            
            if (fmd.getDataFile().getId().equals(this.getFile().getId())){
                markedForDelete = fmd;
            }
        }

            if (markedForDelete.getId() != null) {
                // the file already exists as part of this dataset
                // so all we remove is the file from the fileMetadatas (for display)
                // and let the delete be handled in the command (by adding it to the filesToBeDeleted list
                editDataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                filesToBeDeleted.add(markedForDelete);
                
            } else {
                 List<FileMetadata> filesToKeep = new ArrayList();
                 for (FileMetadata fmo: editDataset.getEditVersion().getFileMetadatas()){
                      if (!fmo.getDataFile().getId().equals(this.getFile().getId())){
                          filesToKeep.add(fmo);
                      }
                 }
                 editDataset.getEditVersion().setFileMetadatas(filesToKeep);
            }

    

     
        if (fileNames != null) {
            String successMessage = JH.localize("file.deleted.success");
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
        
        save();
        return returnToDatasetOnly();
        
    }
    
    private int activeTabIndex;

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }
    
    public void tabChanged(TabChangeEvent event) {
        TabView tv = (TabView) event.getComponent();
        this.activeTabIndex = tv.getActiveIndex();
        if (this.activeTabIndex == 1) {
            setFileMetadatasForTab(loadFileMetadataTabList());
        } else {
            setFileMetadatasForTab( new ArrayList());         
        }
    }
    
    
    private List<FileMetadata> loadFileMetadataTabList() {
        List<DataFile> allfiles = allRelatedFiles();
        List<FileMetadata> retList = new ArrayList();
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;
            
            if (versionLoop.isReleased() || versionLoop.isDeaccessioned() || permissionService.on(fileMetadata.getDatasetVersion().getDataset()).has(Permission.ViewUnpublishedDataset)) {
                foundFmd = false;
                for (DataFile df : allfiles) {
                    FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionLoop.getId(), df.getId());
                    if (fmd != null) {
                        fmd.setContributorNames(datasetVersionService.getContributorsNames(versionLoop));
                        FileVersionDifference fvd = new FileVersionDifference(fmd, getPreviousFileMetadata(fmd));
                        fmd.setFileVersionDifference(fvd);
                        retList.add(fmd);
                        foundFmd = true;
                        break;
                    }
                }
                //no File metadata found make dummy one
                if (!foundFmd) {
                    FileMetadata dummy = new FileMetadata();
                    dummy.setDatasetVersion(versionLoop);
                    dummy.setDataFile(null);
                    FileVersionDifference fvd = new FileVersionDifference(dummy, getPreviousFileMetadata(versionLoop));
                    dummy.setFileVersionDifference(fvd);
                    retList.add(dummy);
                }
            }
        }
        return retList;
    }
    
    private FileMetadata getPreviousFileMetadata(DatasetVersion currentversion) {
        List<DataFile> allfiles = allRelatedFiles();
        boolean foundCurrent = false;
        DatasetVersion priorVersion = null;
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (foundCurrent) {
                priorVersion = versionLoop;
                break;
            }
            if (versionLoop.equals(currentversion)) {
                foundCurrent = true;
            }

        }
        if (priorVersion != null && priorVersion.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : priorVersion.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }

        return null;

    }
    
    private FileMetadata getPreviousFileMetadata(FileMetadata fmdIn){
        
        DataFile dfPrevious = datafileService.findPreviousFile(fmdIn.getDataFile());
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (DatasetVersion dvloop: fileMetadata.getDatasetVersion().getDataset().getVersions()){
            if(gotCurrent){
                dvPrevious  = dvloop;
                break;
            }
             if(dvloop.equals(fmdIn.getDatasetVersion())){
                 gotCurrent = true;
             }
        } 
        
        List<DataFile> allfiles = allRelatedFiles();
        
        if (dvPrevious != null && dvPrevious.getFileMetadatasSorted() != null) {
            for (FileMetadata fmdTest : dvPrevious.getFileMetadatasSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }
        
        Long dfId = fmdIn.getDataFile().getId();
        if (dfPrevious != null){
            dfId = dfPrevious.getId();
        }
        Long versionId = null;       
        if (dvPrevious !=null){
            versionId = dvPrevious.getId();
        }
        
        FileMetadata fmd = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId);
        
        return fmd;
    }
    
    public List<FileMetadata> getFileMetadatasForTab() {
        return fileMetadatasForTab;
    }

    public void setFileMetadatasForTab(List<FileMetadata> fileMetadatasForTab) {
        this.fileMetadatasForTab = fileMetadatasForTab;
    }
    
    
    public List<DatasetVersion> getDatasetVersionsForTab() {
        return datasetVersionsForTab;
    }

    public void setDatasetVersionsForTab(List<DatasetVersion> datasetVersionsForTab) {
        this.datasetVersionsForTab = datasetVersionsForTab;
    }
    
    public String save() {
        // Validate
        Set<ConstraintViolation> constraintViolations = this.fileMetadata.getDatasetVersion().validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
             JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.message.validationError"));
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
               

        Command<Dataset> cmd;
        try {
            cmd = new UpdateDatasetCommand(editDataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
            commandEngine.submit(cmd);

        } catch (EJBException ex) {
            
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            
            
            Throwable cause = ex;
            while (cause.getCause()!= null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            return null;
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            return null;
        }


        JsfHelper.addSuccessMessage(JH.localize("file.message.editSuccess"));
        setVersion("DRAFT");
        return "";
    }
    
    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.  
        
        if (!fileDownloadHelper.canDownloadFile(fileMetadata)) {
            return false;
        }
     
        return datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
    }
    
    private String returnToDatasetOnly(){
        
         return "/dataset.xhtml?persistentId=" + editDataset.getGlobalId()  + "&version=DRAFT" + "&faces-redirect=true";   
    }
    
    private String returnToDraftVersion(){ 
        
         return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";    
    }
    
    public FileDownloadServiceBean getFileDownloadService() {
        return fileDownloadService;
    }

    public void setFileDownloadService(FileDownloadServiceBean fileDownloadService) {
        this.fileDownloadService = fileDownloadService;
    }
    
    
    public GuestbookResponseServiceBean getGuestbookResponseService() {
        return guestbookResponseService;
    }

    public void setGuestbookResponseService(GuestbookResponseServiceBean guestbookResponseService) {
        this.guestbookResponseService = guestbookResponseService;
    }
    
    
    public GuestbookResponse getGuestbookResponse() {
        return guestbookResponse;
    }

    public void setGuestbookResponse(GuestbookResponse guestbookResponse) {
        this.guestbookResponse = guestbookResponse;
    }
    
    
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.file.getOwner());
    }
    
    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
    
    
    private List<DataFile> allRelatedFiles() {
        List<DataFile> dataFiles = new ArrayList();
        DataFile dataFileToTest = fileMetadata.getDataFile();
        Long rootDataFileId = dataFileToTest.getRootDataFileId();
        if (rootDataFileId < 0) {
            dataFiles.add(dataFileToTest);
        } else {
            dataFiles.addAll(datafileService.findAllRelatedByRootDatafileId(rootDataFileId));
        }

        return dataFiles;
    }
    
    public boolean isDraftReplacementFile(){
        /*
        This method tests to see if the file has been replaced in a draft version of the dataset
        Since it must must work when you are on prior versions of the dataset 
        it must accrue all replacement files that may have been created
        */
        Dataset datasetToTest = fileMetadata.getDataFile().getOwner();
        DataFile dataFileToTest = fileMetadata.getDataFile();
        
        DatasetVersion currentVersion = datasetToTest.getLatestVersion();
        
        if (!currentVersion.isDraft()){
            return false;
        }
        
        if (datasetToTest.getReleasedVersion() == null){
            return false;
        }
        
        List<DataFile> dataFiles = new ArrayList();
        
        dataFiles.add(dataFileToTest);
        
        while (datafileService.findReplacementFile(dataFileToTest.getId()) != null ){
            dataFiles.add(datafileService.findReplacementFile(dataFileToTest.getId()));
            dataFileToTest = datafileService.findReplacementFile(dataFileToTest.getId());
        }
        
        if(dataFiles.size() <2){
            return false;
        }
        
        int numFiles = dataFiles.size();
        
        DataFile current = dataFiles.get(numFiles - 1 );       
        
        DatasetVersion publishedVersion = datasetToTest.getReleasedVersion();
        
        if( datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(publishedVersion.getId(), current.getId()) == null){
            return true;
        }
        
        return false;
    }
    


    
    /**
     * To help with replace development 
     * @return 
     */
    public boolean isReplacementFile(){
   
        return this.datafileService.isReplacementFile(this.getFile());
    }

    public boolean isPubliclyDownloadable() {
        return FileUtil.isPubliclyDownloadable(fileMetadata);
    }

    public String getPublicDownloadUrl() {
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), fileId);
    }

}
