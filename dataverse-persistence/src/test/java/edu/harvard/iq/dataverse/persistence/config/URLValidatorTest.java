package edu.harvard.iq.dataverse.persistence.config;

import edu.harvard.iq.dataverse.persistence.config.URLValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
