package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;
import edu.harvard.iq.dataverse.validation.field.validators.StandardInputValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardNumberValidator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class ValidationEnhancerTest {

    private ValidationEnhancer enhancer = new ValidationEnhancer();

    @Test
    void createDatasetFieldType() {
        // given
        StandardNumberValidator validator = new StandardNumberValidator();
        Map<String, Object> params = Collections.singletonMap("param", Arrays.asList("value_1", "value_2"));

        // when
        DatasetFieldType fieldType = enhancer.createDatasetFieldType("fieldType", "Field Type", "Description",
                enhancer.createValidation(validator, params));

        // then
        assertThat(fieldType)
                .extracting(DatasetFieldType::getName, DatasetFieldType::getDisplayName, DatasetFieldType::getDescription)
                .containsExactly("fieldType", "Field Type", "Description");
        assertThat(fieldType.getValidation())
                .isEqualTo("[{\"name\":\"standard_number\",\"parameters\":{\"param\":[\"value_1\",\"value_2\"]}}]");
    }

    @Test
    void createValidation() {
        // given
        StandardInputValidator validator = new StandardInputValidator();
        Map<String, Object> params = Collections.singletonMap("param", "value");

        // when
        ValidationDescriptor descriptor = enhancer.createValidation(validator, params);

        // then
        assertThat(descriptor.getName()).isEqualTo("standard_input");
        assertThat(descriptor.getParameters()).containsExactly(entry("param", "value"));
    }
}