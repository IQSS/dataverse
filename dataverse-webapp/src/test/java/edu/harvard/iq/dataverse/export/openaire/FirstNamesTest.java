package edu.harvard.iq.dataverse.export.openaire;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class FirstNamesTest {

    private final FirstNames firstNames;

    public FirstNamesTest() {
        firstNames = FirstNames.getInstance();
    }

    @Test
    public void testFirstName() {
        assertTrue(firstNames.isFirstName("Francesco"));
        assertTrue(firstNames.isFirstName("Abdul Salam"));
        assertTrue(firstNames.isFirstName("Lainey"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Awesome, Audrey|Audrey|Awesome",
            "Bergoglio, Jorge Mario|Jorge Mario|Bergoglio",
            "Cadili, Francesco|Francesco|Cadili",
            "Bergoglio|''|Bergoglio",
            "Francesco Cadili|Francesco|Cadili",
            "Philip Seymour Hoffman|Philip Seymour|Hoffman",
            "John Smith|John|Smith",
            "Guido van Rossum|Guido|van Rossum",
            "Francesco|Francesco|''",
            "Cadili|''|Cadili",
            "Jorge Mario Bergoglio|Jorge Mario|Bergoglio",
            "Jorge Bergoglio Mario Luigi|Jorge|Bergoglio Mario Luigi"
    })
    public void testExtractFirstAndLastNames(String fullname, String expectedFirstName, String expectedLastName) {
        assertThat(firstNames.extractFirstAndLastName(fullname))
                .returns(expectedFirstName, from(firstAndLastName -> firstAndLastName._1()))
                .returns(expectedLastName, from(firstAndLastName -> firstAndLastName._2()));
    }

}