package edu.harvard.iq.dataverse.export.openaire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals(Cleanup.normalize("    Francesco    "), "Francesco");
        Assertions.assertEquals(Cleanup.normalize("Francesco  Cadili "), "Francesco Cadili");
        Assertions.assertEquals(Cleanup.normalize("  Cadili,Francesco"), "Cadili, Francesco");
        Assertions.assertEquals(Cleanup.normalize("Cadili,     Francesco  "), "Cadili, Francesco");
        Assertions.assertEquals(Cleanup.normalize(null), "");

        // TODO: organization examples...
    }

    @Test
    public void normalizeSlash() {
        //given
        String funderInfo = "funder/example";

        //when
        String normalizationResult = Cleanup.normalizeSlash(funderInfo);

        //then
        Assertions.assertEquals("funder%2Fexample", normalizationResult);
    }
}