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
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredPermissions(Permission.EditDataset)
public class UpdateDatasetFieldsCommand extends AbstractDatasetCommand<Dataset> {

    private static final Logger logger = Logger.getLogger(UpdateDatasetFieldsCommand.class.getName());

    private final Dataset dataset;
    private final List<DatasetField> updatedFields;
    private final boolean replaceData;

    public UpdateDatasetFieldsCommand(Dataset dataset, List<DatasetField> updatedFields, boolean replaceData, DataverseRequest aRequest) {
        super(aRequest, dataset);
        this.dataset = dataset;
        this.updatedFields = updatedFields;
        this.replaceData = replaceData;
    }

    @Override
    public Dataset execute(CommandContext ctxt) throws CommandException {
        DatasetVersion datasetVersion = dataset.getOrCreateEditVersion();
        datasetVersion.getTermsOfUseAndAccess().setDatasetVersion(datasetVersion);

        String validationErrors = ctxt.datasetFieldsValidator().validateFields(updatedFields, datasetVersion);
        if (!validationErrors.isEmpty()) {
            logger.log(Level.SEVERE, "Semantic error parsing dataset update Json: " + validationErrors, validationErrors);
            throw new InvalidCommandArgumentsException(BundleUtil.getStringFromBundle("updateDatasetFieldsCommand.api.processDatasetUpdate.parseError", List.of(validationErrors)), this);
        }

        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);

        //loop through the update fields
        // and compare to the version fields
        //if exist add/replace values
        //if not add entire dsf
        for (DatasetField updatedField : updatedFields) {
            boolean found = false;
            for (DatasetField datasetVersionField : datasetVersion.getDatasetFields()) {
                if (datasetVersionField.getDatasetFieldType().equals(updatedField.getDatasetFieldType())) {
                    found = true;
                    if (datasetVersionField.isEmpty() || datasetVersionField.getDatasetFieldType().isAllowMultiples() || replaceData) {
                        List priorCVV = new ArrayList<>();
                        String cvvDisplay = "";

                        if (updatedField.getDatasetFieldType().isControlledVocabulary()) {
                            cvvDisplay = datasetVersionField.getDisplayValue();
                            for (ControlledVocabularyValue cvvOld : datasetVersionField.getControlledVocabularyValues()) {
                                priorCVV.add(cvvOld);
                            }
                        }

                        if (replaceData) {
                            if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
                                datasetVersionField.setDatasetFieldCompoundValues(new ArrayList<>());
                                datasetVersionField.setDatasetFieldValues(new ArrayList<>());
                                datasetVersionField.setControlledVocabularyValues(new ArrayList<>());
                                priorCVV.clear();
                                datasetVersionField.getControlledVocabularyValues().clear();
                            } else {
                                datasetVersionField.setSingleValue("");
                                datasetVersionField.setSingleControlledVocabularyValue(null);
                            }
                            cvvDisplay = "";
                        }
                        if (updatedField.getDatasetFieldType().isControlledVocabulary()) {
                            updateControlledVocabularyDatasetField(updatedField, datasetVersionField, cvvDisplay, priorCVV);
                        } else {
                            updateRegularDatasetField(updatedField, datasetVersionField, datasetVersion);
                        }
                    } else {
                        if (!datasetVersionField.isEmpty() && !datasetVersionField.getDatasetFieldType().isAllowMultiples() || !replaceData) {
                            throw new InvalidCommandArgumentsException("You may not add data to a field that already has data and does not allow multiples. Use replace=true to replace existing data (" + datasetVersionField.getDatasetFieldType().getDisplayName() + ")", this);
                        }
                    }
                    break;
                }
            }
            if (!found) {
                updatedField.setDatasetVersion(datasetVersion);
                datasetVersion.getDatasetFields().add(updatedField);
            }
        }

        return ctxt.engine().submit(new UpdateDatasetVersionCommand(this.dataset, getRequest()));
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
            for (DatasetFieldValue dfv : updatedField.getDatasetFieldValues()) {
                if (!datasetVersionField.getDisplayValue().contains(dfv.getDisplayValue())) {
                    dfv.setDatasetField(datasetVersionField);
                    datasetVersionField.getDatasetFieldValues().add(dfv);
                }
            }
        } else {
            datasetVersionField.setSingleValue(updatedField.getValue());
        }
    }

    private static void updateCompoundDatasetField(DatasetField updatedField, DatasetField datasetVersionField, DatasetVersion datasetVersion) {
        for (DatasetFieldCompoundValue dfcv : updatedField.getDatasetFieldCompoundValues()) {
            if (!datasetVersionField.getCompoundDisplayValue().contains(updatedField.getCompoundDisplayValue())) {
                dfcv.setParentDatasetField(datasetVersionField);
                datasetVersionField.setDatasetVersion(datasetVersion);
                datasetVersionField.getDatasetFieldCompoundValues().add(dfcv);
            }
        }
    }

    private static void updateControlledVocabularyDatasetField(DatasetField updatedField, DatasetField datasetVersionField, String cvvDisplay, List priorCVV) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            for (ControlledVocabularyValue cvv : updatedField.getControlledVocabularyValues()) {
                if (!cvvDisplay.contains(cvv.getStrValue())) {
                    priorCVV.add(cvv);
                }
            }
            datasetVersionField.setControlledVocabularyValues(priorCVV);
        } else {
            datasetVersionField.setSingleControlledVocabularyValue(updatedField.getSingleControlledVocabularyValue());
        }
    }
}
