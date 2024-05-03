package edu.harvard.iq.dataverse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class FirstNameTest {

    private final FirstNames firstNames;

    public FirstNameTest() {
        firstNames = FirstNames.getInstance();
    }

    @Test
    public void testFirstName() {
        assertTrue(firstNames.isFirstName("Francesco"));
        assertTrue(firstNames.isFirstName("Abdul Salam"));
        assertTrue(firstNames.isFirstName("Lainey"));
    }

    /**
     * Name is composed of:
     * <First Names> <Family Name>
     */
    @Test
    public void testName() {
        assertEquals(firstNames.getFirstName("Jorge Mario Bergoglio"), "Jorge Mario");
        assertNull(firstNames.getFirstName("Bergoglio"));
        assertEquals(firstNames.getFirstName("Francesco Cadili"), "Francesco");
        // This Philip Seymour Hoffman example is from ShibUtilTest.
        assertEquals("Philip Seymour", firstNames.getFirstName("Philip Seymour Hoffman"));

        // test Smith (is also a name)
        assertEquals("John", firstNames.getFirstName("John Smith"));
        // resolved using hint file
        assertEquals("Guido", firstNames.getFirstName("Guido van Rossum"));
        // test only name
        assertEquals(firstNames.getFirstName("Francesco"), "Francesco");
        // test only family name
        assertEquals(firstNames.getFirstName("Cadili"), null);
    }

    /**
     * Name is composed of: The string is composed of:
     * <Family Name>, <First Names>
     */
    @Test
    public void testNameConvention2() {
        assertEquals(firstNames.getFirstName("Awesome, Audrey"), "Audrey");
        assertEquals(firstNames.getFirstName("Bergoglio, Jorge Mario"), "Jorge Mario");
        assertEquals(firstNames.getFirstName("Cadili, Francesco"), "Francesco");
        assertEquals("Guido", firstNames.getFirstName("van Rossum, Guido"));
    }

}
