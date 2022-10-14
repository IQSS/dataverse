package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.export.openaire.Cleanup;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class CleanupTest {
    
    /**
     * full name or organization name cleanup.
     *
     * Name is composed of:
     * <First Names> <Family Name>
     */
    @Test
    public void testNormalize() {
        assertEquals(Cleanup.normalize("    Francesco    "), "Francesco");
        assertEquals(Cleanup.normalize("Francesco  Cadili "), "Francesco Cadili");
        assertEquals(Cleanup.normalize("  Cadili,Francesco"), "Cadili, Francesco");
        assertEquals(Cleanup.normalize("Cadili,     Francesco  "), "Cadili, Francesco");
        assertEquals(Cleanup.normalize(null), "");
        
        // TODO: organization examples...
    }
}
