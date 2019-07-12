/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.dublincore;

import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import org.junit.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
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
        byte[] file = Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource("json/export/ddi/dataset-finch1.json").toURI()));
        String datasetVersionAsJson = new String(file);
        JsonReader jsonReader = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject obj = jsonReader.readObject();

        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files
                .readAllBytes(Paths.get(getClass().getClassLoader()
                        .getResource("xml/export/ddi/dataset-finchDC.xml").toURI()))));
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
