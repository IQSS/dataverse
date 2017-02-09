package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataaccess.ImageThumbConverter;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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

    private Long datasetId;
    private Dataset dataset;
    private List<DatasetThumbnail> datasetThumbnails;
    private DatasetThumbnail datasetThumbnail;

    @Inject
    PermissionsWrapper permissionsWrapper;

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
        datasetThumbnails = new ArrayList<>();
        String altThumbnail = dataset.getAltThumbnail();
        if (altThumbnail != null) {
            DatasetThumbnail datasetThumbnail = new DatasetThumbnail(BundleUtil.getStringFromBundle("dataset.thumbnailsAndWidget.thumbnailImage.nonDatasetFile"), altThumbnail);
            datasetThumbnails.add(datasetThumbnail);
        }
        for (FileMetadata fileMetadata : dataset.getLatestVersion().getFileMetadatas()) {
            DataFile dataFile = fileMetadata.getDataFile();
            if (dataFile != null && dataFile.isImage()) {
                String imageSourceBase64 = ImageThumbConverter.getImageThumbAsBase64(dataFile, ImageThumbConverter.DEFAULT_CARDIMAGE_SIZE);
                DatasetThumbnail datasetThumbnail = new DatasetThumbnail(fileMetadata.getLabel(), imageSourceBase64);
                datasetThumbnails.add(datasetThumbnail);
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

    public void handleImageFileUpload(FileUploadEvent event) {
        logger.fine("handleImageFileUpload clicked");
        UploadedFile uploadedFile = event.getFile();
        try {
            InputStream fileInputStream = uploadedFile.getInputstream();
            File file = FileUtil.inputStreamToFile(fileInputStream);
            String rescaledThumbnail = FileUtil.rescaleImage(file);
            logger.fine("rescaledThumbnail: " + rescaledThumbnail);
            dataset.setAltThumbnail(rescaledThumbnail);
        } catch (IOException ex) {
            logger.info("Problem uploading thumbnail to dataset id " + dataset.getId() + ": " + ex);
        }
    }

    public void removeThumbnail() {
        logger.fine("removeThumbnail clicked");
        dataset.setAltThumbnail(null);
    }

    public void save() {
        Dataset merged = datasetService.merge(dataset);
        logger.fine("Save clicked. Alternative thumbnail is now: " + merged.getAltThumbnail());
    }

    public String cancel() {
        logger.fine("cancel clicked");
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

}
