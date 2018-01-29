package edu.harvard.iq.dataverse.provenance;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ProvUtilTest {

    @Test
    public void testGetCplVersion() {
        assertEquals("3.0", ProvUtil.getCplVersion());
    }

}
