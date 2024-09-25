package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.ControlledVocabularyValueServiceBean;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControlledVocabularyValidatorTest {

    @Mock
    private ControlledVocabularyValueServiceBean vocabularyDao;

    @InjectMocks
    private ControlledVocabularyValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testValidateValue_success() {
        String testValue = "TestValue";
        ValidatableField testField = mock(ValidatableField.class);
        DatasetFieldType testFieldType = mock(DatasetFieldType.class);

        when(testField.getDatasetFieldType()).thenReturn(testFieldType);
        when(testFieldType.getName()).thenReturn("TestFieldType");

        ControlledVocabularyValue vocabularyValue = mock(ControlledVocabularyValue.class);
        when(vocabularyValue.getStrValue()).thenReturn(testValue);

        when(vocabularyDao.findByDatasetFieldTypeNameAndValueLike("TestFieldType", testValue, 1))
                .thenReturn(Collections.singletonList(vocabularyValue));

        FieldValidationResult result = validator.validateValue(
                testValue, testField, new HashMap<>(), new HashMap<>());

        assertTrue(result.isOk());
    }

    @Test
    void testValidateValue_failure() {
        String testValue = "InvalidValue";
        ValidatableField testField = mock(ValidatableField.class);
        DatasetFieldType testFieldType = mock(DatasetFieldType.class);

        when(testField.getDatasetFieldType()).thenReturn(testFieldType);
        when(testFieldType.getName()).thenReturn("TestFieldType");

        when(vocabularyDao.findByDatasetFieldTypeNameAndValueLike("TestFieldType", testValue, 1))
                .thenReturn(Collections.emptyList());

        FieldValidationResult result = validator.validateValue(
                testValue, testField, new HashMap<>(), new HashMap<>());

        assertFalse(result.isOk());
        assertEquals("Value \"InvalidValue\" is not allowed. Please select one from the list.", result.getMessage());
    }
}