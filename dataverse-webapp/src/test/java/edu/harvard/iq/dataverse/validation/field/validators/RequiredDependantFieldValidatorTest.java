package edu.harvard.iq.dataverse.validation.field.validators;

import com.google.common.collect.ImmutableMap;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import io.vavr.collection.List;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Filipe Dias Lewandowski, Krzysztof Mądry, Daniel Korbel, Sylwester Niewczas
 */
public class RequiredDependantFieldValidatorTest {

    private final RequiredDependantFieldValidator validator = new RequiredDependantFieldValidator();

    @Test
    public void validate() {
        // given
        DatasetField titleLanguage = buildDatasetField("Title text");
        Map<String, Object> params = ImmutableMap.of("dependantField", "titleTranslationText");

        // when
        FieldValidationResult result = validator.validate(titleLanguage, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getField()).isNull();
        assertThat(result.getMessage()).isEmpty();
    }

    @Test
    public void validate__empty_dependant_field() {
        // given
        DatasetField titleLanguage = buildDatasetField("");
        Map<String, Object> params = ImmutableMap.of("dependantField", "titleTranslationText");

        // when
        FieldValidationResult result = validator.validate(titleLanguage, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isFalse();
        assertThat(((DatasetField) result.getField()).getTypeName()).isEqualTo("titleTranslationText");
    }

    @Test
    public void validate__dependant_field_missing() {
        // given
        DatasetField titleLanguage = buildDatasetField("");
        Map<String, Object> params = ImmutableMap.of("dependantField", "unknownField");

        // when
        FieldValidationResult result = validator.validate(titleLanguage, params, Collections.emptyMap());

        // then
        assertThat(result.isOk()).isTrue();
        assertThat(result.getField()).isNull();
        assertThat(result.getMessage()).isEmpty();
    }

    // -------------------- PRIVATE ---------------------

    private static DatasetField buildDatasetField(String text) {
        DatasetField titleTranslation = new DatasetField();
        titleTranslation.setDatasetFieldType(new DatasetFieldType());
        titleTranslation.getDatasetFieldType().setName("titleTranslation");

        DatasetField titleTranslationText = new DatasetField();
        titleTranslationText.setValue(text);
        titleTranslationText.setDatasetFieldType(new DatasetFieldType());
        titleTranslationText.getDatasetFieldType().setName("titleTranslationText");
        titleTranslationText.setDatasetFieldParent(titleTranslation);

        DatasetField titleTranslationLanguage = new DatasetField();
        titleTranslationLanguage.setValue("English");
        titleTranslationLanguage.setDatasetFieldType(new DatasetFieldType());
        titleTranslationLanguage.getDatasetFieldType().setName("titleTranslationLanguage");
        titleTranslationLanguage.setDatasetFieldParent(titleTranslation);

        titleTranslation.setDatasetFieldsChildren(List.of(titleTranslationText, titleTranslationLanguage).asJava());

        return titleTranslationLanguage;
    }
}
