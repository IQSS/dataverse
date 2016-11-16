
package edu.harvard.iq.dataverse;

// TODO: 
// clean up the imports. -- L.A. 4.2

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.ApiToken;
import edu.harvard.iq.dataverse.authorization.users.User;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.datavariable.VariableServiceBean;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreateGuestbookResponseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeaccessionDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDataFileCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.ingest.IngestRequest;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.metadataimport.ForeignMetadataImportServiceBean;
import edu.harvard.iq.dataverse.search.FacetCategory;
import edu.harvard.iq.dataverse.search.FileView;
import edu.harvard.iq.dataverse.search.SearchFilesServiceBean;
import edu.harvard.iq.dataverse.search.SolrSearchResult;
import edu.harvard.iq.dataverse.search.SortBy;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.JsfHelper;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import edu.harvard.iq.dataverse.util.StringUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.primefaces.context.RequestContext;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.HashSet;
import javax.faces.model.SelectItem;
import java.util.logging.Level;
import javax.faces.event.AjaxBehaviorEvent;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named("EditDatafilesPage")
public class EditDatafilesPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(EditDatafilesPage.class.getCanonicalName());
    private FileView fileView;

    public enum FileEditMode {

        EDIT, UPLOAD, CREATE, SINGLE
    };
    
    @EJB
    DatasetServiceBean datasetService;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    IngestServiceBean ingestService;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;
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
    @Inject PermissionsWrapper permissionsWrapper;

    private final DateFormat displayDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    private Dataset dataset = new Dataset();
    
    private String selectedFileIdsString = null; 
    private FileEditMode mode = FileEditMode.EDIT; 
    private List<Long> selectedFileIdsList = new ArrayList<>(); 
    private List<FileMetadata> fileMetadatas = new ArrayList<>();

    
    private Long ownerId;
    private Long versionId;
    private List<DataFile> newFiles = new ArrayList();
    private DatasetVersion workingVersion;
    private String dropBoxSelection = "";
    private String displayCitation;
    private boolean datasetUpdateRequired = false; 
    private boolean tabularDataTagsUpdated = false; 
    
    private String persistentId;
    
    private String versionString = "";
            
    
    private boolean saveEnabled = false; 

    // Used to store results of permissions checks
    private final Map<String, Boolean> datasetPermissionMap = new HashMap<>(); // { Permission human_name : Boolean }

    private Long maxFileUploadSizeInBytes = null;

    public String getSelectedFileIds() {
        return selectedFileIdsString;
    }
    
    public void setSelectedFileIds(String selectedFileIds) {
        selectedFileIdsString = selectedFileIds;
    }
    
    public FileEditMode getMode() {
        return mode;
    }
    
    public void setMode(FileEditMode mode) {
        this.mode = mode;
    }
    
    public List<FileMetadata> getFileMetadatas() {
        if (fileMetadatas != null) {
            logger.fine("Returning a list of "+fileMetadatas.size()+" file metadatas.");
        } else {
            logger.fine("File metadatas list hasn't been initialized yet.");
        }
        return fileMetadatas;
    }
    
    public void setFileMetadatas(List<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }
    
    /*
        Any settings, such as the upload size limits, should be saved locally - 
        so that the db doesn't get hit repeatedly. (this setting is initialized 
        in the init() method)
    
        This may be "null", signifying unlimited download size.
    */
    
    public Long getMaxFileUploadSizeInBytes(){
        return this.maxFileUploadSizeInBytes;
    }
    
    public boolean isUnlimitedUploadFileSize(){
        
        if (this.maxFileUploadSizeInBytes == null){
            return true;
        }
        return false;
    }
    

    /**
     * Check Dataset related permissions
     * 
     * @param permissionToCheck
     * @return 
     */
    public boolean doesSessionUserHaveDataSetPermission(Permission permissionToCheck){
        if (permissionToCheck == null){
            return false;
        }
               
        String permName = permissionToCheck.getHumanName();
       
        // Has this check already been done? 
        // 
        if (this.datasetPermissionMap.containsKey(permName)){
            // Yes, return previous answer
            return this.datasetPermissionMap.get(permName);
        }
        
        // Check the permission
        //
        boolean hasPermission = this.permissionService.userOn(this.session.getUser(), this.dataset).has(permissionToCheck);

        // Save the permission
        this.datasetPermissionMap.put(permName, hasPermission);
        
        // return true/false
        return hasPermission;
    }
    
    public void reset() {
        // ?
    }

    public String getGlobalId() {
        return persistentId;
    }
        
    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getDisplayCitation() {
        //displayCitation = dataset.getCitation(false, workingVersion);
        return displayCitation;
    }

    public void setDisplayCitation(String displayCitation) {
        this.displayCitation = displayCitation;
    }

    public String getDropBoxSelection() {
        return dropBoxSelection;
    }

    public String getDropBoxKey() {
        // Site-specific DropBox application registration key is configured 
        // via a JVM option under glassfish.
        //if (true)return "some-test-key";  // for debugging

        String configuredDropBoxKey = System.getProperty("dataverse.dropbox.key");
        if (configuredDropBoxKey != null) {
            return configuredDropBoxKey;
        }
        return "";
    }

    public void setDropBoxSelection(String dropBoxSelection) {
        this.dropBoxSelection = dropBoxSelection;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String initCreateMode(String modeToken, DatasetVersion version, List<DataFile> newFilesList, List<FileMetadata> selectedFileMetadatasList) {
        if (modeToken == null) {
            logger.fine("Request to initialize Edit Files page with null token (aborting).");
            return null;
        }
        
        if (!modeToken.equals("CREATE")) {
            logger.fine("Request to initialize Edit Files page with token " + modeToken + " (aborting).");
            return null; 
        }
        
        logger.fine("Initializing Edit Files page in CREATE mode;");
        
        if (version == null) {
            return permissionsWrapper.notFound();
        }
        
        workingVersion = version; 
        dataset = version.getDataset();
        mode = FileEditMode.CREATE;
        newFiles = newFilesList;
        selectedFiles = selectedFileMetadatasList;
        
        logger.fine("done");
        
        saveEnabled = true;
        
        return null; 
    }
    
    public String init() {
        //String nonNullDefaultIfKeyNotFound = "";
        this.maxFileUploadSizeInBytes = systemConfig.getMaxFileUploadSize();
        
        /*
        protocol = settingsService.getValueForKey(SettingsServiceBean.Key.Protocol, nonNullDefaultIfKeyNotFound);
        authority = settingsService.getValueForKey(SettingsServiceBean.Key.Authority, nonNullDefaultIfKeyNotFound);
        separator = settingsService.getValueForKey(SettingsServiceBean.Key.DoiSeparator, nonNullDefaultIfKeyNotFound);
        */
        
        if (dataset.getId() != null){
            // Set Working Version and Dataset by Datasaet Id and Version
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), null);
            dataset = datasetService.find(dataset.getId());
            // Is the Dataset harvested? (because we don't allow editing of harvested 
            // files!)
            if (dataset == null || dataset.isHarvested()) {
                return permissionsWrapper.notFound();
            }
        } else {
            // It could be better to show an error page of some sort, explaining
            // that the dataset id is mandatory... But 404 will do for now.
            return permissionsWrapper.notFound();
        }
        
        workingVersion = dataset.getEditVersion();

        if (workingVersion == null || !workingVersion.isDraft()) {
            // Sorry, we couldn't find/obtain a draft version for this dataset!
            return permissionsWrapper.notFound();
        }
        
        // Check if they have permission to modify this dataset: 
        
        if (!permissionService.on(dataset).has(Permission.EditDataset)) {
            return permissionsWrapper.notAuthorized();
        }
        
        if (mode == FileEditMode.EDIT || mode == FileEditMode.SINGLE) {

            if (selectedFileIdsString != null) {
                String[] ids = selectedFileIdsString.split(",");

                for (String id : ids) {
                    Long test = null;
                    try {
                        test = new Long(id);
                    } catch (NumberFormatException nfe) {
                        // do nothing...
                        test = null;
                    }
                    if (test != null) {
                        selectedFileIdsList.add(test);
                    }
                }
            }

            if (selectedFileIdsList.size() < 1) {
                logger.fine("No numeric file ids supplied to the page, in the edit mode. Redirecting to the 404 page.");
                // If no valid file IDs specified, send them to the 404 page...
                return permissionsWrapper.notFound();
            }

            logger.fine("The page is called with " + selectedFileIdsList.size() + " file ids.");

            populateFileMetadatas();

            // and if no filemetadatas can be found for the specified file ids 
            // and version id - same deal, send them to the "not found" page. 
            // (at least for now; ideally, we probably want to show them a page 
            // with a more informative error message; something alonog the lines 
            // of - could not find the files for the ids specified; or, these 
            // datafiles are not present in the version specified, etc.
            if (fileMetadatas.size() < 1) {
                return permissionsWrapper.notFound();
            }
            
            if (FileEditMode.SINGLE == mode){
                if (fileMetadatas.get(0).getDatasetVersion().getId() != null){
                    versionString = "DRAFT";
                }
            }
            
        }
        
        saveEnabled = true; 

        if (mode == FileEditMode.UPLOAD) {
            JH.addMessage(FacesMessage.SEVERITY_INFO, JH.localize("dataset.message.uploadFiles"));
        }
        return null;
    }
    

    private List<FileMetadata> selectedFiles; // = new ArrayList<>();

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }
    
    private boolean selectAllFiles;

    public boolean isSelectAllFiles() {
        return selectAllFiles;
    }

    public void setSelectAllFiles(boolean selectAllFiles) {
        this.selectAllFiles = selectAllFiles;
    }
    
    public void toggleSelectedFiles(){
        this.selectedFiles = new ArrayList();
        if(this.selectAllFiles){
            if (mode == FileEditMode.CREATE) {
                for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                    this.selectedFiles.add(fmd);
                }
            } else {
                for (FileMetadata fmd : fileMetadatas) {
                    this.selectedFiles.add(fmd);
                }
            }
        }
    }
    
    public String getSelectedFilesIdsString() {        
        String downloadIdString = "";
        for (FileMetadata fmd : this.selectedFiles){
            if (!StringUtil.isEmpty(downloadIdString)) {
                downloadIdString += ",";
            }
            downloadIdString += fmd.getDataFile().getId();
        }
        return downloadIdString;
      
    }

    /*
    public void updateFileCounts(){
        
        setSelectedUnrestrictedFiles(new ArrayList<FileMetadata>());
        setSelectedRestrictedFiles(new ArrayList<FileMetadata>());
        for (FileMetadata fmd : this.selectedFiles){
            if(fmd.isRestricted()){
                getSelectedRestrictedFiles().add(fmd);
            } else {
                getSelectedUnrestrictedFiles().add(fmd);
            }
        }
    }*/
    
    List<FileMetadata> previouslyRestrictedFiles = null;
    
    public boolean isShowAccessPopup() {
        //System.out.print("in get show access popup");
        //System.out.print("previously restricted :" + previouslyRestrictedFiles);
        for (FileMetadata fmd : this.fileMetadatas) {
            //System.out.print("restricted :" + fmd.isRestricted());
            //System.out.print("file id :" + fmd.getDataFile().getId());
            
            if (fmd.isRestricted()) {
            
                if (fmd.getDataFile().getId() == null) {
                    // if this is a brand new file, it's definitely not 
                    // of a previously restricted kind!
                    return true; 
                }
            
                if (previouslyRestrictedFiles != null) {
                    boolean contains = false;
                    for (FileMetadata fmp : previouslyRestrictedFiles) {
                        // OK, we've already checked if it's a brand new file - 
                        // above. So we can safely assume that this datafile
                        // has a valid db id... so it is safe to use the 
                        // equals() method:
                        if (fmp.getDataFile().equals(fmd.getDataFile())) {
                            contains = true;
                            break;
                        }
                    }
                    if (!contains) {
                        return true;
                    }
                }
            }
        }
        //System.out.print("returning false");
        return false;
    }
    
    public void setShowAccessPopup(boolean showAccessPopup) {} // dummy set method
    
    public void restrictFiles(boolean restricted) {
        // since we are restricted files, first set the previously restricted file list, so we can compare for
        // determinin whether to show the access popup
        previouslyRestrictedFiles = new ArrayList();
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.isRestricted()) {
                previouslyRestrictedFiles.add(fmd);
            }
        }

        //System.out.print(previouslyRestrictedFiles.size());
        
        String fileNames = null;
        
        for (FileMetadata fmd : this.getSelectedFiles()) {
            if (restricted && !fmd.isRestricted()) {
                // collect the names of the newly-restrticted files, 
                // to show in the success message:
                if (fileNames == null) {
                    fileNames = fmd.getLabel();
                } else {
                    fileNames = fileNames.concat(", " + fmd.getLabel());
                }
            }
            fmd.setRestricted(restricted);
            if (workingVersion.isDraft() && !fmd.getDataFile().isReleased()) {
                // We do not really need to check that the working version is 
                // a draft here - it must be a draft, if we've gotten this
                // far. But just in case. -- L.A. 4.2.1
                fmd.getDataFile().setRestricted(restricted);
            }
        }
        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }
    } 

    public void restrictFilesDP(boolean restricted) {
        // since we are restricted files, first set the previously restricted file list, so we can compare for
        // determinin whether to show the access popup
        if (previouslyRestrictedFiles == null) {
            previouslyRestrictedFiles = new ArrayList();
            for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
                if (fmd.isRestricted()) {
                    previouslyRestrictedFiles.add(fmd);
                }
            }
        }        
        
        String fileNames = null;       
        for (FileMetadata fmw : workingVersion.getFileMetadatas()) {
            for (FileMetadata fmd : this.getSelectedFiles()) {
                if (restricted && !fmw.isRestricted()) {
                // collect the names of the newly-restrticted files, 
                    // to show in the success message:
                    if (fileNames == null) {
                        fileNames = fmd.getLabel();
                    } else {
                        fileNames = fileNames.concat(", " + fmd.getLabel());
                    }
                }
                if (fmd.getDataFile().equals(fmw.getDataFile())) {
                    fmw.setRestricted(restricted);
                }
            }
        }
        if (fileNames != null) {
            String successMessage = JH.localize("file.restricted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);    
        }
    } 
    
    public int getRestrictedFileCount() {
        int restrictedFileCount = 0;
        for (FileMetadata fmd : workingVersion.getFileMetadatas()) {
            if (fmd.isRestricted()) {
                restrictedFileCount++;
            }
        }

        return restrictedFileCount;
    }

    private List<FileMetadata> filesToBeDeleted = new ArrayList();

    public void deleteFiles() {

        String fileNames = null;
        for (FileMetadata fmd : this.getSelectedFiles()) {
                // collect the names of the newly-restrticted files, 
            // to show in the success message:
            if (fileNames == null) {
                fileNames = fmd.getLabel();
            } else {
                fileNames = fileNames.concat(", " + fmd.getLabel());
            }
        }

        for (FileMetadata markedForDelete : selectedFiles) {
            logger.fine("delete requested on file "+markedForDelete.getLabel());
            logger.fine("file metadata id: "+markedForDelete.getId());
            logger.fine("datafile id: "+markedForDelete.getDataFile().getId());
            logger.fine("page is in edit mode "+mode.name());
            
            
            // TODO: 
            // some duplicated code below... needs to be refactored as follows: 
            // 1. check if the filemetadata has the id; if not - remove 
            // from the appropriate lists using the iterators; 
            // then 2. check if the file has the id; if not - remove it quietly 
            // (as specified below; otherwise - do the quick .remoove() of the 
            // filemetadata from the appropriate lists, and add the file to the
            // "filestobedeleted" list... as it is now, the code for step 1. 
            // is duplicated in 2 places below. I just don't have time to 
            // rewrite it now. -- L.A. Sep. 15, 4.2
            
            if (markedForDelete.getDataFile().getId() != null) {
                logger.fine("this is an existing (saved) file.");
                // the file already exists as part of this dataset
                // so all we remove is the file from the fileMetadatas (from the 
                // file metadatas attached to the editVersion, and from the
                // display list of file metadatas that are being edited)
                // and let the delete be handled in the command (by adding it to the filesToBeDeleted list):
                
                // has this filemetadata been saved already? (or is it a brand new
                // filemetadata, created as part of a brand new version, created when 
                // the user clicked 'delete', that hasn't been saved in the db yet?)
                if (markedForDelete.getId() != null) {
                    logger.fine("this is a filemetadata from an existing draft version");
                    dataset.getEditVersion().getFileMetadatas().remove(markedForDelete);
                    fileMetadatas.remove(markedForDelete);
                    filesToBeDeleted.add(markedForDelete);
                } else {
                    logger.fine("this is a brand-new (unsaved) filemetadata");
                    // ok, this is a brand-new DRAFT version. 
                    // 1. delete the filemetadata from the local display list: 
                    Iterator fmit = fileMetadatas.iterator();
                    while (fmit.hasNext()) {
                        FileMetadata fmd = (FileMetadata) fmit.next();
                        if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                            fmit.remove();
                            break;
                        }
                    }
                    // 2. delete the filemetadata from the version: 
                    fmit = dataset.getEditVersion().getFileMetadatas().iterator();
                    while (fmit.hasNext()) {
                        FileMetadata fmd = (FileMetadata) fmit.next();
                        if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                            fmit.remove();
                            break;
                        }
                    }
                }
            } else {
                logger.fine("this is a brand new file.");
                // the file was just added during this step, so in addition to 
                // removing it from the fileMetadatas list, we also remove it from 
                // the newFiles list and the dataset's files, so it never gets saved.
                
                if (mode != FileEditMode.CREATE) {
                    // If the bean is in the 'CREATE' mode, the page is using
                    // dataset.getEditVersion().getFileMetadatas() directly, 
                    // so there's no need to delete this meta from the local
                    // fileMetadatas list. 
                    
                    // (we can't just do 
                    // fileMetadatas.remove(markedForDelete);
                    // - because the filemetadata doesn't have the id yet!)
                    
                    Iterator fmit = fileMetadatas.iterator();
                    while (fmit.hasNext()) {
                        FileMetadata fmd = (FileMetadata) fmit.next();
                        if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                            fmit.remove();
                            break;
                        }
                    }
                }
                
                Iterator fmit = dataset.getEditVersion().getFileMetadatas().iterator();
                while (fmit.hasNext()) {
                    FileMetadata fmd = (FileMetadata) fmit.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(fmd.getDataFile().getStorageIdentifier())) {
                        fmit.remove();
                        break;
                    }
                }
                
                Iterator<DataFile> dfIt = dataset.getFiles().iterator();
                while (dfIt.hasNext()) {
                    DataFile dfn = dfIt.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                        
                        // Before we remove the file from the list and forget about 
                        // it:
                        // The physical uploaded file is still sitting in the temporary
                        // directory. If it were saved, it would be moved into its 
                        // permanent location. But since the user chose not to save it,
                        // we have to delete the temp file too. 
                        // 
                        // Eventually, we will likely add a dedicated mechanism
                        // for managing temp files, similar to (or part of) the storage 
                        // access framework, that would allow us to handle specialized
                        // configurations - highly sensitive/private data, that 
                        // has to be kept encrypted even in temp files, and such. 
                        // But for now, we just delete the file directly on the 
                        // local filesystem: 

                        try {
                            Files.delete(Paths.get(ingestService.getFilesTempDirectory() + "/" + dfn.getStorageIdentifier()));
                        } catch (IOException ioEx) {
                            // safe to ignore - it's just a temp file. 
                            logger.warning("Failed to delete temporary file " + ingestService.getFilesTempDirectory() + "/" + dfn.getStorageIdentifier());
                        }
                        
                        dfIt.remove();

                    }
                }
                
                

                Iterator<DataFile> nfIt = newFiles.iterator();
                while (nfIt.hasNext()) {
                    DataFile dfn = nfIt.next();
                    if (markedForDelete.getDataFile().getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                        nfIt.remove();
                    }
                }                
                
            }
        }

     
        if (fileNames != null) {
            String successMessage = JH.localize("file.deleted.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileNames);
            JsfHelper.addFlashMessage(successMessage);
        }
    }

    public String saveWithTermsOfUse() {
        logger.fine("saving terms of use, and the dataset version");
        datasetUpdateRequired = true; 
        return save();
    }
    
    public String save() {
        
        /*
        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
             //JsfHelper.addFlashMessage(JH.localize("dataset.message.validationError"));
            logger.fine("Constraint violation detected on SAVE: "+constraintViolations.toString());
             JH.addMessage(FacesMessage.SEVERITY_ERROR, JH.localize("dataset.message.validationError"));
             
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "See below for details."));
            return "";
        }
        }*/
        
        // Once all the filemetadatas pass the validation, we'll only allow the user 
        // to try to save once; (this it to prevent them from creating multiple
        // DRAFT versions, if the page gets stuck in that state where it 
        // successfully creates a new version, but can't complete the remaining
        // tasks. -- L.A. 4.2
        
        if (!saveEnabled) {
            return "";
        }
        
        // Save the NEW files permanently: 
        ingestService.addFiles(workingVersion, newFiles);
        //boolean newDraftVersion = false; 
         
        if (workingVersion.getId() == null  || datasetUpdateRequired) {
            logger.info("issuing the dataset update command");
            // We are creating a new draft version; 
            // (OR, a full update of the dataset has been explicitly requested, 
            // because of the nature of the updates the user has made).
            // We'll use an Update command for this: 
            
            //newDraftVersion = true;
            
            if (datasetUpdateRequired) {
                for (int i = 0; i < workingVersion.getFileMetadatas().size(); i++) {
                    for (FileMetadata fileMetadata : fileMetadatas) {
                        if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                            if (fileMetadata.getDataFile().getStorageIdentifier().equals(workingVersion.getFileMetadatas().get(i).getDataFile().getStorageIdentifier())) {
                                workingVersion.getFileMetadatas().set(i, fileMetadata);
                            }
                        }
                    }
                }
                
                // Tabular data tags are assigned to datafiles, not to  
                // version-specfic filemetadatas!
                // So if tabular tags have been modified, we also need to 
                // refresh the list of datafiles, as found in dataset.getFiles(),
                // similarly to what we've just done, above, for the filemetadatas.
                // Otherwise, when we call UpdateDatasetCommand, it's not going 
                // to update the tags in the database (issue #2798). 
                
                if (tabularDataTagsUpdated) {
                    for (int i = 0; i < dataset.getFiles().size(); i++) {
                        for (FileMetadata fileMetadata : fileMetadatas) {
                            if (fileMetadata.getDataFile().getStorageIdentifier() != null) {
                                if (fileMetadata.getDataFile().getStorageIdentifier().equals(dataset.getFiles().get(i).getStorageIdentifier())) {
                                    dataset.getFiles().set(i, fileMetadata.getDataFile());
                                }
                            }
                        }
                    }
                    tabularDataTagsUpdated = false;
                }
            }
            
            
            Command<Dataset> cmd;
            try {
                cmd = new UpdateDatasetCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
                ((UpdateDatasetCommand) cmd).setValidateLenient(true);
                dataset = commandEngine.submit(cmd);
            
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
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", error.toString());
                populateDatasetUpdateFailureMessage();
                return null;
            } catch (CommandException ex) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
                logger.severe(ex.getMessage());
                populateDatasetUpdateFailureMessage();
                return null;
            }
            datasetUpdateRequired = false;
            saveEnabled = false; 
        } else {
            // This is an existing Draft version (and nobody has explicitly 
            // requested that the entire dataset is updated). So we'll try to update 
            // only the filemetadatas and/or files affected, and not the 
            // entire version. 
            // TODO: in 4.3, create SaveDataFileCommand!
            // -- L.A. Sep. 21 2015, 4.2
            Timestamp updateTime = new Timestamp(new Date().getTime());
        
            workingVersion.setLastUpdateTime(updateTime);
            dataset.setModificationTime(updateTime);
        
            StringBuilder saveError = new StringBuilder();
        
            for (FileMetadata fileMetadata : fileMetadatas) {

                if (fileMetadata.getDataFile().getCreateDate() == null) {
                    fileMetadata.getDataFile().setCreateDate(updateTime);
                    fileMetadata.getDataFile().setCreator((AuthenticatedUser) session.getUser());
                }
                fileMetadata.getDataFile().setModificationTime(updateTime);
                try {
                    //DataFile savedDatafile = datafileService.save(fileMetadata.getDataFile());
                    fileMetadata = datafileService.mergeFileMetadata(fileMetadata);
                } catch (EJBException ex) {
                    saveError.append(ex).append(" ");
                    saveError.append(ex.getMessage()).append(" ");
                    Throwable cause = ex;
                    while (cause.getCause() != null) {
                        cause = cause.getCause();
                        saveError.append(cause).append(" ");
                        saveError.append(cause.getMessage()).append(" ");
                    }
                }
            }

            // Remove / delete any files that were removed
            for (FileMetadata fmd : filesToBeDeleted) {
                //  check if this file is being used as the default thumbnail
                if (fmd.getDataFile().equals(dataset.getThumbnailFile())) {
                    logger.fine("deleting the dataset thumbnail designation");
                    dataset.setThumbnailFile(null);
                }

                if (!fmd.getDataFile().isReleased()) {
                    // if file is draft (ie. new to this version, delete; otherwise just remove filemetadata object)
                    try {
                        commandEngine.submit(new DeleteDataFileCommand(fmd.getDataFile(), dvRequestService.getDataverseRequest()));
                        dataset.getFiles().remove(fmd.getDataFile());
                        workingVersion.getFileMetadatas().remove(fmd);
                        // added this check to handle issue where you could not deleter a file that shared a category with a new file
                        // the relationship does not seem to cascade, yet somehow it was trying to merge the filemetadata
                        // todo: clean this up some when we clean the create / update dataset methods
                        for (DataFileCategory cat : dataset.getCategories()) {
                            cat.getFileMetadatas().remove(fmd);
                        }
                    } catch (CommandException cmde) {
                        // TODO: 
                        // add diagnostics reporting for individual data files that 
                        // we failed to delete.
                    }
                } else {
                    datafileService.removeFileMetadata(fmd);
                    fmd.getDataFile().getFileMetadatas().remove(fmd);
                    workingVersion.getFileMetadatas().remove(fmd);
                }
            }
            
            String saveErrorString = saveError.toString();
            if (saveErrorString != null && !saveErrorString.equals("")) {
                logger.log(Level.INFO, "Couldn''t save dataset: {0}", saveErrorString);
                populateDatasetUpdateFailureMessage();
                return null;
            }

            
            // Refresh the instance of the dataset object:
            // (being in the UPLOAD mode more or less guarantees that the 
            // dataset object already exists in the database, but we'll check 
            // the id for null, just in case)
            if (mode == FileEditMode.UPLOAD) {
                if (dataset.getId() != null) {
                    dataset = datasetService.find(dataset.getId());
                }
            }
        }
           
        newFiles.clear();
                
        workingVersion = dataset.getEditVersion();
        logger.fine("working version id: "+workingVersion.getId());
        
        JsfHelper.addSuccessMessage(JH.localize("dataset.message.filesSuccess"));
        

        // Call Ingest Service one more time, to 
        // queue the data ingest jobs for asynchronous execution:
        if (mode == FileEditMode.UPLOAD) {
            ingestService.startIngestJobs(dataset, (AuthenticatedUser) session.getUser());
        }

        if (mode == FileEditMode.SINGLE && fileMetadatas.size() > 0) {
            // If this was a "single file edit", i.e. an edit request sent from 
            // the individual File Landing page, we want to redirect back to 
            // the landing page. BUT ONLY if the file still exists - i.e., if 
            // the user hasn't just deleted it!
            versionString = "DRAFT";
            return returnToFileLandingPage();
        }
        
        //if (newDraftVersion) {
        //    return returnToDraftVersionById();
        //}
        
        return returnToDraftVersion();
    }
    
    private void populateDatasetUpdateFailureMessage(){
            
        JH.addMessage(FacesMessage.SEVERITY_FATAL, JH.localize("dataset.message.filesFailure"));
    }
    
    
    
    private String returnToDraftVersion(){      
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&version=DRAFT&faces-redirect=true";    
    }
    
    private String returnToDatasetOnly(){
         dataset = datasetService.find(dataset.getId());
         return "/dataset.xhtml?persistentId=" + dataset.getGlobalId()  +  "&faces-redirect=true";       
    }
    
    private String returnToFileLandingPage() {
        
        Long fileId = fileMetadatas.get(0).getDataFile().getId();       
        if (versionString.equals("DRAFT")){
            return  "/file.xhtml?fileId=" + fileId  +  "&version=DRAFT&faces-redirect=true";
        }
        return  "/file.xhtml?fileId=" + fileId  +  "&faces-redirect=true";

    }
    
    public String cancel() {
        if (mode == FileEditMode.SINGLE) {
            return returnToFileLandingPage();
        }
        if (workingVersion.getId() != null) {
            return returnToDraftVersion();
        }
        return returnToDatasetOnly();
    }

    public boolean isDuplicate(FileMetadata fileMetadata) {
        String thisMd5 = fileMetadata.getDataFile().getChecksumValue();
        if (thisMd5 == null) {
            return false;
        }

        Map<String, Integer> MD5Map = new HashMap<String, Integer>();

        // TODO: 
        // think of a way to do this that doesn't involve populating this 
        // map for every file on the page? 
        // man not be that much of a problem, if we paginate and never display 
        // more than a certain number of files... Still, needs to be revisited
        // before the final 4.0. 
        // -- L.A. 4.0

        // make a "defensive copy" to avoid java.util.ConcurrentModificationException from being thrown
        // when uploading 100+ files
        List<FileMetadata> wvCopy = new ArrayList<>(workingVersion.getFileMetadatas());
        Iterator<FileMetadata> fmIt = wvCopy.iterator();

        while (fmIt.hasNext()) {
            FileMetadata fm = fmIt.next();
            String md5 = fm.getDataFile().getChecksumValue();
            if (md5 != null) {
                if (MD5Map.get(md5) != null) {
                    MD5Map.put(md5, MD5Map.get(md5).intValue() + 1);
                } else {
                    MD5Map.put(md5, 1);
                }
            }
        }

        return MD5Map.get(thisMd5) != null && MD5Map.get(thisMd5).intValue() > 1;
    }

    private HttpClient getClient() {
        // TODO: 
        // cache the http client? -- L.A. 4.0 alpha
        return new HttpClient();
    }

    public boolean showFileUploadFileComponent(){
        
        if (mode == FileEditMode.UPLOAD || mode == FileEditMode.CREATE) {
           return true;
        }
        return false;
    }
    
    

    /**
     * Download a file from drop box
     * 
     * @param fileLink
     * @return 
     */
    private InputStream getDropBoxInputStream(String fileLink, GetMethod dropBoxMethod){
        
        if (fileLink == null){
            return null;
        }
        
        // -----------------------------------------------------------
        // Make http call, download the file: 
        // -----------------------------------------------------------
        int status = 0;
        //InputStream dropBoxStream = null;

        try {
            status = getClient().executeMethod(dropBoxMethod);
            if (status == 200) {
                return dropBoxMethod.getResponseBodyAsStream();
            }
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to access DropBox url: {0}!", fileLink);
            return null;
        } 

        logger.log(Level.WARNING, "Failed to get DropBox InputStream for file: {0}", fileLink);
        return null;
    } // end: getDropBoxInputStream
                  
    
    /**
     * Using information from the DropBox choose, ingest the chosen files
     *  https://www.dropbox.com/developers/dropins/chooser/js
     * 
     * @param event
     */
    public void handleDropBoxUpload(ActionEvent event) {
        
        logger.fine("handleDropBoxUpload");
        // -----------------------------------------------------------
        // Read JSON object from the output of the DropBox Chooser: 
        // -----------------------------------------------------------
        JsonReader dbJsonReader = Json.createReader(new StringReader(dropBoxSelection));
        JsonArray dbArray = dbJsonReader.readArray();
        dbJsonReader.close();

        // -----------------------------------------------------------
        // Iterate through the Dropbox file information (JSON)
        // -----------------------------------------------------------
        DataFile dFile = null;
        GetMethod dropBoxMethod = null;
        for (int i = 0; i < dbArray.size(); i++) {
            JsonObject dbObject = dbArray.getJsonObject(i);

            // -----------------------------------------------------------
            // Parse information for a single file
            // -----------------------------------------------------------
            String fileLink = dbObject.getString("link");
            String fileName = dbObject.getString("name");
            int fileSize = dbObject.getInt("bytes");

            logger.fine("DropBox url: " + fileLink + ", filename: " + fileName + ", size: " + fileSize);


            /* ----------------------------
                Check file size
                - Max size NOT specified in db: default is unlimited
                - Max size specified in db: check too make sure file is within limits
            // ---------------------------- */
            if ((!this.isUnlimitedUploadFileSize())&&(fileSize > this.getMaxFileUploadSizeInBytes())){
                String warningMessage = "Dropbox file \"" + fileName + "\" exceeded the limit of " + fileSize + " bytes and was not uploaded.";
                //msg(warningMessage);
                FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
                continue; // skip to next file, and add error mesage
            }

            
            dFile = null;
            dropBoxMethod = new GetMethod(fileLink);

            // -----------------------------------------------------------
            // Download the file
            // -----------------------------------------------------------
            InputStream dropBoxStream = this.getDropBoxInputStream(fileLink, dropBoxMethod);
            if (dropBoxStream==null){
                logger.severe("Could not retrieve dropgox input stream for: " + fileLink);
                continue;  // Error skip this file
            }
            
            List<DataFile> datafiles = new ArrayList<DataFile>(); 
            
            // -----------------------------------------------------------
            // Send it through the ingest service
            // -----------------------------------------------------------
            try {
                // Note: A single file may be unzipped into multiple files
                datafiles = ingestService.createDataFiles(workingVersion, dropBoxStream, fileName, "application/octet-stream");     
                
            } catch (IOException ex) {
                this.logger.log(Level.SEVERE, "Error during ingest of DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            }finally {
                // -----------------------------------------------------------
                // release connection for dropBoxMethod
                // -----------------------------------------------------------
                
                if (dropBoxMethod != null) {
                    dropBoxMethod.releaseConnection();
                }
                
                // -----------------------------------------------------------
                // close the  dropBoxStream
                // -----------------------------------------------------------
                try {
                    dropBoxStream.close();
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Failed to close the dropBoxStream for file: {0}", fileLink);
                }
            }
            
            if (datafiles == null){
                this.logger.log(Level.SEVERE, "Failed to create DataFile for DropBox file {0} from link {1}", new Object[]{fileName, fileLink});
                continue;
            }else{    
                // -----------------------------------------------------------
                // Check if there are duplicate files or ingest warnings
                // -----------------------------------------------------------
                String warningMessage = processUploadedFileList(datafiles);
                logger.fine("Warning message during upload: " + warningMessage);
                if (warningMessage != null){
                     logger.fine("trying to send faces message to " + event.getComponent().getClientId());
                     FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
                }
            }
        }
    }

    
    
    public void handleFileUpload(FileUploadEvent event) {
        UploadedFile uFile = event.getFile();
        List<DataFile> dFileList = null;

        
        try {
            // Note: A single file may be unzipped into multiple files
            dFileList = ingestService.createDataFiles(workingVersion, uFile.getInputstream(), uFile.getFileName(), uFile.getContentType());
        } catch (IOException ioex) {
            logger.warning("Failed to process and/or save the file " + uFile.getFileName() + "; " + ioex.getMessage());
            return;
        }

        // -----------------------------------------------------------
        // Check if there are duplicate files or ingest warnings
        // -----------------------------------------------------------
        String warningMessage = processUploadedFileList(dFileList);
        if (warningMessage != null){
            logger.fine("trying to send faces message to " + event.getComponent().getClientId());
            FacesContext.getCurrentInstance().addMessage(event.getComponent().getClientId(), new FacesMessage(FacesMessage.SEVERITY_ERROR, "upload failure", warningMessage));
        }
    }

    /**
     *  After uploading via the site or Dropbox, 
     *  check the list of DataFile objects
     * @param dFileList 
     */
    private String processUploadedFileList(List<DataFile> dFileList){

        DataFile dataFile = null;
        String duplicateFileNames = null;
        boolean multipleFiles = dFileList.size() > 1;
        boolean multipleDupes = false;
        String warningMessage = null;

        // -----------------------------------------------------------
        // Iterate through list of DataFile objects
        // -----------------------------------------------------------
        if (dFileList != null) {
            for (int i = 0; i < dFileList.size(); i++) {
                dataFile = dFileList.get(i);

                //logger.info("dataFile: " + dataFile);
                
                // -----------------------------------------------------------
                // Check for ingest warnings
                // -----------------------------------------------------------
                if (dataFile.isIngestProblem()) {
                    if (dataFile.getIngestReportMessage() != null) {
                        if (warningMessage == null) {
                            warningMessage = dataFile.getIngestReportMessage();
                        } else {
                            warningMessage = warningMessage.concat("; " + dataFile.getIngestReportMessage());
                        }
                    }
                    dataFile.setIngestDone();
                }

                // -----------------------------------------------------------
                // Check for duplicates -- e.g. file is already in the dataset
                // -----------------------------------------------------------
                if (!isDuplicate(dataFile.getFileMetadata())) {
                    newFiles.add(dataFile);        // looks good
                    fileMetadatas.add(dataFile.getFileMetadata());
                } else {
                    if (duplicateFileNames == null) {
                        duplicateFileNames = dataFile.getFileMetadata().getLabel();
                    } else {
                        duplicateFileNames = duplicateFileNames.concat(", " + dataFile.getFileMetadata().getLabel());
                        multipleDupes = true;
                    }

                    // remove the file from the dataset (since createDataFiles has already linked
                    // it to the dataset!
                    // first, through the filemetadata list, then through tht datafiles list:
                    Iterator<FileMetadata> fmIt = dataset.getEditVersion().getFileMetadatas().iterator();
                    while (fmIt.hasNext()) {
                        FileMetadata fm = fmIt.next();
                        if (fm.getId() == null && dataFile.getStorageIdentifier().equals(fm.getDataFile().getStorageIdentifier())) {
                            fmIt.remove();
                            break;
                        }
                    }

                    Iterator<DataFile> dfIt = dataset.getFiles().iterator();
                    while (dfIt.hasNext()) {
                        DataFile dfn = dfIt.next();
                        if (dfn.getId() == null && dataFile.getStorageIdentifier().equals(dfn.getStorageIdentifier())) {
                            dfIt.remove();
                            break;
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------
        // Formate error message for duplicate files
        // -----------------------------------------------------------
        if (duplicateFileNames != null) {
            String duplicateFilesErrorMessage = null;
            if (multipleDupes) {
                duplicateFilesErrorMessage = "The following files already exist in the dataset: " + duplicateFileNames;
            } else {
                if (multipleFiles) {
                    duplicateFilesErrorMessage = "The following file already exists in the dataset: " + duplicateFileNames;
                } else {
                    duplicateFilesErrorMessage = "This file already exists in this dataset. Please upload another file.";
                }
            }
            if (warningMessage == null) {
                warningMessage = duplicateFilesErrorMessage;
            } else {
                warningMessage = warningMessage.concat("; " + duplicateFilesErrorMessage);
            }
        }
        
        if (warningMessage != null) {
            logger.severe(warningMessage);
            return warningMessage;     // there's an issue return error message
        } else {
            return null;    // looks good, return null
        }
    }
    

    public boolean isLocked() {
        if (dataset != null) {
            logger.fine("checking lock status of dataset " + dataset.getId());
            if (dataset.isLocked()) {
                // refresh the dataset and version, if the current working
                // version of the dataset is locked:
            }
            Dataset lookedupDataset = datasetService.find(dataset.getId());
            DatasetLock datasetLock = null;
            if (lookedupDataset != null) {
                datasetLock = lookedupDataset.getDatasetLock();
                if (datasetLock != null) {
                    logger.fine("locked!");
                    return true;
                }
            }
        }
        return false;
    }

    
    // Methods for edit functions that are performed on one file at a time, 
    // in popups that block the rest of the page:
    
    private FileMetadata fileMetadataSelected = null;

    public void  setFileMetadataSelected(FileMetadata fm){
       setFileMetadataSelected(fm, null); 
    }
    
    public void setFileMetadataSelected(FileMetadata fm, String guestbook) {

        fileMetadataSelected = fm;
        logger.fine("set the file for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
    }

    public FileMetadata getFileMetadataSelected() {
        if (fileMetadataSelected != null) {
            logger.fine("returning file metadata for the advanced options popup (" + fileMetadataSelected.getLabel() + ")");
        } else {
            logger.fine("file metadata for the advanced options popup is null.");
        }
        return fileMetadataSelected;
    }

    public void clearFileMetadataSelected() {
        fileMetadataSelected = null;
    }
    
    public boolean isDesignatedDatasetThumbnail (FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            if (fileMetadata.getDataFile() != null) {
                if (fileMetadata.getDataFile().getId() != null) {
                    //if (fileMetadata.getDataFile().getOwner() != null) {
                        if (fileMetadata.getDataFile().equals(dataset.getThumbnailFile())) {
                            return true;
                        }
                    //}
                }
            }
        }
        return false;
    }
    
    /* 
     * Items for the "Designated this image as the Dataset thumbnail: 
     */
    
    private FileMetadata fileMetadataSelectedForThumbnailPopup = null; 

    public void  setFileMetadataSelectedForThumbnailPopup(FileMetadata fm){
       fileMetadataSelectedForThumbnailPopup = fm; 
       alreadyDesignatedAsDatasetThumbnail = getUseAsDatasetThumbnail();

    }
    
    public FileMetadata getFileMetadataSelectedForThumbnailPopup() {
        return fileMetadataSelectedForThumbnailPopup;
    }
    
    public void clearFileMetadataSelectedForThumbnailPopup() {
        fileMetadataSelectedForThumbnailPopup = null;
    }
    
    private boolean alreadyDesignatedAsDatasetThumbnail = false; 
    
    public boolean getUseAsDatasetThumbnail() {

        return isDesignatedDatasetThumbnail(fileMetadataSelectedForThumbnailPopup);
    }

    public void setUseAsDatasetThumbnail(boolean useAsThumbnail) {
        if (fileMetadataSelectedForThumbnailPopup != null) {
            if (fileMetadataSelectedForThumbnailPopup.getDataFile() != null) {
                if (useAsThumbnail) {
                    dataset.setThumbnailFile(fileMetadataSelectedForThumbnailPopup.getDataFile());
                } else if (getUseAsDatasetThumbnail()) {
                    dataset.setThumbnailFile(null);
                }
            }
        }
    }

    public void saveAsDesignatedThumbnail() {
        logger.fine("saving as the designated thumbnail");
        // We don't need to do anything specific to save this setting, because
        // the setUseAsDatasetThumbnail() method, above, has already updated the
        // file object appropriately. 
        // However, once the "save" button is pressed, we want to show a success message, if this is 
        // a new image has been designated as such:
        if (getUseAsDatasetThumbnail() && !alreadyDesignatedAsDatasetThumbnail) {
            String successMessage = JH.localize("file.assignedDataverseImage.success");
            logger.fine(successMessage);
            successMessage = successMessage.replace("{0}", fileMetadataSelectedForThumbnailPopup.getLabel());
            JsfHelper.addFlashMessage(successMessage);

            datasetUpdateRequired = true;
        }

        // And reset the selected fileMetadata:
        fileMetadataSelectedForThumbnailPopup = null;
    }
    

    /* 
     * Items for the "Tags (Categories)" popup.
     *
     */
    private FileMetadata fileMetadataSelectedForTagsPopup = null; 

    public void  setFileMetadataSelectedForTagsPopup(FileMetadata fm){
       fileMetadataSelectedForTagsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForTagsPopup() {
        return fileMetadataSelectedForTagsPopup;
    }
    
    public void clearFileMetadataSelectedForTagsPopup() {
        fileMetadataSelectedForTagsPopup = null;
    }
    
    /*
     * 1. Tabular File Tags: 
     */
    
    private List<String> tabFileTags = null;

    public List<String> getTabFileTags() {
        if (tabFileTags == null) {
            tabFileTags = DataFileTag.listTags();
        }
        return tabFileTags;
    }

    public void setTabFileTags(List<String> tabFileTags) {
        this.tabFileTags = tabFileTags;
    }
    
    private String[] selectedTabFileTags = {};

    public String[] getSelectedTabFileTags() {
        return selectedTabFileTags;
    }

    public void setSelectedTabFileTags(String[] selectedTabFileTags) {
        this.selectedTabFileTags = selectedTabFileTags;
    }

    private String[] selectedTags = {};
    
    public void refreshTagsPopUp(FileMetadata fm){
        setFileMetadataSelectedForTagsPopup(fm);
        refreshCategoriesByName();
        refreshTabFileTagsByName();
    }
    
    private List<String> tabFileTagsByName;

    public List<String> getTabFileTagsByName() {
        return tabFileTagsByName;
    }

    public void setTabFileTagsByName(List<String> tabFileTagsByName) {
        this.tabFileTagsByName = tabFileTagsByName;
    }
    
    private void refreshTabFileTagsByName() {
        tabFileTagsByName = new ArrayList<>();
        if (fileMetadataSelectedForTagsPopup.getDataFile().getTags() != null) {
            for (int i = 0; i < fileMetadataSelectedForTagsPopup.getDataFile().getTags().size(); i++) {
                tabFileTagsByName.add(fileMetadataSelectedForTagsPopup.getDataFile().getTags().get(i).getTypeLabel());
            }
        }
        refreshSelectedTabFileTags();
    }

    private void refreshSelectedTabFileTags() {
        selectedTabFileTags = null;
        selectedTabFileTags = new String[0];
        if (tabFileTagsByName.size() > 0) {
            selectedTabFileTags = new String[tabFileTagsByName.size()];
            for (int i = 0; i < tabFileTagsByName.size(); i++) {
                selectedTabFileTags[i] = tabFileTagsByName.get(i);
            }
        }
        Arrays.sort(selectedTabFileTags);
    }
    
    private void refreshCategoriesByName(){
        categoriesByName= new ArrayList<>();
        for (String category: dataset.getCategoriesByName() ){
            categoriesByName.add(category);
        }
        refreshSelectedTags();
    }
    
    
    private List<String> categoriesByName;

    public List<String> getCategoriesByName() {
        return categoriesByName;
    }

    public void setCategoriesByName(List<String> categoriesByName) {
        this.categoriesByName = categoriesByName;
    }
    
    private void refreshSelectedTags() {
        selectedTags = null;
        selectedTags = new String[0];
        List selectedCategoriesByName = new ArrayList<>();

        if (fileMetadataSelectedForTagsPopup.getCategories() != null) {
            for (int i = 0; i < fileMetadataSelectedForTagsPopup.getCategories().size(); i++) {
                if (!selectedCategoriesByName.contains(fileMetadataSelectedForTagsPopup.getCategories().get(i).getName())) {
                    selectedCategoriesByName.add(fileMetadataSelectedForTagsPopup.getCategories().get(i).getName());
                }
            }
        }

        if (selectedCategoriesByName.size() > 0) {
            selectedTags = new String[selectedCategoriesByName.size()];
            for (int i = 0; i < selectedCategoriesByName.size(); i++) {
                selectedTags[i] = (String) selectedCategoriesByName.get(i);
            }
        }
        Arrays.sort(selectedTags);
    }

    public String[] getSelectedTags() {
        return selectedTags;
    }

    public void setSelectedTags(String[] selectedTags) {
        this.selectedTags = selectedTags;
    }
    


    /*
     * "File Tags" (aka "File Categories"): 
    */
    
    private String newCategoryName = null;

    public String getNewCategoryName() {
        return newCategoryName;
    }

    public void setNewCategoryName(String newCategoryName) {
        this.newCategoryName = newCategoryName;
    }
       
    public String saveNewCategory() {
       
        if (newCategoryName != null && !newCategoryName.isEmpty()) {
            categoriesByName.add(newCategoryName);
        }
        //Now increase size of selectedTags and add new category
        String[] temp = new String[selectedTags.length + 1];
        System.arraycopy(selectedTags, 0, temp, 0, selectedTags.length);
        selectedTags = temp;
        selectedTags[selectedTags.length - 1] = newCategoryName;
        //Blank out added category
        newCategoryName = "";
        return "";
    }

    /* This method handles saving both "tabular file tags" and 
     * "file categories" (which are also considered "tags" in 4.0)
    */
    public void saveFileTagsAndCategories() {
        // 1. File categories:
        /*
        In order to get the cancel button to work we had to separate the selected tags 
        from the file metadata and re-add them on save
        
        */
        
        fileMetadataSelectedForTagsPopup.setCategories(new ArrayList());
        if (newCategoryName != null) {
            fileMetadataSelectedForTagsPopup.addCategoryByName(newCategoryName);
        }
        // 2. Tabular DataFile Tags: 
        if (selectedTags != null) {
            for (int i = 0; i < selectedTags.length; i++) {
                fileMetadataSelectedForTagsPopup.addCategoryByName(selectedTags[i]);
            }
        }


        logger.fine("New category name: " + newCategoryName);

        if (fileMetadataSelectedForTagsPopup != null && newCategoryName != null) {
            logger.fine("Adding new category, for file " + fileMetadataSelectedForTagsPopup.getLabel());
            fileMetadataSelectedForTagsPopup.addCategoryByName(newCategoryName);
        } else {
            logger.fine("No FileMetadata selected, or no category specified!");
        }
        newCategoryName = null;
        
        // 2. Tabular DataFile Tags: 

        if (tabularDataTagsUpdated && selectedTabFileTags != null) {
            if (fileMetadataSelectedForTagsPopup != null && fileMetadataSelectedForTagsPopup.getDataFile() != null) {
                fileMetadataSelectedForTagsPopup.getDataFile().setTags(null);
                for (int i = 0; i < selectedTabFileTags.length; i++) {
                    
                    DataFileTag tag = new DataFileTag();
                    try {
                        tag.setTypeByLabel(selectedTabFileTags[i]);
                        tag.setDataFile(fileMetadataSelectedForTagsPopup.getDataFile());
                        fileMetadataSelectedForTagsPopup.getDataFile().addTag(tag);
                        
                    } catch (IllegalArgumentException iax) {
                        // ignore 
                    }
                }
                
                datasetUpdateRequired = true;
                
                // success message: 
                String successMessage = JH.localize("file.assignedTabFileTags.success");
                logger.fine(successMessage);
                successMessage = successMessage.replace("{0}", fileMetadataSelectedForTagsPopup.getLabel());
                JsfHelper.addFlashMessage(successMessage);
            }
            // reset:
            selectedTags = null;
        }
        
        fileMetadataSelectedForTagsPopup = null;

    }
    
    public void handleSelection(final AjaxBehaviorEvent event) {
        tabularDataTagsUpdated = true;
        if (selectedTags != null) {
            selectedTags = selectedTags.clone();
        }
    }
    
    
    /* 
     * Items for the "Advanced (Ingest) Options" popup. 
     * 
     */
    private FileMetadata fileMetadataSelectedForIngestOptionsPopup = null; 

    public void  setFileMetadataSelectedForIngestOptionsPopup(FileMetadata fm){
       fileMetadataSelectedForIngestOptionsPopup = fm; 
    }
    
    public FileMetadata getFileMetadataSelectedForIngestOptionsPopup() {
        return fileMetadataSelectedForIngestOptionsPopup;
    }
    
    public void clearFileMetadataSelectedForIngestOptionsPopup() {
        fileMetadataSelectedForIngestOptionsPopup = null;
    }
    
    private String ingestLanguageEncoding = null;

    public String getIngestLanguageEncoding() {
        if (ingestLanguageEncoding == null) {
            return "UTF8 (default)";
        }
        return ingestLanguageEncoding;
    }

    public void setIngestLanguageEncoding(String ingestLanguageEncoding) {
        this.ingestLanguageEncoding = ingestLanguageEncoding;
    }

    public void setIngestEncoding(String ingestEncoding) {
        ingestLanguageEncoding = ingestEncoding;
    }

    private String savedLabelsTempFile = null;

    public void handleLabelsFileUpload(FileUploadEvent event) {
        logger.fine("entering handleUpload method.");
        UploadedFile file = event.getFile();

        if (file != null) {

            InputStream uploadStream = null;
            try {
                uploadStream = file.getInputstream();
            } catch (IOException ioex) {
                logger.info("the file " + file.getFileName() + " failed to upload!");
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_WARN, "upload failure", "the file " + file.getFileName() + " failed to upload!");
                FacesContext.getCurrentInstance().addMessage(null, message);
                return;
            }

            savedLabelsTempFile = saveTempFile(uploadStream);

            logger.fine(file.getFileName() + " is successfully uploaded.");
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

        // process file (i.e., just save it in a temp location; for now):
    }

    private String saveTempFile(InputStream input) {
        if (input == null) {
            return null;
        }
        byte[] buffer = new byte[8192];
        int bytesRead = 0;
        File labelsFile = null;
        FileOutputStream output = null;
        try {
            labelsFile = File.createTempFile("tempIngestLabels.", ".txt");
            output = new FileOutputStream(labelsFile);
            while ((bytesRead = input.read(buffer)) > -1) {
                output.write(buffer, 0, bytesRead);
            }
        } catch (IOException ioex) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
            return null;
        }

        if (labelsFile != null) {
            return labelsFile.getAbsolutePath();
        }
        return null;
    }

    public void saveAdvancedOptions() {
        
        // Language encoding for SPSS SAV (and, possibly, other tabular ingests:) 
        if (ingestLanguageEncoding != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);

                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setTextEncoding(ingestLanguageEncoding);
            }
        }
        ingestLanguageEncoding = null;

        // Extra labels for SPSS POR (and, possibly, other tabular ingests:)
        // (we are adding this parameter to the IngestRequest now, instead of back
        // when it was uploaded. This is because we want the user to be able to 
        // hit cancel and bail out, until they actually click 'save' in the 
        // "advanced options" popup) -- L.A. 4.0 beta 11
        if (savedLabelsTempFile != null) {
            if (fileMetadataSelectedForIngestOptionsPopup != null && fileMetadataSelectedForIngestOptionsPopup.getDataFile() != null) {
                if (fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest() == null) {
                    IngestRequest ingestRequest = new IngestRequest();
                    ingestRequest.setDataFile(fileMetadataSelectedForIngestOptionsPopup.getDataFile());
                    fileMetadataSelectedForIngestOptionsPopup.getDataFile().setIngestRequest(ingestRequest);
                }
                fileMetadataSelectedForIngestOptionsPopup.getDataFile().getIngestRequest().setLabelsFile(savedLabelsTempFile);
            }
        }
        savedLabelsTempFile = null;

        fileMetadataSelectedForIngestOptionsPopup = null;
    }

    private void populateFileMetadatas() {

        if (selectedFileIdsList != null) {

            Long datasetVersionId = workingVersion.getId();

            if (datasetVersionId != null) {
                // The version has a database id - this is an existing version, 
                // that had been saved previously. So we can look up the file metadatas
                // by the file and version ids:
                for (Long fileId : selectedFileIdsList) {
                    logger.fine("attempting to retrieve file metadata for version id " + datasetVersionId + " and file id " + fileId);
                    FileMetadata fileMetadata = datafileService.findFileMetadataByFileAndVersionId(fileId, datasetVersionId);
                    if (fileMetadata != null) {
                        logger.fine("Success!");
                        fileMetadatas.add(fileMetadata);
                    } else {
                        logger.fine("Failed to find file metadata.");
                    }
                }
            } else {
                logger.fine("Brand new edit version - no database id.");
                for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {
                    for (Long fileId : selectedFileIdsList) {
                        if (fileId.equals(fileMetadata.getDataFile().getId())) {
                            logger.fine("Success! - found the file id "+fileId+" in the brand new edit version.");
                            fileMetadatas.add(fileMetadata);
                            selectedFileIdsList.remove(fileId);
                            break;
                        }
                    }
                    
                    // If we've already gone through all the file ids on the list - 
                    // we can stop going through the filemetadatas:
                    
                    if (selectedFileIdsList.size() < 1) {
                        break;
                    }
                }
            }
        }
    }

    
}
