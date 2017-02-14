package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
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
    private DatasetThumbnail datasetThumbnail;
    private DataFile datasetFileThumbnailToSwitchTo;
    private String randomlySelectedThumbnail;

    @Inject
    PermissionsWrapper permissionsWrapper;
    private boolean considerDatasetLogoAsCandidate = false;

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
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
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

    public String getRandomlySelectedThumbnail() {
        return randomlySelectedThumbnail;
    }

    public boolean isDefaultIconShouldBeShown() {
        if (datasetThumbnail == null) {
            if (isUnspecifiedThumbnailFromDatafileShouldBeShown()) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean isUnspecifiedThumbnailFromDatafileShouldBeShown() {
        String thumbnailFromUnspecifiedDataFile = findThumnailThatMightBeShowingOnTheSearchCards();
        if (thumbnailFromUnspecifiedDataFile == null) {
            return false;
        } else if (datasetThumbnail == null) {
            return true;
        } else {
            return false;
        }
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
        DatasetUtil.writeDatasetLogoToDisk(dataset, file);
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        datasetThumbnails = DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate);
    }

    public void deleteDatasetLogo() {
        logger.info("deleteDatasetLogo clicked");
        boolean deleted = DatasetUtil.deleteDatasetLogo(dataset);
        logger.info("deleteDatasetLogo returned with " + deleted);
        datasetThumbnails = DatasetUtil.getThumbnailCandidates(dataset, considerDatasetLogoAsCandidate);
        logger.info("number of thumbnail candidates update to " + datasetThumbnails.size());
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
    }

    public void useGenericThumbnail() {
        dataset.setUseGenericThumbnail(true);
        datasetService.merge(dataset);
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
    }

    public void stopUsingAnyDatasetFileAsThumbnail() {
        logger.info("stopUsingAnyDatasetFileAsThumbnail clicked... this is new functionality that hasn't been implemented yet.");
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        DataFile dataFileAsThumbnailBefore = dataset.getThumbnailFile();
        logger.info("dataFileAsThumbnailBefore: " + dataFileAsThumbnailBefore);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        datasetService.merge(dataset);
        DataFile dataFileAsThumbnailAfter = dataset.getThumbnailFile();
        logger.info("dataFileAsThumbnailAfter: " + dataFileAsThumbnailAfter);
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
    }

    public void letDataversePickThumbnail() {
        dataset.setUseGenericThumbnail(false);
        datasetService.merge(dataset);
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
    }

    public void setDataFileAsThumbnail() {
        if (datasetFileThumbnailToSwitchTo != null) {
            deleteDatasetLogo();
            dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
            dataset.setUseGenericThumbnail(false);
            datasetService.merge(dataset);
            datasetThumbnail = DatasetUtil.getThumbnail(dataset);
        }
    }

    public String save() {
//        Dataset merged = datasetService.merge(dataset);
        logger.info("Save clicked. Alternative thumbnail is now... FIXME: persist selection somehow");
        JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("dataset.thumbnailsAndWidget.success"));
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

    public String cancel() {
        logger.fine("cancel clicked");
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

    // copied and modified from SearchIncludeFragment.java
    public String findThumnailThatMightBeShowingOnTheSearchCards() {
        Long randomThumbnail = datasetVersionService.getThumbnailByVersionId(dataset.getLatestVersion().getId());
        if (randomThumbnail != null) {
            DataFile thumbnailImageFile = null;
            thumbnailImageFile = dataFileService.findCheapAndEasy(randomThumbnail);
            if (dataFileService.isThumbnailAvailable(thumbnailImageFile)) {
                randomlySelectedThumbnail = ImageThumbConverter.getImageThumbAsBase64(
                        thumbnailImageFile,
                        ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                return randomlySelectedThumbnail;
            }
        }
        return null;
    }

}
