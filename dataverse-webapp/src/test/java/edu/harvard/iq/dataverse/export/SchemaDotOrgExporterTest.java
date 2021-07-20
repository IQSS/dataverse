package edu.harvard.iq.dataverse.export;

import edu.harvard.iq.dataverse.DataFileServiceBean;
import edu.harvard.iq.dataverse.DatasetFieldServiceBean;
import edu.harvard.iq.dataverse.UnitTestUtils;
import edu.harvard.iq.dataverse.persistence.MocksFactory;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.datafile.FileMetadata;
import edu.harvard.iq.dataverse.persistence.datafile.license.FileTermsOfUse;
import edu.harvard.iq.dataverse.persistence.datafile.license.License;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.TermsOfUseAndAccess;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.qualifiers.TestBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonParser;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * For docs see {@link SchemaDotOrgExporter}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SchemaDotOrgExporterTest {

    @InjectMocks
    private SchemaDotOrgExporter schemaDotOrgExporter;
    
    @Mock
    private SettingsServiceBean settingsService;
    
    @Mock
    private SystemConfig systemConfig;

    @Mock
    DataFileServiceBean dataFileService;

    private JsonLdBuilder jsonLdBuilder;
    
    MockDatasetFieldSvc datasetFieldTypeSvc = null;


    @BeforeEach
    public void setUp() {
        when(systemConfig.getDataverseSiteUrl()).thenReturn("https://librascholar.org");
        when(settingsService.isTrueForKey(SettingsServiceBean.Key.HideSchemaDotOrgDownloadUrls)).thenReturn(false);
        when(dataFileService.isSameTermsOfUse(any(), any())).thenReturn(true);
        jsonLdBuilder = new JsonLdBuilder(dataFileService, settingsService, systemConfig);
        schemaDotOrgExporter = new SchemaDotOrgExporter(jsonLdBuilder);

        datasetFieldTypeSvc = new MockDatasetFieldSvc();

        DatasetFieldType titleType = datasetFieldTypeSvc.add(new DatasetFieldType("title", FieldType.TEXTBOX, false));
        DatasetFieldType authorType = datasetFieldTypeSvc.add(new DatasetFieldType("author", FieldType.TEXT, true));
        Set<DatasetFieldType> authorChildTypes = new HashSet<>();
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorName", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorAffiliation", FieldType.TEXT, false)));
        authorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifier", FieldType.TEXT, false)));
        DatasetFieldType authorIdentifierSchemeType = datasetFieldTypeSvc.add(new DatasetFieldType("authorIdentifierScheme", FieldType.TEXT, false));
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

        DatasetFieldType datasetContactType = datasetFieldTypeSvc.add(new DatasetFieldType("datasetContact", FieldType.TEXT, true));
        Set<DatasetFieldType> datasetContactTypes = new HashSet<>();
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactEmail", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactName", FieldType.TEXT, false)));
        datasetContactTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("datasetContactAffiliation", FieldType.TEXT, false)));
        for (DatasetFieldType t : datasetContactTypes) {
            t.setParentDatasetFieldType(datasetContactType);
        }
        datasetContactType.setChildDatasetFieldTypes(datasetContactTypes);

        DatasetFieldType dsDescriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("dsDescription", FieldType.TEXT, true));
        Set<DatasetFieldType> dsDescriptionTypes = new HashSet<>();
        dsDescriptionTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("dsDescriptionValue", FieldType.TEXT, false)));
        for (DatasetFieldType t : dsDescriptionTypes) {
            t.setParentDatasetFieldType(dsDescriptionType);
        }
        dsDescriptionType.setChildDatasetFieldTypes(dsDescriptionTypes);

        DatasetFieldType keywordType = datasetFieldTypeSvc.add(new DatasetFieldType("keyword", FieldType.TEXT, true));
        Set<DatasetFieldType> keywordChildTypes = new HashSet<>();
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordValue", FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabulary", FieldType.TEXT, false)));
        keywordChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("keywordVocabularyURI", FieldType.TEXT, false)));
        keywordType.setChildDatasetFieldTypes(keywordChildTypes);

        DatasetFieldType topicClassificationType = datasetFieldTypeSvc.add(new DatasetFieldType("topicClassification", FieldType.TEXT, true));
        Set<DatasetFieldType> topicClassificationTypes = new HashSet<>();
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassValue", FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocab", FieldType.TEXT, false)));
        topicClassificationTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("topicClassVocabURI", FieldType.TEXT, false)));
        topicClassificationType.setChildDatasetFieldTypes(topicClassificationTypes);

        DatasetFieldType descriptionType = datasetFieldTypeSvc.add(new DatasetFieldType("description", FieldType.TEXTBOX, false));

        DatasetFieldType subjectType = datasetFieldTypeSvc.add(new DatasetFieldType("subject", FieldType.TEXT, true));
        subjectType.setAllowControlledVocabulary(true);
        subjectType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "mgmt", subjectType),
                new ControlledVocabularyValue(2l, "law", subjectType),
                new ControlledVocabularyValue(3l, "cs", subjectType)
        ));

        DatasetFieldType pubIdType = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIdType", FieldType.TEXT, false));
        pubIdType.setAllowControlledVocabulary(true);
        pubIdType.setControlledVocabularyValues(Arrays.asList(
                new ControlledVocabularyValue(1l, "ark", pubIdType),
                new ControlledVocabularyValue(2l, "doi", pubIdType),
                new ControlledVocabularyValue(3l, "url", pubIdType)
        ));

        DatasetFieldType compoundSingleType = datasetFieldTypeSvc.add(new DatasetFieldType("coordinate", FieldType.TEXT, true));
        Set<DatasetFieldType> childTypes = new HashSet<>();
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lat", FieldType.TEXT, false)));
        childTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("lon", FieldType.TEXT, false)));

        for (DatasetFieldType t : childTypes) {
            t.setParentDatasetFieldType(compoundSingleType);
        }
        compoundSingleType.setChildDatasetFieldTypes(childTypes);

        DatasetFieldType contributorType = datasetFieldTypeSvc.add(new DatasetFieldType("contributor", FieldType.TEXT, true));
        Set<DatasetFieldType> contributorChildTypes = new HashSet<>();
        contributorChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("contributorName", FieldType.TEXT, false)));
        DatasetFieldType contributorTypes = datasetFieldTypeSvc.add(new DatasetFieldType("contributorType", FieldType.TEXT, false));
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

        DatasetFieldType grantNumberType = datasetFieldTypeSvc.add(new DatasetFieldType("grantNumber", FieldType.TEXT, true));
        Set<DatasetFieldType> grantNumberChildTypes = new HashSet<>();
        grantNumberChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("grantNumberAgency", FieldType.TEXT, false)));
        grantNumberChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("grantNumberValue", FieldType.TEXT, false)));
        grantNumberType.setChildDatasetFieldTypes(grantNumberChildTypes);

        DatasetFieldType publicationType = datasetFieldTypeSvc.add(new DatasetFieldType("publication", FieldType.TEXT, true));
        Set<DatasetFieldType> publicationChildTypes = new HashSet<>();
        publicationChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("publicationCitation", FieldType.TEXT, false)));
        DatasetFieldType publicationIdTypes = datasetFieldTypeSvc.add(new DatasetFieldType("publicationIDType", FieldType.TEXT, false));
        publicationIdTypes.setAllowControlledVocabulary(true);
        publicationIdTypes.setControlledVocabularyValues(Arrays.asList(
                // Why aren't these enforced?
                new ControlledVocabularyValue(1l, "ark", publicationIdTypes),
                new ControlledVocabularyValue(2l, "arXiv", publicationIdTypes),
                new ControlledVocabularyValue(3l, "bibcode", publicationIdTypes),
                new ControlledVocabularyValue(4l, "doi", publicationIdTypes),
                new ControlledVocabularyValue(5l, "ean13", publicationIdTypes),
                new ControlledVocabularyValue(6l, "handle", publicationIdTypes)
                // Etc. There are more.
        ));
        publicationChildTypes.add(datasetFieldTypeSvc.add(publicationIdTypes));
        publicationChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("publicationIDNumber", FieldType.TEXT, false)));
        DatasetFieldType publicationURLType = new DatasetFieldType("publicationURL", FieldType.URL, false);
        publicationURLType.setDisplayFormat("<a href=\"#VALUE\" target=\"_blank\">#VALUE</a>");
        publicationChildTypes.add(datasetFieldTypeSvc.add(publicationURLType));
        publicationType.setChildDatasetFieldTypes(publicationChildTypes);

        DatasetFieldType timePeriodCoveredType = datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCovered", FieldType.NONE, true));
        Set<DatasetFieldType> timePeriodCoveredChildTypes = new HashSet<>();
        timePeriodCoveredChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCoveredStart", FieldType.DATE, false)));
        timePeriodCoveredChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("timePeriodCoveredEnd", FieldType.DATE, false)));
        timePeriodCoveredType.setChildDatasetFieldTypes(timePeriodCoveredChildTypes);

        DatasetFieldType geographicCoverageType = datasetFieldTypeSvc.add(new DatasetFieldType("geographicCoverage", FieldType.TEXT, true));
        Set<DatasetFieldType> geographicCoverageChildTypes = new HashSet<>();
        DatasetFieldType countries = datasetFieldTypeSvc.add(new DatasetFieldType("country", FieldType.TEXT, false));
        countries.setAllowControlledVocabulary(true);
        countries.setControlledVocabularyValues(Arrays.asList(
                // Why aren't these enforced?
                new ControlledVocabularyValue(1l, "Afghanistan", countries),
                new ControlledVocabularyValue(2l, "Albania", countries)
                // And many more countries.
        ));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(countries));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("state", FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("city", FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("otherGeographicCoverage", FieldType.TEXT, false)));
        geographicCoverageChildTypes.add(datasetFieldTypeSvc.add(new DatasetFieldType("geographicUnit", FieldType.TEXT, false)));
        for (DatasetFieldType t : geographicCoverageChildTypes) {
            t.setParentDatasetFieldType(geographicCoverageType);
        }
        geographicCoverageType.setChildDatasetFieldTypes(geographicCoverageChildTypes);

    }

    /**
     * Test of exportDataset method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testExportDataset() throws Exception {
        String datasetVersionAsJson = UnitTestUtils.readFileToString("json/dataset-finch2.json");

        JsonReader jsonReader1 = Json.createReader(new StringReader(datasetVersionAsJson));
        JsonObject json1 = jsonReader1.readObject();
        JsonParser jsonParser = new JsonParser(datasetFieldTypeSvc, null, null);
        DatasetVersion version = jsonParser.parseDatasetVersion(json1.getJsonObject("datasetVersion"));
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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

        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "README.md", 0);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setDescription("README file.");

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        FileTermsOfUse fileTermsOfUse = new FileTermsOfUse();
        License license = new License();
        license.setId(1L);
        license.setUrl("testLicenseUrl");
        license.setName("test universal name");
        license.setActive(true);
        fileTermsOfUse.setLicense(license);
        fileTermsOfUse.setId(1L);
        fileTermsOfUse.setAllRightsReserved(false);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);
        dataFile.setFileMetadatas(fileMetadatas);
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        String jsonLd = schemaDotOrgExporter.exportDataset(version);
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();
        assertEquals("http://schema.org", json2.getString("@context"));
        assertEquals("Dataset", json2.getString("@type"));
        assertEquals("https://doi.org/10.5072/FK2/IMK5A4", json2.getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/IMK5A4", json2.getString("identifier"));
        assertEquals("Darwin's Finches", json2.getString("name"));
        assertEquals("Finch, Fiona", json2.getJsonArray("creator").getJsonObject(0).getString("name"));
        assertEquals("Birds Inc.", json2.getJsonArray("creator").getJsonObject(0).getString("affiliation"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("creator").getJsonObject(0).getString("@id"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("creator").getJsonObject(0).getString("identifier"));
        assertEquals("Finch, Fiona", json2.getJsonArray("author").getJsonObject(0).getString("name"));
        assertEquals("Birds Inc.", json2.getJsonArray("author").getJsonObject(0).getString("affiliation"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("author").getJsonObject(0).getString("@id"));
        assertEquals("https://orcid.org/0000-0002-1825-0097", json2.getJsonArray("author").getJsonObject(0).getString("identifier"));
        assertEquals("1955-11-05", json2.getString("datePublished"));
        assertEquals("1955-11-05", json2.getString("dateModified"));
        assertEquals("1", json2.getString("version"));
        assertEquals("Darwin's finches (also known as the Galápagos finches) are a group of about fifteen species of passerine birds.", json2.getJsonArray("description").getString(0));
        assertEquals("Bird is the word.", json2.getJsonArray("description").getString(1));
        assertEquals(2, json2.getJsonArray("description").size());
        assertEquals("Medicine, Health and Life Sciences", json2.getJsonArray("keywords").getString(0));
        assertEquals("tcTerm1", json2.getJsonArray("keywords").getString(1));
        assertEquals("KeywordTerm1", json2.getJsonArray("keywords").getString(2));
        assertEquals("KeywordTerm2", json2.getJsonArray("keywords").getString(3));
        // This dataset, for example, has multiple keywords separated by commas: https://dataverse.harvard.edu/dataset.xhtml?persistentId=doi:10.7910/DVN/24034&version=2.0
        assertEquals("keywords, with, commas", json2.getJsonArray("keywords").getString(4));
        assertEquals("CreativeWork", json2.getJsonArray("citation").getJsonObject(0).getString("@type"));
        assertEquals("Finch, Fiona 2018. \"The Finches.\" American Ornithological Journal 60 (4): 990-1005.", json2.getJsonArray("citation").getJsonObject(0).getString("text"));
        assertEquals("https://doi.org/10.5072/FK2/RV16HK", json2.getJsonArray("citation").getJsonObject(0).getString("@id"));
        assertEquals("https://doi.org/10.5072/FK2/RV16HK", json2.getJsonArray("citation").getJsonObject(0).getString("identifier"));
        assertEquals("2002/2005", json2.getJsonArray("temporalCoverage").getString(0));
        assertEquals("2001-10-01/2015-11-15", json2.getJsonArray("temporalCoverage").getString(1));
        assertEquals(null, json2.getString("schemaVersion", null));
        assertNotNull(json2.getJsonObject("license"));
        assertEquals("DataCatalog", json2.getJsonObject("includedInDataCatalog").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("includedInDataCatalog").getString("name"));
        assertEquals("https://librascholar.org", json2.getJsonObject("includedInDataCatalog").getString("url"));
        assertEquals("Organization", json2.getJsonObject("publisher").getString("@type"));
        assertEquals("LibraScholar", json2.getJsonObject("provider").getString("name"));
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
    }

    @Test
    public void testExportDataset_sameLicenses() throws Exception {
        DatasetVersion version = new DatasetVersion();
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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

        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "README.md", 0);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setDescription("README file.");

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        FileTermsOfUse fileTermsOfUse = new FileTermsOfUse();
        License license = new License();
        license.setId(1L);
        license.setUrl("testLicenseUrl");
        license.setName("test universal name");
        license.setActive(true);
        fileTermsOfUse.setLicense(license);
        fileTermsOfUse.setId(1L);
        fileTermsOfUse.setAllRightsReserved(false);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        fileTermsOfUse = new FileTermsOfUse();
        license = new License();
        license.setId(2L);
        license.setUrl("testLicenseUrl");
        license.setName("test universal name");
        license.setActive(true);
        fileTermsOfUse.setLicense(license);
        fileTermsOfUse.setId(2L);
        fileTermsOfUse.setAllRightsReserved(false);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        dataFile.setFileMetadatas(fileMetadatas);
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        String jsonLd = schemaDotOrgExporter.exportDataset(version);
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();

        assertEquals("CreativeWork", json2.getJsonObject("license").getString("@type"));
        assertEquals("test universal name", json2.getJsonObject("license").getString("name"));
        assertEquals("testLicenseUrl", json2.getJsonObject("license").getString("url"));
    }

    @Test
    public void testExportDataset_differentLicenses() throws Exception {

        when(dataFileService.isSameTermsOfUse(any(), any())).thenReturn(false);
        jsonLdBuilder = new JsonLdBuilder(dataFileService, settingsService, systemConfig);
        schemaDotOrgExporter = new SchemaDotOrgExporter(jsonLdBuilder);

        DatasetVersion version = new DatasetVersion();
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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

        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "README.md", 0);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setDescription("README file.");

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        FileTermsOfUse fileTermsOfUse = new FileTermsOfUse();
        License license = new License();
        license.setId(1L);
        license.setUrl("testLicenseUrl");
        license.setName("test universal name");
        license.setActive(true);
        fileTermsOfUse.setLicense(license);
        fileTermsOfUse.setId(1L);
        fileTermsOfUse.setAllRightsReserved(false);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        fileTermsOfUse = new FileTermsOfUse();
        license = new License();
        license.setId(2L);
        license.setUrl("some testLicenseUrl");
        license.setName("Different test universal name");
        license.setActive(true);
        fileTermsOfUse.setLicense(license);
        fileTermsOfUse.setId(2L);
        fileTermsOfUse.setAllRightsReserved(false);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        dataFile.setFileMetadatas(fileMetadatas);
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        String jsonLd = schemaDotOrgExporter.exportDataset(version);
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();

        assertEquals("CreativeWork", json2.getJsonObject("license").getString("@type"));
        assertEquals("Different licenses or terms for individual files", json2.getJsonObject("license").getString("name"));
        assertNull(json2.getJsonObject("license").getOrDefault("url", null));
    }

    @Test
    public void testExportDataset_restrictedAccess() throws Exception {

        DatasetVersion version = new DatasetVersion();
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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

        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "README.md", 0);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setDescription("README file.");

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        FileTermsOfUse fileTermsOfUse = new FileTermsOfUse();
        fileTermsOfUse.setLicense(null);
        fileTermsOfUse.setId(1L);
        fileTermsOfUse.setAllRightsReserved(false);
        fileTermsOfUse.setRestrictType(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        fileTermsOfUse = new FileTermsOfUse();
        fileTermsOfUse.setLicense(null);
        fileTermsOfUse.setId(2L);
        fileTermsOfUse.setAllRightsReserved(false);
        fileTermsOfUse.setRestrictType(FileTermsOfUse.RestrictType.ACADEMIC_PURPOSE);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        dataFile.setFileMetadatas(fileMetadatas);
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        String jsonLd = schemaDotOrgExporter.exportDataset(version);
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();

        assertEquals("CreativeWork", json2.getJsonObject("license").getString("@type"));
        assertEquals("Restricted access", json2.getJsonObject("license").getString("name"));
        assertNull(json2.getJsonObject("license").getOrDefault("url", null));

    }

    @Test
    public void testExportDataset_allRightsReserved() throws Exception {

        DatasetVersion version = new DatasetVersion();
        version.setVersionState(DatasetVersion.VersionState.RELEASED);
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");
        version.setReleaseTime(publicationDate);
        version.setVersionNumber(1l);

        TermsOfUseAndAccess terms = new TermsOfUseAndAccess();
        terms.setLicense(TermsOfUseAndAccess.License.CC0);
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

        FileMetadata fmd = MocksFactory.makeFileMetadata(10L, "README.md", 0);
        DataFile dataFile = new DataFile();
        dataFile.setId(42l);
        dataFile.setFilesize(1234);
        dataFile.setContentType("text/plain");
        dataFile.setProtocol("doi");
        dataFile.setAuthority("10.5072/FK2");
        dataFile.setIdentifier("7V5MPI");
        fmd.setDatasetVersion(version);
        fmd.setDataFile(dataFile);
        fmd.setDescription("README file.");

        List<FileMetadata> fileMetadatas = new ArrayList<>();
        FileTermsOfUse fileTermsOfUse = new FileTermsOfUse();
        fileTermsOfUse.setLicense(null);
        fileTermsOfUse.setId(1L);
        fileTermsOfUse.setAllRightsReserved(true);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        fileTermsOfUse = new FileTermsOfUse();
        fileTermsOfUse.setLicense(null);
        fileTermsOfUse.setId(2L);
        fileTermsOfUse.setAllRightsReserved(true);
        fmd.setTermsOfUse(fileTermsOfUse);
        fileMetadatas.add(fmd);

        dataFile.setFileMetadatas(fileMetadatas);
        dataFile.setOwner(dataset);
        version.setFileMetadatas(fileMetadatas);

        String jsonLd = schemaDotOrgExporter.exportDataset(version);
        JsonReader jsonReader2 = Json.createReader(new StringReader(jsonLd));
        JsonObject json2 = jsonReader2.readObject();

        assertEquals("CreativeWork", json2.getJsonObject("license").getString("@type"));
        assertEquals("All rights reserved", json2.getJsonObject("license").getString("name"));
        assertNull(json2.getJsonObject("license").getOrDefault("url", null));
    }

    /**
     * Test of getProviderName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetProviderName() {
        assertEquals(ExporterType.SCHEMADOTORG.toString(), schemaDotOrgExporter.getProviderName());
    }

    /**
     * Test of getDisplayName method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetDisplayName() {
        System.out.println("getDisplayName");
        // We capitalize "Schema.org" because it looks better in the dropdown list and it's what DataCite does in their UI.
        assertEquals("Schema.org JSON-LD", schemaDotOrgExporter.getDisplayName());
    }

    /**
     * Test of isXMLFormat method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsXMLFormat() {
        System.out.println("isXMLFormat");
        assertEquals(false, schemaDotOrgExporter.isXMLFormat());
    }

    /**
     * Test of isHarvestable method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsHarvestable() {
        System.out.println("isHarvestable");
        assertEquals(false, schemaDotOrgExporter.isHarvestable());
    }

    /**
     * Test of isAvailableToUsers method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testIsAvailableToUsers() {
        System.out.println("isAvailableToUsers");
        assertEquals(true, schemaDotOrgExporter.isAvailableToUsers());
    }

    /**
     * Test of getXMLNameSpace method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLNameSpace() {

        String result = schemaDotOrgExporter.getXMLNameSpace();

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Test of getXMLSchemaLocation method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLSchemaLocation() {

        String result = schemaDotOrgExporter.getXMLSchemaLocation();

        Assert.assertTrue(result.isEmpty());
    }

    /**
     * Test of getXMLSchemaVersion method, of class SchemaDotOrgExporter.
     */
    @Test
    public void testGetXMLSchemaVersion() {

        String result = schemaDotOrgExporter.getXMLSchemaVersion();

        Assert.assertTrue(result.isEmpty());
    }
    

    @TestBean
    static class MockDatasetFieldSvc extends DatasetFieldServiceBean {

        Map<String, DatasetFieldType> fieldTypes = new HashMap<>();
        long nextId = 1;

        public DatasetFieldType add(DatasetFieldType t) {
            if (t.getId() == null) {
                t.setId(nextId++);
            }
            fieldTypes.put(t.getName(), t);
            return t;
        }

        @Override
        public DatasetFieldType findByName(String name) {
            return fieldTypes.get(name);
        }

        @Override
        public DatasetFieldType findByNameOpt(String name) {
            return findByName(name);
        }

        @Override
        public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
            ControlledVocabularyValue cvv = new ControlledVocabularyValue();
            cvv.setDatasetFieldType(dsft);
            cvv.setStrValue(strValue);
            return cvv;
        }

    }

}
