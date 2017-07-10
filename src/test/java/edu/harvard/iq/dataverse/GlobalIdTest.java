/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import java.net.URL;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 *
 * @author rmp553
 */
public class GlobalIdTest {
    
    public GlobalIdTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();


    @Test
    public void testValidDOI() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId("doi:10.5072/FK2/BYM3IW");
            
        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072/FK2", instance.getAuthority());
        assertEquals("BYM3IW", instance.getIdentifier());
        // TODO review the generated test code and remove the default call to fail.
    }

    /**
     * test string -> doi -> string round trip
     */
    @Test
    public void testRoundTripDOI()
    {
	    System.out.println("testRountTripDOI");
	    String doi0 = "doi:10.7910/DVN1/22641";
	    GlobalId instance = new GlobalId( doi0 );
	    String doi1 = instance.toString();
	    assertTrue( doi0.equals( doi1 ) );
    }
    /**
     * test DOI URL generation
     */
    @Test
    public void testDOIURL()
    {
	    System.out.println("testDOIURL");
	    String doi0 = "doi:10.7910/DVN1/22641";
	    GlobalId instance = new GlobalId( doi0 );
	    String url_e = "https://dx.doi.org/10.7910/DVN1/22641";
	    URL url = instance.toURL();
	    String url_r = url.toString();
	    System.out.format("url_e : %s \n", url_e);
	    System.out.format("url_r : %s \n", url_r);
	    assertTrue( url_e.equals( url_r ) );
    }

    /**
     * test if a DOI has been publicly registered; using the oldest DOI dataset
     * from Harvard Dataverse
     */
    @Test
    public void testRegisteredDOI()
    {
	    String existingDOI = "doi:10.7910/DVN1/22641"; // hdl:1902.1/10379 is oldest handle, looks older - should anyone have interest in extending this
    }
    
    
    @Test
    public void testValidHandle() {
        System.out.println("testValidDOI");
        GlobalId instance = new GlobalId("hdl:1902.1/111012");
            
        assertEquals("hdl", instance.getProtocol());
        assertEquals("1902.1", instance.getAuthority());
        assertEquals("111012", instance.getIdentifier());
        // TODO review the generated test code and remove the default call to fail.
    }
    
    @Test
    public void testContructFromDataset(){
        Dataset testDS = new Dataset();
        
        testDS.setProtocol("doi");
        testDS.setAuthority("10.5072/FK2");
        testDS.setIdentifier("BYM3IW");
        
        GlobalId instance = new GlobalId(testDS);
            
        assertEquals("doi", instance.getProtocol());
        assertEquals("10.5072/FK2", instance.getAuthority());
        assertEquals("BYM3IW", instance.getIdentifier());
        
    }
    

    @Test
    public void testInject(){
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
    public void testUnknownProtocol(){
        System.out.println("testUnknownProtocol");
        
        String badProtocol = "doy:10.5072/FK2/BYM3IW";
        
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: " + badProtocol);
        new GlobalId(badProtocol);
    }


    @Test
    public void testBadIdentifierOnePart(){
        System.out.println("testBadIdentifierOnePart");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: 1part");
        new GlobalId("1part");
    }
    

    @Test
    public void testBadIdentifierTwoParts(){
        System.out.println("testBadIdentifierTwoParts");

        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Failed to parse identifier: doi:2part/blah");
        new GlobalId("doi:2part/blah");
    }

    
    /**
     * Test of toURL method, of class GlobalId.
     */
    //TODO(PM) - why was this commented out / not testing the toURL call?
    /*
    @Test
    public void testToURL() {
        System.out.println("toURL");
        GlobalId instance = null;
        URL expResult = null;
        URL result = instance.toURL();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    */
}
