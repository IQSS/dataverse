package edu.harvard.iq.dataverse.validation.datasetfield;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValidationDispatcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<DatasetField>> fieldIndex = Collections.emptyMap();

    private FieldValidatorRegistry registry;

    // -------------------- CONSTRUCTORS --------------------

    public FieldValidationDispatcher(FieldValidatorRegistry registry) {
        this.registry = registry;
    }

    // -------------------- LOGIC --------------------

    public FieldValidationDispatcher init(DatasetVersion datasetVersion) {
        return init(datasetVersion.getFlatDatasetFields());
    }

    public FieldValidationDispatcher init(List<DatasetField> parentAndChildrenFields) {
        fieldIndex = parentAndChildrenFields.stream()
                .collect(Collectors.groupingBy(f -> f.getDatasetFieldType().getName()));
        return this;
    }

    public List<ValidationResult> executeValidations() {
        return fieldIndex.values().stream()
                .flatMap(Collection::stream)
                .filter(this::isNotTemplateField)
                .map(this::executeSingleValidation)
                .filter(r -> !r.isOk())
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private boolean isNotTemplateField(DatasetField field) {
        return field.getTopParentDatasetField().getTemplate() == null;
    }

    private ValidationResult executeSingleValidation(DatasetField field) {
        DatasetFieldType fieldType = field.getDatasetFieldType();
        if (StringUtils.isBlank(field.getValue()) && fieldType.isPrimitive() && isRequiredInDataverse(field)) {
            return ValidationResult.invalid(field,
                    BundleUtil.getStringFromBundle("isrequired", fieldType.getDisplayName()));
        }

        if (StringUtils.isBlank(field.getValue()) || StringUtils.equals(field.getValue(), DatasetField.NA_VALUE)) {
            return ValidationResult.ok();
        }

        String configJson = field.getDatasetFieldType().getValidation();
        ValidationConfiguration configuration = readConfiguration(configJson);
        return !configuration.shouldValidate()
                ? ValidationResult.ok()
                : executeConfiguredValidations(field, configuration);
    }

    private boolean isRequiredInDataverse(DatasetField field) {
        DatasetFieldType fieldType = field.getDatasetFieldType();
        if (fieldType.isRequired()) {
            return true;
        }

        Dataverse dataverse = getDataverse(field).getMetadataBlockRootDataverse();
        return dataverse.getDataverseFieldTypeInputLevels().stream()
                .filter(inputLevel -> inputLevel.getDatasetFieldType().equals(field.getDatasetFieldType()))
                .map(DataverseFieldTypeInputLevel::isRequired)
                .findFirst()
                .orElse(false);
    }

    private Dataverse getDataverse(DatasetField field) {
        return field.getTopParentDatasetField()
                .getDatasetVersion()
                .getDataset()
                .getOwner();
    }

    private ValidationConfiguration readConfiguration(String configurationJson) {
        try {
            configurationJson = String.format("{\"validations\":%s}", configurationJson.trim());
            return objectMapper.readValue(configurationJson, ValidationConfiguration.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ValidationResult executeConfiguredValidations(DatasetField field, ValidationConfiguration configuration) {
        for (ValidationConfiguration.ValidationDescriptor validation : configuration.getValidations()) {
            String validatorName = validation.getName();
            FieldValidator validator = registry.get(validatorName);
            if (validator == null) {
                throw new RuntimeException(String.format("Cannot find validator [%s]. Registered validators: [%s]",
                        validatorName, String.join(", ", registry.getRegisteredValidatorNames())));
            }
            ValidationResult result = validator.isValid(field, validation.getParametersAsMap(), fieldIndex);
            if (!result.isOk()) {
                return result;
            }
        }
        return ValidationResult.ok();
    }
}
