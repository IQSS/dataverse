package edu.harvard.iq.dataverse.externaltools;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ExternalToolTest {

    @Test
    public void testGetButtonLabel() {
        System.out.println("getButtonLabel");
        ExternalTool externalTool = new ExternalTool();
        externalTool.setDisplayName("Privacy-Preserving Data Preview");
        String buttonLabel = externalTool.getButtonLabel();
        assertEquals("Privacy-Preserving Data Preview", buttonLabel);
    }

}
