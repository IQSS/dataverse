package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.branding.BrandingUtil;
import edu.harvard.iq.dataverse.branding.BrandingUtilTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Testing DataCitation class
 * @author pkiraly@gwdg.de
 */
public class DataCitationTest {
    
    /**
     * This test relies on {@link BrandingUtil}. We need to provide mocks for it.
     */
    @BeforeAll
    static void setup() {
        BrandingUtilTest.setupMocks();
    }
    /**
     * After this test is done, the mocks should be turned of
     * (so we keep atomicity and no one relies on them being present).
     */
    @AfterAll
    static void tearDown() {
        BrandingUtilTest.tearDownMocks();
    }
    
    /**
     * Test the public properties of DataCitation class via their getters
     * @throws ParseException
     */
    @Test
    public void testProperties() throws ParseException {
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

    /**
     * Test DataCite metadata
     * @throws ParseException
     */
    @Test
    public void testGetDataCiteMetadata() throws ParseException {
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

    /**
     * Test that bibtex data export contains a closing bracket
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testWriteAsBibtexCitation() throws ParseException, IOException {
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);

        DataCitation dataCitation = new DataCitation(datasetVersion);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataCitation.writeAsBibtexCitation(os);
        String out = new String(os.toByteArray(), "UTF-8");
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{Dataset Title}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
                out
        );
    }

    /**
     * Test that bibtex data export contains a closing bracket
     *
     * @throws ParseException
     */
    @Test
    public void testToBibtexString() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{Dataset Title}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
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
    public void testToBibtexString_withoutAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{Dataset Title}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
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
    public void testToBibtexString_withoutTitle() throws ParseException {
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
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
    public void testToBibtexString_withoutTitleAndAuthor() throws ParseException {
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
                dataCitation.toBibtexString()
        );
    }

    @Test
    public void testToRISString_withTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "Provider: LibraScholar\r\n" +
           "Content: text/plain; charset=\"utf-8\"\r\n" +
           "TY  - DATA\r\n" +
           "T1  - Dataset Title\r\n" +
           "AU  - First Last\r\n" +
           "DO  - doi:10.5072/FK2/LK0D1H\r\n" +
           "ET  - V1\r\n" +
           "PY  - 1955\r\n" +
           "SE  - 1955-11-05 00:00:00.0\r\n" +
           "UR  - https://doi.org/10.5072/FK2/LK0D1H\r\n" +
           "PB  - LibraScholar\r\n" +
           "ER  - \r\n",
           dataCitation.toRISString()
        );
    }

    @Test
    public void testToRISString_withoutTitleAndAuthor() throws ParseException {
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "Provider: LibraScholar\r\n" +
           "Content: text/plain; charset=\"utf-8\"\r\n" +
           "TY  - DATA\r\n" +
           "T1  - \r\n" +
           "DO  - doi:10.5072/FK2/LK0D1H\r\n" +
           "ET  - V1\r\n" +
           "PY  - 1955\r\n" +
           "SE  - 1955-11-05 00:00:00.0\r\n" +
           "UR  - https://doi.org/10.5072/FK2/LK0D1H\r\n" +
           "PB  - LibraScholar\r\n" +
           "ER  - \r\n",
           dataCitation.toRISString()
        );
    }

    @Test
    public void testToEndNoteString_withTitleAndAuthor() throws ParseException {
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
            "<electronic-resource-num>doi/10.5072/FK2/LK0D1H</electronic-resource-num>" +
            "</record>" +
            "</records>" +
            "</xml>";
    
        // similar = the content of the nodes in the documents are the same, but minor differences exist
        //           e.g. sequencing of sibling elements, values of namespace prefixes, use of implied attribute values
        // https://www.xmlunit.org/api/java/2.8.2/org/custommonkey/xmlunit/Diff.html
        XmlAssert.assertThat(dataCitation.toEndNoteString()).and(expected).areSimilar();
    }

    @Test
    public void testToEndNoteString_withoutTitleAndAuthor() throws ParseException {
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
           "<electronic-resource-num>doi/10.5072/FK2/LK0D1H</electronic-resource-num>" +
           "</record>" +
           "</records>" +
           "</xml>";
    
        // similar = the content of the nodes in the documents are the same, but minor differences exist
        //           e.g. sequencing of sibling elements, values of namespace prefixes, use of implied attribute values
        // https://www.xmlunit.org/api/java/2.8.2/org/custommonkey/xmlunit/Diff.html
        XmlAssert.assertThat(dataCitation.toEndNoteString()).and(expected).areSimilar();
    }

    @Test
    public void testToString_withTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "First Last, 1955, \"Dataset Title\", https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
           dataCitation.toString()
        );
    }

    @Test
    public void testToString_withoutTitleAndAuthor() throws ParseException {
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "1955, https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
           dataCitation.toString()
        );
    }

    @Test
    public void testToHtmlString_withTitleAndAuthor() throws ParseException {
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
    public void testToHtmlString_withoutTitleAndAuthor() throws ParseException {
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
    public void testTitleWithQuotes() throws ParseException {
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
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {{This Title ``Has Quotes'' In It}},\r\n"
                + "year = {1955},\r\n"
                + "version = {V1},\r\n"
                + "doi = {10.5072/FK2/LK0D1H},\r\n"
                + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                + "}\r\n",
                dataCitation.toBibtexString()
        );

    }

    @Test
    public void testFileCitationToStringHtml() throws ParseException {
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
    public void testFileCitationToStringHtmlFilePid() throws ParseException {
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

    private DatasetVersion createATestDatasetVersion(String withTitle, boolean withAuthor) throws ParseException {
        
        Dataverse dataverse = new Dataverse();
        dataverse.setName("LibraScholar");

        Dataset dataset = new Dataset();
        dataset.setProtocol("doi");
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("LK0D1H");
        dataset.setOwner(dataverse);

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
