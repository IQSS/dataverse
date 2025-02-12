package edu.harvard.iq.dataverse.export.ddi;

import edu.harvard.iq.dataverse.pidproviders.PidProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.PidUtil;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteDOIProvider;
import edu.harvard.iq.dataverse.pidproviders.doi.datacite.DataCiteProviderFactory;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkPidProvider;
import edu.harvard.iq.dataverse.pidproviders.perma.PermaLinkProviderFactory;
import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import edu.harvard.iq.dataverse.util.testing.LocalJvmSettings;
import edu.harvard.iq.dataverse.util.xml.XmlPrinter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import edu.harvard.iq.dataverse.util.xml.html.HtmlPrinter;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.junit.jupiter.api.BeforeAll;
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
@LocalJvmSettings
//Perma 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 1", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "PERM", varArgs = "perma1")
@JvmSetting(key = JvmSettings.PERMALINK_BASE_URL, value = "https://example.org/citation?persistentId=perma:", varArgs = "perma1")
//Perma 2
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "perma 2", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = PermaLinkPidProvider.TYPE, varArgs = "perma2")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "PERM2", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_SEPARATOR, value = "-", varArgs = "perma2")
@JvmSetting(key = JvmSettings.PERMALINK_BASE_URL, value = "https://example.org/citation?persistentId=perma:", varArgs = "perma2")
// Datacite 1
@JvmSetting(key = JvmSettings.PID_PROVIDER_LABEL, value = "dataCite 1", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_TYPE, value = DataCiteDOIProvider.TYPE, varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_AUTHORITY, value = "10.5072", varArgs = "dc1")
@JvmSetting(key = JvmSettings.PID_PROVIDER_SHOULDER, value = "FK2", varArgs = "dc1")
@JvmSetting(key = JvmSettings.DATACITE_MDS_API_URL, value = "https://mds.test.datacite.org/", varArgs = "dc1")
@JvmSetting(key = JvmSettings.DATACITE_REST_API_URL, value = "https://api.test.datacite.org", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_USERNAME, value = "test", varArgs ="dc1")
@JvmSetting(key = JvmSettings.DATACITE_PASSWORD, value = "changeme", varArgs ="dc1")

//List to instantiate
@JvmSetting(key = JvmSettings.PID_PROVIDERS, value = "perma1, perma2, dc1")
public class DdiExportUtilTest {

    private static final Logger logger = Logger.getLogger(DdiExportUtilTest.class.getCanonicalName());

    @Mock
    SettingsServiceBean settingsSvc;
    
    @BeforeEach
    void setup() {
        Mockito.lenient().when(settingsSvc.isTrueForKey(SettingsServiceBean.Key.ExportInstallationAsDistributorOnlyWhenNotSet, false)).thenReturn(false);
        DdiExportUtil.injectSettingsService(settingsSvc);
    }

    @BeforeAll
    public static void setUpClass() throws Exception {
        Map<String, PidProviderFactory> pidProviderFactoryMap = new HashMap<>();
        pidProviderFactoryMap.put(PermaLinkPidProvider.TYPE, new PermaLinkProviderFactory());
        pidProviderFactoryMap.put(DataCiteDOIProvider.TYPE, new DataCiteProviderFactory());

        PidUtil.clearPidProviders();

        //Read list of providers to add
        List<String> providers = Arrays.asList(JvmSettings.PID_PROVIDERS.lookup().split(",\\s"));
        //Iterate through the list of providers and add them using the PidProviderFactory of the appropriate type
        for (String providerId : providers) {
            System.out.println("Loading provider: " + providerId);
            String type = JvmSettings.PID_PROVIDER_TYPE.lookup(providerId);
            PidProviderFactory factory = pidProviderFactoryMap.get(type);
            PidUtil.addToProviderList(factory.createPidProvider(providerId));
        }
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
    public void testJson2DdiPermaLink() throws Exception {
        // given
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-perma.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-perma.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(ddiFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);

        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.fine(result);

        // then
        XmlAssert.assertThat(result).and(datasetAsDdi).ignoreWhitespace().areSimilar();
    }


    @Test
    public void testJson2DdiPermaLinkWithSeparator() throws Exception {
        // given
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-perma-w-separator.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-perma-w-separator.xml");
        String datasetAsDdi = XmlPrinter.prettyPrintXml(Files.readString(ddiFile, StandardCharsets.UTF_8));
        logger.fine(datasetAsDdi);

        // when
        String result = DdiExportUtil.datasetDtoAsJson2ddi(datasetVersionAsJson);
        logger.fine(result);

        // then
        XmlAssert.assertThat(result).and(datasetAsDdi).ignoreWhitespace().areSimilar();
    }

    @Test
    public void testJson2DdiNoFilesTermsOfUse() throws Exception {
        // given
        Path datasetVersionJson = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch-terms-of-use.json");
        String datasetVersionAsJson = Files.readString(datasetVersionJson, StandardCharsets.UTF_8);
        Path ddiFile = Path.of("src/test/java/edu/harvard/iq/dataverse/export/ddi/dataset-finch-terms-of-use.xml");
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
