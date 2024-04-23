package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.stream.XMLStreamException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Mock
    SettingsServiceBean settingsSvc;
    
    @BeforeEach
    void setup() {
        Mockito.lenient().when(settingsSvc.isTrueForKey(SettingsServiceBean.Key.ExportInstallationAsDistributorOnlyWhenNotSet, false)).thenReturn(false);
        DdiExportUtil.injectSettingsService(settingsSvc);
    }
    
    
    @Test
    public void testJson2DdiNoFiles() throws Exception {
        // given
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(ddiFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);
        
        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.fine(result);
        
        // then
        XmlAssert.assertThat(result).and(datasetAsDdi).ignoreWhitespace().areSimilar();
    }

    @Test
    public void testExportDDI() throws Exception {
        // given
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-create-new-all-ddi-fields.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/exportfull.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(ddiFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);
        
        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.fine(XmlPrinter.prettyPrintXml(result));
        
        // then
        XmlAssert.assertThat(result).and(datasetAsDdi).ignoreWhitespace().areSimilar();
    }

    @Test
    public void testJson2ddiHasFiles() throws Exception {
        /**
         * Note that `cat dataset-spruce1.json | jq
         * .datasetVersion.files[0].datafile.description` yields an empty string
         * but datasets created in the GUI sometimes don't have a description
         * field at all.
         */
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-spruce1.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(ddiFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.fine(XmlPrinter.prettyPrintXml(result));
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
    public void testDatasetHtmlDDI() throws IOException, XMLStreamException {
        // given
        File fileXML = new File("src/test/resources/xml/dct_codebook.xml");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // when
        DdiExportUtil.datasetHtmlDDI( new FileInputStream(fileXML), byteArrayOutputStream);
        String generatedDdiHTML = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
        
        // then
        assertNotNull(generatedDdiHTML);
        assertFalse(generatedDdiHTML.isEmpty());
        assertFalse(generatedDdiHTML.isBlank());
    
        // pipe through pretty printer before parsing as DOM
        generatedDdiHTML = HtmlPrinter.prettyPrint(generatedDdiHTML);
        Document generatedDdiHtmlDom = W3CDom.convert(Jsoup.parse(generatedDdiHTML));
        
        // read comparison file to build diff
        Path htmlFile = Path.of("src/test/resources/html/dct_codebook.html");
        String subjectHtml = HtmlPrinter.prettyPrint(new String(Files.readAllBytes(htmlFile)));
        Document subjectHtmlDom = W3CDom.convert(Jsoup.parse(subjectHtml));
    
        // compare generated and sample
        Diff diff = DiffBuilder.compare(subjectHtmlDom)
                        .withTest(generatedDdiHtmlDom)
                        .ignoreComments()
                        .ignoreWhitespace()
                        .checkForSimilar()
                        .build();
    
        // assert matching and print differences if any.
        diff.getDifferences().forEach(d -> logger.info(d.toString()));
        assertFalse(diff.hasDifferences());
    }

}
