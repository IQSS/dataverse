package edu.harvard.iq.dataverse.validation.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SearchFormValidationDispatcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<ValidatableField>> fieldIndex = new HashMap<>();
    private Map<String, List<ValidatableField>> fieldsToValidate = new HashMap<>();
    private Map<String, List<ValidationDescriptor>> descriptorsCache = new HashMap<>();

    private FieldValidatorRegistry registry;

    // -------------------- CONSTRUCTORS --------------------

    SearchFormValidationDispatcher(FieldValidatorRegistry registry) {
        this.registry = registry;
    }

    // -------------------- LOGIC --------------------

    SearchFormValidationDispatcher init(Map<String, SearchField> searchFields,
                                          Map<String, SearchField> nonSearchFields) {
        fieldIndex = Stream.of(searchFields, nonSearchFields)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        fieldsToValidate = searchFields.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return this;
    }

    public List<ValidationResult> executeValidations() {
        return fieldsToValidate.values().stream()
                .flatMap(Collection::stream)
                .map(this::validateField)
                .filter(r -> !r.isOk())
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private ValidationResult validateField(ValidatableField field) {
        boolean effectivelyEmptyValue = field.getValidatableValues().stream()
                .allMatch(StringUtils::isBlank);
        for (ValidationDescriptor descriptor : retrieveDescriptors(field)) {
            Map<String, Object> parameters = descriptor.getParameters();
            @SuppressWarnings("unchecked") List<String> contexts = (List<String>) parameters.get(ValidationDescriptor.CONTEXT_PARAM);
            boolean properContext = contexts == null || contexts.contains(ValidationDescriptor.SEARCH_CONTEXT);
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

    private List<ValidationDescriptor> retrieveDescriptors(ValidatableField field) {
        String configJson = Optional.ofNullable(field.getDatasetFieldType())
                .map(DatasetFieldType::getValidation).orElse(null);
        if (configJson == null) {
            return Collections.emptyList();
        }
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
