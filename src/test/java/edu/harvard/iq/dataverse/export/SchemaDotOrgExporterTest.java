package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.*;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import io.gdcc.spi.export.ExportDataProvider;
import io.gdcc.spi.export.ExportException;
import io.gdcc.spi.export.XMLExporter;
import edu.harvard.iq.dataverse.license.License;
import edu.harvard.iq.dataverse.license.LicenseServiceBean;
import edu.harvard.iq.dataverse.mocks.MockDatasetFieldSvc;

import static edu.harvard.iq.dataverse.util.SystemConfig.FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS;

import edu.harvard.iq.dataverse.settings.JvmSettings;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.json.JsonParseException;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.json.JsonObject;

import edu.harvard.iq.dataverse.util.testing.JvmSetting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For docs see {@link SchemaDotOrgExporter}.
 */
public class SchemaDotOrgExporterTest {

    private static final Logger logger = Logger.getLogger(SchemaDotOrgExporterTest.class.getCanonicalName());
    private static final MockDatasetFieldSvc datasetFieldTypeSvc = new MockDatasetFieldSvc();
    private static final SettingsServiceBean settingsService = Mockito.mock(SettingsServiceBean.class);
    private static final LicenseServiceBean licenseService = Mockito.mock(LicenseServiceBean.class);
    private static final SchemaDotOrgExporter schemaDotOrgExporter = new SchemaDotOrgExporter();

    @BeforeAll
    public static void setUpClass() {
        BrandingUtilTest.setupMocks();
        mockDatasetFieldSvc();
    }

    @AfterAll
    public static void tearDownClass() {
        BrandingUtilTest.tearDownMocks();
    }

    /**
     * Test of exportDataset method, of class SchemaDotOrgExporter.
     * @throws IOException
     * @throws JsonParseException
     * @throws ParseException
     * 
     */
    @Test
    @JvmSetting(key = JvmSettings.SITE_URL, value = "https://librascholar.org")
    public void testExportDataset() throws JsonParseException, ParseException, IOException {
        File datasetVersionJson = new File("src/test/resources/json/dataset-finch2.json");
        String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

        JsonObject json = JsonUtil.getJsonObject(datasetVersionAsJson);
        ExportDataProvider exportDataProviderStub = Mockito.mock(ExportDataProvider.class);
        Mockito.when(exportDataProviderStub.getDatasetJson()).thenReturn(json);
        
        JsonObject json2 = createExportFromJson(exportDataProviderStub);
        
        assertEquals("http://schema.org", json2.getString("@context"));
        assertEquals("Dataset", json2.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/IMK5A4", json2.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/IMK5A4", json2.getString("identifier"));
        assertEquals("Darwin's Finches", json2.getString("name"));
        assertEquals("Finch, Fiona", json2.getJsonArray("creator").getJsonObject(0).getString("name"));
        assertEquals("Birds Inc.", json2.getJsonArray("creator").getJsonObject(0).getJsonObject("affiliation").getString("name"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("creator").getJsonObject(0).getString("@id"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("creator").getJsonObject(0).getString("identifier"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("creator").getJsonObject(0).getString("sameAs"));
        assertEquals("Finch, Fiona", json2.getJsonArray("author").getJsonObject(0).getString("name"));
        assertEquals("Birds Inc.", json2.getJsonArray("author").getJsonObject(0).getJsonObject("affiliation").getString("name"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("author").getJsonObject(0).getString("@id"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("author").getJsonObject(0).getString("identifier"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("author").getJsonObject(0).getString("sameAs"));
        assertEquals("1955-11-05", json2.getString("datePublished"));
        assertEquals("1955-11-05", json2.getString("dateModified"));
        assertEquals("1", json2.getString("version"));
        assertEquals("Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.\nBird is the word.", json2.getString("description"));
        assertEquals("Medicine, Health and Life Sciences", json2.getJsonArray("keywords").getString(0));
        assertEquals("tcTerm1", json2.getJsonArray("keywords").getString(1));
        assertEquals("KeywordTerm1", json2.getJsonArray("keywords").getString(2));
        assertEquals("KeywordTerm2", json2.getJsonArray("keywords").getString(3));
        // This dataset, for example, has multiple keywords separated by commas: https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/24034&version=2.0
        assertEquals("keywords, with, commas", json2.getJsonArray("keywords").getString(4));
        assertEquals("CreativeWork", json2.getJsonArray("citation").getJsonObject(0).getString("@type"));
        assertEquals("Finch, Fiona 2018. \"The Finches.\" American Ornithological Journal 60 (4): 990-1005.", json2.getJsonArray("citation").getJsonObject(0).getString("name"));
        assertEquals("https://doi.org/10.5072/FK2/RV16HK", json2.getJsonArray("citation").getJsonObject(0).getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/RV16HK", json2.getJsonArray("citation").getJsonObject(0).getString("identifier"));
        assertEquals("https://doi.org/10.5072/FK2/RV16HK", json2.getJsonArray("citation").getJsonObject(0).getString("url"));
        assertEquals("2002/2005", json2.getJsonArray("temporalCoverage").getString(0));
        assertEquals("2001-10-01/2015-11-15", json2.getJsonArray("temporalCoverage").getString(1));
        assertEquals(null, json2.getString("schemaVersion", null));
        assertEquals("http://creativecommons.org/publicdomain/zero/1.0/", json2.getString("license"));
        assertEquals("DataCatalog", json2.getJsonObject("includedInDataCatalog").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("includedInDataCatalog").getString("name"));
        assertEquals("https://librascholar.org", json2.getJsonObject("includedInDataCatalog").getString("url"));
        assertEquals("Organization", json2.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("publisher").getString("name"));
        assertEquals("Organization", json2.getJsonObject("provider").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("provider").getString("name"));
        assertEquals("Organization", json2.getJsonArray("funder").getJsonObject(0).getString("@type"));
        assertEquals("National Science Foundation", json2.getJsonArray("funder").getJsonObject(0).getString("name"));
        // The NIH grant number is not shown because don't have anywhere in schema.org to put it. :(
        assertEquals("National Institutes of Health", json2.getJsonArray("funder").getJsonObject(1).getString("name"));
        assertEquals(2, json2.getJsonArray("funder").size());
        assertEquals("Columbus, Ohio, United States, North America", json2.getJsonArray("spatialCoverage").getString(0));
        assertEquals("Wisconsin, United States", json2.getJsonArray("spatialCoverage").getString(1));
        assertEquals(2, json2.getJsonArray("spatialCoverage").size());
        assertEquals("DataDownload", json2.getJsonArray("distribution").getJsonObject(0).getString("@type"));
        assertEquals("README.md", json2.getJsonArray("distribution").getJsonObject(0).getString("name"));
        assertEquals("text/plain", json2.getJsonArray("distribution").getJsonObject(0).getString("encodingFormat"));
        assertEquals(1234, json2.getJsonArray("distribution").getJsonObject(0).getInt("contentSize"));
        assertEquals("README file.", json2.getJsonArray("distribution").getJsonObject(0).getString("description"));
        assertEquals("https://doi.org/10.5072/FK2/7V5MPI", json2.getJsonArray("distribution").getJsonObject(0).getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/7V5MPI", json2.getJsonArray("distribution").getJsonObject(0).getString("identifier"));
        assertEquals("https://librascholar.org/api/access/datafile/42", json2.getJsonArray("distribution").getJsonObject(0).getString("contentUrl"));
        assertEquals(1, json2.getJsonArray("distribution").size());
        try (PrintWriter printWriter = new PrintWriter("/tmp/dvjsonld.json")) {
            printWriter.println(JsonUtil.prettyPrint(json2));
        }
        
    }

    /**
     * Test description truncation in exportDataset method, of class SchemaDotOrgExporter.
     * @throws IOException
     * @throws JsonParseException
     * @throws ParseException
     * 
     */
    @Test
    public void testExportDescriptionTruncation() throws JsonParseException, ParseException, IOException {
    File datasetVersionJson = new File("src/test/resources/json/dataset-long-description.json");
    String datasetVersionAsJson = new String(Files.readAllBytes(Paths.get(datasetVersionJson.getAbsolutePath())));

    JsonObject json = JsonUtil.getJsonObject(datasetVersionAsJson);
    ExportDataProvider exportDataProviderStub = Mockito.mock(ExportDataProvider.class);
    Mockito.when(exportDataProviderStub.getDatasetJson()).thenReturn(json);
    
    JsonObject json2 = createExportFromJson(exportDataProviderStub);

    assertTrue(json2.getString("description").endsWith("at..."));
    }
    
    private JsonObject createExportFromJson(ExportDataProvider provider) throws JsonParseException, ParseException {
        License license = new License("CC0 1.0", "You can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.", URI.create("http://creativecommons.org/publicdomain/zero/1.0/"), URI.create("/resources/images/cc0.png"), true, 1l);
        license.setDefault(true);
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, settingsService, licenseService);
        DatasetVersion version = jsonParser.parseDatasetVersion(provider.getDatasetJson().getJsonObject("datasetVersion"));
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);
        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(license);
        version.setTermsOfUseAndAccess(terms);

        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("IMK5A4");
        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));
        version.setDataset(dataset);
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        dataset.setOwner(dataverse);
        boolean hideFileUrls = false;
        if (hideFileUrls) {
            System.setProperty(FILES_HIDE_SCHEMA_DOT_ORG_DOWNLOAD_URLS, "true");
        }

        FileMetadata fmd = new FileMetadata();
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setLabel("README.md");
        fmd.setDescription("README file.");
        List<FileMetadata> fileMetadatas = new ArrayList<>();
        fileMetadatas.add(fmd);
        dataFile.setFileMetadatas(fileMetadatas);
        ;
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        if(schemaDotOrgExporter == null) logger.fine("sdoe" + " null");
        try {
            ExportDataProvider provider2 = new InternalExportDataProvider(version);
            schemaDotOrgExporter.exportDataset(provider2, byteArrayOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String jsonLdStr = byteArrayOutputStream.toString();
        return JsonUtil.getJsonObject(jsonLdStr);
    }

    /**
     * Test of getProviderName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetProviderName() {
        assertEquals("schema.org", schemaDotOrgExporter.getFormatName());
    }

    /**
     * Test of getDisplayName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetDisplayName() {
        // We capitalize "Schema.org" because it looks better in the dropdown list and it's what DataCite does in their UI.
        assertEquals("Schema.org JSON-LD", schemaDotOrgExporter.getDisplayName(null));
    }

    /**
     * Test that SchemaDotOrgExporter is not an XMLExporter
     */
    @Test
    public void testIsXMLFormat() {
        assertEquals(false, schemaDotOrgExporter instanceof XMLExporter);
    }

    /**
     * Test of isHarvestable method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsHarvestable() {
        assertEquals(false, schemaDotOrgExporter.isHarvestable());
    }

    /**
     * Test of isAvailableToUsers method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsAvailableToUsers() {
        assertEquals(true, schemaDotOrgExporter.isAvailableToUsers());
    }

    /**
     * Test of XMLExporter interface, of class SchemaDotOrgExporter.
     */
    @Test
    public void testNotAnXMLExporter() {
        assertFalse(schemaDotOrgExporter instanceof XMLExporter);
    }

    private static void mockDatasetFieldSvc() {
        datasetFieldTypeSvc.setMetadataBlock("citation");
    
        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", DatasetFieldType.FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", DatasetFieldType.FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", DatasetFieldType.FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", DatasetFieldType.FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", DatasetFieldType.FieldType.TEXT, false));
        authorIdentifierSchemeType.setAllowControlledVocabulary(true);
        authorIdentifierSchemeType.setControlledVocabularyValues(Arrays.asList(
            // Why aren't these enforced? Should be ORCID, etc.
            new ControlledVocabularyValue(1l, "ark", authorIdentifierSchemeType),
            new ControlledVocabularyValue(2l, "doi", authorIdentifierSchemeType),
            new ControlledVocabularyValue(3l, "url", authorIdentifierSchemeType)
        ));
        authorChildTypes.add(datasetFieldTypeSvc.add(authorIdentifierSchemeType));
        for (DatasetFieldType t : authorChildTypes) {
            t.setParentDatasetFieldType(authorType);
        }
        authorType.setChildDatasetFieldTypes(authorChildTypes);
    
        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactEmail", DatasetFieldType.FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", DatasetFieldType.FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", DatasetFieldType.FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);
    
        DatasetFieldType dsDescriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("dsDescription", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> dsDescriptionTypes = new HashSet<>();
        dsDescriptionTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("dsDescriptionValue", DatasetFieldType.FieldType.TEXT, false)));
        for (DatasetFieldType t : dsDescriptionTypes) {
            t.setParentDatasetFieldType(dsDescriptionType);
        }
        dsDescriptionType.setChildDatasetFieldTypes(dsDescriptionTypes);
    
        DatasetFieldType keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> keywordChildTypes = new HashSet<>();
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordValue", DatasetFieldType.FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabulary", DatasetFieldType.FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabularyURI", DatasetFieldType.FieldType.TEXT, false)));
        keywordType.setChildDatasetFieldTypes(keywordChildTypes);
    
        DatasetFieldType topicClassificationType = datasetFieldTypeSvc.add(new DatasetFieldType("topicClassification", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> topicClassificationTypes = new HashSet<>();
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassValue", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocab", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocabURI", DatasetFieldType.FieldType.TEXT, false)));
        topicClassificationType.setChildDatasetFieldTypes(topicClassificationTypes);
    
        DatasetFieldType descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", DatasetFieldType.FieldType.TEXTBOX, false));
    
        DatasetFieldType subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", DatasetFieldType.FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
            new ControlledVocabularyValue(1l, "mgmt", subjectType),
            new ControlledVocabularyValue(2l, "law", subjectType),
            new ControlledVocabularyValue(3l, "cs", subjectType)
        ));
    
        DatasetFieldType pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", DatasetFieldType.FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
            new ControlledVocabularyValue(1l, "ark", pubIdType),
            new ControlledVocabularyValue(2l, "doi", pubIdType),
            new ControlledVocabularyValue(3l, "url", pubIdType)
        ));
    
        DatasetFieldType compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", DatasetFieldType.FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", DatasetFieldType.FieldType.TEXT, false)));
    
        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);
    
        DatasetFieldType contributorType = datasetFieldTypeSvc.add(new DatasetFieldType("contributor", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> contributorChildTypes = new HashSet<>();
        contributorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("contributorName", DatasetFieldType.FieldType.TEXT, false)));
        DatasetFieldType contributorTypes = datasetFieldTypeSvc.add(new DatasetFieldType("contributorType", DatasetFieldType.FieldType.TEXT, false));
        contributorTypes.setAllowControlledVocabulary(true);
        contributorTypes.setControlledVocabularyValues(Arrays.asList(
            // Why aren't these enforced?
            new ControlledVocabularyValue(1l, "Data Collector", contributorTypes),
            new ControlledVocabularyValue(2l, "Data Curator", contributorTypes),
            new ControlledVocabularyValue(3l, "Data Manager", contributorTypes),
            new ControlledVocabularyValue(3l, "Editor", contributorTypes),
            new ControlledVocabularyValue(3l, "Funder", contributorTypes),
            new ControlledVocabularyValue(3l, "Hosting Institution", contributorTypes)
            // Etc. There are more.
        ));
        contributorChildTypes.add(datasetFieldTypeSvc.add(contributorTypes));
        for (DatasetFieldType t : contributorChildTypes) {
            t.setParentDatasetFieldType(contributorType);
        }
        contributorType.setChildDatasetFieldTypes(contributorChildTypes);
    
        DatasetFieldType grantNumberType = datasetFieldTypeSvc.add(new DatasetFieldType("grantNumber", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> grantNumberChildTypes = new HashSet<>();
        grantNumberChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("grantNumberAgency", DatasetFieldType.FieldType.TEXT, false)));
        grantNumberChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("grantNumberValue", DatasetFieldType.FieldType.TEXT, false)));
        grantNumberType.setChildDatasetFieldTypes(grantNumberChildTypes);
    
        DatasetFieldType publicationType = datasetFieldTypeSvc.add(new DatasetFieldType("publication", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> publicationChildTypes = new HashSet<>();
        publicationChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("publicationCitation", DatasetFieldType.FieldType.TEXT, false)));
        DatasetFieldType publicationIdTypes = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIDType", DatasetFieldType.FieldType.TEXT, false));
        publicationIdTypes.setAllowControlledVocabulary(true);
        publicationIdTypes.setControlledVocabularyValues(Arrays.asList(
            // Why aren't these enforced?
            new ControlledVocabularyValue(1l, "ark", publicationIdTypes),
            new ControlledVocabularyValue(2l, "arXiv", publicationIdTypes),
            new ControlledVocabularyValue(3l, "bibcode", publicationIdTypes),
            new ControlledVocabularyValue(4l, "cstr", publicationIdTypes),
            new ControlledVocabularyValue(5l, "doi", publicationIdTypes),
            new ControlledVocabularyValue(6l, "ean13", publicationIdTypes),
            new ControlledVocabularyValue(7l, "handle", publicationIdTypes)
            // Etc. There are more.
        ));
        publicationChildTypes.add(datasetFieldTypeSvc.add(publicationIdTypes));
        publicationChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("publicationIDNumber", DatasetFieldType.FieldType.TEXT, false)));
        DatasetFieldType publicationURLType = new DatasetFieldType("publicationURL", DatasetFieldType.FieldType.URL, false);
        publicationURLType.setDisplayFormat("<a href=\"#VALUE\" target=\"_blank\">#VALUE</a>");
        publicationChildTypes.add(datasetFieldTypeSvc.add(publicationURLType));
        publicationType.setChildDatasetFieldTypes(publicationChildTypes);
    
        DatasetFieldType timePeriodCoveredType = datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCovered", DatasetFieldType.FieldType.NONE, true));
        Set<DatasetFieldType> timePeriodCoveredChildTypes = new HashSet<>();
        timePeriodCoveredChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCoveredStart", DatasetFieldType.FieldType.DATE, false)));
        timePeriodCoveredChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCoveredEnd", DatasetFieldType.FieldType.DATE, false)));
        timePeriodCoveredType.setChildDatasetFieldTypes(timePeriodCoveredChildTypes);
    
        DatasetFieldType geographicCoverageType = datasetFieldTypeSvc.add(new DatasetFieldType("geographicCoverage", DatasetFieldType.FieldType.TEXT, true));
        Set<DatasetFieldType> geographicCoverageChildTypes = new HashSet<>();
        DatasetFieldType countries = datasetFieldTypeSvc.add(new DatasetFieldType("country", DatasetFieldType.FieldType.TEXT, false));
        countries.setAllowControlledVocabulary(true);
        countries.setControlledVocabularyValues(Arrays.asList(
            // Why aren't these enforced?
            new ControlledVocabularyValue(1l, "Afghanistan", countries),
            new ControlledVocabularyValue(2l, "Albania", countries)
            // And many more countries.
        ));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(countries));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("state", DatasetFieldType.FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("city", DatasetFieldType.FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("otherGeographicCoverage", DatasetFieldType.FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("geographicUnit", DatasetFieldType.FieldType.TEXT, false)));
        for (DatasetFieldType t : geographicCoverageChildTypes) {
            t.setParentDatasetFieldType(geographicCoverageType);
        }
        geographicCoverageType.setChildDatasetFieldTypes(geographicCoverageChildTypes);
    }

}
