package edu.harvard.iq.dataverse.validation;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.validation.datasetfield.FieldValidatorRegistry;
import edu.harvard.iq.dataverse.validation.datasetfield.ValidationResult;
import edu.harvard.iq.dataverse.validation.datasetfield.validators.StandardIntegerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetFieldValidationServiceTest {

    @Mock
    private FieldValidatorRegistry registry;

    @Mock
    private StandardIntegerValidator intValidator;

    @Mock
    private DatasetVersion datasetVersion;

    @Mock
    private DatasetField datasetField;

    @Mock
    private DatasetFieldType datasetFieldType;

    private DatasetFieldValidationService service;

    @BeforeEach
    void setUp() {
        when(registry.get("standard_int")).thenReturn(intValidator);
        when(intValidator.isValid(Mockito.any(DatasetField.class), Mockito.anyMap(), Mockito.anyMap()))
                .thenReturn(ValidationResult.ok());
        when(datasetField.getTopParentDatasetField()).thenReturn(datasetField);
        when(datasetField.getDatasetFieldType()).thenReturn(datasetFieldType);
        when(datasetField.getValue()).thenReturn("7");
        when(datasetFieldType.getName()).thenReturn("testField");
        when(datasetVersion.getFlatDatasetFields()).thenReturn(Collections.singletonList(datasetField));
        when(datasetFieldType.getValidation()).thenReturn("[{\"name\":\"standard_int\"}]");
        service = new DatasetFieldValidationService(registry);

    }

    // -------------------- TESTS --------------------

    @Test
    void validateFieldsOfDatasetVersion() {
        // given & when
        service.validateFieldsOfDatasetVersion(datasetVersion);

        // then
        Mockito.verify(registry, only()).get(eq("standard_int"));
        Mockito.verify(intValidator, only()).isValid(eq(datasetField), anyMap(), anyMap());
    }
}