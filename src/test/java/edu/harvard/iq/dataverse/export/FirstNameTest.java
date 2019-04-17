package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.openaire.FirstNames;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author fcadili
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
    
    /***
     * Name is composed of:
     *      <First Names> <Family Name>
     */
    @Test
    public void testName() {
        assertEquals(firstNames.getFirstName("Jorge Mario Bergoglio"), "Jorge Mario");
        assertNull(firstNames.getFirstName("Bergoglio"));
        assertEquals(firstNames.getFirstName("Francesco Cadili"), "Francesco");
    }
    
    /***
     * Name is composed of:
     *     <Family Name>, <First Names>
     */
    @Test
    public void testNameConvention2() {
       assertEquals(firstNames.getFirstName("Awesome, Audrey"), "Audrey");
       assertEquals(firstNames.getFirstName("Bergoglio, Jorge Mario"), "Jorge Mario");
       assertEquals(firstNames.getFirstName("Cadili, Francesco"), "Francesco");
    }
    
}
