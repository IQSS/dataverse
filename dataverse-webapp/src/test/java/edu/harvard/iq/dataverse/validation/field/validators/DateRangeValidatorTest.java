package edu.harvard.iq.dataverse.validation.field.validators;

import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.validation.field.ValidationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeValidatorTest {

    private DateRangeValidator validator = new DateRangeValidator();

    @ParameterizedTest(name = "It''s {2} that date range from [{0}] to [{1}] is valid")
    @CsvSource(delimiter = '|', value = {
        // Date FROM |    Date TO | Expected result
        "       1980 |       2001 |            true",
        "    1980-01 |    2001-10 |            true",
        " 1980-01-12 | 2001-10-07 |            true",
        " 1980-01-12 |            |            true",
        "            | 2001-10-07 |            true",
        "         '' | 2001-10-07 |            true",
        "            |            |            true",
        "         '' |         '' |            true",
        "      -1000 |       1000 |            true",
        "        987 |            |           false",
        "       qwer |       1234 |           false",
        "       1234 |       qwer |           false",
        "        987 |            |           false",
        "    0987-13 |            |           false",
        " 0987-12-33 |            |           false",
        "       2000 |       1000 |           false",
    })
    void validate(String from, String to, boolean expected) {
        // given
        SearchField field = new SearchField(null, null, null, null) {
            @Override
            public List<String> getValidatableValues() { return Arrays.asList(from, to); }

            @Override
            public QueryPart getQueryPart() { return QueryPart.EMPTY; }
        };

        // when
        ValidationResult result = validator.validate(field, Collections.emptyMap(), Collections.emptyMap());

        // then
        assertThat(result.isOk()).isEqualTo(expected);
    }
}