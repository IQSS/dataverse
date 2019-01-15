
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class URLValidatorTest {

    @Test
    public void testIsEmailValid() {

        assertEquals(true, URLValidator.isURLValid("https://twitter.com/", null));

        assertEquals(false, URLValidator.isURLValid("cnn.com", null));

    }

}
