package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import static edu.harvard.iq.dataverse.dataset.DatasetUtil.datasetLogoFilenameStaging;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
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

    private Long datasetId;
    private Dataset dataset;
    private List<DatasetThumbnail> datasetThumbnails;
    /**
     * A preview image of either the current or (potentially unsaved) future
     * thumbnail.
     */
    private DatasetThumbnail datasetThumbnail;
    private DataFile datasetFileThumbnailToSwitchTo;
    private boolean userWantsToRemoveThumbnail;
    private boolean userHasSelectedDataFileAsThumbnail;

    @Inject
    PermissionsWrapper permissionsWrapper;
    private final boolean considerDatasetLogoAsCandidate = false;
    private JsonObjectBuilder resultFromAttemptToStageDatasetLogoObjectBuilder;

    public String init() {
        if (datasetId == null || datasetId.intValue() <= 0) {
            return permissionsWrapper.notFound();
        }
        dataset = datasetService.find(datasetId);
        if (dataset == null) {
            return permissionsWrapper.notFound();
        }
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

    public boolean isUserWantsToRemoveThumbnail() {
        return userWantsToRemoveThumbnail;
    }

    public void handleImageFileUpload(FileUploadEvent event) {
        logger.fine("handleImageFileUpload clicked");
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
        resultFromAttemptToStageDatasetLogoObjectBuilder = datasetService.writeDatasetLogoToStagingArea(dataset, file);
        String base64image = null;
        try {
            base64image = FileUtil.rescaleImage(file);
            datasetThumbnail = new DatasetThumbnail(base64image, datasetFileThumbnailToSwitchTo);
        } catch (IOException ex) {
            Logger.getLogger(DatasetWidgetsPage.class.getName()).log(Level.SEVERE, null, ex);
        }
        userWantsToRemoveThumbnail = false;
        userHasSelectedDataFileAsThumbnail = false;
    }

    public void deleteDatasetLogo() {
        logger.fine("deleteDatasetLogo");
        userWantsToRemoveThumbnail = true;
        userHasSelectedDataFileAsThumbnail = false;
        datasetFileThumbnailToSwitchTo = null;
        datasetThumbnail = null;
    }

    public void stopUsingAnyDatasetFileAsThumbnail() {
        logger.fine("stopUsingAnyDatasetFileAsThumbnail clicked");
        userWantsToRemoveThumbnail = true;
        userHasSelectedDataFileAsThumbnail = false;
        datasetThumbnail = null;
        datasetFileThumbnailToSwitchTo = null;
    }

    public void setDataFileAsThumbnail() {
        logger.fine("setDataFileAsThumbnail clicked");
        if (datasetFileThumbnailToSwitchTo != null) {
            String base64image = ImageThumbConverter.getImageThumbAsBase64(datasetFileThumbnailToSwitchTo, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
            datasetThumbnail = new DatasetThumbnail(base64image, datasetFileThumbnailToSwitchTo);
            userWantsToRemoveThumbnail = false;
            userHasSelectedDataFileAsThumbnail = true;
        }
    }

    public String save() {
        logger.fine("save clicked");
        if (resultFromAttemptToStageDatasetLogoObjectBuilder != null) {
            JsonObject resultFromAttemptToStageDatasetLogoObject = resultFromAttemptToStageDatasetLogoObjectBuilder.build();
            String stagingFilePath = resultFromAttemptToStageDatasetLogoObject.getString(DatasetUtil.stagingFilePathKey);
            File stagingFile = new File(stagingFilePath);
            if (stagingFile.exists()) {
                logger.fine("Copying dataset logo from staging area to final location.");
                dataset = datasetService.moveDatasetLogoFromStagingToFinal(dataset, stagingFile.getAbsolutePath());
            } else {
                logger.fine("No dataset logo in staging area to copy.");
            }
        }
        if (userHasSelectedDataFileAsThumbnail) {
            logger.fine("switching thumbnail to DataFile id " + datasetFileThumbnailToSwitchTo.getId());
            dataset = datasetService.setDataFileAsThumbnail(dataset, datasetFileThumbnailToSwitchTo);
        } else {
            logger.fine("not switching to a DataFile thumbnail");
        }
        if (userWantsToRemoveThumbnail) {
            logger.fine("user doesn't want a thumbnail, blowing them away");
            dataset = datasetService.stopUsingAnyDatasetFileAsThumbnail(dataset);
            /**
             * @todo Delete staging file too?
             */
            dataset = datasetService.deleteDatasetLogo(dataset);
        } else {
            logger.fine("user doesn't want to remove the thumbnail");
        }
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.thumbnailsAndWidget.success"));
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

    public String cancel() {
        logger.fine("cancel clicked");
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

}
