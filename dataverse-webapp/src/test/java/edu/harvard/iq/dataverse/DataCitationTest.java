package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DataCitation;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

/**
 * Testing DataCitation class
 *
 * @author pkiraly@gwdg.de
 */
public class DataCitationTest {

    /**
     * Test the public properties of DataCitation class via their getters
     *
     * @throws ParseException
     */
    @Test
    public void testProperties() throws ParseException {
        //given
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion("Dataset Title", true));

        //when & then
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
     *
     * @throws ParseException
     */
    @Test
    public void testGetDataCiteMetadata() throws ParseException {
        //given
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion("Dataset Title", true));

        //when
        Map<String, String> properties = dataCitation.getDataCiteMetadata();

        //then
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
     *
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testWriteAsBibtexCitation() throws ParseException, IOException {
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        //when
        dataCitation.writeAsBibtexCitation(os);
        String out = new String(os.toByteArray(), StandardCharsets.UTF_8);

        //then
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                        + "author = {First Last},\r\n"
                        + "publisher = {LibraScholar},\r\n"
                        + "title = \"{Dataset Title}\",\r\n"
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
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                        + "author = {First Last},\r\n"
                        + "publisher = {LibraScholar},\r\n"
                        + "title = \"{Dataset Title}\",\r\n"
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
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                        + "author = {},\r\n"
                        + "publisher = {LibraScholar},\r\n"
                        + "title = \"{Dataset Title}\",\r\n"
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
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                        + "author = {First Last},\r\n"
                        + "publisher = {LibraScholar},\r\n"
                        + "title = \"{}\",\r\n"
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
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "@data{LK0D1H_1955,\r\n"
                        + "author = {},\r\n"
                        + "publisher = {LibraScholar},\r\n"
                        + "title = \"{}\",\r\n"
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
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
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
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
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
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
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
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
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
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "First Last, 1955, \"Dataset Title\", https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
                dataCitation.toString()
        );
    }

    @Test
    public void testToString_withoutTitleAndAuthor() throws ParseException {
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "1955, https://doi.org/10.5072/FK2/LK0D1H, LibraScholar, V1",
                dataCitation.toString()
        );
    }

    @Test
    public void testToHtmlString_withTitleAndAuthor() throws ParseException {
        //given
        DatasetVersion datasetVersion = createATestDatasetVersion("Dataset Title", true);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "First Last, 1955, \"Dataset Title\"," +
                        " <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>," +
                        " LibraScholar, V1",
                dataCitation.toString(true)
        );
    }

    @Test
    public void testToHtmlString_withoutTitleAndAuthor() throws ParseException {
        //given
        String nullDatasetTitle = null;
        DatasetVersion datasetVersion = createATestDatasetVersion(nullDatasetTitle, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);

        //when & then
        assertEquals(
                "1955," +
                        " <a href=\"https://doi.org/10.5072/FK2/LK0D1H\" target=\"_blank\">https://doi.org/10.5072/FK2/LK0D1H</a>," +
                        " LibraScholar, V1",
                dataCitation.toString(true)
        );
    }

    @Test
    public void testTitleWithQuotes() throws ParseException {
        //given
        DataCitation dataCitation = new DataCitation(createATestDatasetVersion("This Title \"Has Quotes\" In It", true));

        //when & then
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
                        + "title = \"{This Title ``Has Quotes'' In It}\",\r\n"
                        + "year = {1955},\r\n"
                        + "version = {V1},\r\n"
                        + "doi = {10.5072/FK2/LK0D1H},\r\n"
                        + "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n"
                        + "}\r\n",
                dataCitation.toBibtexString()
        );

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
        author.setDatasetFieldType(new DatasetFieldType(DatasetFieldConstant.author, FieldType.TEXT, false));
        DatasetField authorName = constructPrimitive(DatasetFieldConstant.authorName, value);
        authorName.setDatasetFieldParent(author);
        author.getDatasetFieldsChildren().add(authorName);

        return author;
    }

    private DatasetField createTitleField(String value) {
        return constructPrimitive(DatasetFieldConstant.title, value);
    }

    DatasetField constructPrimitive(String fieldName, String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(
                new DatasetFieldType(fieldName, FieldType.TEXT, false));
        field.setFieldValue(value);
        return field;
    }
}
