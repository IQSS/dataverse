/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.dublincore;

import edu.harvard.iq.dataverse.export.ddi.DdiExportUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.primefaces.json.JSONObject;

/**
 *
 * @author skraffmi
 */
public class DublinCoreExportUtilTest {

    private static final Logger logger = Logger.getLogger(DublinCoreExportUtilTest.class.getCanonicalName());
    
    public DublinCoreExportUtilTest() {
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

    /**
     * Test of datasetJson2dublincore method, of class DublinCoreExportUtil.
     */
    @Test
    public void testDatasetJson2dublincore() throws Exception {

        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
       
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject obj = jsonReader.readObject();
        
        File dubCoreFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finchDC.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(dubCoreFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        
        OutputStream output = new ByteArrayOutputStream();
        DublinCoreExportUtil.datasetJson2dublincore(obj, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = XmlPrinter.prettyPrintXml(output.toString());
        
        logger.info(result);
        assertEquals(datasetAsDdi, result);
        
        /*
        System.out.println("datasetJson2dublincore");
        JsonObject datasetDtoAsJson = null;
        OutputStream expResult = null;
        OutputStream result = DublinCoreExportUtil.datasetJson2dublincore(datasetDtoAsJson);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
*/
    }
    
}
