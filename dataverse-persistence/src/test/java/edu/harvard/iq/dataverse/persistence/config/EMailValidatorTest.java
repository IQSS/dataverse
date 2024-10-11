package edu.harvard.iq.dataverse.persistence.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author philip durbin
 * @author oscardssmith
 * @author rohit bhattacharjee
 * @author stephen kraffmiller
 * @author alex scheitlin
 */
public class EMailValidatorTest {

    public static Stream<Arguments> parameters() {
        return Stream.of(
                Arguments.of(true, "pete@mailinator.com"),

                // @todo How can this be a valid email address?
                Arguments.of(true, " leadingWhitespace@mailinator.com"),

                // @todo How can this be a valid email address?
                Arguments.of(true, "trailingWhitespace@mailinator.com "),

                Arguments.of(false, "elisah.da mota@example.com"),
                Arguments.of(false, "pete1@mailinator.com;pete2@mailinator.com"),

                /**
                 * These examples are all from https://randomuser.me and seem to be
                 * valid according to
                 * http://sphinx.mythic-beasts.com/~pdw/cgi-bin/emailvalidate (except
                 * رونیکا.محمدخان@example.com).
                */
                Arguments.of(true, "michélle.pereboom@example.com"),
                Arguments.of(true, "begüm.vriezen@example.com"),
                Arguments.of(true, "lótus.gonçalves@example.com"),
                Arguments.of(true, "lótus.gonçalves@éxample.com"),
                Arguments.of(true, "begüm.vriezen@example.cologne"),
                Arguments.of(true, "رونیکا.محمدخان@example.com"),
                Arguments.of(false, "lótus.gonçalves@example.cóm"),
                Arguments.of(false, "dora@.com"),
                Arguments.of(false, ""),
                Arguments.of(false, null),

                // add tests for 4601
                Arguments.of(true, "blah@wiso.uni-unc.de"),
                Arguments.of(true, "foo@essex.co.uk"),
                Arguments.of(true, "jack@bu.cloud")
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    public void testIsEmailValid(boolean isValid, String email) {
        assertEquals(isValid, EMailValidator.isEmailValid(email, null));
    }

}
