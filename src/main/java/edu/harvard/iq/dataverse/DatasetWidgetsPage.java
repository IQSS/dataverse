package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

@ViewScoped
@Named("DatasetWidgetsPage")
public class DatasetWidgetsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetWidgetsPage.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    @EJB
    DatasetVersionServiceBean datasetVersionService;

    @EJB
    DataFileServiceBean dataFileService;

    @EJB
    EjbDataverseEngine commandEngine;

    @Inject
    DataverseRequestServiceBean dvRequestService;

    private Long datasetId;
    private Dataset dataset;
    private List<DatasetThumbnail> datasetThumbnails;
    /**
     * A preview image of either the current or (potentially unsaved) future
     * thumbnail.
     */
    private DatasetThumbnail datasetThumbnail;
    private DataFile datasetFileThumbnailToSwitchTo;
    private Long datasetFileIdToSwitchThumbnailTo;

    @Inject
    PermissionsWrapper permissionsWrapper;
    private final boolean considerDatasetLogoAsCandidate = false;
    private String stagingFilePath;
    UpdateDatasetThumbnailCommand.UserIntent thumbnailUpdateIntent;

    public String init() {
        if (datasetId == null || datasetId.intValue() <= 0) {
            return permissionsWrapper.notFound();
        }
        dataset = datasetService.find(datasetId);
        if (dataset == null) {
            return permissionsWrapper.notFound();
        }
        /**
         * @todo Consider changing this to "can issue"
         * UpdateDatasetThumbnailCommand since it's really the only update you
         * can do from this page.
         */
        if (!permissionsWrapper.canIssueCommand(dataset, UpdateDatasetCommand.class)) {
            return permissionsWrapper.notAuthorized();
        }
        datasetThumbnails = DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate);
        datasetThumbnail = DatasetUtil.getThumbnail(dataset, datasetVersionService, dataFileService);
        if (datasetThumbnail != null) {
            DataFile dataFile = datasetThumbnail.getDataFile();
            if (dataFile != null) {
                datasetFileThumbnailToSwitchTo = dataFile;
            }
        }
        return null;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public Dataset getDataset() {
        return dataset;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public List<DatasetThumbnail> getDatasetThumbnails() {
        return datasetThumbnails;
    }

    public DatasetThumbnail getDatasetThumbnail() {
        return datasetThumbnail;
    }

    public void setDatasetThumbnail(DatasetThumbnail datasetThumbnail) {
        this.datasetThumbnail = datasetThumbnail;
    }

    public DataFile getDatasetFileThumbnailToSwitchTo() {
        return datasetFileThumbnailToSwitchTo;
    }

    public void setDatasetFileThumbnailToSwitchTo(DataFile datasetFileThumbnailToSwitchTo) {
        this.datasetFileThumbnailToSwitchTo = datasetFileThumbnailToSwitchTo;
    }

    public void setDataFileAsThumbnail() {
        logger.fine("setDataFileAsThumbnail clicked");
        thumbnailUpdateIntent = UpdateDatasetThumbnailCommand.UserIntent.userHasSelectedDataFileAsThumbnail;
        if (datasetFileThumbnailToSwitchTo != null) {
            String base64image = ImageThumbConverter.getImageThumbAsBase64(datasetFileThumbnailToSwitchTo, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            datasetThumbnail = new DatasetThumbnail(base64image, datasetFileThumbnailToSwitchTo);
            datasetFileIdToSwitchThumbnailTo = datasetFileThumbnailToSwitchTo.getId();
        }
    }

    public void flagDatasetThumbnailForRemoval() {
        logger.fine("flagDatasetThumbnailForRemoval");
        thumbnailUpdateIntent = UpdateDatasetThumbnailCommand.UserIntent.userWantsToRemoveThumbnail;
        datasetFileThumbnailToSwitchTo = null;
        datasetThumbnail = null;
        datasetFileIdToSwitchThumbnailTo = null;
    }

    public void handleImageFileUpload(FileUploadEvent event) {
        logger.fine("handleImageFileUpload clicked");
        thumbnailUpdateIntent = UpdateDatasetThumbnailCommand.UserIntent.userWantsToUseNonDatasetFile;
        UploadedFile uploadedFile = event.getFile();
        InputStream fileInputStream = null;
        try {
            fileInputStream = uploadedFile.getInputstream();
        } catch (IOException ex) {
            Logger.getLogger(DatasetWidgetsPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        File file = null;
        try {
            file = FileUtil.inputStreamToFile(fileInputStream);
        } catch (IOException ex) {
            Logger.getLogger(DatasetWidgetsPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        JsonObjectBuilder resultFromAttemptToStageDatasetLogoObjectBuilder = datasetService.writeDatasetLogoToStagingArea(dataset, file);
        JsonObject resultFromAttemptToStageDatasetLogoObject = resultFromAttemptToStageDatasetLogoObjectBuilder.build();
        stagingFilePath = resultFromAttemptToStageDatasetLogoObject.getString(DatasetUtil.stagingFilePathKey);
        String base64image = null;
        try {
            base64image = FileUtil.rescaleImage(file);
            datasetThumbnail = new DatasetThumbnail(base64image, datasetFileThumbnailToSwitchTo);
        } catch (IOException ex) {
            Logger.getLogger(DatasetWidgetsPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        datasetFileIdToSwitchThumbnailTo = null;
    }

    public String save() {
        logger.fine("save clicked");
        try {
            DatasetThumbnail datasetThumbnailFromCommand = commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(), dataset, thumbnailUpdateIntent, datasetFileIdToSwitchThumbnailTo, null, stagingFilePath));
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.thumbnailsAndWidget.success"));
            return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
        } catch (CommandException ex) {
            String error = ex.getLocalizedMessage();
            logger.info(error);
            JsfHelper.addErrorMessage(error);
            return null;
        }
    }

    public String cancel() {
        logger.fine("cancel clicked");
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

}
