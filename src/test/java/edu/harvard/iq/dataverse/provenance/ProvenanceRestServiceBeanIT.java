package edu.harvard.iq.dataverse.provenance;

import java.util.Map;
import javax.json.JsonObject;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

/**
 * These tests are not expected to pass unless you have the prov service
 * installed and configured properly . They are intended to help a developer get
 * set up for prov development.
 */
public class ProvenanceRestServiceBeanIT {

    static ProvenanceRestServiceBean provenanceRestServiceBean;
    static final String provUrlInDev = "http://localhost:7777";

    public ProvenanceRestServiceBeanIT() {
    }

    @BeforeClass
    public static void setUpClass() {
        provenanceRestServiceBean = new ProvenanceRestServiceBean();
        provenanceRestServiceBean.setProvBaseUrl(provUrlInDev);
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

    /**
     * Test of init method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testInit() {
        System.out.println("init");
        Exception expectedException = null;
        try {
            provenanceRestServiceBean.init();
        } catch (Exception ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    /**
     * Test of setProvBaseUrl method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testSetProvBaseUrl() throws Exception {
        System.out.println("setProvBaseUrl");
        String provUrlInProd = "http://localhost:5000";
        provenanceRestServiceBean.setProvBaseUrl(provUrlInProd);
        assertEquals(provUrlInProd, provenanceRestServiceBean.provBaseUrl);
        provenanceRestServiceBean.setProvBaseUrl(provUrlInDev);
        assertEquals(provUrlInDev, provenanceRestServiceBean.provBaseUrl);
    }

    /**
     * Test of getVersion method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testGetVersion() throws Exception {
        System.out.println("getVersion");
        assertEquals("3.0", provenanceRestServiceBean.getVersion());
    }

    /**
     * Test of getBundleId method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testGetBundleId() throws Exception {
        System.out.println("getBundleId");
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName("someName");
        Map<String, String> result = provenanceRestServiceBean.getBundleId(bundleId);
        System.out.println("result: " + result);
        assertEquals("someName", result.get("name"));
        // FIXME: Trying to get the id of a non-existent should simply return null rather than throwing a JSONException.
        Exception expectedException = null;
        try {
            Map<String, String> maxResult = provenanceRestServiceBean.getBundleId(Long.MAX_VALUE);
        } catch (org.json.JSONException ex) {
            expectedException = ex;
        }
        assertNotNull(expectedException);
    }

    /**
     * Test of createEmptyBundleFromName method, of class
     * ProvenanceRestServiceBean.
     */
    @Test
    public void testCreateEmptyBundleFromName() throws Exception {
        System.out.println("createEmptyBundleFromName");
        String bundleName = "foo";
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName(bundleName);
        System.out.println("bundleId: " + bundleId);
        // Should be a positive number (long).
        assertTrue(bundleId > 0 && bundleId <= Long.MAX_VALUE);
    }

    /**
     * Test of deleteBundle method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testDeleteBundle() throws Exception {
        System.out.println("deleteBundle");
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName("deleteMe");
        boolean deleted = provenanceRestServiceBean.deleteBundle(bundleId);
        assertTrue(deleted);
        // Make sure you can't look it up any more.
        Map<String, String> result = provenanceRestServiceBean.getBundleId(bundleId);
        assertNull(result);
        boolean deletedUnknownId = provenanceRestServiceBean.deleteBundle(Long.MAX_VALUE);
        assertFalse(deletedUnknownId);
    }

    /**
     * Test of getBundleJson method, of class ProvenanceRestServiceBean.
     */
    @Test
    public void testGetBundleJson() throws Exception {
        System.out.println("getBundleJson");
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName("justAnIdWithoutJsonUploadedYet");
        JsonObject result = provenanceRestServiceBean.getBundleJson(bundleId);
        System.out.println("result: " + result);
        assertEquals("", result.getString("JSON"));
        JsonObject maxResult = provenanceRestServiceBean.getBundleJson(Long.MAX_VALUE);
        // TODO: Report that it's weird to get a 200 response and "JSON": "" back rather than a 404 for a bundle that doesn't exit.
        System.out.println("maxResult: " + maxResult);
    }

    /**
     * Test of uploadProvJsonForBundle method, of class
     * ProvenanceRestServiceBean.
     */
    // FIXME: Remove this Ignore and test this.
    @Ignore
    @Test
    public void testUploadProvJsonForBundle() throws Exception {
        System.out.println("uploadProvJsonForBundle");
        // FIXME: Support sending standard javax.json.JsonObject (JSON-P, JRS 353) rather than org.json.JSONObject.
        JSONObject provJson = null;
        boolean expResult = false;
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName("weAreGoingToUploadSomeJson");
        String bundleName = "huh? Shouldn't we be sending an id?";
        boolean result = provenanceRestServiceBean.uploadProvJsonForBundle(provJson, bundleName);
    }

}
