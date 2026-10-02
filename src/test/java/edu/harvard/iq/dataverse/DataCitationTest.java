package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import edu.harvard.iq.dataverse.dataset.DatasetType;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.pidproviders.AbstractPidProvider;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.xmlunit.assertj3.XmlAssert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testing DataCitation class
 * @author pkiraly@gwdg.de
 */
class DataCitationTest {
    
    private static final String CRLF = "\r\n";
    /** 2024-01-01T12:00:00Z */
    private static final long FIXED_MILLIS = 1704110400000L;
    /** {@code Date#toString()} of FIXED_MILLIS with the default time zone pinned to UTC. */
    private static final String FIXED_DATE_TOSTRING = "Mon Jan 01 12:00:00 UTC 2024";
    private static final String DOI_URL = "https://doi.org/10.5072/FK2/LK0D1H";
    private static final String HDL_URL = "https://hdl.handle.net/1902.1/111012";
    
    private static TimeZone originalTimeZone;
    private static Jsonb jsonb;
    
    /**
     * This test relies on {@link BrandingUtil}. We need to provide mocks for it.
     */
    @BeforeAll
    static void setup() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        jsonb = JsonbBuilder.create();
        BrandingUtilTest.setupMocks();
    }
    /**
     * After this test is done, the mocks should be turned of
     * (so we keep atomicity and no one relies on them being present).
     */
    @AfterAll
    static void tearDown() throws Exception {
        BrandingUtilTest.tearDownMocks();
        jsonb.close();
        TimeZone.setDefault(originalTimeZone);
    }
    
    /**
     * Test the public properties of DataCitation class via their getters
     * @throws ParseException
     */
    @Test
    void testProperties() throws ParseException {
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion("Dataset Title", true));
        assertEquals("First Last", dataCitation.getAuthorsString());
        assertNull(dataCitation.getFileTitle());
        assertEquals("doi:10.5072/FK2/LK0D1H", dataCitation.getPersistentId().asString());
        assertEquals("LibraScholar", dataCitation.getPublisher());
        assertEquals("Dataset Title", dataCitation.getTitle());
        assertNull(dataCitation.getUNF());
        assertEquals("V1", dataCitation.getVersion());
        assertEquals("1955", dataCitation.getYear());
    }
    
    @Nested
    class FormatsEnum {
        @ParameterizedTest
        @CsvSource({
            "CSL,      false, application/json",
            "CSL,      true,  application/json",
            "EndNote,  false, text/xml",
            "Internal, false, text/plain",
            "Internal, true,  text/html",
            "RIS,      false, text/plain",
            "BibTeX,   true,  text/plain"
        })
        void testMediaTypePerFormat(DataCitation.Format format, boolean html, String expected) {
            assertEquals(expected, DataCitation.getCitationFormatMediaType(format, html));
        }
        
        @Test
        void testFormatLookupIsCaseInsensitiveAndNullForUnknown() {
            assertEquals(DataCitation.Format.BibTeX, DataCitation.Format.lookup("bibtex"));
            assertEquals(DataCitation.Format.CSL, DataCitation.Format.lookup("CSL"));
            assertEquals(DataCitation.Format.EndNote, DataCitation.Format.lookup("endNOTE"));
            assertNull(DataCitation.Format.lookup("nope"));
            assertNull(DataCitation.Format.lookup(null));
        }
    }
    
    @Nested
    class Construction {
        @Test
        void testDraftVersionIsMarked() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            dsv.setVersionState(DatasetVersion.VersionState.DRAFT);
            
            assertEquals(BundleUtil.getStringFromBundle("draftversion"), new DataCitation(dsv).getVersion());
        }
        
        @Test
        void testDeaccessionedVersionIsMarked() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            dsv.setVersionState(DatasetVersion.VersionState.DEACCESSIONED);
            
            assertEquals("V1, " + BundleUtil.getStringFromBundle("deaccessionedversion"),
                new DataCitation(dsv).getVersion());
        }
        
        @Test
        void testUnfIsTakenFromTheDatasetVersion() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            dsv.setUNF("UNF:6:xyz");
            
            DataCitation c = new DataCitation(dsv);
            
            assertEquals("UNF:6:xyz", c.getUNF());
            assertTrue(c.toString().endsWith(", V1, UNF:6:xyz [fileUNF]"), c.toString());
        }
        
        @Test
        void testHarvestedDatasetUsesDistributorAndProductionDateAndHasNoVersion() throws ParseException {
            DataCitation c = new DataCitation(harvestedVersion("some-unlisted-style"));
            
            assertNull(c.getPersistentId());
            assertEquals("Remote Repository", c.getPublisher());
            assertEquals("", c.getVersion());
            assertEquals("1990", c.getYear());
            assertEquals("First Last, 1990, \"Dataset Title\", Remote Repository", c.toString());
        }
        
        @Test
        void testHarvestedDatasetKeepsPidForKnownHarvestStyles() throws ParseException {
            DataCitation c = new DataCitation(harvestedVersion(HarvestingClient.HARVEST_STYLE_DEFAULT));
            
            assertEquals("doi:10.5072/FK2/LK0D1H", c.getPersistentId().asString());
        }
        
        @Test
        void testOptionalValueCapturesDisplayValuesWithoutEntityReferences() {
            DatasetField field = urlField("Reference", "https://example.org/reference");
            DatasetFieldType type = field.getDatasetFieldType();
            
            DataCitation.OptionalValue captured = DataCitation.OptionalValue.from(field);
            
            assertEquals(new DataCitation.OptionalValue("Reference", "https://example.org/reference", true), captured);
            
            type.setTitle("Changed");
            field.setSingleValue("https://example.org/changed");
            assertEquals("Reference", captured.displayName());
            assertEquals("https://example.org/reference", captured.displayValue());
        }
        
        @Test
        void testOptionalValueRejectsNullField() {
            assertThrows(NullPointerException.class, () -> DataCitation.OptionalValue.from(null));
        }
    }

    @Nested
    class GettersAndMetadata {
        /**
         * Test DataCite metadata
         * @throws ParseException
         */
        @Test
        void testGetDataCiteMetadata() throws ParseException {
            DataCitation dataCitation = new DataCitation(createATestDatasetVersion("Dataset Title", true));
            Map<String, String> properties = dataCitation.getDataCiteMetadata();
            assertEquals(4, properties.size());
            assertEquals(
                "datacite.creator, datacite.publisher, datacite.title, datacite.publicationyear",
                StringUtils.join(properties.keySet(), ", ")
            );
            assertEquals("First Last", properties.get("datacite.creator"));
            assertEquals("LibraScholar", properties.get("datacite.publisher"));
            assertEquals("Dataset Title", properties.get("datacite.title"));
            assertEquals("1955", properties.get("datacite.publicationyear"));
        }
        
        @Test
        void testGetDataCiteMetadataFallsBackToUnavailable() {
            Map<String, String> metadata = citation("authors", List.of(), "publisher", "").getDataCiteMetadata();
            
            assertEquals(AbstractPidProvider.UNAVAILABLE, metadata.get("datacite.creator"));
            assertEquals(AbstractPidProvider.UNAVAILABLE, metadata.get("datacite.publisher"));
            assertEquals("A Title", metadata.get("datacite.title"));
            assertEquals("2024", metadata.get("datacite.publicationyear"));
        }
        
        @Test
        void testToStringDispatchesToTheMatchingWriter() {
            DataCitation c = citation();
            assertEquals(c.toBibtexString(), c.toString(DataCitation.Format.BibTeX, false, false));
            assertEquals(c.toRISString(), c.toString(DataCitation.Format.RIS, false, false));
            assertEquals(c.toEndNoteString(), c.toString(DataCitation.Format.EndNote, false, false));
            assertEquals(c.toString(true), c.toString(DataCitation.Format.Internal, true, false));
            assertEquals(JsonUtil.prettyPrint(c.getCSLJsonFormat()), c.toString(DataCitation.Format.CSL, false, false));
        }
    }
    
    @Nested
    class BibTexFormat {
        /**
         * Test that bibtex data export contains a closing bracket
         *
         * @throws ParseException
         * @throws IOException
         */
        @Test
        void testWriteAsBibtexCitation() throws ParseException, IOException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            
            DataCitation dataCitation = new DataCitation(datasetVersion);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            dataCitation.writeAsBibtexCitation(os);
            String out = os.toString(StandardCharsets.UTF_8);
            assertEquals(
                crlf(
                    "@data{LK0D1H_1955,", "author = {First Last},", "publisher = {LibraScholar},",
                    "title = {{Dataset Title}},", "year = {1955},", "version = {V1},", "doi = {10.5072/FK2/LK0D1H},",
                    "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                out
            );
        }
        
        /**
         * Test that bibtex data export contains a closing bracket
         *
         * @throws ParseException
         */
        @Test
        void testToBibtexString() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                crlf(
                    "@data{LK0D1H_1955,", "author = {First Last},", "publisher = {LibraScholar},",
                    "title = {{Dataset Title}},", "year = {1955},", "version = {V1},", "doi = {10.5072/FK2/LK0D1H},",
                    "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                dataCitation.toBibtexString()
            );
        }
        
        /**
         * Test that bibtex data export contains an empty author if no author is
         * specified
         *
         * @throws ParseException
         */
        @Test
        void testToBibtexString_withoutAuthor() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                crlf(
                    "@data{LK0D1H_1955,", "author = {},", "publisher = {LibraScholar},",
                    "title = {{Dataset Title}},", "year = {1955},", "version = {V1},", "doi = {10.5072/FK2/LK0D1H},",
                    "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                dataCitation.toBibtexString()
            );
        }
        
        /**
         * Test that bibtex data export contains an empty title if no title is
         * specified
         *
         * @throws ParseException
         */
        @Test
        void testToBibtexString_withoutTitle() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                crlf(
                    "@data{LK0D1H_1955,", "author = {First Last},", "publisher = {LibraScholar},",
                    "title = {{}},", "year = {1955},", "version = {V1},", "doi = {10.5072/FK2/LK0D1H},",
                    "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                dataCitation.toBibtexString()
            );
        }
        
        /**
         * Test that bibtex data export contains an empty author and title if no
         * author, nor title is specified
         *
         * @throws ParseException
         */
        @Test
        void testToBibtexString_withoutTitleAndAuthor() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                crlf(
                    "@data{LK0D1H_1955,", "author = {},", "publisher = {LibraScholar},", "title = {{}},",
                    "year = {1955},", "version = {V1},", "doi = {10.5072/FK2/LK0D1H},",
                    "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                dataCitation.toBibtexString()
            );
        }
        
        @Test
        void testTitleWithQuotes() throws ParseException {
            DataCitation dataCitation = new DataCitation(createATestDatasetVersion("This Title \"Has Quotes\" In It", true));
            assertEquals("First Last", dataCitation.getAuthorsString());
            assertNull(dataCitation.getFileTitle());
            assertEquals("doi:10.5072/FK2/LK0D1H", dataCitation.getPersistentId().asString());
            assertEquals("LibraScholar", dataCitation.getPublisher());
            assertEquals("This Title \"Has Quotes\" In It", dataCitation.getTitle());
            assertNull(dataCitation.getUNF());
            assertEquals("V1", dataCitation.getVersion());
            assertEquals("1955", dataCitation.getYear());
            assertEquals(
                crlf(
                "@data{LK0D1H_1955,", "author = {First Last},", "publisher = {LibraScholar},",
                    "title = {{This Title ``Has Quotes'' In It}},", "year = {1955},", "version = {V1},",
                    "doi = {10.5072/FK2/LK0D1H},", "url = {https://doi.org/10.5072/FK2/LK0D1H}", "}"
                ),
                dataCitation.toBibtexString()
            );
        }
        
        @Test
        void testBibtexDirectFileCitationIsAnIncollection() {
            DataCitation c = citation("fileTitle", "data.csv", "direct", true);
            
            assertEquals(
                crlf(
                    "@incollection{LK0D1H_2024,", "author = {Doe, Jane and Example Institute},",
                    "publisher = {LibraScholar},", "title = {data.csv},", "booktitle = {A Title},", "year = {2024},",
                    "version = {V1},", "doi = {10.5072/FK2/LK0D1H},", "url = {" + DOI_URL + "}", "}"
                ),
                c.toBibtexString()
            );
        }
        
        @Test
        void testBibtexIncludesUnfBeforeYear() {
            DataCitation c = citation("unf", "UNF:6:abc");
            
            assertEquals(
                crlf(
                    "@data{LK0D1H_2024,", "author = {Doe, Jane and Example Institute},", "publisher = {LibraScholar},",
                    "title = {{A Title}},", "UNF = {UNF:6:abc},", "year = {2024},", "version = {V1},",
                    "doi = {10.5072/FK2/LK0D1H},", "url = {" + DOI_URL + "}", "}"
                ),
                c.toBibtexString());
        }
        
        @Test
        void testBibtexOmitsDoiLineForOtherProtocols() {
            DataCitation c = citation("persistentId", pid("hdl", "1902.1", "111012", "https://hdl.handle.net/"));
            
            assertEquals(
                crlf(
                    "@data{111012_2024,", "author = {Doe, Jane and Example Institute},",
                    "publisher = {LibraScholar},", "title = {{A Title}},", "year = {2024},", "version = {V1},",
                    "url = {" + HDL_URL + "}", "}"
                ),
                c.toBibtexString()
            );
        }
    }
    
    @Nested
    class RisFormat {
        @Test
        void testToRISString_withTitleAndAuthor() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "Provider: LibraScholar\r\n" +
                    "Content: text/plain; charset=\"utf-8\"\r\n" +
                    "TY  - DATA\r\n" +
                    "T1  - Dataset Title\r\n" +
                    "AU  - First Last\r\n" +
                    "DO  - doi:10.5072/FK2/LK0D1H\r\n" +
                    "UR  - https://doi.org/10.5072/FK2/LK0D1H\r\n" +
                    "ET  - V1\r\n" +
                    "PY  - 1955\r\n" +
                    "SE  - 1955-11-05 00:00:00.0\r\n" +
                    "PB  - LibraScholar\r\n" +
                    "ER  - \r\n",
                dataCitation.toRISString()
            );
        }
        
        @Test
        void testToRISString_withoutTitleAndAuthor() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "Provider: LibraScholar\r\n" +
                    "Content: text/plain; charset=\"utf-8\"\r\n" +
                    "TY  - DATA\r\n" +
                    "T1  - \r\n" +
                    "DO  - doi:10.5072/FK2/LK0D1H\r\n" +
                    "UR  - https://doi.org/10.5072/FK2/LK0D1H\r\n" +
                    "ET  - V1\r\n" +
                    "PY  - 1955\r\n" +
                    "SE  - 1955-11-05 00:00:00.0\r\n" +
                    "PB  - LibraScholar\r\n" +
                    "ER  - \r\n",
                dataCitation.toRISString()
            );
        }
        
        @Test
        void risWritesEveryRepeatableField() {
            DataCitation c = citation(
                "seriesTitles", List.of("Series One", "Series Two"),
                "producers", List.of("Producer"),
                "funders", List.of("Funder"),
                "kindsOfData", List.of("survey"),
                "datesOfCollection", List.of("2020-01-01/2020-12-31"),
                "keywords", List.of("kw1", "kw2"),
                "languages", List.of("en"),
                "spatialCoverages", List.of("Earth"));
            
            assertEquals(crlf(
                "Provider: LibraScholar",
                "Content: text/plain; charset=\"utf-8\"",
                "TY  - DATA",
                "T1  - A Title",
                "T3  - Series One",
                "T3  - Series Two",
                "AU  - Doe, Jane",
                "AU  - Example Institute",
                "A2  - Producer",
                "A4  - Funder",
                "C3  - survey",
                "DA  - 2020-01-01/2020-12-31",
                "DO  - doi:10.5072/FK2/LK0D1H",
                "UR  - " + DOI_URL,
                "ET  - V1",
                "KW  - kw1",
                "KW  - kw2",
                "LA  - en",
                "PY  - 2024",
                "RI  - Earth",
                "SE  - " + FIXED_DATE_TOSTRING,
                "PB  - LibraScholar",
                "ER  - "), c.toRISString());
        }
        
        @Test
        void risNonDirectFileCitationAddsFileNameAndUnf() {
            String ris = citation("fileTitle", "data.csv", "unf", "UNF:6:abc").toRISString();
            
            assertTrue(ris.contains("T1  - A Title" + CRLF), ris);
            assertTrue(ris.endsWith("PB  - LibraScholar" + CRLF
                + "C1  - data.csv" + CRLF
                + "C2  - UNF:6:abc" + CRLF
                + "ER  - " + CRLF), ris);
        }
        
        @Test
        void risDirectFileCitationSwapsTitlesAndOmitsFileName() {
            String ris = citation("fileTitle", "data.csv", "direct", true, "unf", "UNF:6:abc").toRISString();
            
            assertTrue(ris.contains("T1  - data.csv" + CRLF + "T2  - A Title" + CRLF), ris);
            assertTrue(ris.endsWith("PB  - LibraScholar" + CRLF
                + "C2  - UNF:6:abc" + CRLF
                + "ER  - " + CRLF), ris);
        }
        
        @Test
        void risOmitsIdentifierLinesWithoutPid() {
            String ris = citation("persistentId", null).toRISString();
            
            assertTrue(!ris.contains("DO  - ") && !ris.contains("UR  - "), ris);
        }
    }

    @Nested
    class EndNoteFormat {
        @Test
        void testToEndNoteString_withTitleAndAuthor() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            String expected = "<?xml version='1.0' encoding='UTF-8'?>" +
                "<xml>" +
                "<records>" +
                "<record>" +
                "<ref-type name=\"Dataset\">59</ref-type>" +
                "<contributors>" +
                "<authors><author>First Last</author></authors>" +
                "</contributors>" +
                "<titles><title>Dataset Title</title></titles>" +
                "<section>1955-11-05</section>" +
                "<dates><year>1955</year></dates>" +
                "<edition>V1</edition>" +
                "<publisher>LibraScholar</publisher>" +
                "<urls><related-urls><url>https://doi.org/10.5072/FK2/LK0D1H</url></related-urls></urls>" +
                "<electronic-resource-num>10.5072/FK2/LK0D1H</electronic-resource-num>" +
                "</record>" +
                "</records>" +
                "</xml>";
            
            // similar = the content of the nodes in the documents are the same, but minor differences exist
            //           e.g. sequencing of sibling elements, values of namespace prefixes, use of implied attribute values
            // https://www.xmlunit.org/api/java/2.8.2/org/custommonkey/xmlunit/Diff.html
            XmlAssert.assertThat(dataCitation.toEndNoteString()).and(expected).areSimilar();
        }
        
        @Test
        void testToEndNoteString_withoutTitleAndAuthor() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            String expected =
                "<?xml version='1.0' encoding='UTF-8'?>" +
                    "<xml>" +
                    "<records>" +
                    "<record>" +
                    "<ref-type name=\"Dataset\">59</ref-type>" +
                    "<contributors />" +
                    "<titles><title></title></titles>" +
                    "<section>1955-11-05</section>" +
                    "<dates><year>1955</year></dates>" +
                    "<edition>V1</edition>" +
                    "<publisher>LibraScholar</publisher>" +
                    "<urls><related-urls><url>https://doi.org/10.5072/FK2/LK0D1H</url></related-urls></urls>" +
                    "<electronic-resource-num>10.5072/FK2/LK0D1H</electronic-resource-num>" +
                    "</record>" +
                    "</records>" +
                    "</xml>";
            
            // similar = the content of the nodes in the documents are the same, but minor differences exist
            //           e.g. sequencing of sibling elements, values of namespace prefixes, use of implied attribute values
            // https://www.xmlunit.org/api/java/2.8.2/org/custommonkey/xmlunit/Diff.html
            XmlAssert.assertThat(dataCitation.toEndNoteString()).and(expected).areSimilar();
        }
        
        @Test
        void endNoteWritesEveryRepeatableField() {
            String xml = citation(
                "producers", List.of("Producer"),
                "funders", List.of("Funder"),
                "seriesTitles", List.of("Series One", "Series Two"),
                "datesOfCollection", List.of("2020-01-01/2020-12-31"),
                "keywords", List.of("kw1", "kw2"),
                "kindsOfData", List.of("survey"),
                "languages", List.of("en"),
                "spatialCoverages", List.of("Earth")).toEndNoteString();
            
            XmlAssert.assertThat(xml).nodesByXPath("/xml/records/record/contributors/authors/author").hasSize(2);
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/contributors/secondary-authors/author")
                .isEqualTo("Producer");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/contributors/subsidiary-authors/author")
                .isEqualTo("Funder");
            XmlAssert.assertThat(xml).nodesByXPath("/xml/records/record/titles/tertiary-titles/tertiary-title")
                .hasSize(2);
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/dates/pub-dates/date")
                .isEqualTo("2020-01-01/2020-12-31");
            XmlAssert.assertThat(xml).nodesByXPath("/xml/records/record/keywords/keyword").hasSize(2);
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/custom3").isEqualTo("survey");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/language").isEqualTo("en");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/reviewed-item").isEqualTo("Earth");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/section").isEqualTo("2024-01-01");
        }
        
        @Test
        void endNoteDoiGetsRelatedUrlsAndElectronicResourceNumber() {
            String xml = citation().toEndNoteString();
            
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/urls/related-urls/url").isEqualTo(DOI_URL);
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/electronic-resource-num")
                .isEqualTo("10.5072/FK2/LK0D1H");
        }
        
        @ParameterizedTest
        @CsvSource({
            "hdl,   1902.1, 111012, https://hdl.handle.net/",
            "perma, 10.1,   abc123, https://perma.example/"
        })
        void endNoteHandleAndPermaGetWebUrls(String protocol, String authority, String identifier, String prefix) {
            String xml = citation("persistentId", pid(protocol, authority, identifier, prefix)).toEndNoteString();
            
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/urls/web-urls/url")
                .isEqualTo(prefix + authority + "/" + identifier);
            XmlAssert.assertThat(xml).doesNotHaveXPath("/xml/records/record/urls/related-urls");
            XmlAssert.assertThat(xml).doesNotHaveXPath("/xml/records/record/electronic-resource-num");
        }
        
        @Test
        void endNoteWithoutPidHasNoUrls() {
            String xml = citation("persistentId", null).toEndNoteString();
            
            XmlAssert.assertThat(xml).hasXPath("/xml/records/record/urls");
            XmlAssert.assertThat(xml).doesNotHaveXPath("/xml/records/record/urls/*");
        }
        
        @Test
        void endNoteFileCitationAddsCustomFields() {
            String xml = citation("fileTitle", "data.csv", "unf", "UNF:6:abc").toEndNoteString();
            
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/titles/title").isEqualTo("A Title");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/custom1").isEqualTo("data.csv");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/custom2").isEqualTo("UNF:6:abc");
        }
        
        @Test
        void endNoteDirectFileCitationUsesSecondaryTitle() {
            String xml = citation("fileTitle", "data.csv", "direct", true).toEndNoteString();
            
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/titles/title").isEqualTo("data.csv");
            XmlAssert.assertThat(xml).valueByXPath("/xml/records/record/titles/secondary-title").isEqualTo("A Title");
        }
    }

    @Nested
    class InternalFormat {
        @ParameterizedTest
        @EnumSource(value = DataCitation.Format.class, names = "Internal", mode = EnumSource.Mode.EXCLUDE)
        void anonymizedIsOnlySupportedForInternalFormat(DataCitation.Format format) {
            assertNull(citation().toString(format, false, true));
        }
        
        @Test
        void testToString_withTitleAndAuthor() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "First Last, 1955, \"Dataset Title\", https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
                dataCitation.toString()
            );
        }
        
        @Test
        void testToString_withoutTitleAndAuthor() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "1955, https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
                dataCitation.toString()
            );
        }
        
        @Test
        void testToHtmlString_withTitleAndAuthor() throws ParseException {
            DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "First Last, 1955, \"Dataset Title\"," +
                    " <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>," +
                    " LibraScholar, V1",
                dataCitation.toString(true)
            );
        }
        
        @Test
        void testToHtmlString_withoutTitleAndAuthor() throws ParseException {
            String nullDatasetTitle = null;
            DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
            DataCitation dataCitation = new DataCitation(datasetVersion);
            assertEquals(
                "1955," +
                    " <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>," +
                    " LibraScholar, V1",
                dataCitation.toString(true)
            );
        }
        
        @Test
        void testInternalFormatBaseline() {
            DataCitation c = citation();
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"A Title\", " + DOI_URL + ", LibraScholar, V1",
                c.toString()
            );
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"A Title\", <a href=\"" + DOI_URL + "\" target=\"_blank\">" + DOI_URL + "</a>, LibraScholar, V1",
                c.toString(true)
            );
        }
        
        @Test
        void testInternalFormatEscapesHtmlOnlyWhenRequested() {
            DataCitation c = citation("title", "Fish & <Chips>", "authors", List.of("Smith & Sons"));
            
            assertTrue(c.toString().startsWith("Smith & Sons, 2024, \"Fish & <Chips>\", "));
            assertTrue(c.toString(true).startsWith("Smith &amp; Sons, 2024, \"Fish &amp; &lt;Chips&gt;\", "));
        }
        
        @Test
        void testInternalFormatAnonymizedReplacesAuthors() {
            String withheld = BundleUtil.getStringFromBundle("file.anonymized.authorsWithheld");
            
            assertEquals(withheld + ", 2024, \"A Title\", " + DOI_URL + ", LibraScholar, V1",
                citation().toString(false, true));
        }
        
        @Test
        void testInternalFormatSkipsMissingPidAndEmptyParts() {
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"A Title\", LibraScholar, V1",
                citation("persistentId", null).toString()
            );
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"A Title\", " + DOI_URL,
                citation("publisher", "", "version", "").toString()
            );
        }
        
        @Test
        void testInternalFormatAppendsFileNameAndUnfForNonDirectFileCitation() {
            DataCitation c = citation("fileTitle", "data.csv", "unf", "UNF:6:abc");
            
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"A Title\", " + DOI_URL +
                ", LibraScholar, V1; data.csv [fileName], UNF:6:abc [fileUNF]",
                c.toString()
            );
        }
        
        @Test
        void testInternalFormatPutsFileTitleFirstForDirectFileCitation() {
            DataCitation c = citation("fileTitle", "data.csv", "direct", true);
            
            assertEquals(
                "Doe, Jane; Example Institute, 2024, \"data.csv\", <em>A Title</em>, " +
                "<a href=\"" + DOI_URL + "\" target=\"_blank\">" + DOI_URL + "</a>, LibraScholar, V1",
                c.toString(true)
            );
        }
        
        @Test
        void testSingleOptionalUrlIsLabelledUrl() {
            DataCitation c = citation("optionalValues", List.of(
                optionalValue("Note", "plain text", false),
                optionalValue("Reference", "https://example.org/data", true)
            ));
            
            assertTrue(c.toString().endsWith("V1 [Note: plain text] [URL: https://example.org/data]"), c.toString());
        }
        
        @Test
        void testMultipleOptionalUrlsKeepTheirNames() {
            DataCitation c = citation("optionalValues", List.of(
                optionalValue("Link A", "https://example.org/a", true),
                optionalValue("Link B", "https://example.org/b", true)
            ));
            
            assertTrue(
                c.toString().endsWith("V1 [Link A: https://example.org/a] [Link B: https://example.org/b]"),
                c.toString());
            assertTrue(c.toString(true)
                .contains("[Link A: <a href=\"https://example.org/a\" target=\"_blank\">https://example.org/a</a>]")
            );
        }
        
        @Test
        void testOptionalNonUrlValuesAreEscapedInHtml() {
            DataCitation c = citation("optionalValues", List.of(optionalValue("Note", "a < b & c", false)));
            
            assertTrue(c.toString(true).endsWith("[Note: a &lt; b &amp; c]"));
            assertTrue(c.toString(false).endsWith("[Note: a < b & c]"));
        }
    }
    
    @Nested
    class FileCitation {
        @Test
        void testFileCitationToStringHtml() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setLabel("foo.txt");
            fileMetadata.setDataFile(new DataFile());
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            fileMetadata.setDatasetVersion(dsv);
            dsv.setDataset(dsv.getDataset());
            DataCitation fileCitation = new DataCitation(fileMetadata, false);
            assertEquals("First Last, 1955, \"Dataset Title\", <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>, LibraScholar, V1; foo.txt [fileName]", fileCitation.toString(true));
        }
        
        @Test
        void testFileCitationToStringHtmlFilePid() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setLabel("foo.txt");
            DataFile dataFile = new DataFile();
            dataFile.setProtocol("doi");
            dataFile.setAuthority("10.42");
            dataFile.setIdentifier("myFilePid");
            fileMetadata.setDataFile(dataFile);
            dsv.setVersionState(DatasetVersion.VersionState.RELEASED);
            fileMetadata.setDatasetVersion(dsv);
            dsv.setDataset(dsv.getDataset());
            DataCitation fileCitation = new DataCitation(fileMetadata, true);
            assertEquals("First Last, 1955, \"foo.txt\", <em>Dataset Title</em>, <a href=\"https://doi.org/10.42/myFilePid\" target=\"_blank\">https://doi.org/10.42/myFilePid</a>, LibraScholar, V1", fileCitation.toString(true));
        }
        
        @Test
        void testDirectFileCitationWithoutFilePidHasNoPid() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setLabel("foo.txt");
            fileMetadata.setDataFile(new DataFile());
            fileMetadata.setDatasetVersion(dsv);
            
            DataCitation c = new DataCitation(fileMetadata, true);
            
            assertNull(c.getPersistentId());
            assertEquals("foo.txt", c.getFileTitle());
            assertTrue(c.isDirect());
        }
    }
    
    @Nested
    class CslFormat {
        @Test
        void testCslJsonContainsAllMappedProperties() {
            JsonObject csl = citation(
                "title", "Fish & Chips",
                "description", "Desc <b>",
                "seriesTitles", List.of("Series One", "Series Two"),
                "keywords", List.of("a & b", "c")).getCSLJsonFormat();
            
            assertEquals("dataset", csl.getString("type"));
            assertEquals("Fish &amp; Chips", csl.getString("title"));
            assertEquals("Desc &lt;b&gt;", csl.getString("abstract"));
            assertEquals("V1", csl.getString("version"));
            assertEquals("10.5072/FK2/LK0D1H", csl.getString("DOI"));
            assertEquals("LibraScholar", csl.getString("publisher"));
            assertEquals(SystemConfig.getDataverseSiteUrlStatic() + "/citation?persistentId=doi:10.5072/FK2/LK0D1H",
                csl.getString("URL"));
            assertEquals("Series One", csl.getString("container-title"));
            assertEquals(2024, csl.getJsonObject("issued").getJsonArray("date-parts").getJsonArray(0).getInt(0));
            
            JsonArray authors = csl.getJsonArray("author");
            assertEquals(2, authors.size());
            assertEquals("Doe", authors.getJsonObject(0).getString("family"));
            assertEquals("Jane", authors.getJsonObject(0).getString("given"));
            assertEquals("Example Institute", authors.getJsonObject(1).getString("literal"));
            
            JsonArray categories = csl.getJsonArray("categories");
            assertEquals(2, categories.size());
            assertEquals("a &amp; b", categories.getString(0));
            assertEquals("c", categories.getString(1));
        }
        
        @Test
        void testCslJsonOmitsContainerTitleWithoutSeries() {
            assertTrue(!citation().getCSLJsonFormat().containsKey("container-title"));
        }
        
        @ParameterizedTest
        @CsvSource({
            "dataset,          dataset",
            "software,         software",
            "review,           review",
            "no-such-csl-type, entry"
        })
        void testCslTypeFollowsDatasetType(String datasetType, String expectedCslType) {
            assertEquals(expectedCslType, citation("datasetType", datasetType).getCSLJsonFormat().getString("type"));
        }
        
        @Test
        void testCslTypeFallsBackToEntryWithoutDatasetType() {
            assertEquals("entry", citation("datasetType", null).getCSLJsonFormat().getString("type"));
        }
        
        @Test
        void testDatasetTypeDrivesCslTypeAndMissingTypeFallsBackToDefault() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            
            DatasetType software = new DatasetType();
            software.setName(DatasetType.DATASET_TYPE_SOFTWARE);
            dsv.getDataset().setDatasetType(software);
            assertEquals("software", new DataCitation(dsv).getCSLJsonFormat().getString("type"));
            
            dsv.getDataset().setDatasetType(null);
            assertEquals("dataset", new DataCitation(dsv).getCSLJsonFormat().getString("type"));
        }
        
        @Test
        void testCitationDatasetFieldsBecomeOptionalValues() throws ParseException {
            DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
            DatasetField reference = urlField("Reference", "https://example.org/reference");
            reference.getDatasetFieldType().setId(100L); // DatasetFieldType#equals is id-based
            dsv.getDataset().getOwner().setCitationDatasetFieldTypes(List.of(reference.getDatasetFieldType()));
            
            List<DatasetField> fields = new ArrayList<>(dsv.getDatasetFields());
            fields.add(reference);
            dsv.setDatasetFields(fields);
            
            assertTrue(new DataCitation(dsv).toString().endsWith(", V1 [URL: https://example.org/reference]"));
        }
    }
    
    // Note: de-serialization is already tested by using the citation() helper!
    @Nested
    class JsonBSerialization {
        @Test
        void testJsonbSerialization() throws ParseException {
            DataCitation citation = new DataCitation(createATestDatasetVersion("Dataset Title", true));
            
            // This is different to the implicit testing in citation() as it uses a different fixture, built from a DSV.
            // That way, the adapter for CSLName will be fully exercised.
            assertDoesNotThrow(() -> jsonb.fromJson(jsonb.toJson(citation), DataCitation.class));
        }
        
    }
    
    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    
    private DatasetVersion harvestedVersion(String harvestStyle) throws ParseException {
        DatasetVersion dsv = createATestDatasetVersion("Dataset Title", true);
        HarvestingClient client = new HarvestingClient();
        client.setHarvestStyle(harvestStyle);
        dsv.getDataset().setHarvestedFrom(client);
        
        List<DatasetField> fields = new ArrayList<>(dsv.getDatasetFields());
        fields.add(constructPrimitive(DatasetFieldConstant.distributorName, "Remote Repository"));
        fields.add(constructPrimitive(DatasetFieldConstant.productionDate, "1990-05-01"));
        dsv.setDatasetFields(fields);
        return dsv;
    }
    
    private static DatasetField urlField(String title, String value) {
        DatasetFieldType type = new DatasetFieldType("reference", DatasetFieldType.FieldType.URL, false);
        type.setTitle(title);
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(type);
        field.setSingleValue(value);
        return field;
    }
    
    private static String crlf(String... lines) {
        return String.join(CRLF, lines) + CRLF;
    }
    
    private static Map<String, Object> pid(String protocol, String authority, String identifier, String urlPrefix) {
        Map<String, Object> pid = new LinkedHashMap<>();
        pid.put("protocol", protocol);
        pid.put("authority", authority);
        pid.put("identifier", identifier);
        pid.put("separator", "/");
        pid.put("urlPrefix", urlPrefix);
        return pid;
    }
    
    private static Map<String, Object> optionalValue(String name, String value, boolean url) {
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("displayName", name);
        optional.put("displayValue", value);
        optional.put("url", url);
        return optional;
    }
    
    /**
     * Builds a citation from JSON with sensible defaults.
     * Overrides are key/value pairs; a null value removes the key (so the field keeps its default or stays null).
     * Tests JSON-B roundtrip for DataCitation at the same time.
     */
    private static DataCitation citation(Object... overrides) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("authors", List.of("Doe, Jane", "Example Institute"));
        json.put("cslAuthors", List.of(
            Map.of("family", "Doe", "given", "Jane"),
            Map.of("literal", "Example Institute")));
        json.put("title", "A Title");
        json.put("year", "2024");
        json.put("date", FIXED_MILLIS);
        json.put("persistentId", pid("doi", "10.5072/FK2", "LK0D1H", "https://doi.org/"));
        json.put("version", "V1");
        json.put("publisher", "LibraScholar");
        json.put("direct", false);
        json.put("datasetType", DatasetType.DEFAULT_DATASET_TYPE);
        
        for (int i = 0; i < overrides.length; i += 2) {
            String key = (String) overrides[i];
            if (overrides[i + 1] == null) {
                json.remove(key);
            } else {
                json.put(key, overrides[i + 1]);
            }
        }
        
        return jsonb.fromJson(jsonb.toJson(json), DataCitation.class);
    }
    
    private DatasetVersion createATestDatasetVersion(String withTitle, boolean withAuthor) throws ParseException {
        
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");
        
        DatasetType datasetType = new DatasetType();
        datasetType.setName(DatasetType.DEFAULT_DATASET_TYPE);

        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        dataset.setOwner(dataverse);
        dataset.setDatasetType(datasetType);

        DatasetVersion datasetVersion = new DatasetVersion();
        datasetVersion.setDataset(dataset);
        datasetVersion.setVersionState(DatasetVersion.VersionState.DRAFT);
        datasetVersion.setVersionState(DatasetVersion.VersionState.RELEASED);
        datasetVersion.setVersionNumber(1L);

        List<DatasetField> fields = new ArrayList<>();
        if (withTitle != null) {
            fields.add(createTitleField(withTitle));
        }
        if (withAuthor) {
            // TODO: "Last, First" would make more sense.
            fields.add(createAuthorField("First Last"));
        }

        if (!fields.isEmpty()) {
            datasetVersion.setDatasetFields(fields);
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");

        datasetVersion.setReleaseTime(publicationDate);

        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));

        return datasetVersion;
    }

    private DatasetField createAuthorField(String value) {
        DatasetField author = new DatasetField();
        author.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.author, DatasetFieldType.FieldType.TEXT, false));
        List<DatasetFieldCompoundValue> compoundValues = new LinkedList<>();
        DatasetFieldCompoundValue compoundValue = new DatasetFieldCompoundValue();
        compoundValue.setParentDatasetField(author);
        compoundValue.setChildDatasetFields(Arrays.asList(
           constructPrimitive(DatasetFieldConstant.authorName, value)
        ));
        compoundValues.add(compoundValue);
        author.setDatasetFieldCompoundValues(compoundValues);
        return author;
    }

    private DatasetField createTitleField(String value) {
        return constructPrimitive(DatasetFieldConstant.title, value);
    }

    DatasetField constructPrimitive(String fieldName, String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(
           new DatasetFieldType(fieldName, DatasetFieldType.FieldType.TEXT, false));
        field.setDatasetFieldValues(
           Collections.singletonList(
              new DatasetFieldValue(field, value)));
        return field;
    }
}
