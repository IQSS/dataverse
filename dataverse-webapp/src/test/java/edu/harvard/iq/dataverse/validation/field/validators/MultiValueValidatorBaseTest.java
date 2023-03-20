package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import io.vavr.control.Option;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiValueValidatorBaseTest {

    private MultiValueValidatorBase validator = new MultiValueValidatorBase() {
        @Override
        public ValidationResult validateValue(String value, ValidatableField field, Map<String, Object> params,
                                              Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
            return !"INVALID".equals(value)
                    ? ValidationResult.ok()
                    : ValidationResult.invalid(field, "Invalid value");
        }

        @Override
        public String getName() { return null; }
    };

    @ParameterizedTest(name = "List with values [{0}, {1}, {2}] is validated to: {3}")
    @CsvSource(delimiter = '|', value = {
            //    1st |     2nd |     3rd | Expected
            "       1 |       2 |       3 |     true",
            "       1 |         |         |     true",
            "         |         |         |     true",
            "         |       2 |         |     true",
            "         |      '' |       3 |     true",
            "      '' |         |         |     true",
            "      '' |         |      '' |     true",
            "       1 |       2 | INVALID |    false",
            "       1 | INVALID |       3 |    false",
            " INVALID |       2 |       3 |    false",
    })
    void validate(String l1, String l2, String l3, boolean expected) {
        // given
        TestField field = new TestField(l1, l2, l3);

        // when
        ValidationResult result = validator.validate(field, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expected);
    }

    // -------------------- INNER CLASSES --------------------

    static class TestField implements ValidatableField {
        private List<String> values = new ArrayList<>();

        public TestField(String... values) {
            this.values.addAll(Arrays.asList(values));
        }

        @Override
        public List<String> getValidatableValues() {
            return values;
        }

        @Override public DatasetFieldType getDatasetFieldType() { return null; }
        @Override public Option<ValidatableField> getParent() { return null; }
        @Override public List<ValidatableField> getChildren() { return null; }
        @Override public void setValidationMessage(String message) { }
        @Override public String getValidationMessage() { return null; }
    }
}