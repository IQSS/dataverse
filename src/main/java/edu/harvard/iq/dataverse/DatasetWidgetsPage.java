package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.dataset.DatasetThumbnail;
import edu.harvard.iq.dataverse.dataset.DatasetUtil;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.util.FileUtil;
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

    private Long datasetId;
    private Dataset dataset;
    private List<DatasetThumbnail> datasetThumbnails;
    private DatasetThumbnail datasetThumbnail;
    /**
     * @todo Remove this if we can't get it working.
     */
    private DatasetThumbnail datasetFileThumbnailToSwitchTo;

    public DatasetThumbnail getDatasetFileThumbnailToSwitchTo() {
        return datasetFileThumbnailToSwitchTo;
    }

    public void setDatasetFileThumbnailToSwitchTo(DatasetThumbnail datasetFileThumbnailToSwitchTo) {
        this.datasetFileThumbnailToSwitchTo = datasetFileThumbnailToSwitchTo;
    }

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

    public void stopUsingAnyDatasetFileAsThumbnail() {
        logger.info("stopUsingAnyDatasetFileAsThumbnail clicked... this is new functionality that hasn't been implemented yet.");
        /**
         * @todo Implement stopUsingAnyDatasetFileAsThumbnail!
         */
        datasetThumbnail = DatasetUtil.getThumbnail(dataset);
    }

    public void setDataFileAsThumbnail() {
        logger.info("setDataFileAsThumbnail clicked.");
        if (datasetThumbnail != null) {
            logger.info("in setDataFileAsThumbnail, datasetThumbnail filename: " + datasetThumbnail.getFilename());
        }
        if (datasetFileThumbnailToSwitchTo != null) {
            logger.info("in setDataFileAsThumbnail, datasetFileThumbnailToSwitchTo filename: " + datasetFileThumbnailToSwitchTo.getFilename());
            // then, set the dataset thumbnail to this one
        } else {
            /**
             * @todo Need JSF help to get this working.
             */
            logger.info("Why is datasetFileThumbnailToSwitchTo null? How do I pass the selection to this method?");
        }
    }

    public void save() {
//        Dataset merged = datasetService.merge(dataset);
        logger.info("Save clicked. Alternative thumbnail is now... FIXME: persist selection somehow");
    }

    public String cancel() {
        logger.fine("cancel clicked");
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId() + "&faces-redirect=true";
    }

}
