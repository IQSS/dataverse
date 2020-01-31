package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import javax.faces.view.ViewScoped;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.harvard.iq.dataverse.util.JsfHelper.JH;

@ViewScoped
@Named("editDatasetMetadataPage")
public class EditDatasetMetadataPage implements Serializable {

    private static final Logger logger = Logger.getLogger(EditDatasetMetadataPage.class.getCanonicalName());

    @EJB
    private DatasetDao datasetDao;
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseRequestServiceBean dvRequestService;
    @EJB
    private EjbDataverseEngine commandEngine;
    @Inject
    private DatasetFieldsInitializer datasetFieldsInitializer;

    private Long datasetId;
    private String persistentId;

    private Dataset dataset;
    private DatasetVersion workingVersion;
    private Map<MetadataBlock, List<DatasetField>> metadataBlocksForEdit;

    // -------------------- GETTERS --------------------

    public Dataset getDataset() {
        return dataset;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public String getPersistentId() {
        return persistentId;
    }

    public DatasetVersion getWorkingVersion() {
        return workingVersion;
    }

    public Map<MetadataBlock, List<DatasetField>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    // -------------------- LOGIC --------------------

    public String init() {

        if (persistentId != null) {
            dataset = datasetDao.findByGlobalId(persistentId);
        } else if (datasetId != null) {
            dataset = datasetDao.find(datasetId);
        }

        if (dataset == null) {
            return permissionsWrapper.notFound();
        }


        // Check permisisons
        if (!permissionsWrapper.canUpdateDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dvRequestService.getDataverseRequest(), dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        workingVersion = dataset.getEditVersion();

        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(workingVersion.getDatasetFields(),
                dataset.getOwner().getMetadataBlockRootDataverse());
        workingVersion.setDatasetFields(datasetFields);
        metadataBlocksForEdit = datasetFieldsInitializer.groupAndUpdateEmptyAndRequiredFlag(datasetFields);

        JH.addMessage(FacesMessage.SEVERITY_INFO,
                BundleUtil.getStringFromBundle("dataset.message.editMetadata.label"),
                BundleUtil.getStringFromBundle("dataset.message.editMetadata.message"));


        return StringUtils.EMPTY;
    }

    public String save() {

        Try<Dataset> updateDataset = Try.of(() -> datasetVersionService.updateDatasetVersion(workingVersion, true))
                .onFailure(this::handleUpdateDatasetExceptions);

        if (updateDataset.isFailure()){
            return "";
        }

        return returnToLatestVersion();
    }

    public String cancel() {
        return returnToLatestVersion();
    }

    // -------------------- PRIVATE --------------------

    private String returnToLatestVersion() {
        dataset = datasetDao.find(dataset.getId());
        workingVersion = dataset.getLatestVersion();
        if (workingVersion.isDeaccessioned() && dataset.getReleasedVersion() != null) {
            workingVersion = dataset.getReleasedVersion();
        }
        return "/dataset.xhtml?persistentId=" + dataset.getGlobalIdString() + "&version=" + workingVersion.getFriendlyVersionNumber() + "&faces-redirect=true";
    }

    private void handleUpdateDatasetExceptions(Throwable throwable) {
        if (throwable instanceof EJBException) {
            throwable = throwable.getCause();
        }

        if (throwable instanceof ValidationException) {
            JH.addMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("dataset.message.validationError"));

        } else if (throwable instanceof CommandException) {

            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + throwable.getMessage(), throwable);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.metadataFailure"));
        } else {

            logger.log(Level.SEVERE, "Couldn't edit dataset metadata: " + throwable.getMessage(), throwable);
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("dataset.message.metadataFailure"));
        }
    }

    // -------------------- SETTERS --------------------

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

}
