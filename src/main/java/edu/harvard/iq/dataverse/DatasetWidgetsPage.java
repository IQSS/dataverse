/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetCommand;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.FileUploadEvent;

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
        logger.info("handleImageFileUpload clicked");
    }

    public void removeLogo() {
        logger.info("remove clicked");
    }

    public void save() {
        logger.info("save clicked");
    }

    public void cancel() {
        logger.info("cancel clicked");
    }

}
