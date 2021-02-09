package edu.harvard.iq.dataverse.api.dto.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MailDomainListValidatorTest {

    private MailDomainListValidator validator = new MailDomainListValidator();

    // -------------------- TESTS --------------------

    @ParameterizedTest
    @ValueSource(strings = { "icm.edu.pl", ".p.lodz.pl", "a-b-c.edu.pl", "Uppercase1.Domain2.Co3.Uk" })
    @DisplayName("Should accept valid domain list values")
    void isValid__accept(String item) {

        // given
        List<String> input = Collections.singletonList(item);

        // when
        boolean valid = validator.isValid(input, null);

        // then
        assertThat(valid).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "@#$%^&", "not a domain" })
    @DisplayName("Should reject invalid domain list values")
    void isValid__reject(String item) {

        // given
        List<String> input = Collections.singletonList(item);

        // when
        boolean valid = validator.isValid(input, null);

        // then
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Should reject null list and null value on list")
    void isValid__nulls() {

        // given
        List<String> inputWithNullItem = Collections.singletonList(null);
        List<String> nullInput = null;

        // when
        boolean isValidInputWithNullItem = validator.isValid(inputWithNullItem, null);
        boolean isValidNullInput = validator.isValid(nullInput, null);

        // then
        assertThat(isValidInputWithNullItem).isFalse();
        assertThat(isValidNullInput).isFalse();
    }

    @Test
    @DisplayName("Should accept list of valid values then reject it after adding one invalid")
    void isValid__multiElementList() {

        // given
        List<String> input = Stream.of(".icm.edu.pl", ".edu.pl", ".pl").collect(Collectors.toList());

        // when
        boolean valid = validator.isValid(input, null);

        // then
        assertThat(valid).isTrue();

        // when
        input.add(" ");
        valid = validator.isValid(input, null);

        // then
        assertThat(valid).isFalse();
    }
}