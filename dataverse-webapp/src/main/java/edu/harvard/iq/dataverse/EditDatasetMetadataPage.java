package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.dataset.DatasetFieldsInitializer;
import edu.harvard.iq.dataverse.dataset.datasetversion.DatasetVersionServiceBean;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRenderer;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRendererManager;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.importer.metadata.ImporterRegistry;
import edu.harvard.iq.dataverse.importer.metadata.MetadataImporter;
import edu.harvard.iq.dataverse.importers.ui.ImporterForm;
import edu.harvard.iq.dataverse.importers.ui.ImportersForView;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsByType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.ValidationException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("editDatasetMetadataPage")
public class EditDatasetMetadataPage implements Serializable {

    private static final Logger logger = Logger.getLogger(EditDatasetMetadataPage.class.getCanonicalName());

    @Inject
    private ImporterRegistry importerRegistry;

    @EJB
    private DatasetDao datasetDao;
    @EJB
    private DatasetVersionServiceBean datasetVersionService;
    @EJB
    private InputFieldRendererManager inputFieldRendererManager;
    @Inject
    private PermissionsWrapper permissionsWrapper;
    @Inject
    private DataverseSession session;

    @Inject
    private DatasetFieldsInitializer datasetFieldsInitializer;

    private Long datasetId;
    private String persistentId;

    private Dataset dataset;
    private DatasetVersion workingVersion;
    private Map<MetadataBlock, List<DatasetFieldsByType>> metadataBlocksForEdit;
    private Map<DatasetFieldType, InputFieldRenderer> inputRenderersByFieldType = new HashMap<>();

    private ImportersForView importers;
    private MetadataImporter selectedImporter;
    private ImporterForm importerForm;

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

    public Map<MetadataBlock, List<DatasetFieldsByType>> getMetadataBlocksForEdit() {
        return metadataBlocksForEdit;
    }

    public Map<DatasetFieldType, InputFieldRenderer> getInputRenderersByFieldType() {
        return inputRenderersByFieldType;
    }

    public ImportersForView getImporters() {
        return importers;
    }

    public MetadataImporter getSelectedImporter() {
        return selectedImporter;
    }

    public ImporterForm getImporterForm() {
        return importerForm;
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
        if (!permissionsWrapper.canCurrentUserUpdateDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }
        if (datasetDao.isInReview(dataset) && !permissionsWrapper.canUpdateAndPublishDataset(dataset)) {
            return permissionsWrapper.notAuthorized();
        }

        workingVersion = dataset.getEditVersion();

        importers = ImportersForView.createInitialized(dataset, importerRegistry.getImporters(), session.getLocale());

        List<DatasetField> datasetFields = datasetFieldsInitializer.prepareDatasetFieldsForEdit(workingVersion.getDatasetFields(),
                dataset.getOwner().getMetadataBlockRootDataverse());
        workingVersion.setDatasetFields(datasetFields);

        inputRenderersByFieldType = inputFieldRendererManager.obtainRenderersByType(datasetFields);
        metadataBlocksForEdit = datasetFieldsInitializer.groupAndUpdateFlagsForEdit(datasetFields, dataset.getOwner().getMetadataBlockRootDataverse());

        return StringUtils.EMPTY;
    }

    public String save() {
        workingVersion.setDatasetFields(DatasetFieldUtil.flattenDatasetFieldsFromBlocks(metadataBlocksForEdit));
        
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

    public void initMetadataImportDialog() {
        importerForm = ImporterForm.createInitializedForm(selectedImporter, session.getLocale(), this::getMetadataBlocksForEdit);
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
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.validationError"), "");

        } else if (throwable instanceof CommandException) {

            logger.log(Level.SEVERE, "CommandException, when attempting to update the dataset: " + throwable.getMessage(), throwable);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.metadataFailure"));
        } else {

            logger.log(Level.SEVERE, "Couldn't edit dataset metadata: " + throwable.getMessage(), throwable);
            JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("dataset.message.metadataFailure"));
        }
    }

    public String getPageTitle() {
        return workingVersion.getParsedTitle() + " - " + Jsoup.parse(dataset.getOwner().getName()).text();
    }

    // -------------------- SETTERS --------------------

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public void setPersistentId(String persistentId) {
        this.persistentId = persistentId;
    }

    public void setSelectedImporter(MetadataImporter selectedImporter) {
        this.selectedImporter = selectedImporter;
    }
}
