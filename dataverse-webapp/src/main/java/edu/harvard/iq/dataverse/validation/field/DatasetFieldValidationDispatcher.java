package edu.harvard.iq.dataverse.validation.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatasetFieldValidationDispatcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<DatasetField>> fieldIndex = Collections.emptyMap();
    private Map<String, List<ValidationDescriptor>> descriptorsCache = new HashMap<>();

    private FieldValidatorRegistry registry;

    // -------------------- CONSTRUCTORS --------------------

    DatasetFieldValidationDispatcher(FieldValidatorRegistry registry) {
        this.registry = registry;
    }

    // -------------------- LOGIC --------------------

    DatasetFieldValidationDispatcher init(List<DatasetField> parentAndChildrenFields) {
        fieldIndex = parentAndChildrenFields.stream()
                .collect(Collectors.groupingBy(DatasetField::getTypeName));
        return this;
    }

    public List<ValidationResult> executeValidations() {
        return fieldIndex.values().stream()
                .flatMap(Collection::stream)
                .filter(this::isNotTemplateField)
                .map(this::validateField)
                .filter(r -> !r.isOk())
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private boolean isNotTemplateField(DatasetField field) {
        return field.getTopParentDatasetField().getTemplate() == null;
    }

    private ValidationResult validateField(DatasetField field) {
        DatasetFieldType fieldType = field.getDatasetFieldType();
        if (StringUtils.isBlank(field.getValue()) && fieldType.isPrimitive() && isRequiredInDataverse(field)) {
            return ValidationResult.invalid(field,
                    BundleUtil.getStringFromBundle("isrequired", fieldType.getDisplayName()));
        }
        boolean effectivelyEmptyValue = StringUtils.isBlank(field.getValue())
                || DatasetField.NA_VALUE.equals(field.getValue());
        for (ValidationDescriptor descriptor : retrieveDescriptors(field)) {
            Map<String, Object> parameters = descriptor.getParameters();
            @SuppressWarnings("unchecked") List<String> contexts = (List<String>) parameters.get(ValidationDescriptor.CONTEXT_PARAM);
            boolean properContext = contexts == null || contexts.contains(ValidationDescriptor.DATASET_CONTEXT);
            if (!properContext || (effectivelyEmptyValue && !parameters.containsKey(ValidationDescriptor.RUN_ON_EMPTY_PARAM))) {
                continue;
            }
            FieldValidator validator = registry.getOrThrow(descriptor.getName());
            ValidationResult result = validator.validate(field, parameters, fieldIndex);
            if (!result.isOk()) {
                return result;
            }
        }
        return ValidationResult.ok();
    }

    private boolean isRequiredInDataverse(DatasetField field) {
        DatasetFieldType fieldType = field.getDatasetFieldType();
        if (fieldType.isRequired()) {
            return true;
        }

        Dataverse dataverse = field.getTopParentDatasetField()
                .getDatasetVersion()
                .getDataset()
                .getOwner().getMetadataBlockRootDataverse();
        return dataverse.getDataverseFieldTypeInputLevels().stream()
                .filter(inputLevel -> inputLevel.getDatasetFieldType().equals(field.getDatasetFieldType()))
                .map(DataverseFieldTypeInputLevel::isRequired)
                .findFirst()
                .orElse(false);
    }

    private List<ValidationDescriptor> retrieveDescriptors(DatasetField field) {
        String configJson = field.getDatasetFieldType().getValidation();
        List<ValidationDescriptor> existing = descriptorsCache.get(configJson);
        if (existing != null) {
            return existing;
        }
        try {
            List<ValidationDescriptor> descriptors = objectMapper.readValue(configJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ValidationDescriptor.class));
            descriptorsCache.put(configJson, descriptors);
            return descriptors;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
