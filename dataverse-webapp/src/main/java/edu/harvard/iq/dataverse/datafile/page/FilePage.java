package edu.harvard.iq.dataverse.datafile.page;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.citation.CitationFactory;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.common.files.mime.TextMimeType;
import edu.harvard.iq.dataverse.datafile.FileService;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean.RetrieveDatasetVersionResponse;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.UpdateDatasetException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ExporterType;
import edu.harvard.iq.dataverse.export.spi.Exporter;
import edu.harvard.iq.dataverse.externaltools.ExternalToolHandler;
import edu.harvard.iq.dataverse.externaltools.ExternalToolServiceBean;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseDialog;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.FileVersionDifference;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse.TermsOfUseType;
import edu.harvard.iq.dataverse.persistence.datafile.license.LicenseIcon;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.guestbook.GuestbookResponse;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import io.vavr.control.Try;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import javax.ejb.EJBException;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;


/**
 * @author skraffmi
 */

@ViewScoped
@Named("FilePage")
public class FilePage implements java.io.Serializable {
    private static final Logger logger = Logger.getLogger(FilePage.class.getCanonicalName());

    private DataFileServiceBean datafileService;
    private DatasetVersionServiceBean datasetVersionService;
    private PermissionServiceBean permissionService;
    private SystemConfig systemConfig;
    private ExternalToolServiceBean externalToolService;
    private DataverseRequestServiceBean dvRequestService;
    private PermissionsWrapper permissionsWrapper;
    private FileDownloadHelper fileDownloadHelper;
    private ExportService exportService;
    private FileService fileService;
    private GuestbookResponseDialog guestbookResponseDialog;
    private CitationFactory citationFactory;
    private DataverseSession session;
    private ExternalToolHandler externalToolHandler;

    private FileMetadata fileMetadata;
    private Long fileId;
    private String version;
    private DataFile file;
    private int selectedTabIndex;
    private Dataset dataset;
    private List<FileMetadata> fileMetadatasForTab;
    private String persistentId;
    private List<ExternalTool> configureTools;
    private List<ExternalTool> exploreTools;
    private List<ExternalTool> previewTools;
    private Boolean thumbnailAvailable = null;
    private Boolean lockedFromEditsVar;
    private Boolean lockedFromDownloadVar;
    private Boolean guestbookResponseProvided = false;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public FilePage() { }

    @Inject
    public FilePage(DataFileServiceBean datafileService, DatasetVersionServiceBean datasetVersionService,
                    PermissionServiceBean permissionService, SystemConfig systemConfig,
                    ExternalToolServiceBean externalToolService, DataverseRequestServiceBean dvRequestService,
                    PermissionsWrapper permissionsWrapper, FileDownloadHelper fileDownloadHelper,
                    ExportService exportService, FileService fileService,
                    GuestbookResponseDialog guestbookResponseDialog, CitationFactory citationFactory,
                    DataverseSession session, ExternalToolHandler externalToolHandler) {
        this.datafileService = datafileService;
        this.datasetVersionService = datasetVersionService;
        this.permissionService = permissionService;
        this.systemConfig = systemConfig;
        this.externalToolService = externalToolService;
        this.dvRequestService = dvRequestService;
        this.permissionsWrapper = permissionsWrapper;
        this.fileDownloadHelper = fileDownloadHelper;
        this.exportService = exportService;
        this.fileService = fileService;
        this.guestbookResponseDialog = guestbookResponseDialog;
        this.citationFactory = citationFactory;
        this.session = session;
        this.externalToolHandler = externalToolHandler;
    }

    // -------------------- GETTERS --------------------

    public FileMetadata getFileMetadata() {
        return fileMetadata;
    }

    public DataFile getFile() {
        return file;
    }

    public Long getFileId() {
        return fileId;
    }

    public String getVersion() {
        return version;
    }

    public List<FileMetadata> getFileMetadatasForTab() {
        return fileMetadatasForTab;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public List<ExternalTool> getConfigureTools() {
        return configureTools;
    }

    public List<ExternalTool> getExploreTools() {
        return exploreTools;
    }

    public List<ExternalTool> getPreviewTools() {
        return previewTools;
    }

    public Boolean getGuestbookResponseProvided() {
        return guestbookResponseProvided;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        if (fileId == null && persistentId == null) {
            return permissionsWrapper.notFound();
        }

        // Set the file and datasetVersion
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

        if (file.getOwner().isHarvested()) {
            // if so, we'll simply forward to the remote URL for the original
            // source of this harvested dataset:
            String originalSourceURL = file.getOwner().getRemoteArchiveURL();
            if (StringUtils.isNotBlank(originalSourceURL)) {
                logger.fine("redirecting to " + originalSourceURL);
                try {
                    FacesContext.getCurrentInstance().getExternalContext().redirect(originalSourceURL);
                } catch (IOException ioe) {
                    // must be a bad URL...
                    // we don't need to do anything special here - we'll redirect
                    // to the local 404 page, below.
                    logger.warning("failed to issue a redirect to " + originalSourceURL);
                }
            }
            return permissionsWrapper.notFound();
        }

        if (!permissionsWrapper.canViewUnpublishedDataset(file.getOwner()) &&
                file.getOwner().hasActiveEmbargo()) {
            return permissionsWrapper.notAuthorized();
        }

        RetrieveDatasetVersionResponse retrieveDatasetVersionResponse
                = datasetVersionService.selectRequestedVersion(file.getOwner().getVersions(), version);
        DatasetVersion version = retrieveDatasetVersionResponse.getDatasetVersion();
        fileMetadata = datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(version.getId(), fileId);

        if (fileMetadata == null) {
            logger.fine("fileMetadata is null! Checking finding most recent version file was in.");
            fileMetadata = datafileService.findMostRecentVersionFileIsIn(file);
            if (fileMetadata == null) {
                return permissionsWrapper.notFound();
            }
        }

        // If this DatasetVersion is unpublished and permission is doesn't have permissions:
        // -> Go to the Login page
        // Check permisisons

        boolean authorized = fileMetadata.getDatasetVersion().isReleased()
                || (!fileMetadata.getDatasetVersion().isReleased() && this.canViewUnpublishedDataset());

        if (!authorized) {
            return permissionsWrapper.notAuthorized();
        }

        guestbookResponseDialog.initForDatasetVersion(fileMetadata.getDatasetVersion());
        this.dataset = fileMetadata.getDataFile().getOwner();

        // Find external tools based on their type, the file content type, and whether
        // ingest has created a derived file for that type
        // Currently, tabular data files are the only type of derived file created, so
        // isTabularData() works - true for tabular types where a .tab file has been
        // created and false for other mimetypes
        // For tabular data, indicate successful ingest by returning a contentType for the derived .tab file
        String contentType = file.isTabularData() ? TextMimeType.TSV_ALT.getMimeValue() : file.getContentType();
        configureTools = externalToolService.findExternalTools(ExternalTool.Type.CONFIGURE, contentType, file, version);
        exploreTools = externalToolService.findExternalTools(ExternalTool.Type.EXPLORE, contentType, file, version);
        previewTools = externalToolService.findExternalTools(ExternalTool.Type.PREVIEW, contentType, file, version);
        return null;
    }

    public boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset(fileMetadata.getDatasetVersion().getDataset());
    }

    public boolean isDownloadPopupRequired() {
        return fileMetadata.getId() != null
                && fileMetadata.getDatasetVersion().getId() != null
                && FileUtil.isDownloadPopupRequired(fileMetadata.getDatasetVersion());
    }

    public boolean isRequestAccessPopupRequired() {
        return fileMetadata.getId() != null
                && fileMetadata.getDatasetVersion().getId() != null
                && FileUtil.isRequestAccessPopupRequired(fileMetadata);
    }

    public List<String[]> getExporters() {
        List<String[]> retList = new ArrayList<>();
        Map<ExporterType, Exporter> exporters = exportService.getAllExporters();

        for (Exporter exporter : exporters.values()) {
            if (exporter.isAvailableToUsers()) {
                String myHostURL = systemConfig.getDataverseSiteUrl();
                String[] temp = new String[2];
                temp[0] = exporter.getDisplayName();
                temp[1] = myHostURL + "/api/datasets/export?exporter=" + exporter.getProviderName() + "&persistentId="
                        + dataset.getGlobalIdString();
                retList.add(temp);
            }
        }
        return retList;
    }

    public String saveProvFreeform(String freeformTextInput, DataFile dataFileFromPopup){

        Try<Dataset> saveProvOperation = Try.of(
                () -> fileService.saveProvenanceFileWithDesc(fileMetadata, dataFileFromPopup, freeformTextInput))
                .onFailure(this::handleProvenanceExceptions);
        if (saveProvOperation.isFailure()){
            return "";
        }
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.editSuccess"));
        return returnToDraftVersion();
    }

    public String createCitation() {
        boolean isDirectCitation = fileMetadata.getDataFile().isIdentifierRegistered();
        return citationFactory
                .create(fileMetadata, isDirectCitation)
                .toString(true);
    }

    public String createCitationFromFileDatasetVersion() {
        return citationFactory
                .create(fileMetadata.getDatasetVersion())
                .toString(true);
    }

    public String deleteFile() {
        Try<Dataset> deleteFileOperation = Try.of(() -> fileService.deleteFile(this.fileMetadata))
                .onFailure(this::handleDeleteFileExceptions);

        if (deleteFileOperation.isFailure()) {
            return "";
        }

        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("file.message.deleteSuccess"));
        return returnToDatasetOnly(fileMetadata.getDataFile().getOwner());
    }

    public void tabChanged(TabChangeEvent<?> event) {
        TabView tv = (TabView) event.getComponent();
        int activeTabIndex = tv.getActiveIndex();
        setFileMetadatasForTab(activeTabIndex == 1 || activeTabIndex == 2
                ? loadFileMetadataTabList()
                : new ArrayList<>());
    }

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

        thumbnailAvailable = fileDownloadHelper.canUserDownloadFile(fileMetadata)
                && datafileService.isThumbnailAvailable(fileMetadata.getDataFile());

        return thumbnailAvailable;
    }

    public boolean isLicenseIconAvailable(FileTermsOfUse termsOfUse) {
        return termsOfUse.getTermsOfUseType() == TermsOfUseType.LICENSE_BASED
                && termsOfUse.getLicense().getIcon() != null;
    }

    public Optional<StreamedContent> getLicenseIconContent(FileTermsOfUse termsOfUse) {
        if (!isLicenseIconAvailable(termsOfUse)) {
            return Optional.empty();
        }
        LicenseIcon licenseIcon = termsOfUse.getLicense().getIcon();
        return Optional.of(DefaultStreamedContent.builder()
                           .contentType(licenseIcon.getContentType())
                           .stream(() -> new ByteArrayInputStream(licenseIcon.getContent()))
                           .build());
    }

    public StreamedContent getOtherTermsIconContent(FileTermsOfUse.TermsOfUseType termsOfUseType) {
        if (termsOfUseType.equals(FileTermsOfUse.TermsOfUseType.RESTRICTED)) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                            "/images/restrictedaccess.png")))
                    .build();
        }

        if (termsOfUseType.equals(FileTermsOfUse.TermsOfUseType.ALL_RIGHTS_RESERVED)) {
            return DefaultStreamedContent.builder()
                    .stream(() -> new ByteArrayInputStream(FileUtil.getFileFromResources(
                            "/images/allrightsreserved.png")))
                    .build();
        }

        return null;
    }

    public boolean canUpdateDataset() {
        return permissionsWrapper.canCurrentUserUpdateDataset(file.getOwner());
    }

    public boolean isDraftReplacementFile() {
        /*
        This method tests to see if the file has been replaced in a draft version of the dataset
        Since it must must work when you are on prior versions of the dataset
        it must accrue all replacement files that may have been created
        */

        DataFile dataFileToTest = fileMetadata.getDataFile();
        DatasetVersion currentVersion = dataset.getLatestVersion();

        if (!currentVersion.isDraft() || dataset.getReleasedVersion() == null) {
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
     */
    public boolean isReplacementFile() {
        return datafileService.isReplacementFile(this.getFile());
    }

    public boolean isPubliclyDownloadable() {
        return FileUtil.isPubliclyDownloadable(fileMetadata);
    }

    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(),
                        new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
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
                permissionService.checkDownloadFileLock(dataset, dvRequestService.getDataverseRequest(),
                        new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest()));
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

    //Provenance fragment bean calls this to show error dialogs after popup failure
    //This can probably be replaced by calling JsfHelper from the provpopup bean
    public void showProvError() {
        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("file.metadataTab.provenance.error"), "");
    }

    public String getPreviewUrl() {
        if (previewTools.isEmpty()) {
            return StringUtils.EMPTY;
        }
        ExternalTool previewer = previewTools.get(0);
        return externalToolHandler.buildToolUrlWithQueryParams(previewer, file, null, session.getLocaleCode()) + "&preview=true";
    }

    public void showPreview(GuestbookResponse guestbookResponse) {
        fileDownloadHelper.writeGuestbookResponseForPreview(guestbookResponse, fileMetadata, previewTools.get(0));
        guestbookResponseProvided = true;
    }

    // -------------------- PRIVATE --------------------

    private void handleProvenanceExceptions(Throwable throwable){
        throwable = throwable instanceof EJBException ? throwable.getCause() : throwable;

        if (throwable instanceof ValidationException){
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataset.message.validationError"), "");
        } else if (throwable instanceof UpdateDatasetException){
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataset.save.fail"),
                    throwable.toString());
        } else {
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataset.save.fail"), "");
        }
    }

    private void handleDeleteFileExceptions(Throwable throwable){
        throwable = throwable instanceof EJBException ? throwable.getCause() : throwable;

        if (throwable instanceof ValidationException){
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataset.message.validationError"), "");
        } else if (throwable instanceof UpdateDatasetException){
            JsfHelper.addErrorMessage(
                    BundleUtil.getStringFromBundle("dataset.delete.fail"),
                    throwable.toString());
        } else {
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.delete.fail"), "");
        }
    }

    private List<FileMetadata> loadFileMetadataTabList() {
        List<DataFile> allfiles = allRelatedFiles();
        List<FileMetadata> retList = new ArrayList<>();
        for (DatasetVersion versionLoop : fileMetadata.getDatasetVersion().getDataset().getVersions()) {
            boolean foundFmd = false;

            if (versionLoop.isReleased() || versionLoop.isDeaccessioned()
                    || permissionsWrapper.canViewUnpublishedDataset(fileMetadata.getDatasetVersion().getDataset())) {
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
                // no File metadata found make dummy one
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
        if (priorVersion != null && priorVersion.getAllFilesMetadataSorted() != null) {
            for (FileMetadata fmdTest : priorVersion.getAllFilesMetadataSorted()) {
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

        if (dvPrevious != null && dvPrevious.getAllFilesMetadataSorted() != null) {
            for (FileMetadata fmdTest : dvPrevious.getAllFilesMetadataSorted()) {
                for (DataFile fileTest : allfiles) {
                    if (fmdTest.getDataFile().equals(fileTest)) {
                        return fmdTest;
                    }
                }
            }
        }

        Long dfId = dfPrevious != null ? dfPrevious.getId() : fmdIn.getDataFile().getId();
        Long versionId = dvPrevious != null ? dvPrevious.getId() : null;

        return datafileService.findFileMetadataByDatasetVersionIdAndDataFileId(versionId, dfId);
    }

    private String returnToDatasetOnly(Dataset draftDataset) {
        return "/dataset.xhtml?persistentId=" + draftDataset.getGlobalIdString() + "&version=DRAFT&faces-redirect=true";
    }

    private String returnToDraftVersion() {
        return "/file.xhtml?fileId=" + fileId + "&version=DRAFT&faces-redirect=true";
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

    // -------------------- SETTERS --------------------

    public void setFileMetadata(FileMetadata fileMetadata) {
        this.fileMetadata = fileMetadata;
    }

    public void setFile(DataFile file) {
        this.file = file;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setFileMetadatasForTab(List<FileMetadata> fileMetadatasForTab) {
        this.fileMetadatasForTab = fileMetadatasForTab;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }
}