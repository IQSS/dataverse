package edu.harvard.iq.dataverse.dataset.tab;

import com.google.common.base.Strings;
import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.dataset.EmbargoAccessService;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.datacapturemodule.DataCaptureModuleUtil;
import edu.harvard.iq.dataverse.datacapturemodule.ScriptRequestResponse;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadHelper;
import edu.harvard.iq.dataverse.datafile.page.FileDownloadRequestHelper;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RequestRsyncScriptCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseDialog;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.license.TermsOfUseFormMapper;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileCategory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFileTag;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.TermsOfUseForm;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.search.SearchServiceBean.SortOrder;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileSortFieldAndOrder;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.PrimefacesUtil;
import edu.harvard.iq.dataverse.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.primefaces.event.data.PageEvent;

import javax.ejb.EJBException;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@ViewScoped
@Named("datasetFilesTab")
public class DatasetFilesTab implements Serializable {

    private static final Logger logger = Logger.getLogger(DatasetFilesTab.class.getCanonicalName());
    
    public static final String DOWNLOAD_POPUP_DIALOG = "downloadPopup";
    public static final String SELECT_FILES_FOR_DOWNLAOD_DIALOG = "selectFilesForDownload";
    public static final String DOWNLOAD_MIXED_DIALOG = "downloadMixed";
    public static final String DOWNLOAD_INVALID_DIALOG = "downloadInvalid";
    
    public static final String SELECT_FILES_FOR_REQUEST_ACCESS_DIALOG = "selectFilesForRequestAccess";
    public static final String REQUEST_ACCESS_DIALOG = "requestAccessPopup";
    

    private DatasetDao datasetDao;
    private DataFileServiceBean datafileService;
    private GuestbookResponseServiceBean guestbookResponseService;
    private ExternalToolServiceBean externalToolService;
    private EjbDataverseEngine commandEngine;
    private FileDownloadHelper fileDownloadHelper;
    private FileDownloadRequestHelper fileDownloadRequestHelper;
    private TermsOfUseFormMapper termsOfUseFormMapper;
    private EmbargoAccessService embargoAccess;

    private PermissionServiceBean permissionService;
    private PermissionsWrapper permissionsWrapper;
    private SettingsServiceBean settingsService;

    private DataverseSession session;
    private DataverseRequestServiceBean dvRequestService;
    
    private GuestbookResponseDialog guestbookResponseDialog;


    private Dataset dataset;
    private DatasetVersion workingVersion;

    private Boolean hasTabular = false;
    /**
     * In the file listing, the page the user is on. This is zero-indexed so if
     * the user clicks page 2 in the UI, this will be 1.
     */
    private int filePaginatorPage;
    private int rowsPerPage;

    private List<FileMetadata> selectedFiles = new ArrayList<>();

    private List<FileMetadata> fileMetadatasSearch;

    private boolean selectAllFiles;

    private String fileLabelSearchTerm;

    /**
     * The contents of the script.
     */
    private String rsyncScript = "";

    private String rsyncScriptFilename;


    private Map<Long, String> datafileThumbnailsMap = new HashMap<>();

    private List<ExternalTool> configureTools = new ArrayList<>();
    private List<ExternalTool> exploreTools = new ArrayList<>();
    private Map<Long, List<ExternalTool>> configureToolsByFileId = new HashMap<>();
    private Map<Long, List<ExternalTool>> exploreToolsByFileId = new HashMap<>();

    private List<FileMetadata> selectedDownloadableFiles;

    private List<FileMetadata> selectedNonDownloadableFiles;
    
    private boolean downloadOriginal = false;


    private List<String> categoriesByName;

    private boolean bulkFileDeleteInProgress = false;

    private List<DataFile> filesToBeDeleted = new ArrayList<>();

    private Boolean lockedFromDownloadVar;
    private Boolean lockedFromEditsVar;
    private boolean lockedDueToDcmUpload;

    private boolean fileAccessRequestMultiButtonRequired;
    private boolean fileAccessRequestMultiSignUpButtonRequired;
    private boolean downloadButtonAvailable;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DatasetFilesTab() {

    }

    @Inject
    public DatasetFilesTab(FileDownloadHelper fileDownloadHelper, DataFileServiceBean datafileService,
                           PermissionServiceBean permissionService, PermissionsWrapper permissionsWrapper,
                           DataverseRequestServiceBean dvRequestService, DatasetDao datasetDao, DataverseSession session,
                           GuestbookResponseServiceBean guestbookResponseService, EmbargoAccessService embargoAccess,
                           SettingsServiceBean settingsService, EjbDataverseEngine commandEngine,
                           ExternalToolServiceBean externalToolService, TermsOfUseFormMapper termsOfUseFormMapper,
                           FileDownloadRequestHelper fileDownloadRequestHelper, GuestbookResponseDialog guestbookResponseDialog) {
        this.fileDownloadHelper = fileDownloadHelper;
        this.datafileService = datafileService;
        this.permissionService = permissionService;
        this.permissionsWrapper = permissionsWrapper;
        this.dvRequestService = dvRequestService;
        this.datasetDao = datasetDao;
        this.session = session;
        this.fileDownloadRequestHelper = fileDownloadRequestHelper;
        this.guestbookResponseService = guestbookResponseService;
        this.embargoAccess = embargoAccess;
        this.settingsService = settingsService;
        this.commandEngine = commandEngine;
        this.externalToolService = externalToolService;
        this.termsOfUseFormMapper = termsOfUseFormMapper;
        this.guestbookResponseDialog = guestbookResponseDialog;
    }


    public void init(DatasetVersion workingVersion) {
        this.dataset = workingVersion.getDataset();
        this.workingVersion = workingVersion;
        rowsPerPage = 10;

        fileMetadatasSearch = getAccessibleFilesMetadata();

        logger.fine("Checking if rsync support is enabled.");
        if (DataCaptureModuleUtil.rsyncSupportEnabled(settingsService.getValueForKey(SettingsServiceBean.Key.UploadMethods))
                && dataset.getFiles().isEmpty()) { //only check for rsync if no files exist
            try {
                ScriptRequestResponse scriptRequestResponse = commandEngine.submit(new RequestRsyncScriptCommand(dvRequestService.getDataverseRequest(), dataset));
                logger.fine("script: " + scriptRequestResponse.getScript());
                if (scriptRequestResponse.getScript() != null && !scriptRequestResponse.getScript().isEmpty()) {
                    rsyncScript = scriptRequestResponse.getScript();
                    rsyncScriptFilename = "upload-" + workingVersion.getDataset().getIdentifier() + ".bash";
                    rsyncScriptFilename = rsyncScriptFilename.replace("/", "_");
                }
            } catch (RuntimeException ex) {
                logger.warning("Problem getting rsync script: " + ex.getLocalizedMessage());
            }
        }

        for (DataFile f : dataset.getFiles()) {
            if (f.isTabularData()) {
                hasTabular = true;
                break;
            }
        }

        if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
            lockedDueToDcmUpload = false;
        }

        configureTools = externalToolService.findByType(ExternalTool.Type.CONFIGURE);
        exploreTools = externalToolService.findByType(ExternalTool.Type.EXPLORE);
        
        guestbookResponseDialog.initForDatasetVersion(workingVersion);
        
        updateMultipleFileOptionFlags();
    }

    public boolean isFilesTabDisplayable() {
        return !embargoAccess.isRestrictedByEmbargo(dataset);
    }

    private List<FileMetadata> getAccessibleFilesMetadata() {
        if (permissionsWrapper.canViewUnpublishedDataset(dataset)) {
            return workingVersion.getAllFilesMetadataSorted();
        } else {
            return workingVersion.getOnlyFilesMetadataNotUnderEmbargoSorted();
        }
    }

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return dataset;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public int getFilePaginatorPage() {
        return filePaginatorPage;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public List<FileMetadata> getSelectedFiles() {
        return selectedFiles;
    }

    public List<FileMetadata> getFileMetadatasSearch() {
        return fileMetadatasSearch;
    }

    public boolean isSelectAllFiles() {
        return selectAllFiles;
    }

    public String getFileLabelSearchTerm() {
        return fileLabelSearchTerm;
    }

    public List<FileMetadata> getSelectedNonDownloadableFiles() {
        return selectedNonDownloadableFiles;
    }

    public boolean isHasTabular() {
        return hasTabular;
    }

    public FileDownloadHelper getFileDownloadHelper() {
        return fileDownloadHelper;
    }

    public boolean isLockedDueToDcmUpload() {
        return lockedDueToDcmUpload;
    }

    public boolean isFileAccessRequestMultiButtonRequired() {
        return fileAccessRequestMultiButtonRequired;
    }

    public boolean isFileAccessRequestMultiSignUpButtonRequired() {
        return fileAccessRequestMultiSignUpButtonRequired;
    }

    // -------------------- LOGIC --------------------

    public void refreshPaginator() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        org.primefaces.component.datatable.DataTable dt = (org.primefaces.component.datatable.DataTable) facesContext.getViewRoot().findComponent("datasetForm:tabView:filesTable");
        filePaginatorPage = dt.getPage();
        rowsPerPage = dt.getRowsToRender();
    }

    public void fileListingPaginatorListener(PageEvent event) {
        filePaginatorPage = event.getPage();
    }

    public void selectAllFiles() {
        logger.fine("selectAllFiles called");
        if (fileLabelSearchTerm.isEmpty()) {
            selectedFiles.clear();
            selectedFiles.addAll(workingVersion.getFileMetadatas());
        } else {
            selectedFiles.clear();
            selectedFiles.addAll(fileMetadatasSearch);
        }
    }

    public void clearSelection() {
        logger.info("clearSelection called");
        selectedFiles.clear();
    }

    public void toggleAllSelected() {
        //This is here so that if the user selects all on the dataset page
        // s/he will get all files on download
        this.selectAllFiles = !this.selectAllFiles;
    }

    public void updateFileSearch() {
        logger.info("updating file search list");
        this.fileMetadatasSearch = selectFileMetadatasForDisplay(this.fileLabelSearchTerm);
    }

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {

        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then check if the thumbnail is available/exists.
        // then cache the results!

        Long dataFileId = fileMetadata.getDataFile().getId();

        if (datafileThumbnailsMap.containsKey(dataFileId)) {
            return !"".equals(datafileThumbnailsMap.get(dataFileId));
        }

        if (!FileUtil.isThumbnailSupported(fileMetadata.getDataFile())) {
            datafileThumbnailsMap.put(dataFileId, "");
            return false;
        }

        if (!this.fileDownloadHelper.canUserDownloadFile(fileMetadata)) {
            datafileThumbnailsMap.put(dataFileId, "");
            return false;
        }


        String thumbnailAsBase64 = ImageThumbConverter.getImageThumbnailAsBase64(fileMetadata.getDataFile(), ImageThumbConverter.DEFAULT_THUMBNAIL_SIZE);


        //if (datafileService.isThumbnailAvailable(fileMetadata.getDataFile())) {
        if (!StringUtil.isEmpty(thumbnailAsBase64)) {
            datafileThumbnailsMap.put(dataFileId, thumbnailAsBase64);
            return true;
        }

        datafileThumbnailsMap.put(dataFileId, "");
        return false;

    }

    public void downloadRsyncScript() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
        response.setContentType("application/download");

        String contentDispositionString;

        contentDispositionString = "attachment;filename=" + rsyncScriptFilename;
        response.setHeader("Content-Disposition", contentDispositionString);
        
        try {
            ServletOutputStream out = response.getOutputStream();
            out.write(rsyncScript.getBytes());
            out.flush();
            ctx.responseComplete();
        } catch (IOException e) {
            String error = "Problem getting bytes from rsync script: " + e;
            logger.warning(error);
            return;
        }

        // If the script has been successfully downloaded, lock the dataset:
        String lockInfoMessage = "script downloaded";
        DatasetLock lock = datasetDao.addDatasetLock(dataset.getId(), DatasetLock.Reason.DcmUpload, session.getUser() != null ? ((AuthenticatedUser) session.getUser()).getId() : null, lockInfoMessage);
        if (lock != null) {
            dataset.addLock(lock);
        } else {
            logger.log(Level.WARNING, "Failed to lock the dataset (dataset id={0})", dataset.getId());
        }

    }

    public String getDataFileThumbnailAsBase64(FileMetadata fileMetadata) {
        return datafileThumbnailsMap.get(fileMetadata.getDataFile().getId());
    }

    /*
       TODO/OPTIMIZATION: This is still costing us N SELECT FROM GuestbookResponse queries, 
       where N is the number of files. This could of course be replaced by a query that'll 
       look up all N at once... Not sure if it's worth it; especially now that N
       will always be 10, for the initial page load. -- L.A. 4.2.1
     */
    public Long getGuestbookResponseCount(FileMetadata fileMetadata) {
        return guestbookResponseService.getCountGuestbookResponsesByDataFileId(fileMetadata.getDataFile().getId());
    }

    public boolean isRequestAccessPopupRequired(FileMetadata fileMetadata) {
        return FileUtil.isRequestAccessPopupRequired(fileMetadata);
    }

    public List<ExternalTool> getConfigureToolsForDataFile(Long fileId) {
        return getCachedToolsForDataFile(fileId, ExternalTool.Type.CONFIGURE);
    }

    public List<ExternalTool> getExploreToolsForDataFile(Long fileId) {
        return getCachedToolsForDataFile(fileId, ExternalTool.Type.EXPLORE);
    }

    // Another convenience method - to cache Update Permission on the dataset: 
    public boolean canUpdateDataset() {
        return permissionsWrapper.canCurrentUserUpdateDataset(dataset);
    }

    public void updateMultipleFileOptionFlags () {
        boolean versionContainsNonDownloadableFiles = workingVersion.getFileMetadatas().stream()
                    .anyMatch(fm -> !fileDownloadHelper.canUserDownloadFile(fm));

        boolean versionContainsDownloadableFiles = workingVersion.getFileMetadatas().stream()
                    .anyMatch(fm -> fileDownloadHelper.canUserDownloadFile(fm));
        
        if (versionContainsNonDownloadableFiles) {
            fileAccessRequestMultiButtonRequired = session.getUser().isAuthenticated();
            fileAccessRequestMultiSignUpButtonRequired = session.getUser().isAuthenticated();
        }
        
        downloadButtonAvailable = versionContainsDownloadableFiles;
    }

    public String requestAccessMultipleFiles() {
        
        if (selectedFiles.isEmpty()) {
            PrimefacesUtil.showDialog(SELECT_FILES_FOR_REQUEST_ACCESS_DIALOG);
            return "";
        }
        
        boolean anyFileAccessPopupRequired = false;

        fileDownloadRequestHelper.clearRequestAccessFiles();
        for (FileMetadata fmd : selectedFiles) {
            if (isRequestAccessPopupRequired(fmd)) {
                fileDownloadRequestHelper.addFileForRequestAccess(fmd.getDataFile());
                anyFileAccessPopupRequired = true;
            }
        }

        if (anyFileAccessPopupRequired) {
            PrimefacesUtil.showDialog(REQUEST_ACCESS_DIALOG);
            return "";
        }
        
        //No popup required
        fileDownloadRequestHelper.requestAccessIndirect();
        return "";
    }
    
    public void validateFilesForDownload(boolean downloadOriginal) {
        selectedDownloadableFiles = new ArrayList<>();
        selectedNonDownloadableFiles = new ArrayList<>();
        this.downloadOriginal = downloadOriginal;

        if (this.selectedFiles.isEmpty()) {
            PrimefacesUtil.showDialog(SELECT_FILES_FOR_DOWNLAOD_DIALOG);
            return;
        }
        for (FileMetadata fmd : this.selectedFiles) {
            if (this.fileDownloadHelper.canUserDownloadFile(fmd)) {
                selectedDownloadableFiles.add(fmd);
            } else {
                selectedNonDownloadableFiles.add(fmd);
            }
        }

        // If some of the files were restricted and we had to drop them off the 
        // list, and NONE of the files are left on the downloadable list
        // - we show them a "you're out of luck" popup: 
        if (selectedDownloadableFiles.isEmpty()) {
            PrimefacesUtil.showDialog(DOWNLOAD_INVALID_DIALOG);
            return;
        }

        // If we have a bunch of files that we can download, AND there were no files 
        // that we had to take off the list, because of permissions - we can 
        // either send the user directly to the download API (if no guestbook/terms
        // popup is required), or send them to the download popup:
        if (selectedNonDownloadableFiles.isEmpty()) {
            fileDownloadHelper.requestDownloadWithFiles(selectedDownloadableFiles, downloadOriginal);
            return;
        }

        // ... and if some files were restricted, but some are downloadable, 
        // we are showing them this "you are somewhat in luck" popup; that will 
        // then direct them to the download, or popup, as needed:
        PrimefacesUtil.showDialog(DOWNLOAD_MIXED_DIALOG);

    }

    public void startMultipleFileDownload() {
        // There's a chance that this is not really a batch download - i.e., 
        // there may only be one file on the downloadable list. But the fileDownloadHelper 
        // method below will check for that, and will redirect to the single download, if
        // that's the case. -- L.A.
        fileDownloadHelper.requestDownloadWithFiles(selectedDownloadableFiles, downloadOriginal);
    }

    public boolean isDownloadButtonAvailable() {
        return downloadButtonAvailable;
    }

    public void refreshTagsPopUp() {
        if (bulkUpdateCheckVersion()) {
            refreshSelectedFiles();
        }
    }

    public String editFileMetadata() {
        // If there are no files selected, return an empty string - which 
        // means, do nothing, don't redirect anywhere, stay on this page. 
        // The dialogue telling the user to select at least one file will 
        // be shown to them by an onclick javascript method attached to the 
        // filemetadata edit button on the page.
        // -- L.A. 4.2.1
        if (this.selectedFiles == null || this.selectedFiles.size() < 1) {
            return "";
        }
        return "/editdatafiles.xhtml?selectedFileIds=" + joinDataFileIdsFromFileMetadata() + "&datasetId=" + dataset.getId() + "&faces-redirect=true";
    }


    /* This method handles saving both "tabular file tags" and
     * "file categories" (which are also considered "tags" in 4.0)
     */
    public void saveFileTagsAndCategories(Collection<FileMetadata> selectedFiles,
                                            Collection<String> selectedFileMetadataTags,
                                            Collection<String> selectedDataFileTags,
                                            boolean removeUnusedTags) {

        for (FileMetadata fmd : selectedFiles) {
            fmd.getCategories().clear();
            selectedFileMetadataTags.forEach(fmd::addCategoryByName);

            if (fmd.getDataFile().isTabularData()) {
                setTagsForTabularData(selectedDataFileTags, fmd);
            }
        }

        addSuccessMessage();

        if (removeUnusedTags) {
            removeUnusedFileTagsFromDataset();
        }

        save();
        try {
            FacesContext.getCurrentInstance().getExternalContext().redirect(returnToDraftVersion());
        } catch (IOException e) {
            logger.info("Failed to issue a redirect to draft version url.");
        }
    }

    public String deleteFilesAndSave() {
        bulkFileDeleteInProgress = true;
        if (bulkUpdateCheckVersion()) {
            refreshSelectedFiles();
        }
        filesToBeDeleted = selectedFiles.stream().map(fm -> fm.getDataFile()).collect(toList());
        return save();
    }

    public String saveTermsOfUse(TermsOfUseForm termsOfUseForm) {

        if (bulkUpdateCheckVersion()) {
            refreshSelectedFiles();
        }

        FileTermsOfUse termsOfUse = termsOfUseFormMapper.mapToFileTermsOfUse(termsOfUseForm);

        for (FileMetadata fm : selectedFiles) {
            fm.setTermsOfUse(termsOfUse.createCopy());
        }

        save();

        return returnToDraftVersion();
    }

    public boolean isLocked() {
        return dataset.isLocked();
    }

    // TODO: investigate why this method was needed in the first place?
    // It appears that it was written under the assumption that downloads 
    // should not be allowed when a dataset is locked... (why?)
    // There are calls to the method throghout the file-download-buttons fragment; 
    // except the way it's done there, it's actually disregarded (??) - so the 
    // download buttons ARE always enabled. The only place where this method is 
    // honored is on the batch (mutliple file) download buttons in filesFragment.xhtml. 
    // As I'm working on #4000, I've been asked to re-enable the batch download 
    // buttons there as well, even when the dataset is locked. I'm doing that - but 
    // I feel we should probably figure out why we went to the trouble of creating 
    // this code in the first place... is there some reason we are forgetting now, 
    // why we do actually want to disable downloads on locked datasets??? 
    // -- L.A. Aug. 2018
    public boolean isLockedFromDownload() {
        if (lockedFromDownloadVar == null) {
            try {
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(), new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromDownloadVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromDownloadVar = true;
                return true;
            }
        }
        return lockedFromDownloadVar;
    }

    public boolean isLockedFromEdits() {
        if (lockedFromEditsVar == null) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }

    public boolean isAllFilesSelected() {
        return selectedFiles.size() >= allCurrentFilesCount();
    }

    public String getEmbargoDateForDisplay() {
        SimpleDateFormat format = new SimpleDateFormat(settingsService.getValueForKey(SettingsServiceBean.Key.DefaultDateFormat));
        return format.format(dataset.getEmbargoDate().getOrNull());
    }

    // -------------------- PRIVATE --------------------
    private Integer allCurrentFilesCount() {
        return Strings.isNullOrEmpty(fileLabelSearchTerm) ? workingVersion.getFileMetadatas().size() : fileMetadatasSearch.size();
    }

    private void addSuccessMessage() {
        String successMessage = BundleUtil.getStringFromBundle("file.assignedTabFileTags.success");
        logger.fine(successMessage);
        successMessage = successMessage.replace("{0}", "Selected Files");
        JsfHelper.addFlashSuccessMessage(successMessage);
    }

    private void setTagsForTabularData(Collection<String> selectedDataFileTags, FileMetadata fmd) {
        fmd.getDataFile().getTags().clear();

        selectedDataFileTags.forEach(selectedTag -> {
            DataFileTag tag = new DataFileTag();
            tag.setTypeByLabel(selectedTag);
            tag.setDataFile(fmd.getDataFile());
            fmd.getDataFile().addTag(tag);
        });
    }

    private List<FileMetadata> selectFileMetadatasForDisplay(String searchTerm) {
        Set<Long> searchResultsIdSet = null;
        List<FileMetadata> retList = new ArrayList<>();

        if (searchTerm != null && !searchTerm.equals("")) {
            List<Integer> searchResultsIdList = datafileService.findFileMetadataIdsByDatasetVersionIdLabelSearchTerm(workingVersion.getId(), searchTerm, new FileSortFieldAndOrder("", SortOrder.asc));
            searchResultsIdSet = new HashSet<>();
            for (Integer id : searchResultsIdList) {
                searchResultsIdSet.add(id.longValue());
            }
        }

        for (FileMetadata fileMetadata : getAccessibleFilesMetadata()) {
            if (searchResultsIdSet == null || searchResultsIdSet.contains(fileMetadata.getId())) {
                retList.add(fileMetadata);
            }
        }

        return retList;
    }

    private List<ExternalTool> getCachedToolsForDataFile(Long fileId, ExternalTool.Type type) {
        Map<Long, List<ExternalTool>> cachedToolsByFileId = new HashMap<>();
        List<ExternalTool> externalTools = new ArrayList<>();
        switch (type) {
            case EXPLORE:
                cachedToolsByFileId = exploreToolsByFileId;
                externalTools = exploreTools;
                break;
            case CONFIGURE:
                cachedToolsByFileId = configureToolsByFileId;
                externalTools = configureTools;
                break;
            default:
                break;
        }
        List<ExternalTool> cachedTools = cachedToolsByFileId.get(fileId);
        if (cachedTools != null) { //if already queried before and added to list
            return cachedTools;
        }
        DataFile dataFile = datafileService.find(fileId);
        cachedTools = ExternalToolServiceBean.findExternalToolsByFile(externalTools, dataFile);
        cachedToolsByFileId.put(fileId, cachedTools); //add to map so we don't have to do the lifting again
        return cachedTools;
    }


    // helper Method
    private String joinDataFileIdsFromFileMetadata() {
        return selectedFiles.stream()
                .map(fileMetadata -> fileMetadata.getDataFile().getId().toString())
                .collect(joining(","));
    }

    private void refreshSelectedFiles() {
        dataset = datasetDao.find(dataset.getId());
        workingVersion = dataset.getEditVersion();

        List<DataFile> selectedDataFiles = selectedFiles.stream()
                .map(FileMetadata::getDataFile)
                .collect(Collectors.toList());

        List<FileMetadata> filteredMetadata = workingVersion.getFileMetadatas().stream()
                .filter(fileMetadata -> selectedDataFiles.contains(fileMetadata.getDataFile()))
                .collect(Collectors.toList());

        selectedFiles.clear();
        selectedFiles.addAll(filteredMetadata);
    }

    private boolean bulkUpdateCheckVersion() {
        return workingVersion.isReleased();
    }

    /*
    Remove unused file tags
    When updating datafile tags see if any custom tags are not in use.
    Remove them
    
    */
    private void removeUnusedFileTagsFromDataset() {
        categoriesByName = new ArrayList<>();
        for (FileMetadata fm : workingVersion.getFileMetadatas()) {
            if (fm.getCategories() != null) {
                for (int i = 0; i < fm.getCategories().size(); i++) {
                    if (!categoriesByName.contains(fm.getCategories().get(i).getName())) {
                        categoriesByName.add(fm.getCategories().get(i).getName());
                    }
                }
            }
        }
        List<DataFileCategory> datasetFileCategoriesToRemove = new ArrayList<>();

        for (DataFileCategory test : dataset.getCategories()) {
            boolean remove = true;
            for (String catByName : categoriesByName) {
                if (catByName.equals(test.getName())) {
                    remove = false;
                    break;
                }
            }
            if (remove) {
                datasetFileCategoriesToRemove.add(test);
            }
        }

        if (!datasetFileCategoriesToRemove.isEmpty()) {
            for (DataFileCategory remove : datasetFileCategoriesToRemove) {
                dataset.getCategories().remove(remove);
            }

        }

    }

    private String save() {

        // Validate
        Set<ConstraintViolation> constraintViolations = workingVersion.validate();
        if (!constraintViolations.isEmpty()) {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.validationError"), "");
            return "";
        }

        // Use the Create or Update command to save the dataset: 
        UpdateDatasetVersionCommand cmd;
        Map<Long, String> deleteStorageLocations = null;

        try {
            if (!filesToBeDeleted.isEmpty()) {
                deleteStorageLocations = datafileService.getPhysicalFilesToDelete(filesToBeDeleted);
            }
            cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest(), filesToBeDeleted);
            cmd.setValidateLenient(true);

            dataset = commandEngine.submit(cmd);
            logger.fine("Successfully executed SaveDatasetCommand.");
        } catch (EJBException ex) {
            StringBuilder error = new StringBuilder();
            error.append(ex).append(" ");
            error.append(ex.getMessage()).append(" ");
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                error.append(cause).append(" ");
                error.append(cause.getMessage()).append(" ");
            }
            logger.log(Level.FINE, "Couldn''t save dataset: {0}", error.toString());
            populateDatasetUpdateFailureMessage();
            return returnToDraftVersion();
        } catch (CommandException ex) {
            //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Dataset Save Failed", " - " + ex.toString()));
            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + ex.getMessage(), ex);
            populateDatasetUpdateFailureMessage();
            return returnToDraftVersion();
        }

        // Have we just deleted some draft datafiles (successfully)? 
        // finalize the physical file deletes:
        // (DataFileService will double-check that the datafiles no 
        // longer exist in the database, before attempting to delete 
        // the physical files)

        if (deleteStorageLocations != null) {
            datafileService.finalizeFileDeletes(deleteStorageLocations);
        }

        // must have been a bulk file update or delete:
        if (bulkFileDeleteInProgress) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileDeleteSuccess"));
        } else {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileUpdateSuccess"));
        }
        bulkFileDeleteInProgress = false;

        logger.fine("Redirecting to the Dataset page.");

        return returnToDraftVersion();
    }

    private void populateDatasetUpdateFailureMessage() {
        // that must have been a bulk file update or delete:
        if (bulkFileDeleteInProgress) {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.bulkFileDeleteFailure"));
        } else {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.filesFailure"));
        }
        bulkFileDeleteInProgress = false;
    }

    private String returnToDraftVersion() {
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=DRAFT" + "&faces-redirect=true";
    }

    // -------------------- SETTERS --------------------

    public void setSelectedFiles(List<FileMetadata> selectedFiles) {
        this.selectedFiles = selectedFiles;
    }

    public void setSelectAllFiles(boolean selectAllFiles) {
        this.selectAllFiles = selectAllFiles;
    }

    public void setFileLabelSearchTerm(String fileLabelSearchTerm) {
        this.fileLabelSearchTerm = StringUtils.trimToEmpty(fileLabelSearchTerm);
    }


}
