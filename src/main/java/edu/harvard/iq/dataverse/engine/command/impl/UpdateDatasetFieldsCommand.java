package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.authorization.Permission;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import edu.harvard.iq.dataverse.engine.command.exception.InvalidCommandArgumentsException;
import edu.harvard.iq.dataverse.util.BundleUtil;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetFieldsCommand extends AbstractDatasetCommand<Dataset> {

    private final Dataset dataset;
    private final List<DatasetField> updatedFields;
    private final boolean replaceData;
    private final UpdateDatasetVersionCommand updateDatasetVersionCommand;

    public UpdateDatasetFieldsCommand(Dataset dataset, List<DatasetField> updatedFields, boolean replaceData, DataverseRequest request) {
        this(dataset, updatedFields, replaceData, request, null);
    }

    // Use only for testing purposes
    public UpdateDatasetFieldsCommand(Dataset dataset, List<DatasetField> updatedFields, boolean replaceData, DataverseRequest request, UpdateDatasetVersionCommand updateDatasetVersionCommand) {
        super(request, dataset);
        this.dataset = dataset;
        this.updatedFields = updatedFields;
        this.replaceData = replaceData;
        this.updateDatasetVersionCommand = updateDatasetVersionCommand;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.getTermsOfUseAndAccess().setDatasetVersion(datasetVersion);

        String validationErrors = ctxt.datasetFieldsValidator().validateFields(updatedFields, datasetVersion);
        if (!validationErrors.isEmpty()) {
            throw new InvalidCommandArgumentsException(
                    BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError",
                            List.of(validationErrors)), this);
        }

        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        updateDatasetVersionFields(datasetVersion);

        return ctxt.engine().submit(updateDatasetVersionCommand == null ? new UpdateDatasetVersionCommand(this.dataset, getRequest()) : updateDatasetVersionCommand);
    }

    /**
     * Updates the dataset version fields by iterating through the updated fields and
     * comparing them to the existing fields in the dataset version.
     * <p>
     * - If a matching field exists, its values are added or replaced based on conditions:
     * - If the field is empty, allows multiple values, or replaceData is enabled, the values are updated.
     * - If replaceData is enabled, the field is reset before updating.
     * - If the field is a controlled vocabulary type, it is updated accordingly.
     * - Otherwise, a regular dataset field update is performed.
     * </p>
     * <p>
     * - If no matching field exists, the updated field is added to the dataset version.
     * </p>
     *
     * @param datasetVersion The dataset version to update with the new fields.
     */
    private void updateDatasetVersionFields(DatasetVersion datasetVersion) {
        for (DatasetField updatedField : updatedFields) {
            boolean found = false;

            for (DatasetField datasetVersionField : datasetVersion.getDatasetFields()) {
                if (!datasetVersionField.getDatasetFieldType().equals(updatedField.getDatasetFieldType())) {
                    continue;
                }

                found = true;

                if (datasetVersionField.isEmpty() || datasetVersionField.getDatasetFieldType().isAllowMultiples() || replaceData) {

                    if (replaceData) {
                        resetDatasetField(datasetVersionField);
                    }

                    if (updatedField.getDatasetFieldType().isControlledVocabulary()) {
                        updateControlledVocabularyDatasetField(updatedField, datasetVersionField);
                    } else {
                        updateRegularDatasetField(updatedField, datasetVersionField, datasetVersion);
                    }
                    break;
                }
            }

            if (!found) {
                updatedField.setDatasetVersion(datasetVersion);
                datasetVersion.getDatasetFields().add(updatedField);
            }
        }
    }

    private void resetDatasetField(DatasetField datasetVersionField) {
        if (datasetVersionField.getDatasetFieldType().isCompound()) {
            datasetVersionField.setDatasetFieldCompoundValues(new ArrayList<>());
        } else if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            datasetVersionField.setDatasetFieldValues(new ArrayList<>());
            datasetVersionField.setControlledVocabularyValues(new ArrayList<>());
            datasetVersionField.setDatasetFieldCompoundValues(new ArrayList<>());
        } else {
            datasetVersionField.setDatasetFieldValues(new ArrayList<>());
            datasetVersionField.setSingleControlledVocabularyValue(null);
        }
    }

    private static void updateRegularDatasetField(DatasetField updatedField, DatasetField datasetVersionField, DatasetVersion datasetVersion) {
        if (!updatedField.getDatasetFieldType().isCompound()) {
            updateSingleDatasetField(updatedField, datasetVersionField);
        } else {
            updateCompoundDatasetField(updatedField, datasetVersionField, datasetVersion);
        }
    }

    private static void updateSingleDatasetField(DatasetField updatedField, DatasetField datasetVersionField) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            for (DatasetFieldValue datasetFieldValue : updatedField.getDatasetFieldValues()) {
                if (StringUtils.isNotBlank(datasetFieldValue.getValue())
                        && datasetVersionField.getDatasetFieldValues().stream()
                                .noneMatch(existing -> existing.valuesEqual(datasetFieldValue))) {
                    datasetFieldValue.setDatasetField(datasetVersionField);
                    datasetVersionField.getDatasetFieldValues().add(datasetFieldValue);
                }
            }
        } else {
            datasetVersionField.setSingleValue(updatedField.getValue());
        }
    }

    private static void updateCompoundDatasetField(DatasetField updatedField, DatasetField datasetVersionField, DatasetVersion datasetVersion) {
        for (DatasetFieldCompoundValue datasetFieldCompoundValue : updatedField.getDatasetFieldCompoundValues()) {
            //  Skip an incoming compound entry if all its children are empty
            if (datasetFieldCompoundValue.getChildDatasetFields().stream().allMatch(DatasetField::isEmpty)) {
                continue;
            }
            if (datasetVersionField.getDatasetFieldCompoundValues().stream()
                    .noneMatch(existing -> existing.valuesEqual(datasetFieldCompoundValue))) {
                datasetFieldCompoundValue.setParentDatasetField(datasetVersionField);
                datasetVersionField.setDatasetVersion(datasetVersion);
                datasetVersionField.getDatasetFieldCompoundValues().add(datasetFieldCompoundValue);
            }
        }
    }

    private static void updateControlledVocabularyDatasetField(DatasetField updatedField,
                                                               DatasetField datasetVersionField) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            List<ControlledVocabularyValue> values = new ArrayList<>(datasetVersionField.getControlledVocabularyValues());
            for (ControlledVocabularyValue controlledVocabularyValue : updatedField.getControlledVocabularyValues()) {
                if (StringUtils.isNotBlank(controlledVocabularyValue.getStrValue())
                        && values.stream().noneMatch(existing -> existing.valuesEqual(controlledVocabularyValue))) {
                    values.add(controlledVocabularyValue);
                }
            }
            datasetVersionField.setControlledVocabularyValues(values);
        } else {
            datasetVersionField.setSingleControlledVocabularyValue(updatedField.getSingleControlledVocabularyValue());
        }
    }
}
