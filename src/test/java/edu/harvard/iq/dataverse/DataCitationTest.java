package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Testing DataCitation class
 * @author pkiraly@gwdg.de
 */
public class DataCitationTest {

    /**
     * Test that bibtex data export contains a closing bracket
     * @throws ParseException
     * @throws IOException
     */
    @Test
    void testWriteAsBibtexCitation() throws ParseException, IOException {
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);

        DataCitation dataCitation = new DataCitation(datasetVersion);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataCitation.writeAsBibtexCitation(os);
        String out = new String(os.toByteArray(), "UTF-8");
        assertEquals(
           "@data{LK0D1H_1955,\r\n" +
              "author = {First Last},\r\n" +
              "publisher = {LibraScholar},\r\n" +
              "title = {Dataset Title},\r\n" +
              "year = {1955},\r\n" +
              "doi = {10.5072/FK2/LK0D1H},\r\n" +
              "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
              "}\r\n",
           out
        );
    }

    /**
     * Test that bibtex data export contains a closing bracket
     * @throws ParseException
     */
    @Test
    void testToBibtexString() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(true, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "@data{LK0D1H_1955,\r\n" +
              "author = {First Last},\r\n" +
              "publisher = {LibraScholar},\r\n" +
              "title = {Dataset Title},\r\n" +
              "year = {1955},\r\n" +
              "doi = {10.5072/FK2/LK0D1H},\r\n" +
              "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
              "}\r\n",
           dataCitation.toBibtexString()
        );
    }

    /**
     * Test that bibtex data export contains an empty author if no author is specified
     * @throws ParseException
     */
    @Test
    void testToBibtexString_withoutAuthor() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(true, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "@data{LK0D1H_1955,\r\n" +
              "author = {},\r\n" +
              "publisher = {LibraScholar},\r\n" +
              "title = {Dataset Title},\r\n" +
              "year = {1955},\r\n" +
              "doi = {10.5072/FK2/LK0D1H},\r\n" +
              "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
              "}\r\n",
           dataCitation.toBibtexString()
        );
    }

    /**
     * Test that bibtex data export contains an empty title if no title is specified
     * @throws ParseException
     */
    @Test
    void testToBibtexString_withoutTitle() throws ParseException {
        DatasetVersion datasetVersion = createATestDatasetVersion(false, true);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "@data{LK0D1H_1955,\r\n" +
              "author = {First Last},\r\n" +
              "publisher = {LibraScholar},\r\n" +
              "title = {},\r\n" +
              "year = {1955},\r\n" +
              "doi = {10.5072/FK2/LK0D1H},\r\n" +
              "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
              "}\r\n",
           dataCitation.toBibtexString()
        );
    }

    /**
     * Test that bibtex data export contains an empty author and title if no author, nor title is specified
     * @throws ParseException
     */
    @Test
    void testToBibtexString_withoutTitleAndAuthor() throws ParseException, IOException {
        DatasetVersion datasetVersion = createATestDatasetVersion(false, false);
        DataCitation dataCitation = new DataCitation(datasetVersion);
        assertEquals(
           "@data{LK0D1H_1955,\r\n" +
              "author = {},\r\n" +
              "publisher = {LibraScholar},\r\n" +
              "title = {},\r\n" +
              "year = {1955},\r\n" +
              "doi = {10.5072/FK2/LK0D1H},\r\n" +
              "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
              "}\r\n",
           dataCitation.toBibtexString()
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
