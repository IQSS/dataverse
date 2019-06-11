package edu.harvard.iq.dataverse;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author rmp553
 */
public class GlobalIdTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testValidDOI() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId("doi:10.5072/FK2/BYM3IW");

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
        // TODO review the generated test code and remove the default call to fail.
    }

    @Test
    public void testValidHandle() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId("hdl:1902.1/111012");

        assertEquals("hdl", instance.getProtocol());
        assertEquals("1902.1", instance.getAuthority());
        assertEquals("111012", instance.getIdentifier());
    }

    @Test
    public void testContructFromDataset() {
        Dataset testDS = new Dataset();

        testDS.setProtocol("doi");
        testDS.setAuthority("10.5072");
        testDS.setIdentifier("FK2/BYM3IW");

        GlobalId instance = new GlobalId(testDS);

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
    }

    @Test
    public void testInject() {
        System.out.println("testInject (weak test)");

        String badProtocol = "hdl:'Select value from datasetfieldvalue';/ha";

        GlobalId instance = new GlobalId(badProtocol);

        assertEquals("hdl", instance.getProtocol());
        assertEquals("Selectvaluefromdatasetfieldvalue", instance.getAuthority());
        assertEquals("ha", instance.getIdentifier());
        //exception.expect(IllegalArgumentException.class);
        //exception.expectMessage("Failed to parse identifier: " + badProtocol);
        //new GlobalId(badProtocol);
    }

    @Test
    public void testUnknownProtocol() {
        System.out.println("testUnknownProtocol");

        String badProtocol = "doy:10.5072/FK2/BYM3IW";

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: " + badProtocol);
        new GlobalId(badProtocol);
    }

    @Test
    public void testBadIdentifierOnePart() {
        System.out.println("testBadIdentifierOnePart");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: 1part");
        new GlobalId("1part");
    }

    @Test
    public void testBadIdentifierTwoParts() {
        System.out.println("testBadIdentifierTwoParts");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: doi:2part/blah");
        new GlobalId("doi:2part/blah");
    }

    @Test
    public void testIsComplete() {
        assertFalse(new GlobalId("doi", "10.123", null).isComplete());
        assertFalse(new GlobalId("doi", null, "123").isComplete());
        assertFalse(new GlobalId(null, "10.123", "123").isComplete());
        assertTrue(new GlobalId("doi", "10.123", "123").isComplete());
    }

    @Test
    public void testVerifyImportCharacters() {
        assertTrue(GlobalId.verifyImportCharacters("-"));
        assertTrue(GlobalId.verifyImportCharacters("qwertyQWERTY"));
        assertFalse(GlobalId.verifyImportCharacters("Hällochen"));
        assertFalse(GlobalId.verifyImportCharacters("*"));
    }
}
