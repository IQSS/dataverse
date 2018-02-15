package edu.harvard.iq.dataverse.provenance;

import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
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
        Map<String, String> maxResult = provenanceRestServiceBean.getBundleId(Long.MAX_VALUE);
        assertNull(maxResult);
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
        // We haven't uploaded any JSON yet, so we expect "null" in the "JSON" object.
        assertEquals("null", result.getString("JSON"));
        // Now let's upload some JSON
        // FIXME: What JSON should we send to the prov service? "entity" seems to be required. See also testUploadProvJsonForBundle below for a more real example involving a news item.
        JsonObjectBuilder innerJson = Json.createObjectBuilder();
        innerJson.add("entity", Json.createObjectBuilder()
                .add("event", Json.createObjectBuilder()
                        .add("prov:type", "fileUploaded"))
        );
        JsonObject provJson = innerJson.build();
        System.out.println("uploading JSON for bundle id " + bundleId);
        // TODO: What would a better bundle name be?
        String bundleName = bundleId + "-uploadJson";
        JsonObject jsonUploadResponse = provenanceRestServiceBean.uploadProvJsonForBundle(provJson, bundleName);
        System.out.println("jsonUploadResponse: " + jsonUploadResponse);
        // FIXME: What should we call this id? It's one higher than the bundle id?
        int idInJson = (int) bundleId + 1;
        assertEquals(idInJson, jsonUploadResponse.getInt("id"));
        JsonObject jsonOut = provenanceRestServiceBean.getBundleJson(idInJson);
        System.out.println("outer json out: " + jsonOut.toString());
        System.out.println("outer json out (pretty print): " + JsonUtil.prettyPrint(jsonOut.toString()));
        String innerJsonAsString = jsonOut.getString("JSON");
        JsonReader jsonReader = Json.createReader(new StringReader(innerJsonAsString));
        JsonObject innerJsonOut = jsonReader.readObject();
        System.out.println("inner json out: " + JsonUtil.prettyPrint(jsonOut.getString("JSON")));
        // FIXME: For real what JSON should we expect to be exported when you click "Export Provenance" from Dataverse?
        assertEquals("fileUploaded", innerJsonOut.getJsonObject("entity").getJsonObject("event").getString("prov:type"));

        // Let's try getting JSON from a bundleId that doesn't exist
        JsonObject maxResult = provenanceRestServiceBean.getBundleJson(Long.MAX_VALUE);
        System.out.println("maxResult: " + maxResult);
        assertEquals("null", result.getString("JSON"));
    }

    /**
     * Test of uploadProvJsonForBundle method, of class
     * ProvenanceRestServiceBean.
     */
    @Test
    public void testUploadProvJsonForBundle() throws UnirestException {
        System.out.println("uploadProvJsonForBundle");
        JsonObjectBuilder innerJson = Json.createObjectBuilder();
        innerJson.add("entity", Json.createObjectBuilder()
                .add("bbc:news/science-environment-17526723", Json.createObjectBuilder()
                        .add("prov:type", "a news item for desktop"))
                .add("bbc:news/mobile/science-environment-17526723", Json.createObjectBuilder()
                        .add("prov:type", "a news item for mobile devices"))
        );
        innerJson.add("alternateOf", Json.createObjectBuilder()
                .add("_:aO1", Json.createObjectBuilder()
                        .add("prov:alternate2", "bbc:news/science-environment-17526723")
                        .add("prov:alternate1", "bbc:news/mobile/science-environment-17526723")
                )
        );
        JsonObject provJson = innerJson.build();
        String uuid = UUID.randomUUID().toString();
        long bundleId = provenanceRestServiceBean.createEmptyBundleFromName(uuid);
        System.out.println("uploading JSON for bundle id " + bundleId);
        // TODO: What would a better bundle name be?
        String bundleName = bundleId + "-uploadJson";
        JsonObject result = provenanceRestServiceBean.uploadProvJsonForBundle(provJson, bundleName);
        System.out.println("result: " + result);
        System.out.println("result: " + JsonUtil.prettyPrint(result.toString()));
        long idReturned = result.getJsonNumber("id").longValue();
        System.out.println("id returned: " + idReturned);
        assertTrue(idReturned > 0 && idReturned <= Long.MAX_VALUE);
        // Is the id returned (i.e. 51) always one greater than the bundle id (i.e. 50)?
        assertEquals(idReturned, bundleId + 1);
    }

}
