/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import edu.harvard.iq.dataverse.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author skraffmi
 */
@ViewScoped
@Named("DatasetWidgetsPage")
public class DatasetWidgetsPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetWidgetsPage.class.getCanonicalName());

    @EJB
    DatasetServiceBean datasetService;

    private Long datasetId;
    private Dataset dataset;

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
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalId()  +  "&faces-redirect=true";
    }

}
