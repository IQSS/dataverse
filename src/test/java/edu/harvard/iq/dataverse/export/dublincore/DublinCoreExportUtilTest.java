/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.export.dublincore;

import edu.harvard.iq.dataverse.util.json.JsonUtil;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;


/**
 *
 * @author skraffmi
 */
public class DublinCoreExportUtilTest {

    private static final Logger logger = Logger.getLogger(DublinCoreExportUtilTest.class.getCanonicalName());

    /**
     * Test of datasetJson2dublincore method, of class DublinCoreExportUtil.
     */
    @Test
    public void testDatasetJson2dublincore() throws Exception {
        // given
        JsonObject obj = JsonUtil.getJsonObjectFromFile("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        
        Path dubCoreFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finchDC.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(dubCoreFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);
        
        OutputStream output = new ByteArrayOutputStream();
        
        // when
        DublinCoreExportUtil.datasetJson2dublincore(obj, output, DublinCoreExportUtil.DC_FLAVOR_DCTERMS);
        String result = XmlPrinter.prettyPrintXml(output.toString());
        
        // then
        logger.fine(result);
        XmlAssert.assertThat(result).and(datasetAsDdi).ignoreWhitespace().areSimilar();
    }
    
}
