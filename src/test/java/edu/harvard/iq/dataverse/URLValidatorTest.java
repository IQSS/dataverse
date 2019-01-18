
package edu.harvard.iq.dataverse;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author skraffmi
 */
public class URLValidatorTest {

    @Test
    public void testIsURLValid() {

        assertEquals(true, URLValidator.isURLValid("https://twitter.com/"));

        assertEquals(false, URLValidator.isURLValid("cnn.com"));

    }

}
