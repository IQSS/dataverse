package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.RorValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RorFieldValidatorTest {

    @Mock
    private RorValidator rorValidator;

    @InjectMocks
    private RorFieldValidator validator;

    @Test
    public void validateValue() {
        // given
        String ror = "https://ror.org/04xfq0f34";
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(ror);
        datasetField.getDatasetFieldType().setName("ror");

        when(rorValidator.validate(ror)).thenReturn(FieldValidationResult.ok());

        // when
        FieldValidationResult result = validator.validateValue(ror, datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getField()).isNull();
        assertThat(result.getMessage()).isEmpty();
    }

    @Test
    public void validateValue__invalid() {
        // given
        String ror = "INVALID";
        DatasetField datasetField = new DatasetField();
        datasetField.setDatasetFieldType(new DatasetFieldType());
        datasetField.setValue(ror);
        datasetField.getDatasetFieldType().setName("ror");

        when(rorValidator.validate(ror)).thenReturn(FieldValidationResult.invalid("INVALID_ROR"));

        // when
        FieldValidationResult result = validator.validateValue(ror, datasetField, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getField()).isEqualTo(datasetField);
    }
}