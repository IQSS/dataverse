package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(OrderAnnotation.class)
public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Mock
    SettingsServiceBean settingsSvc;
    
    @Test
    @Order(1)
    public void testJson2DdiNoFiles() throws Exception {
        Mockito.when(settingsSvc.isTrueForKey(SettingsServiceBean.Key.ExportInstallationAsDistributorOnlyWhenNotSet, false)).thenReturn(false);
        DdiExportUtil.injectSettingsService(settingsSvc);
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.info(result);
        assertEquals(datasetAsDdi, result);
    }

    @Test
    public void testExportDDI() throws Exception {
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-create-new-all-ddi-fields.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/exportfull.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.info(result);
        assertEquals(datasetAsDdi, result);
    }

    @Test
    public void testJson2ddiHasFiles() throws Exception {
        /**
         * Note that `cat dataset-spruce1.json | jq
         * .datasetVersion.files[0].datafile.description` yields an empty string
         * but datasets created in the GUI sometimes don't have a description
         * field at all.
         */
        File datasetVersionJson = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));
        File ddiFile = new File("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(new String(Files.readAllBytes(Paths.get(ddiFile.getAbsolutePath()))));
        logger.info(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.info(result);
        boolean filesMinimallySupported = false;
        // TODO: 
        // setting "filesMinimallySupported" to false here, thus disabling the test;
        // before we can reenable it again, we'll need to figure out what to do 
        // with the access URLs, that are now included in the fileDscr and otherMat
        // sections. So a) we'll need to add something like URI=http://localhost/api/access/datafile/12 to 
        // dataset-spruce1.xml, above; and modify the DDI export util so that 
        // it can be instructed to use "localhost" for the API urls (otherwise 
        // it will use the real hostname). -- L.A. 4.5
        if (filesMinimallySupported) {
            assertEquals(datasetAsDdi, result);
        }
    }

    @Test
    public void testDatasetHtmlDDI() throws Exception {
        File fileXML = new File("src/test/resources/xml/dct_codebook.xml");

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        DdiExportUtil.datasetHtmlDDI( new FileInputStream(fileXML), byteArrayOutputStream);
        assertNotNull(byteArrayOutputStream);
        assertNotEquals("",byteArrayOutputStream.toString());
        String datasetAsDdiHtml = HtmlPrinter.prettyPrint(byteArrayOutputStream.toString());

        File htmlFile = new File("src/test/resources/html/dct_codebook.html");
        String sampleHtml = HtmlPrinter.prettyPrint(new String(Files.readAllBytes(Paths.get(htmlFile.getAbsolutePath()))));
        assertEquals(sampleHtml,datasetAsDdiHtml);

    }

}
