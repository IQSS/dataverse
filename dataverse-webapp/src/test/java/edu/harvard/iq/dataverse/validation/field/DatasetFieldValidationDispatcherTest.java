package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.validation.field.validators.FieldValidatorBase;
import edu.harvard.iq.dataverse.validation.field.validators.StandardInputValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardIntegerValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DatasetFieldValidationDispatcherTest {

    private FieldValidatorRegistry registry = new FieldValidatorRegistry();

    private DatasetField datasetField;
    private DatasetFieldType datasetFieldType;

    private DatasetFieldValidationDispatcher dispatcher = new DatasetFieldValidationDispatcher(registry);

    private static final FieldValidator FAILING_VALIDATOR = new FieldValidatorBase() {
        @Override
        public String getName() {
            return "failing_validator";
        }

        @Override
        public ValidationResult validate(ValidatableField field, Map<String, Object> params,
                                         Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
            return ValidationResult.invalid(field, "message");
        }
    };


    @BeforeEach
    void setUp() {
        registry.register(new StandardIntegerValidator());
        registry.register(new StandardInputValidator());

        datasetField = new DatasetField();
        DatasetVersion datasetVersion = new DatasetVersion();
        Dataset dataset = new Dataset();
        Dataverse dataverse = new Dataverse();
        dataset.setOwner(dataverse);
        datasetVersion.setDataset(dataset);
        datasetFieldType = new DatasetFieldType() {
            @Override
            public String getDisplayName() { return "testField"; }
        };
        datasetFieldType.setName("testField");
        datasetFieldType.setFieldType(FieldType.TEXT);
        datasetField.setDatasetFieldType(datasetFieldType);
        datasetField.setDatasetVersion(datasetVersion);
    }

    // -------------------- TESTS --------------------

    @Test
    void executeValidations() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}]");
        datasetField.setValue("44.5");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results).extracting(ValidationResult::isOk).containsExactly(false);
    }

    @Test
    @DisplayName("In case of no errors executeValidations(…) should return empty list")
    void executeValidations__ok() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}]");
        datasetField.setValue("1024");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Execute chain of validators (last should fail)")
    void executeValidations__chain() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}," +
                "{\"name\":\"standard_input\",\"parameters\":{\"format\":\"[0-9]\"}}]");
        datasetField.setValue("44");
        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::getMessage, ValidationResult::isOk)
                .containsExactly(tuple("testField is not a valid entry.", false));
    }

    @Test
    @DisplayName("Run validators from proper context")
    void executeValidations_context() {
        // given
        registry.register(FAILING_VALIDATOR);
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\", \"parameters\":{\"context\":[\"SEARCH\"]}}," +
                "{\"name\":\"standard_input\",\"parameters\":{\"format\":\"[A-Z]\", \"context\":[\"DATASET\"]}}]");
        datasetField.setValue("abc");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::getMessage, ValidationResult::isOk)
                .containsExactly(tuple("testField is not a valid entry.", false));
    }

    @Test
    void executeValidations__emptyRequiredField() {
        // given
        datasetFieldType.setRequired(true);

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::getMessage, ValidationResult::isOk)
                .containsExactly(tuple("testField is required.", false));
    }

    @Test
    void executeValidations__nonEmptyRequired() {
        // given
        datasetFieldType.setRequired(true);
        datasetField.setFieldValue("abc");
        datasetFieldType.setValidation("[]");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("N/A or null value should bypass any further validations")
    @ValueSource(strings = { DatasetField.NA_VALUE })
    @NullSource
    void executeValidations__NAValue(String value) {
        // given
        datasetField.setFieldValue(value);

        registry.register(FAILING_VALIDATOR);
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\"}]");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("When runOnEmpty parameter is present in descriptor, N/A or null value should not bypass validations")
    @ValueSource(strings = { DatasetField.NA_VALUE })
    @NullSource
    void executeValidations__NAValue_runOnEmpty(String value) {
        // given
        datasetField.setFieldValue(value);

        registry.register(FAILING_VALIDATOR);
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\", \"parameters\":{\"runOnEmpty\":\"true\"}}]");

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::isOk, ValidationResult::getField)
                .containsExactly(tuple(false, datasetField));
    }

    @Test
    @DisplayName("Fields with template should bypass validation")
    void templatesBypassesValidation() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}]");
        datasetField.setValue("abcd");
        datasetField.setTemplate(new Template());

        // when
        List<ValidationResult> results = dispatcher.init(Collections.singletonList(datasetField)).executeValidations();

        // then
        assertThat(results).isEmpty();
    }
}