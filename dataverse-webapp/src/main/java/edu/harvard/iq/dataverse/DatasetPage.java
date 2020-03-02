package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.datafile.FileDownloadServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.tab.DatasetFilesTab;
import edu.harvard.iq.dataverse.dataset.tab.DatasetMetadataTab;
import edu.harvard.iq.dataverse.engine.command.Command;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.engine.command.exception.NoDatasetFilesException;
import edu.harvard.iq.dataverse.engine.command.impl.AbstractSubmitToArchiveCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CreatePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.CuratePublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeletePrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DestroyDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetLatestPublishedDatasetVersionCommand;
import edu.harvard.iq.dataverse.engine.command.impl.GetPrivateUrlCommand;
import edu.harvard.iq.dataverse.engine.command.impl.LinkDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDatasetResult;
import edu.harvard.iq.dataverse.engine.command.impl.PublishDataverseCommand;
import edu.harvard.iq.dataverse.engine.command.impl.ReturnDatasetToAuthorCommand;
import edu.harvard.iq.dataverse.engine.command.impl.SubmitDatasetForReviewCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.error.DataverseError;
import edu.harvard.iq.dataverse.export.ExportService;
import edu.harvard.iq.dataverse.export.ExporterType;
import edu.harvard.iq.dataverse.guestbook.GuestbookResponseServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.ExternalTool;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.MapLayerMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRelPublication;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtil;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.ArchiverUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

/**
 * @author gdurand
 */
@ViewScoped
@Named("DatasetPage")
public class DatasetPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());


    @EJB
    DatasetDao datasetDao;
    @EJB
    DatasetVersionServiceBean datasetVersionService;
    @EJB
    DataFileServiceBean datafileService;
    @EJB
    PermissionServiceBean permissionService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseSession session;
    @EJB
    MapLayerMetadataServiceBean mapLayerMetadataService;
    @EJB
    SettingsServiceBean settingsService;
    @EJB
    GuestbookResponseServiceBean guestbookResponseService;
    @EJB
    FileDownloadServiceBean fileDownloadService;
    @Inject
    DataverseRequestServiceBean dvRequestService;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @Inject
    ThumbnailServiceWrapper thumbnailServiceWrapper;
    @Inject
    private ExportService exportService;
    @Inject
    private DatasetMetadataTab metadataTab;
    @Inject
    private DatasetFilesTab datasetFilesTab;
    @Inject
    private DatasetService datasetService;

    private Dataset dataset = new Dataset();

    private Long versionId;
    private int selectedTabIndex;
    private DatasetVersion workingVersion;
    private int releaseRadio = 1;
    private String datasetNextMajorVersion = "1.0";
    private String datasetNextMinorVersion = "";
    private String displayCitation;

    private String persistentId;
    private String version;

    private boolean stateChanged = false;
    private Boolean sameTermsOfUseForAllFiles;
    private String thumbnailString = null;
    private String returnToAuthorReason;

    private Date currentEmbargoDate;

    // This is the Dataset-level thumbnail;
    // it's either the thumbnail of the designated datafile, 
    // or scaled down uploaded "logo" file, or randomly selected
    // image datafile from this dataset. 
    public String getThumbnailString() {
        // This method gets called 30 (!) times, just to load the page!
        // - so let's cache that string the first time it's called. 

        if (thumbnailString != null) {
            if ("".equals(thumbnailString)) {
                return null;
            }
            return thumbnailString;
        }

        if (!readOnly) {
            DatasetThumbnail datasetThumbnail = DatasetUtil.getThumbnail(dataset);
            if (datasetThumbnail == null) {
                thumbnailString = "";
                return null;
            }

            if (datasetThumbnail.isFromDataFile()) {
                if (!datasetThumbnail.getDataFile().equals(dataset.getThumbnailFile())) {
                    datasetDao.assignDatasetThumbnailByNativeQuery(dataset, datasetThumbnail.getDataFile());
                    // refresh the dataset:
                    dataset = datasetDao.find(dataset.getId());
                }
            }

            thumbnailString = datasetThumbnail.getBase64image();
        } else {
            thumbnailString = thumbnailServiceWrapper.getDatasetCardImageAsBase64Url(dataset, workingVersion.getId(), !workingVersion.isDraft(), new DataAccess());
            if (thumbnailString == null) {
                thumbnailString = "";
                return null;
            }


        }
        return thumbnailString;
    }

    public void setThumbnailString(String thumbnailString) {
        //Dummy method
    }

    private Boolean thisLatestReleasedVersion = null;

    /**
     * Used in dataset.xhmtl
     */
    public String getJsonLd() {
        if (isThisLatestReleasedVersion()) {
            Either<DataverseError, String> exportedDataset =
                    exportService.exportDatasetVersionAsString(dataset.getReleasedVersion(),
                                                               ExporterType.SCHEMADOTORG);

            if (exportedDataset.isLeft()) {
                logger.fine(exportedDataset.getLeft().getErrorMsg());
                return StringUtils.EMPTY;
            }

            return exportedDataset.get();
        }
        return StringUtils.EMPTY;
    }

    public boolean isThisLatestReleasedVersion() {
        if (thisLatestReleasedVersion != null) {
            return thisLatestReleasedVersion;
        }

        if (!workingVersion.isPublished()) {
            thisLatestReleasedVersion = false;
            return false;
        }

        DatasetVersion latestPublishedVersion = null;
        Command<DatasetVersion> cmd = new GetLatestPublishedDatasetVersionCommand(dvRequestService.getDataverseRequest(), dataset);
        try {
            latestPublishedVersion = commandEngine.submit(cmd);
        } catch (Exception ex) {
            // whatever...
        }

        thisLatestReleasedVersion = workingVersion.equals(latestPublishedVersion);

        return thisLatestReleasedVersion;

    }

    // Another convenience method - to cache Update Permission on the dataset: 
    public boolean canUpdateDataset() {
        return permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), this.dataset);
    }

    public boolean canPublishDataverse() {
        return permissionsWrapper.canIssuePublishDataverseCommand(dataset.getOwner());
    }

    public boolean isLatestDatasetWithAnyFilesIncluded(){
        return !dataset.getLatestVersion().getFileMetadatas().isEmpty();
    }

    public boolean canViewUnpublishedDataset() {
        return permissionsWrapper.canViewUnpublishedDataset(dvRequestService.getDataverseRequest(), dataset);
    }

    /*
     * 4.2.1 optimization.
     * HOWEVER, this doesn't appear to be saving us anything!
     * i.e., it's just as cheap to use session.getUser().isAuthenticated()
     * every time; it doesn't do any new db lookups.
     */
    public boolean isSessionUserAuthenticated() {
        return session.getUser().isAuthenticated();
    }

    private final Map<Long, MapLayerMetadata> mapLayerMetadataLookup = new HashMap<>();

    public String getGlobalId() {
        return persistentId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDisplayCitation() {
        //displayCitation = dataset.getCitation(false, workingVersion);
        return displayCitation;
    }

    public void setDisplayCitation(String displayCitation) {
        this.displayCitation = displayCitation;
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

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public int getSelectedTabIndex() {
        return selectedTabIndex;
    }

    public void setSelectedTabIndex(int selectedTabIndex) {
        this.selectedTabIndex = selectedTabIndex;
    }

    public int getReleaseRadio() {
        return releaseRadio;
    }

    public void setReleaseRadio(int releaseRadio) {
        this.releaseRadio = releaseRadio;
    }

    public String getDatasetNextMajorVersion() {
        return datasetNextMajorVersion;
    }

    public void setDatasetNextMajorVersion(String datasetNextMajorVersion) {
        this.datasetNextMajorVersion = datasetNextMajorVersion;
    }

    public String getDatasetNextMinorVersion() {
        return datasetNextMinorVersion;
    }

    public void setDatasetNextMinorVersion(String datasetNextMinorVersion) {
        this.datasetNextMinorVersion = datasetNextMinorVersion;
    }

    /**
     * Create a hashmap consisting of { DataFile.id : MapLayerMetadata object}
     * <p>
     * Very few DataFiles will have associated MapLayerMetadata objects so only
     * use 1 query to get them
     */
    private void loadMapLayerMetadataLookup() {
        if (this.dataset == null) {
        }
        if (this.dataset.getId() == null) {
            return;
        }
        List<MapLayerMetadata> mapLayerMetadataList = mapLayerMetadataService.getMapLayerMetadataForDataset(this.dataset);
        if (mapLayerMetadataList == null) {
            return;
        }
        for (MapLayerMetadata layer_metadata : mapLayerMetadataList) {
            mapLayerMetadataLookup.put(layer_metadata.getDataFile().getId(), layer_metadata);
        }

    }// A DataFile may have a related MapLayerMetadata object


    private boolean readOnly = true;

    public String init() {
        return init(true);
    }

    public String initCitation() {
        return init(false);
    }

    private String init(boolean initFull) {
        if (dataset.getId() != null || versionId != null || persistentId != null) { // view mode for a dataset     

            DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

            // ---------------------------------------
            // Set the workingVersion and Dataset
            // ---------------------------------------           
            if (persistentId != null) {
                logger.fine("initializing DatasetPage with persistent ID " + persistentId);
                // Set Working Version and Dataset by PersistentID
                dataset = datasetDao.findByGlobalId(persistentId);
                if (dataset == null) {
                    logger.warning("No such dataset: " + persistentId);
                    return permissionsWrapper.notFound();
                }
                logger.fine("retrieved dataset, id=" + dataset.getId());

                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.fine("retrieved version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (dataset.getId() != null) {
                // Set Working Version and Dataset by Datasaet Id and Version
                dataset = datasetDao.find(dataset.getId());
                if (dataset == null) {
                    logger.warning("No such dataset: " + dataset);
                    return permissionsWrapper.notFound();
                }
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
                retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
                this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();
                logger.info("retreived version: id: " + workingVersion.getId() + ", state: " + this.workingVersion.getVersionState());

            } else if (versionId != null) {
                // TODO: 4.2.1 - this method is broken as of now!
                // Set Working Version and Dataset by DatasaetVersion Id
                //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);

            }

            if (retrieveDatasetVersionResponse == null) {
                return permissionsWrapper.notFound();
            }


            //this.dataset = this.workingVersion.getDataset();

            // end: Set the workingVersion and Dataset
            // ---------------------------------------
            // Is the DatasetVersion or Dataset null?
            //
            if (workingVersion == null || this.dataset == null) {
                return permissionsWrapper.notFound();
            }

            // Is the Dataset harvested?

            if (dataset.isHarvested()) {
                // if so, we'll simply forward to the remote URL for the original
                // source of this harvested dataset:
                String originalSourceURL = dataset.getRemoteArchiveURL();
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
                    return originalSourceURL;
                }

                return permissionsWrapper.notFound();
            }

            // Check permisisons           
            if (!(workingVersion.isReleased() || workingVersion.isDeaccessioned()) && !this.canViewUnpublishedDataset()) {
                return permissionsWrapper.notAuthorized();
            }

            if (!retrieveDatasetVersionResponse.wasRequestedVersionRetrieved()) {
                //msg("checkit " + retrieveDatasetVersionResponse.getDifferentVersionMessage());
                JsfHelper.addFlashWarningMessage(retrieveDatasetVersionResponse.getDifferentVersionMessage());//BundleUtil.getStringFromBundle("dataset.message.metadataSuccess"));
            }

            // init the citation
            displayCitation = dataset.getCitation(true, workingVersion);
            initCurrentEmbargo();


            if (initFull) {
                // init the list of FileMetadatas
                if (workingVersion.isDraft() && canUpdateDataset()) {
                    readOnly = false;
                }

                datasetNextMajorVersion = this.dataset.getNextMajorVersionString();
                datasetNextMinorVersion = this.dataset.getNextMinorVersionString();
                returnToAuthorReason = StringUtils.EMPTY;

                setExistReleasedVersion(resetExistRealeaseVersion());
                //moving setVersionTabList to tab change event
                //setVersionTabList(resetVersionTabList());
                //setReleasedVersionTabList(resetReleasedVersionTabList());
                //SEK - lazymodel may be needed for datascroller in future release
                // lazyModel = new LazyFileMetadataDataModel(workingVersion.getId(), datafileService );
                // populate MapLayerMetadata
                this.loadMapLayerMetadataLookup();  // A DataFile may have a related MapLayerMetadata object

            }
        } else {
            return permissionsWrapper.notFound();
        }
        try {
            privateUrl = commandEngine.submit(new GetPrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            if (privateUrl != null) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            }
        } catch (CommandException ex) {
            // No big deal. The user simply doesn't have access to create or delete a Private URL.
        }
        if (session.getUser() instanceof PrivateUrlUser) {
            PrivateUrlUser privateUrlUser = (PrivateUrlUser) session.getUser();
            if (dataset != null && dataset.getId().equals(privateUrlUser.getDatasetId())) {
                JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageReviewer"));
            }
        }

        // Various info messages, when the dataset is locked (for various reasons):
        if (dataset.isLocked() && canUpdateDataset()) {
            if (dataset.isLockedFor(DatasetLock.Reason.Workflow)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.message"),
                              BundleUtil.getStringFromBundle("dataset.locked.message.details"));
            }
            if (dataset.isLockedFor(DatasetLock.Reason.InReview)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.inReview.message"),
                              BundleUtil.getStringFromBundle("dataset.inreview.infoMessage"));
            }
            if (dataset.isLockedFor(DatasetLock.Reason.DcmUpload)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.summary"),
                              BundleUtil.getStringFromBundle("file.rsyncUpload.inProgressMessage.details"));
            }
            //This is a hack to remove dataset locks for File PID registration if 
            //the dataset is released
            //in testing we had cases where datasets with 1000 files were remaining locked after being published successfully
                /*if(dataset.getLatestVersion().isReleased() && dataset.isLockedFor(DatasetLock.Reason.pidRegister)){
                    datasetDao.removeDatasetLocks(dataset.getId(), DatasetLock.Reason.pidRegister);
                }*/
            if (dataset.isLockedFor(DatasetLock.Reason.pidRegister)) {
                JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.publish.workflow.message"),
                              BundleUtil.getStringFromBundle("dataset.pidRegister.workflow.inprogress"));
            }
        }

        return null;
    }

    public String releaseDraft() {
        if (releaseRadio == 1) {
            return releaseDataset(true);
        } else if (releaseRadio == 2) {
            return releaseDataset(false);
        } else if (releaseRadio == 3) {
            return updateCurrentVersion();
        } else {
            return "Invalid Choice";
        }
    }

    public String releaseMajor() {
        return releaseDataset(false);
    }

    public String sendBackToContributor() {
        Try.of(() -> commandEngine.submit(new ReturnDatasetToAuthorCommand(dvRequestService.getDataverseRequest(), dataset, returnToAuthorReason)))
                .onSuccess(ds -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.reject.success")))
                .onFailure(throwable -> logger.log(Level.SEVERE, "Sending back to Contributor failed:", throwable))
                .onFailure(throwable -> JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.reject.failure", Collections.singletonList(throwable.getMessage()))));

        return returnToLatestVersion();
    }

    public String submitDataset() {
        try {
            Command<Dataset> cmd = new SubmitDatasetForReviewCommand(dvRequestService.getDataverseRequest(), dataset);
            dataset = commandEngine.submit(cmd);

        } catch (CommandException ex) {
            String message = ex.getMessage();
            logger.log(Level.SEVERE, "submitDataset: {0}", message);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.submit.failure", Collections.singletonList(message)));
        }
        return returnToLatestVersion();
    }

    public String releaseParentDVAndDataset() {
        releaseParentDV();
        return releaseDataset(false);
    }

    public String releaseDataset() {
        return releaseDataset(false);
    }

    private void releaseParentDV() {
        if (session.getUser() instanceof AuthenticatedUser) {
            PublishDataverseCommand cmd = new PublishDataverseCommand(dvRequestService.getDataverseRequest(), dataset.getOwner());
            try {
                commandEngine.submit(cmd);
                JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataverse.publish.success"));

            } catch (CommandException ex) {
                logger.log(Level.SEVERE, "Unexpected Exception calling  publish dataverse command", ex);
                JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataverse.publish.failure"));

            }
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataverse.notreleased"), BundleUtil.getStringFromBundle("dataverse.release.authenticatedUsersOnly"));
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

    }

    private String releaseDataset(boolean minor) {
        if (session.getUser() instanceof AuthenticatedUser) {
            try {
                final PublishDatasetResult result = commandEngine.submit(
                        new PublishDatasetCommand(dataset, dvRequestService.getDataverseRequest(), minor)
                );
                dataset = result.getDataset();
                // Sucessfully executing PublishDatasetCommand does not guarantee that the dataset 
                // has been published. If a publishing workflow is configured, this may have sent the 
                // dataset into a workflow limbo, potentially waiting for a third party system to complete 
                // the process. So it may be premature to show the "success" message at this point. 

                if (result.isCompleted()) {
                    JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.publishSuccess"));
                } else {
                    JH.addMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.locked.message"), BundleUtil.getStringFromBundle("dataset.locked.message.details"));
                }

            } catch (CommandException ex) {
                JsfHelper.addFlashErrorMessage(ex.getLocalizedMessage());
                logger.severe(ex.getMessage());
            } catch (NoDatasetFilesException ex){
                JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.publish.error.noFiles"));
                logger.log(Level.SEVERE,"", ex);
            }

        } else {
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.only.authenticatedUsers"));
        }
        return returnToDatasetOnly();
    }

    @Deprecated
    public String registerDataset() {
        try {
            UpdateDatasetVersionCommand cmd = new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest());
            cmd.setValidateLenient(true);
            dataset = commandEngine.submit(cmd);
        } catch (CommandException ex) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, BundleUtil.getStringFromBundle("dataset.registration.failed"), " - " + ex.toString()));
            logger.severe(ex.getMessage());
        }
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.registered"), BundleUtil.getStringFromBundle("dataset.registered.msg"));
        FacesContext.getCurrentInstance().addMessage(null, message);
        return returnToDatasetOnly();
    }

    public String updateCurrentVersion() {
        /*
         * Note: The code here mirrors that in the
         * edu.harvard.iq.dataverse.api.Datasets:publishDataset method (case
         * "updatecurrent"). Any changes to the core logic (i.e. beyond updating the
         * messaging about results) should be applied to the code there as well.
         */
        String errorMsg = null;
        String successMsg = BundleUtil.getStringFromBundle("datasetversion.update.success");
        try {
            CuratePublishedDatasetVersionCommand cmd = new CuratePublishedDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest());
            dataset = commandEngine.submit(cmd);
            // If configured, and currently published version is archived, try to update archive copy as well
            DatasetVersion updateVersion = dataset.getLatestVersion();
            if (updateVersion.getArchivalCopyLocation() != null) {
                String className = settingsService.getValueForKey(SettingsServiceBean.Key.ArchiverClassName);
                AbstractSubmitToArchiveCommand archiveCommand = ArchiverUtil.createSubmitToArchiveCommand(className, dvRequestService.getDataverseRequest(), updateVersion);
                if (archiveCommand != null) {
                    // Delete the record of any existing copy since it is now out of date/incorrect
                    updateVersion.setArchivalCopyLocation(null);
                    /*
                     * Then try to generate and submit an archival copy. Note that running this
                     * command within the CuratePublishedDatasetVersionCommand was causing an error:
                     * "The attribute [id] of class
                     * [edu.harvard.iq.dataverse.DatasetFieldCompoundValue] is mapped to a primary
                     * key column in the database. Updates are not allowed." To avoid that, and to
                     * simplify reporting back to the GUI whether this optional step succeeded, I've
                     * pulled this out as a separate submit().
                     */
                    try {
                        updateVersion = commandEngine.submit(archiveCommand);
                        if (updateVersion.getArchivalCopyLocation() != null) {
                            successMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.success");
                        } else {
                            errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure");
                        }
                    } catch (CommandException ex) {
                        errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.archive.failure") + " - " + ex.toString();
                        logger.severe(ex.getMessage());
                    }
                }
            }
        } catch (CommandException ex) {
            errorMsg = BundleUtil.getStringFromBundle("datasetversion.update.failure") + " - " + ex.toString();
            logger.severe(ex.getMessage());
        }
        if (errorMsg != null) {
            JsfHelper.addFlashErrorMessage(errorMsg);
        } else {
            JsfHelper.addFlashSuccessMessage(successMsg);
        }
        return returnToDatasetOnly();
    }


    public void refresh(ActionEvent e) {
        refresh();
    }

    public void refresh() {
        logger.fine("refreshing");

        //dataset = datasetDao.find(dataset.getId());
        dataset = null;

        logger.fine("refreshing working version");

        DatasetVersionServiceBean.RetrieveDatasetVersionResponse retrieveDatasetVersionResponse = null;

        if (persistentId != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByPersistentId(persistentId, version);
            dataset = datasetDao.findByGlobalId(persistentId);
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        } else if (versionId != null) {
            retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionByVersionId(versionId);
        } else if (dataset.getId() != null) {
            //retrieveDatasetVersionResponse = datasetVersionService.retrieveDatasetVersionById(dataset.getId(), version);
            dataset = datasetDao.find(dataset.getId());
            retrieveDatasetVersionResponse = datasetVersionService.selectRequestedVersion(dataset.getVersions(), version);
        }

        if (retrieveDatasetVersionResponse == null) {
            // TODO: 
            // should probably redirect to the 404 page, if we can't find 
            // this version anymore. 
            // -- L.A. 4.2.3 
            return;
        }
        this.workingVersion = retrieveDatasetVersionResponse.getDatasetVersion();

        if (this.workingVersion == null) {
            // TODO: 
            // same as the above

            return;
        }


        if (dataset == null) {
            // this would be the case if we were retrieving the version by 
            // the versionId, above.
            this.dataset = this.workingVersion.getDataset();
        }

        displayCitation = dataset.getCitation(true, workingVersion);
        stateChanged = false;
        metadataTab.updateDatasetLockState(isLocked());
        datasetFilesTab.refresh();
    }

    public String deleteDataset() {

        DestroyDatasetCommand cmd;
        boolean deleteCommandSuccess = false;
        Map<Long, String> deleteStorageLocations = datafileService.getPhysicalFilesToDelete(dataset);

        try {
            cmd = new DestroyDatasetCommand(dataset, dvRequestService.getDataverseRequest());
            commandEngine.submit(cmd);
            deleteCommandSuccess = true;
            /* - need to figure out what to do 
             Update notification in Delete Dataset Method
             for (UserNotification und : userNotificationService.findByDvObject(dataset.getId())){
             userNotificationService.delete(und);
             } */
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }

        if (deleteCommandSuccess) {
            datafileService.finalizeFileDeletes(deleteStorageLocations);
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.deleteSuccess"));
        }

        return "/dataverse.xhtml?alias=" + dataset.getOwner().getAlias() + "&faces-redirect=true";
    }

    public String deleteDatasetVersion() {
        DeleteDatasetVersionCommand cmd;
        try {
            cmd = new DeleteDatasetVersionCommand(dvRequestService.getDataverseRequest(), dataset);
            commandEngine.submit(cmd);
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("datasetVersion.message.deleteSuccess"));
        } catch (CommandException ex) {
            JH.addMessage(FacesMessage.SEVERITY_FATAL, BundleUtil.getStringFromBundle("dataset.message.deleteFailure"));
            logger.severe(ex.getMessage());
        }

        return returnToDatasetOnly();
    }

    private Dataverse selectedDataverseForLinking;

    public Dataverse getSelectedDataverseForLinking() {
        return selectedDataverseForLinking;
    }

    public void setSelectedDataverseForLinking(Dataverse sdvfl) {
        this.selectedDataverseForLinking = sdvfl;
    }


    private List<String> getSuccessMessageArguments() {
        List<String> arguments = new ArrayList<>();
        String dataverseString = "";
        arguments.add(StringEscapeUtils.escapeHtml(dataset.getDisplayName()));
        dataverseString += " <a href=\"/dataverse/" + selectedDataverseForLinking.getAlias() + "\">" + StringEscapeUtils.escapeHtml(selectedDataverseForLinking.getDisplayName()) + "</a>";
        arguments.add(dataverseString);
        return arguments;
    }


    public void saveLinkingDataverses(ActionEvent evt) {

        if (saveLink(selectedDataverseForLinking)) {
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.message.linkSuccess", getSuccessMessageArguments()));
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.notlinked"), linkingDataverseErrorMessage);
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

    }

    private String linkingDataverseErrorMessage = "";


    private UIInput selectedLinkingDataverseMenu;

    public UIInput getSelectedDataverseMenu() {
        return selectedLinkingDataverseMenu;
    }

    public void setSelectedDataverseMenu(UIInput selectedDataverseMenu) {
        this.selectedLinkingDataverseMenu = selectedDataverseMenu;
    }


    private Boolean saveLink(Dataverse dataverse) {
        boolean retVal = true;
        if (readOnly) {
            // Pass a "real", non-readonly dataset the the LinkDatasetCommand: 
            dataset = datasetDao.find(dataset.getId());
        }
        LinkDatasetCommand cmd = new LinkDatasetCommand(dvRequestService.getDataverseRequest(), dataverse, dataset);
        try {
            commandEngine.submit(cmd);
        } catch (CommandException ex) {
            String msg = "There was a problem linking this dataset to yours: " + ex;
            logger.severe(msg);
            msg = BundleUtil.getStringFromBundle("dataset.notlinked.msg") + ex;
            /**
             * @todo how do we get this message to show up in the GUI?
             */
            linkingDataverseErrorMessage = msg;
            retVal = false;
        }
        return retVal;
    }


    public List<Dataverse> completeLinkingDataverse(String query) {
        dataset = datasetDao.find(dataset.getId());
        if (session.getUser().isAuthenticated()) {
            return dataverseDao.filterDataversesForLinking(query, dvRequestService.getDataverseRequest(), dataset);
        } else {
            return null;
        }
    }

    private String returnToLatestVersion() {
        dataset = datasetDao.find(dataset.getId());
        workingVersion = dataset.getLatestVersion();
        if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
            workingVersion = dataset.getReleasedVersion();
        }
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=" + workingVersion.getFriendlyVersionNumber() + "&faces-redirect=true";
    }

    private String returnToDatasetOnly() {
        dataset = datasetDao.find(dataset.getId());
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&faces-redirect=true";
    }

    public String cancel() {
        return returnToLatestVersion();
    }

    public void refreshAllLocks() {

        logger.fine("checking all locks");
        if (isStillLockedForAnyReason()) {
            logger.fine("(still locked)");
        } else {

            logger.fine("no longer locked!");
            stateChanged = true;
            lockedFromEditsVar = null;
        }
    }

    public boolean isDatasetLockedInWorkflow() {
        return (dataset != null) && dataset.isLockedFor(DatasetLock.Reason.Workflow);
    }


    public boolean isStillLockedForAnyReason() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetDao.find(dataset.getId());
            if (testDataset != null && testDataset.getId() != null) {
                logger.log(Level.FINE, "checking lock status of dataset {0}", dataset.getId());
                return testDataset.getLocks().size() > 0;
            }
        }
        return false;
    }

    public boolean isLocked() {
        if (stateChanged) {
            return false;
        }

        if (dataset != null) {
            return dataset.isLocked();
        }
        return false;
    }

    public boolean isLockedForAnyReason() {
        if (dataset.getId() != null) {
            Dataset testDataset = datasetDao.find(dataset.getId());
            if (stateChanged) {
                return false;
            }

            if (testDataset != null) {
                return testDataset.getLocks().size() > 0;
            }
        }
        return false;
    }

    private Boolean lockedFromEditsVar;

    /**
     * Authors are not allowed to edit but curators are allowed - when Dataset is inReview
     * For all other locks edit should be locked for all editors.
     */
    public boolean isLockedFromEdits() {
        if (null == lockedFromEditsVar || stateChanged) {
            try {
                permissionService.checkEditDatasetLock(dataset, dvRequestService.getDataverseRequest(), new UpdateDatasetVersionCommand(dataset, dvRequestService.getDataverseRequest()));
                lockedFromEditsVar = false;
            } catch (IllegalCommandException ex) {
                lockedFromEditsVar = true;
            }
        }
        return lockedFromEditsVar;
    }

    public void setLocked(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }

    public void setLockedForAnyReason(boolean locked) {
        // empty method, so that we can use DatasetPage.locked in a hidden 
        // input on the page. 
    }

    public boolean isStateChanged() {
        return stateChanged;
    }

    public void setStateChanged(boolean stateChanged) {
        // empty method, so that we can use DatasetPage.stateChanged in a hidden 
        // input on the page. 
    }

    
    private boolean existReleasedVersion;

    public boolean isExistReleasedVersion() {
        return existReleasedVersion;
    }

    public void setExistReleasedVersion(boolean existReleasedVersion) {
        this.existReleasedVersion = existReleasedVersion;
    }

    private boolean resetExistRealeaseVersion() {

        for (DatasetVersion version : dataset.getVersions()) {
            if (version.isReleased() || version.isArchived()) {
                return true;
            }
        }
        return false;

    }


    public String getDatasetPublishCustomText() {
        return settingsService.getValueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomText);
    }

    public Boolean isDatasetPublishPopupCustomTextOnAllVersions() {
        return settingsService.isTrueForKey(SettingsServiceBean.Key.DatasetPublishPopupCustomTextOnAllVersions);
    }

    private PrivateUrl privateUrl;

    public PrivateUrl getPrivateUrl() {
        return privateUrl;
    }

    public void setPrivateUrl(PrivateUrl privateUrl) {
        this.privateUrl = privateUrl;
    }

    public void initPrivateUrlPopUp() {
        if (privateUrl != null) {
            setPrivateUrlJustCreatedToFalse();
        }
    }

    private boolean privateUrlWasJustCreated;

    public boolean isPrivateUrlWasJustCreated() {
        return privateUrlWasJustCreated;
    }

    public void setPrivateUrlJustCreatedToFalse() {
        privateUrlWasJustCreated = false;
    }

    public void createPrivateUrl() {
        try {
            PrivateUrl createdPrivateUrl = commandEngine.submit(new CreatePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = createdPrivateUrl;
            JH.addMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("dataset.privateurl.infoMessageAuthor", Arrays.asList(getPrivateUrlLink(privateUrl))));
            privateUrlWasJustCreated = true;
        } catch (CommandException ex) {
            String msg = BundleUtil.getStringFromBundle("dataset.privateurl.noPermToCreate", PrivateUrlUtil.getRequiredPermissions(ex));
            logger.info("Unable to create a Private URL for dataset id " + dataset.getId() + ". Message to user: " + msg + " Exception: " + ex);
            JsfHelper.addFlashErrorMessage(msg);
        }
    }

    public void disablePrivateUrl() {
        try {
            commandEngine.submit(new DeletePrivateUrlCommand(dvRequestService.getDataverseRequest(), dataset));
            privateUrl = null;
            JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.privateurl.disabledSuccess"));
        } catch (CommandException ex) {
            logger.info("CommandException caught calling DeletePrivateUrlCommand: " + ex);
        }
    }

    public String getPrivateUrlLink(PrivateUrl privateUrl) {
        return privateUrl.getLink();
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


    /**
     * dataset title
     *
     * @return title of workingVersion
     */
    public String getTitle() {
        assert (null != workingVersion);
        return workingVersion.getTitle();
    }

    /**
     * dataset description
     *
     * @return description of workingVersion
     */
    public String getDescription() {
        return workingVersion.getDescriptionPlainText();
    }

    /**
     * dataset authors
     *
     * @return list of author names
     */
    public List<String> getDatasetAuthors() {
        assert (workingVersion != null);
        return workingVersion.getDatasetAuthorNames();
    }

    /**
     * @return Comment written by dataset admin explaining why was the dataset "Returned to Author"
     */
    public String getReturnToAuthorReason() {
        return returnToAuthorReason;
    }

    /**
     *
     * @return current embargo date set on dataset or [TODAY] whichever is greater
     */
    public Date getCurrentEmbargoDate() {
        return currentEmbargoDate;
    }

    /**
     * publisher (aka - name of root dataverse)
     *
     * @return the publisher of the version
     */
    public String getPublisher() {
        assert (null != workingVersion);
        return workingVersion.getRootDataverseNameforCitation();
    }

    /**
     * this method returns the dataset fields to be shown in the dataset summary
     * on the dataset page.
     * It returns the default summary fields( subject, description, keywords, related publications and notes)
     * if the custom summary datafields has not been set, otherwise will set the custom fields set by the sysadmins
     *
     * @return the dataset fields to be shown in the dataset summary
     */
    public List<DatasetFieldsByType> getDatasetSummaryFields() {
        List<String> customFields = settingsService.getValueForKeyAsList(SettingsServiceBean.Key.CustomDatasetSummaryFields);

        return DatasetUtil.getDatasetSummaryFields(workingVersion, customFields);
    }

    public String getKeywordsDisplaySummary() {
        return StringUtils.join(workingVersion.getKeywords(), ", ");
    }

    public DatasetRelPublication getFirstRelPublication() {
        List<DatasetRelPublication> datasetRelPublications = workingVersion.getRelatedPublications();
        DatasetRelPublication firstRelPublication = datasetRelPublications.size() > 0 ? datasetRelPublications.get(0) : null;
        
        return firstRelPublication;
    }

    public String redirectToMetrics() {
        return "/metrics.xhtml?faces-redirect=true";
    }

    public boolean isSameTermsOfUseForAllFiles() {
        if (sameTermsOfUseForAllFiles != null) {
            return sameTermsOfUseForAllFiles;
        }
        if (workingVersion.getFileMetadatas().isEmpty()) {
            sameTermsOfUseForAllFiles = true;
            return sameTermsOfUseForAllFiles;
        }
        FileTermsOfUse firstTermsOfUse = workingVersion.getFileMetadatas().get(0).getTermsOfUse();

        for (FileMetadata fileMetadata : workingVersion.getFileMetadatas()) {
            if (!datafileService.isSameTermsOfUse(firstTermsOfUse, fileMetadata.getTermsOfUse())) {
                sameTermsOfUseForAllFiles = false;
                return sameTermsOfUseForAllFiles;
            }
        }

        sameTermsOfUseForAllFiles = true;
        return sameTermsOfUseForAllFiles;
    }

    public Optional<FileTermsOfUse> getTermsOfUseOfFirstFile() {
        if (workingVersion.getFileMetadatas().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(workingVersion.getFileMetadatas().get(0).getTermsOfUse());
    }

    public void setReturnToAuthorReason(String returnToAuthorReason) {
        this.returnToAuthorReason = returnToAuthorReason;
    }

    public void initCurrentEmbargo() {
        currentEmbargoDate = dataset.getEmbargoDate().getOrNull();
    }

    public void setCurrentEmbargoDate(Date currentEmbargoDate) {
        this.currentEmbargoDate = currentEmbargoDate;
    }

    public Date getTomorrowsDate() {
        return Date.from(Instant.now().truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS));
    }

    public void updateEmbargoDate() {
        Try.of(() -> datasetService.setDatasetEmbargoDate(dataset, currentEmbargoDate))
                .onSuccess(ds -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.embargo.save.successMessage")))
                .onFailure(ds -> JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.embargo.save.failureMessage")));
    }

    public void liftEmbargo() {
        Try.of(() -> datasetService.liftDatasetEmbargoDate(dataset))
                .onSuccess(ds -> currentEmbargoDate = null)
                .onSuccess(ds -> JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("dataset.embargo.lift.successMessage")))
                .onFailure(ds -> JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.embargo.lift.failureMessage")));
    }

    public String getCurrentEmbargoDateForDisplay() {
        SimpleDateFormat format = new SimpleDateFormat(settingsService.getValueForKey(SettingsServiceBean.Key.DefaultDateFormat));
        return currentEmbargoDate != null ? format.format(currentEmbargoDate) : "";
    }

    public String getMaximumEmbargoDateForDisplay() {
        SimpleDateFormat format = new SimpleDateFormat(settingsService.getValueForKey(SettingsServiceBean.Key.DefaultDateFormat));
        return getMaximumEmbargoDate().isDefined() ? format.format(getMaximumEmbargoDate().get()) : "";
    }

    public void validateEmbargoDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        validateVersusMinimumDate(context, toValidate, embargoDate);
        validateVersusMaximumDate(context, toValidate, embargoDate);
    }

    public boolean isUserUnderEmbargo() {
        return dataset.hasActiveEmbargo() && !permissionsWrapper.canViewUnpublishedDataset(dvRequestService.getDataverseRequest(), dataset);
    }

    public boolean isUserAbleToSetOrUpdateEmbargo() {
        return !dataset.hasEverBeenPublished() || session.getUser().isSuperuser();
    }

    public boolean isUserAbleToLiftEmbargo() {
        return dataset.hasActiveEmbargo();
    }

    public int getMaximumEmbargoLength() {
        return settingsService.getValueForKeyAsInt(SettingsServiceBean.Key.MaximumEmbargoLength);
    }

    public boolean isMaximumEmbargoLengthSet() {
        return getMaximumEmbargoLength() > 0;
    }

    public Option<Date> getMaximumEmbargoDate() {
        if(isMaximumEmbargoLengthSet()) {
            return Option.of(Date.from(Instant
                    .now().atOffset(ZoneOffset.UTC)
                    .plus(settingsService.getValueForKeyAsLong(SettingsServiceBean.Key.MaximumEmbargoLength), ChronoUnit.MONTHS)
                    .toInstant()));
        }
        return Option.none();
    }

    // -------------------- PRIVATE ---------------------
    private void validateVersusMaximumDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        if(isMaximumEmbargoLengthSet() &&
                !Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isAfter(getMaximumEmbargoDate().get().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("dataset.embargo.validate.max.failureMessage", getMaximumEmbargoDateForDisplay()), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    private void validateVersusMinimumDate(FacesContext context, UIComponent toValidate, Object embargoDate) {
        if(!Objects.isNull(embargoDate) &&
                ((Date) embargoDate).toInstant().isBefore(getTomorrowsDate().toInstant())) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.embargo.validate.min.failureMessage"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }
}
