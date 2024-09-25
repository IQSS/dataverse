package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RequiredByValueDependantFieldValidatorTest {

    private RequiredByValueDependantFieldValidator validator;

    @Mock
    private ValidatableField field;

    @Mock
    private ValidatableField dependantField;

    @Mock
    private DatasetFieldType datasetFieldType;

    @Before
    public void setUp() {
        validator = new RequiredByValueDependantFieldValidator();
        when(field.getDatasetFieldType()).thenReturn(datasetFieldType);
        when(datasetFieldType.getDisplayName()).thenReturn("Field Display Name");
    }

    @Test
    public void testValidateWithDependantField_InvalidWhenCompanyWithoutCompanyName() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredByValueDependantFieldValidator.DEPENDANT_FIELD_VALUE_PARAM, "company");

        when(dependantField.getValidatableValues()).thenReturn(Collections.singletonList("company"));
        when(field.getValidatableValues()).thenReturn(Collections.singletonList(""));

        try (MockedStatic<BundleUtil> bundleUtilMock = mockStatic(BundleUtil.class)) {
            bundleUtilMock.when(() -> BundleUtil.getStringFromBundle("isrequired", "Field Display Name"))
                    .thenReturn("Field Display Name is required");

            // When
            FieldValidationResult result = validator.validateWithDependantField(field, dependantField, params, Collections.emptyMap());

            // Then
            assertFalse(result.isOk());
        }
    }

    @Test
    public void testValidateWithDependantField_ValidWhenCompanyWithCompanyName() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredByValueDependantFieldValidator.DEPENDANT_FIELD_VALUE_PARAM, "company");

        when(dependantField.getValidatableValues()).thenReturn(Collections.singletonList("company"));
        when(field.getValidatableValues()).thenReturn(Collections.singletonList("Some Company Name"));

        // When
        FieldValidationResult result = validator.validateWithDependantField(field, dependantField, params, Collections.emptyMap());

        // Then
        assertTrue(result.isOk());
    }

    @Test
    public void testValidateWithDependantField_ValidWhenIndividualWithFirstAndLastName() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredByValueDependantFieldValidator.DEPENDANT_FIELD_VALUE_PARAM, "individual");

        when(dependantField.getValidatableValues()).thenReturn(Collections.singletonList("individual"));
        when(field.getValidatableValues()).thenReturn(Collections.singletonList("John"));

        // When
        FieldValidationResult result = validator.validateWithDependantField(field, dependantField, params, Collections.emptyMap());

        // Then
        assertTrue(result.isOk());
    }

    @Test
    public void testValidateWithDependantField_ValidWhenNotMatchingDependantFieldValue() {
        // Given
        Map<String, Object> params = new HashMap<>();
        params.put(RequiredByValueDependantFieldValidator.DEPENDANT_FIELD_VALUE_PARAM, "individual");
        when(dependantField.getValidatableValues()).thenReturn(Collections.singletonList("company"));
        // When
        FieldValidationResult result = validator.validateWithDependantField(field, dependantField, params, Collections.emptyMap());

        // Then
        assertTrue(result.isOk());
    }
}
