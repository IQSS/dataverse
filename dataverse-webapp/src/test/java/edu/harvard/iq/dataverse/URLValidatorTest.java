package edu.harvard.iq.dataverse;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author skraffmi
 */
public class URLValidatorTest {

    @Test
    public void testIsURLValid() {

        assertEquals(true, URLValidator.isURLValid("https://twitter.com/"));

        assertEquals(false, URLValidator.isURLValid("cnn.com"));

    }

}
