package edu.harvard.iq.dataverse.validation.field.validators;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.OrcidValidator;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.collection.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OrcidFieldValidatorTest {

    @Mock
    private OrcidValidator orcidValidator;

    @InjectMocks
    private OrcidFieldValidator validator;

    @Test
    public void validateValue() {
        // given
        String orcid = "0000-0002-1825-0097";
        DatasetField identifier = buildDatasetField(orcid, "ORCID");
        Map<String, Object> params = ImmutableMap.of("authorIdentifierScheme", "ORCID");

        when(orcidValidator.validate(orcid)).thenReturn(FieldValidationResult.ok());

        // when
        FieldValidationResult result = validator.validateValue(orcid, identifier, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getField()).isNull();
        assertThat(result.getMessage()).isEmpty();
    }

    @Test
    public void validateValue__invalid() {
        // given
        String orcid = "INVALID";
        DatasetField identifierField = buildDatasetField(orcid, "ORCID");
        Map<String, Object> params = ImmutableMap.of("authorIdentifierScheme", "ORCID");

        when(orcidValidator.validate(orcid)).thenReturn(FieldValidationResult.invalid("INVALID_ORCID"));

        // when
        FieldValidationResult result = validator.validateValue(orcid, identifierField, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(result.getField()).isEqualTo(identifierField);
    }

    @Test
    public void validateValue__non_matching_identifier_type() {
        // given
        String isni = "0000000109010190";
        DatasetField identifier = buildDatasetField(isni, "ISNI");
        Map<String, Object> params = ImmutableMap.of("authorIdentifierScheme", "ORCID");

        // when
        FieldValidationResult result = validator.validateValue(isni, identifier, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getField()).isNull();
        assertThat(result.getMessage()).isEmpty();
        verify(orcidValidator, never()).validate(isni);
    }

    // -------------------- PRIVATE ---------------------

    private static DatasetField buildDatasetField(String identifierValue, String identifierType) {
        DatasetField authorField = new DatasetField();
        authorField.setDatasetFieldType(new DatasetFieldType());
        authorField.getDatasetFieldType().setName("author");

        DatasetField identifierScheme = new DatasetField();
        identifierScheme.setValue(identifierType);
        identifierScheme.setDatasetFieldType(new DatasetFieldType());
        identifierScheme.getDatasetFieldType().setName("authorIdentifierScheme");
        identifierScheme.setDatasetFieldParent(authorField);

        DatasetField identifier = new DatasetField();
        identifier.setValue(identifierValue);
        identifier.setDatasetFieldType(new DatasetFieldType());
        identifier.getDatasetFieldType().setName("authorIdentifier");
        identifier.setDatasetFieldParent(authorField);

        authorField.setDatasetFieldsChildren(List.of(identifierScheme, identifier).asJava());

        return identifier;
    }
}