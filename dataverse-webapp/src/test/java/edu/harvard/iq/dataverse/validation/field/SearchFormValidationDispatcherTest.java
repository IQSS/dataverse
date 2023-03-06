package edu.harvard.iq.dataverse.validation.field;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.SearchField;
import edu.harvard.iq.dataverse.search.advanced.SearchFieldType;
import edu.harvard.iq.dataverse.validation.field.validators.FieldValidatorBase;
import edu.harvard.iq.dataverse.validation.field.validators.StandardInputValidator;
import edu.harvard.iq.dataverse.validation.field.validators.StandardIntegerValidator;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SearchFormValidationDispatcherTest {
    private FieldValidatorRegistry registry = new FieldValidatorRegistry();

    private TestField field;
    private DatasetFieldType datasetFieldType;

    private SearchFormValidationDispatcher dispatcher = new SearchFormValidationDispatcher(registry);

    private static final FieldValidator FAILING_VALIDATOR = new FieldValidatorBase() {
        @Override
        public String getName() {
            return "failing_validator";
        }

        @Override
        public ValidationResult isValid(ValidatableField field, Map<String, Object> params,
                                        Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
            return ValidationResult.invalid(field, "message");
        }
    };

    @BeforeEach
    void setUp() {
        registry.register(new StandardIntegerValidator());
        registry.register(new StandardInputValidator());

        datasetFieldType = new DatasetFieldType() {
            @Override
            public String getDisplayName() { return "testField"; }
        };
        datasetFieldType.setName("testField");
        datasetFieldType.setFieldType(FieldType.TEXT);
        field = new TestField("testField", datasetFieldType);
    }

    // -------------------- TESTS --------------------

    @Test
    void executeValidations() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}]");
        field.setValues("44.5", "44");

        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

        // then
        assertThat(results).extracting(ValidationResult::isOk).containsExactly(false);
    }

    @Test
    @DisplayName("In case of no errors executeValidations(…) should return empty list")
    void executeValidations__ok() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}]");
        field.setValues("1024", "2048");

        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Execute chain of validators (last should fail)")
    void executeValidations__chain() {
        // given
        datasetFieldType.setValidation("[{\"name\":\"standard_int\"}," +
                "{\"name\":\"standard_input\",\"parameters\":{\"format\":\"[0-9]\"}}]");
        field.setValues("44", "321", "123535");
        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

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
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\", \"parameters\":{\"context\":[\"DATASET\"]}}," +
                "{\"name\":\"standard_input\",\"parameters\":{\"format\":\"[A-Z]\", \"context\":[\"SEARCH\"]}}]");
        field.setValues("abc");

        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::getMessage, ValidationResult::isOk)
                .containsExactly(tuple("testField is not a valid entry.", false));
    }

    @ParameterizedTest
    @DisplayName("Empty or null value should bypass any further validations")
    @ValueSource(strings = {StringUtils.EMPTY})
    @NullSource
    void executeValidations__NAValue(String value) {
        // given
        field.setValues(value);

        registry.register(FAILING_VALIDATOR);
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\"}]");

        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

        // then
        assertThat(results).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("When runOnEmpty parameter is present in descriptor, null or empty value should not bypass validations")
    @ValueSource(strings = {StringUtils.EMPTY})
    @NullSource
    void executeValidations__NAValue_runOnEmpty(String value) {
        // given
        field.setValues(value);

        registry.register(FAILING_VALIDATOR);
        datasetFieldType.setValidation("[{\"name\":\"failing_validator\", \"parameters\":{\"runOnEmpty\":\"true\"}}]");

        // when
        List<ValidationResult> results = initDispatcher().executeValidations();

        // then
        assertThat(results)
                .extracting(ValidationResult::isOk, ValidationResult::getField)
                .containsExactly(tuple(false, field));
    }

    // -------------------- PRIVATE --------------------

    private SearchFormValidationDispatcher initDispatcher() {
        return dispatcher.init(Collections.singletonMap(field.getName(), field), Collections.emptyMap());
    }

    // -------------------- INNER CLASSES --------------------

    private static class TestField extends SearchField {
        private List<String> values = new ArrayList<>();

        public TestField(String name, DatasetFieldType datasetFieldType) {
            super(name, name, name, SearchFieldType.TEXT, datasetFieldType);
        }

        public void setValues(String... values) {
            this.values.addAll(Arrays.asList(values));
        }

        @Override
        public List<String> getValidatableValues() {
            return values;
        }
    }
}