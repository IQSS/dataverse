package edu.harvard.iq.dataverse.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class OrcidValidatorTest {

    private final OrcidValidator validator = new OrcidValidator();

    @ParameterizedTest
    @CsvSource({
            "0000-0002-1825-0097, true,",
            "0000-0001-5109-3700, true,",
            "0000-0002-1694-233X, true,",
            "000000021694233X, false, orcid.invalid.format",
            "0000-B002-1694-233X, false, orcid.invalid.format",
            "000-80029-1694-233X, false, orcid.invalid.format",
            ", false, orcid.invalid.format",
            "0000-0002-1694-2331, false, orcid.invalid.checksum"
    })
    void validate(String value, boolean expectedResult, String expectedErrorCode) {
        // when
        ValidationResult result = validator.validate(value);

        // then
        assertThat(result.isOk()).isEqualTo(expectedResult);
        assertThat(result.getErrorCode()).isEqualTo(expectedErrorCode);
    }
}
