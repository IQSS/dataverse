package edu.harvard.iq.dataverse.util;

import org.junit.Test;

import edu.harvard.iq.dataverse.util.Organizations;

import static org.junit.Assert.*;

/**
 *
 * @author francesco.cadili@4science.it
 */
public class OrganizationsTest {

    private final Organizations organizations;

    public OrganizationsTest() {
        organizations = Organizations.getInstance();
    }

    @Test
    public void checkLanguageTest() {
        assertEquals("en", organizations.getLanguage(Organizations.ORGANIZATION_MODELS[0]));
        assertEquals("es", organizations.getLanguage(Organizations.ORGANIZATION_MODELS[1]));
        assertEquals("en", organizations.getLanguage(Organizations.TOCKENIZER_MODELS[0]));
    }
    
    @Test
    public void testOrganizationSimpleName() {
        assertTrue(organizations.isOrganization("IBM"));
        assertTrue(organizations.isOrganization("Harvard University"));
    }

    @Test
    public void testOrganizationCOMPLEXName() {
        assertTrue(organizations.isOrganization("The Institute for Quantitative Social Science"));
        assertTrue(organizations.isOrganization("Council on Aging"));
        assertTrue(organizations.isOrganization("The Ford Foundation"));
        assertTrue(organizations.isOrganization("United Nations Economic and Social Commission for Asia and the Pacific (UNESCAP)"));
        assertTrue(organizations.isOrganization("Michael J. Fox Foundation for Parkinson's Research"));
    }

    @Test
    public void testOrganizationComaOrDash() {
        assertTrue(organizations.isOrganization("Digital Archive of Massachusetts Anti-Slavery and Anti-Segregation Petitions, Massachusetts Archives, Boston MA"));
        assertTrue(organizations.isOrganization("U.S. Department of Commerce, Bureau of the Census, Geography Division"));
        assertTrue(organizations.isOrganization("Harvard Map Collection, Harvard College Library"));
        assertTrue(organizations.isOrganization("Geographic Data Technology, Inc. (GDT)"));
    }

    //@Test
    //public void testOrganizationES() {
    //    assertTrue(organizations.isOrganization("Compañía de San Fernando"));
    //}
    
    /**
     * Name is composed of:
     * <First Names> <Family Name>
     */
    @Test
    public void testName() {
        assertFalse(organizations.isOrganization("Jorge Mario Bergoglio"));
        assertFalse(organizations.isOrganization("Bergoglio"));
        assertFalse(organizations.isOrganization("Francesco Cadili"));
        // This Philip Seymour Hoffman example is from ShibUtilTest.
        assertFalse(organizations.isOrganization("Philip Seymour Hoffman"));

        // test Smith (is also a name)
        assertFalse(organizations.isOrganization("John Smith"));
        // resolved using hint file
        assertFalse(organizations.isOrganization("Guido van Rossum"));
        // test only name
        assertFalse(organizations.isOrganization("Francesco"));
        // test only family name
        assertFalse(organizations.isOrganization("Cadili"));
    }
}
