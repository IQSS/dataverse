/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.FileDownloadHelper;
import edu.harvard.iq.dataverse.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.datasetutility.WorldMapPermissionHelper;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UpdateDatasetException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ExporterType;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileVersionDifference;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseIcon;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.ByteArrayContent;
import org.primefaces.model.StreamedContent;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;


/**
 * @author skraffmi
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
    private String persistentId;
    private List<ExternalTool> configureTools;
    private List<ExternalTool> exploreTools;

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
    ExternalToolServiceBean externalToolService;

    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    FileDownloadHelper fileDownloadHelper;
    @Inject
    WorldMapPermissionHelper worldMapPermissionHelper;
    @Inject
    private ExportService exportService;

    @Inject
    private FileService fileService;

    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    public String init() {


        if (fileId != null || persistentId != null) {

            // ---------------------------------------
            // Set the file and datasetVersion 
            // ---------------------------------------           
            if (fileId != null) {
                file = datafileService.find(fileId);

            } else {
                file = datafileService.findByGlobalId(persistentId);
                if (file != null) {
                    fileId = file.getId();
                }

            }

            if (file == null || fileId == null) {
                return permissionsWrapper.notFound();
            }

            // Is the Dataset harvested?
            if (file.getOwner().isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = file.getOwner().getRemoteArchiveURL();
                if (originalSourceURL != null && !originalSourceURL.equals("")) {
                    logger.fine("redirecting to " + originalSourceURL);
                    try {
                        FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                    } catch (IOException ioex) {
                        // must be a bad URL...
                        // we don't need to do anything special here - we'll redirect
                        // to the local 404 page, below.
                        logger.warning("failed to issue a redirect to " + originalSourceURL);
                    }
                }

                return permissionsWrapper.notFound();
            }

            RetrieveDatasetVersionResponse retrieveDatasetVersionResponse;
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
            Long getDatasetVersionID = retrieveDatasetVersionResponse.getDatasetVersion().getId();
            fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(getDatasetVersionID, fileId);


            if (fileMetadata == null) {
                logger.fine("fileMetadata is null! Checking finding most recent version file was in.");
                fileMetadata = datafileService.findMostRecentVersionFileIsIn(file);
                if (fileMetadata == null) {
                    return permissionsWrapper.notFound();
                }
            }

            // If this DatasetVersion is unpublished and permission is doesn't have permissions:
            //  > Go to the Login page
            //
            // Check permisisons       


            boolean authorized = (fileMetadata.getDatasetVersion().isReleased()) ||
                    (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());

            if (!authorized) {
                return permissionsWrapper.notAuthorized();
            }

            this.guestbookResponse = this.guestbookResponseService.initGuestbookResponseForFragment(fileMetadata, session);
            this.dataset = fileMetadata.getDataFile().getOwner();

            // Find external tools based on their type, the file content type, and whether
            // ingest has created a derived file for that type
            // Currently, tabular data files are the only type of derived file created, so
            // isTabularData() works - true for tabular types where a .tab file has been
            // created and false for other mimetypes
            String contentType = file.getContentType();
            //For tabular data, indicate successful ingest by returning a contentType for the derived .tab file
            if (file.isTabularData()) {
                contentType = TextMimeType.TSV_ALT.getMimeValue();
            }
            configureTools = externalToolService.findByType(ExternalTool.Type.CONFIGURE, contentType);
            exploreTools = externalToolService.findByType(ExternalTool.Type.EXPLORE, contentType);

        } else {

            return permissionsWrapper.notFound();
        }

        return null;
    }

    private boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset(dvRequestService.getDataverseRequest(), fileMetadata.getDatasetVersion().getDataset());
    }


    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }


    public boolean isDownloadPopupRequired() {
        if (fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null) {
            return false;
        }
        return FileUtil.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
    }

    public boolean isRequestAccessPopupRequired() {
        if (fileMetadata.getId() == null || fileMetadata.getDatasetVersion().getId() == null) {
            return false;
        }
        return FileUtil.isRequestAccessPopupRequired(fileMetadata);
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

    public List<String[]> getExporters() {
        List<String[]> retList = new ArrayList<>();

        Map<ExporterType, Exporter> exporters = exportService.getAllExporters();

        for (Exporter exporter : exporters.values()) {

            if (exporter.isAvailableToUsers()) {
                String myHostURL = systemConfig.getDataverseSiteUrl();

                String[] temp = new String[2];
                temp[0] = exporter.getDisplayName();
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName() + "&persistentId=" + dataset.getGlobalIdString();
                retList.add(temp);
            }

        }
        return retList;
    }

    public String saveProvFreeform(String freeformTextInput, DataFile dataFileFromPopup){

        Try<Dataset> saveProvOperation = Try.of(() -> fileService.saveProvenanceFileWithDesc(fileMetadata, dataFileFromPopup, freeformTextInput))
                .onFailure(this::handleProvenanceExceptions);

        if (saveProvOperation.isFailure()){
            return "";
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.editSuccess"));
        setVersion("DRAFT");
        init();
        return returnToDraftVersion();
    }

    public String deleteFile() {
        Try<Dataset> deleteFileOperation = Try.of(() -> fileService.deleteFile(this.fileMetadata))
                .onFailure(this::handleDeleteFileExceptions);

        if (deleteFileOperation.isFailure()) {
            return "";
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.deleteSuccess"));

        setVersion("DRAFT");
        return returnToDatasetOnly(fileMetadata.getDataFile().getOwner());
    }

    public void tabChanged(TabChangeEvent event) {
        TabView tv = (TabView) event.getComponent();
        int activeTabIndex = tv.getActiveIndex();
        if (activeTabIndex == 1 || activeTabIndex == 2) {
            setFileMetadatasForTab(loadFileMetadataTabList());
        } else {
            setFileMetadatasForTab(new ArrayList<>());
        }
    }

    private void handleProvenanceExceptions(Throwable throwable){
        if (throwable instanceof EJBException){
            throwable = throwable.getCause();
        }

        if (throwable instanceof ValidationException){
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.message.validationError"));

        } else if (throwable instanceof UpdateDatasetException){
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.save.fail"),
                          " - " + throwable.toString());
        } else {
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.save.fail"));
        }
    }

    private void handleDeleteFileExceptions(Throwable throwable){
        if (throwable instanceof EJBException){
            throwable = throwable.getCause();
        }

        if (throwable instanceof ValidationException){
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.message.validationError"));

        } else if (throwable instanceof UpdateDatasetException){
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.delete.fail"),
                          " - " + throwable.toString());
        } else {
            JH.addMessage(FacesMessage.SEVERITY_ERROR,
                          BundleUtil.getStringFromBundle("dataset.delete.fail"));
        }
    }

    private List<FileMetadata> loadFileMetadataTabList() {
        List<DataFile> allfiles = allRelatedFiles();
        List<FileMetadata> retList = new ArrayList<>();
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;

            if (versionLoop.isReleased() || versionLoop.isDeaccessioned() || permissionService.on(fileMetadata.getDatasetVersion().getDataset()).has(Permission.ViewUnpublishedDataset)) {
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

    private FileMetadata getPreviousFileMetadata(FileMetadata fmdIn) {

        DataFile dfPrevious = datafileService.findPreviousFile(fmdIn.getDataFile());
        DatasetVersion dvPrevious = null;
        boolean gotCurrent = false;
        for (DatasetVersion dvloop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            if (gotCurrent) {
                dvPrevious = dvloop;
                break;
            }
            if (dvloop.equals(fmdIn.getDatasetVersion())) {
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
        if (dfPrevious != null) {
            dfId = dfPrevious.getId();
        }
        Long versionId = null;
        if (dvPrevious != null) {
            versionId = dvPrevious.getId();
        }

        return datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId);
    }

    public List<FileMetadata> getFileMetadatasForTab() {
        return fileMetadatasForTab;
    }

    public void setFileMetadatasForTab(List<FileMetadata> fileMetadatasForTab) {
        this.fileMetadatasForTab = fileMetadatasForTab;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    private Boolean thumbnailAvailable = null;

    public boolean isThumbnailAvailable(FileMetadata fileMetadata) {
        // new and optimized logic: 
        // - check download permission here (should be cached - so it's free!)
        // - only then ask the file service if the thumbnail is available/exists.
        // the service itself no longer checks download permissions.
        // (Also, cache the result the first time the check is performed... 
        // remember - methods referenced in "rendered=..." attributes are 
        // called *multiple* times as the page is loading!)

        if (thumbnailAvailable != null) {
            return thumbnailAvailable;
        }

        if (!fileDownloadHelper.canDownloadFile(fileMetadata)) {
            thumbnailAvailable = false;
        } else {
            thumbnailAvailable = datafileService.isThumbnailAvailable(fileMetadata.getDataFile());
        }

        return thumbnailAvailable;
    }

    public boolean isLicenseIconAvailable(FileMetadata fileMetadata) {
        if (fileMetadata.getTermsOfUse().getTermsOfUseType() != TermsOfUseType.LICENSE_BASED) {
            return false;
        }
        return fileMetadata.getTermsOfUse().getLicense().getIcon() != null;
    }

    public Optional<StreamedContent> getLicenseIconContent(FileMetadata fileMetadata) {
        if (!isLicenseIconAvailable(fileMetadata)) {
            Optional.empty();
        }
        LicenseIcon licenseIcon = fileMetadata.getTermsOfUse().getLicense().getIcon();
        return Optional.of(new ByteArrayContent(licenseIcon.getContent(), licenseIcon.getContentType()));
    }

    private String returnToDatasetOnly(Dataset draftDataset) {

        return "/dataset.xhtml?persistentId=" + draftDataset.getGlobalIdString() + "&version=DRAFT" + "&faces-redirect=true";
    }

    private String returnToDraftVersion() {

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
        List<DataFile> dataFiles = new ArrayList<>();
        DataFile dataFileToTest = fileMetadata.getDataFile();
        Long rootDataFileId = dataFileToTest.getRootDataFileId();
        if (rootDataFileId < 0) {
            dataFiles.add(dataFileToTest);
        } else {
            dataFiles.addAll(datafileService.findAllRelatedByRootDatafileId(rootDataFileId));
        }

        return dataFiles;
    }

    public boolean isDraftReplacementFile() {
        /*
        This method tests to see if the file has been replaced in a draft version of the dataset
        Since it must must work when you are on prior versions of the dataset 
        it must accrue all replacement files that may have been created
        */

        DataFile dataFileToTest = fileMetadata.getDataFile();

        DatasetVersion currentVersion = dataset.getLatestVersion();

        if (!currentVersion.isDraft()) {
            return false;
        }

        if (dataset.getReleasedVersion() == null) {
            return false;
        }

        List<DataFile> dataFiles = new ArrayList<>();

        dataFiles.add(dataFileToTest);

        while (datafileService.findReplacementFile(dataFileToTest.getId()) != null) {
            dataFiles.add(datafileService.findReplacementFile(dataFileToTest.getId()));
            dataFileToTest = datafileService.findReplacementFile(dataFileToTest.getId());
        }

        if (dataFiles.size() < 2) {
            return false;
        }

        int numFiles = dataFiles.size();

        DataFile current = dataFiles.get(numFiles - 1);

        DatasetVersion publishedVersion = dataset.getReleasedVersion();

        return datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(publishedVersion.getId(), current.getId()) == null;

    }


    /**
     * To help with replace development
     *
     * @return
     */
    public boolean isReplacementFile() {

        return this.datafileService.isReplacementFile(this.getFile());
    }

    public boolean isPubliclyDownloadable() {
        return FileUtil.isPubliclyDownloadable(fileMetadata);
    }

    private Boolean lockedFromEditsVar;
    private Boolean lockedFromDownloadVar;

    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }

    public boolean isLockedFromDownload() {
        if (null == lockedFromDownloadVar) {
            try {
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(), new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromDownloadVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromDownloadVar = true;
            }
        }
        return lockedFromDownloadVar;
    }

    public String getPublicDownloadUrl() {
        return FileUtil.getPublicDownloadUrl(systemConfig.getDataverseSiteUrl(), persistentId, fileId);
    }

    public List<ExternalTool> getConfigureTools() {
        return configureTools;
    }

    public List<ExternalTool> getExploreTools() {
        return exploreTools;
    }

    //Provenance fragment bean calls this to show error dialogs after popup failure
    //This can probably be replaced by calling JsfHelper from the provpopup bean
    public void showProvError() {
        JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"));
    }

}
