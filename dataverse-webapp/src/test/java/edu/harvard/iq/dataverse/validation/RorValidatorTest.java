package edu.harvard.iq.dataverse.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class RorValidatorTest {

    private RorValidator validator = new RorValidator();

    @ParameterizedTest
    @CsvSource({
            "https://ror.org/01q8f6705, true,",
            "https://ror.org/01pp8nd67, true,",
            "https://ror.org/00kv9pj15, true,",
            "https://ror.org/019wvm592, true,",
            "https://ror.org/05cggb038, true,",
            "https://ror.org/04xfq0f34, true,",
            "https://ror.org/04xfq0f34, true,",
            "https://ror.org/14xfq0f34, false, ror.invalid.format",
            "https://ror.org/04XFQ0F34, false, ror.invalid.format",
            ", false, ror.invalid.format",
            "https://ror.org/ZENNEZ12345, false, ror.invalid.format",
            "https://ror.org/012222233, false, ror.invalid.checksum",
            "https://ror.org/0m12t2p21, false, ror.invalid.checksum",
            "https://ror.org/, false, ror.invalid.format",
            "11abcde12, false, ror.invalid.format"
    })
    void validate(String value, boolean expectedResult, String expectedErrorCode) {
        // when
        ValidationResult result = validator.validate(value);

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
        assertThat(result.getErrorCode()).isEqualTo(expectedErrorCode);
    }
}
