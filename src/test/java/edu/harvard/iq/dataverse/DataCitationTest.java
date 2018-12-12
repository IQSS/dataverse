package edu.harvard.iq.dataverse;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Testing DataCitation class
 * @author pkiraly@gwdg.de
 */
public class DataCitationTest {

    /**
     * Test the public properties of DataCitation class via their getters
     * @throws ParseException
     */
    @Test
    public void testProperties() throws ParseException {
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion(true, true));
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
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion(true, true));
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
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);

        DataCitation dataCitation = new DataCitation(datasetVersion);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataCitation.writeAsBibtexCitation(os);
        String out = new String(os.toByteArray(), "UTF-8");
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {Dataset Title},\r\n"
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
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {Dataset Title},\r\n"
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
        DatasetVersion datasetVersion = createATestDatasetVersion(true, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {Dataset Title},\r\n"
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
        DatasetVersion datasetVersion = createATestDatasetVersion(false, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {First Last},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {},\r\n"
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
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                + "author = {},\r\n"
                + "publisher = {LibraScholar},\r\n"
                + "title = {},\r\n"
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
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
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
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
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
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "<?xml version='1.0' encoding='UTF-8'?>" +
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
           "</xml>",
           dataCitation.toEndNoteString()
        );
    }

    @Test
    public void testToEndNoteString_withoutTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
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
           "</xml>",
           dataCitation.toEndNoteString()
        );
    }

    @Test
    public void testToString_withTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "First Last, 1955, \"Dataset Title\", https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
           dataCitation.toString()
        );
    }

    @Test
    public void testToString_withoutTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "1955, https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
           dataCitation.toString()
        );
    }

    @Test
    public void testToHtmlString_withTitleAndAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
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
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "1955," +
           " <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>," +
           " LibraScholar, V1",
           dataCitation.toString(true)
        );
    }

    private DatasetVersion createATestDatasetVersion(boolean withTitle, boolean withAuthor) throws ParseException {
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
        if (withTitle) {
            fields.add(createTitleField("Dataset Title"));
        }
        if (withAuthor) {
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
