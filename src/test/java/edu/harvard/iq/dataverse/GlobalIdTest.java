package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.pidproviders.PidUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author rmp553
 */
public class GlobalIdTest {

    @Test
    public void testValidDOI() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId(DOIServiceBean.DOI_PROTOCOL,"10.5072","FK2/BYM3IW", "/", DOIServiceBean.DOI_RESOLVER_URL, null);

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
        // TODO review the generated test code and remove the default call to fail.
    }

    @Test
    public void testValidHandle() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId(HandlenetServiceBean.HDL_PROTOCOL, "1902.1","111012", "/", HandlenetServiceBean.HDL_RESOLVER_URL, null);

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

        GlobalId instance = testDS.getGlobalId();

        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072", instance.getAuthority());
        assertEquals("FK2/BYM3IW", instance.getIdentifier());
    }

    @Test
    public void testInject() {
        System.out.println("testInject (weak test)");

        // String badProtocol = "hdl:'Select value from datasetfieldvalue';/ha";
        GlobalId instance = PidUtil.parseAsGlobalID(HandlenetServiceBean.HDL_PROTOCOL, "'Select value from datasetfieldvalue';", "ha");
        assertNull(instance); 

        //exception.expect(IllegalArgumentException.class);
        //exception.expectMessage("Failed to parse identifier: " + badProtocol);
        //new GlobalId(badProtocol);
    }

    @Test
    @Disabled /* Could now add a 'doy' protocol so the test would have to check against registered PIDProviders (currently Beans)*/
    public void testUnknownProtocol() {
        System.out.println("testUnknownProtocol");

        String badProtocol = "doy:10.5072/FK2/BYM3IW";
        
        //exception.expect(IllegalArgumentException.class);
        //exception.expectMessage("Failed to parse identifier: " + badProtocol);
        //new GlobalId(badProtocol);
    }

    @Test
    @Disabled /* Could now change parsing rules so the test would have to check against registered PIDProviders (currently Beans)*/
    public void testBadIdentifierOnePart() {
        System.out.println("testBadIdentifierOnePart");

        //exception.expect(IllegalArgumentException.class);
        //exception.expectMessage("Failed to parse identifier: 1part");
        //new GlobalId("1part");
    }

    @Test
    @Disabled /* Could now change parsing rules so the test would have to check against registered PIDProviders (currently Beans)*/
    public void testBadIdentifierTwoParts() {
        System.out.println("testBadIdentifierTwoParts");

        //exception.expect(IllegalArgumentException.class);
        //exception.expectMessage("Failed to parse identifier: doi:2part/blah");
        //new GlobalId("doi:2part/blah");
    }

    @Test
    public void testIsComplete() {
        assertFalse(new GlobalId("doi", "10.123", null, null, null, null).isComplete());
        assertFalse(new GlobalId("doi", null, "123", null, null, null).isComplete());
        assertFalse(new GlobalId(null, "10.123", "123", null, null, null).isComplete());
        assertTrue(new GlobalId("doi", "10.123", "123", null, null, null).isComplete());
    }

    @Test
    public void testVerifyImportCharacters() {
        assertTrue(GlobalId.verifyImportCharacters("-"));
        assertTrue(GlobalId.verifyImportCharacters("qwertyQWERTY"));
        assertFalse(GlobalId.verifyImportCharacters("Hällochen"));
        assertFalse(GlobalId.verifyImportCharacters("*"));
    }
}
