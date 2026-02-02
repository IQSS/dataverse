/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.DatasetField;
import edu.harvard.iq.dataverse.DatasetFieldCompoundValue;
import edu.harvard.iq.dataverse.DatasetFieldValue;
import edu.harvard.iq.dataverse.Dataverse;
import edu.harvard.iq.dataverse.Template;
import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.exception.CommandException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author stephenkraffmiller
 */
public class UpdateTemplateFieldsCommand extends AbstractCommand<Template> {

    private final Template template;   
    private final List<DatasetField> updatedFields;
    private final boolean replaceData;

    public UpdateTemplateFieldsCommand(Template template, Dataverse dataverse,  List<DatasetField> updatedFields, boolean replaceData, DataverseRequest request) {
        super(request, dataverse);
        this.template = template;
        this.updatedFields = updatedFields;
        this.replaceData = replaceData;
    }

    @Override
    public Template execute(CommandContext ctxt) throws CommandException {
        /*
        Since this is a template we do not need to validate the incoming fields - that will occur if
        the template is used to create a new dataset.
        */
                
        Template savedTemplate;
        updateTemplateFields(template);
        savedTemplate = ctxt.templates().save(template);

        return savedTemplate;
    }

    /**
     * Updates the template fields by iterating through the updated fields and
     * comparing them to the existing fields in the template
     * <p>
     * - If a matching field exists, its values are added or replaced based on conditions:
     * - If the field is empty, allows multiple values, or replaceData is enabled, the values are updated.
     * - If replaceData is enabled, the field is reset before updating.
     * - If the field is a controlled vocabulary type, it is updated accordingly.
     * - Otherwise, a regular template field update is performed.
     * </p>
     * <p>
     * - If no matching field exists, the updated field is added to the template.
     * </p>
     *
     * @param template The template to update with the new fields.
     */
    private void updateTemplateFields(Template template) {
        for (DatasetField updatedField : updatedFields) {
            boolean found = false;

            for (DatasetField datasetVersionField : template.getDatasetFields()) {
                if (!datasetVersionField.getDatasetFieldType().equals(updatedField.getDatasetFieldType())) {
                    continue;
                }

                found = true;

                if (datasetVersionField.isEmpty() || datasetVersionField.getDatasetFieldType().isAllowMultiples() || replaceData) {

                    if (replaceData) {
                        resetDatasetField(datasetVersionField);
                    }

                    if (updatedField.getDatasetFieldType().isControlledVocabulary()) {
                        updateControlledVocabularyDatasetField(
                                updatedField, datasetVersionField, datasetVersionField.getDisplayValue(),
                                new ArrayList<>(datasetVersionField.getControlledVocabularyValues())
                        );
                    } else {
                        updateRegularDatasetField(updatedField, datasetVersionField, template);
                    }
                    break;
                }
            }

            if (!found) {
                updatedField.setTemplate(template);
                template.getDatasetFields().add(updatedField);
            }
        }
    }

    private void resetDatasetField(DatasetField datasetVersionField) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            datasetVersionField.setDatasetFieldValues(new ArrayList<>());
            datasetVersionField.setControlledVocabularyValues(new ArrayList<>());
            datasetVersionField.setDatasetFieldCompoundValues(new ArrayList<>());
        } else {
            datasetVersionField.setSingleValue("");
            datasetVersionField.setSingleControlledVocabularyValue(null);
        }
    }

    private static void updateRegularDatasetField(DatasetField updatedField, DatasetField datasetVersionField, Template template) {
        if (!updatedField.getDatasetFieldType().isCompound()) {
            updateSingleDatasetField(updatedField, datasetVersionField);
        } else {
            updateCompoundDatasetField(updatedField, datasetVersionField, template);
        }
    }

    private static void updateSingleDatasetField(DatasetField updatedField, DatasetField datasetVersionField) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            for (DatasetFieldValue datasetFieldValue : updatedField.getDatasetFieldValues()) {
                if (!datasetVersionField.getDisplayValue().contains(datasetFieldValue.getDisplayValue())) {
                    datasetFieldValue.setDatasetField(datasetVersionField);
                    datasetVersionField.getDatasetFieldValues().add(datasetFieldValue);
                }
            }
        } else {
            datasetVersionField.setSingleValue(updatedField.getValue());
        }
    }

    private static void updateCompoundDatasetField(DatasetField updatedField, DatasetField datasetVersionField, Template template) {
        for (DatasetFieldCompoundValue datasetFieldCompoundValue : updatedField.getDatasetFieldCompoundValues()) {
            if (!datasetVersionField.getCompoundDisplayValue().contains(updatedField.getCompoundDisplayValue())) {
                datasetFieldCompoundValue.setParentDatasetField(datasetVersionField);
                datasetVersionField.setTemplate(template);
                datasetVersionField.getDatasetFieldCompoundValues().add(datasetFieldCompoundValue);
            }
        }
    }

    private static void updateControlledVocabularyDatasetField(DatasetField updatedField,
                                                               DatasetField datasetVersionField,
                                                               String controlledVocabularyDisplayValue,
                                                               List<ControlledVocabularyValue> priorControlledVocabularyValues) {
        if (datasetVersionField.getDatasetFieldType().isAllowMultiples()) {
            for (ControlledVocabularyValue controlledVocabularyValue : updatedField.getControlledVocabularyValues()) {
                if (!controlledVocabularyDisplayValue.contains(controlledVocabularyValue.getStrValue())) {
                    priorControlledVocabularyValues.add(controlledVocabularyValue);
                }
            }
            datasetVersionField.setControlledVocabularyValues(priorControlledVocabularyValues);
        } else {
            datasetVersionField.setSingleControlledVocabularyValue(updatedField.getSingleControlledVocabularyValue());
        }
    }
    
}
