package edu.harvard.iq.dataverse.export.openaire;

import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals(Cleanup.normalize("    Francesco    "), "Francesco");
        Assert.assertEquals(Cleanup.normalize("Francesco  Cadili "), "Francesco Cadili");
        Assert.assertEquals(Cleanup.normalize("  Cadili,Francesco"), "Cadili, Francesco");
        Assert.assertEquals(Cleanup.normalize("Cadili,     Francesco  "), "Cadili, Francesco");
        Assert.assertEquals(Cleanup.normalize(null), "");

        // TODO: organization examples...
    }
}