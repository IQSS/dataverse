package edu.harvard.iq.dataverse.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * The class is used to add validation info for fields that are not
 * DatasetFieldType-based (eg. some programmatically constructed search
 * fields).
 */
public class ValidationEnhancer {
    private static final Logger logger = LoggerFactory.getLogger(ValidationEnhancer.class);

    private ObjectMapper mapper = new ObjectMapper();

    // -------------------- LOGIC --------------------

    public DatasetFieldType createDatasetFieldType(String name, String displayName, String desctiprtion, ValidationDescriptor validation) {
        SimpleDatasetFieldType simpleDatasetFieldType = new SimpleDatasetFieldType();
        simpleDatasetFieldType.setName(name);
        simpleDatasetFieldType.setDisplayName(displayName);
        simpleDatasetFieldType.setDescription(desctiprtion);
        try {
            String validationJson = mapper.writeValueAsString(Collections.singletonList(validation));
            simpleDatasetFieldType.setValidation(validationJson);
        } catch (JsonProcessingException jpe) {
            logger.warn("Cannot write validator as string", jpe);
        }
        return simpleDatasetFieldType;
    }

    public ValidationDescriptor createValidation(FieldValidator validator, Map<String, Object> params) {
        ValidationDescriptor descriptor = new ValidationDescriptor();
        descriptor.setName(validator.getName());
        descriptor.getParameters().putAll(params);
        return descriptor;
    }

    // -------------------- INNER CLASSES --------------------

    public static class SimpleDatasetFieldType extends DatasetFieldType {
        private String displayName;

        public SimpleDatasetFieldType() {
            setControlledVocabularyValues(Collections.emptyList());
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}
