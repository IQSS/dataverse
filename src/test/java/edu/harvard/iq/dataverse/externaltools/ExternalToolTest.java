package edu.harvard.iq.dataverse.externaltools;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolTest {

    @Test
    public void testGetButtonLabel() {
        System.out.println("getButtonLabel");
        ExternalTool externalTool = new ExternalTool();
        // TODO: In order for external tools like PSI to be modular, they should not have entries in Bundle.properties.
        externalTool.setDisplayNameBundleKey("psi.displayName");
        assertEquals("Privacy-Preserving Data Preview", externalTool.getButtonLabel());
    }

}
