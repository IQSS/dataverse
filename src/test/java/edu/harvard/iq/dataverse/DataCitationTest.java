package edu.harvard.iq.dataverse;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;

/**
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
        DatasetVersion datasetVersion = createATestDatasetVersion();

        DataCitation dataCitation = new DataCitation(datasetVersion);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        dataCitation.writeAsBibtexCitation(os);
        String out = new String(os.toByteArray(), "UTF-8");
        assertEquals(
            "@data{LK0D1H_1955,\r\n" +
            "author = {},\r\n" +
            "publisher = {LibraScholar},\r\n" +
            "title = {},\r\n" +
            "year = {1955},\r\n" +
            "doi = {10.5072/FK2/LK0D1H},\r\n" +
            "url = {https://doi.org/10.5072/FK2/LK0D1H}\r\n" +
            "}\r\n",
            out
        );
    }

    private DatasetVersion createATestDatasetVersion() throws ParseException {
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

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        Date publicationDate = dateFmt.parse("19551105");

        datasetVersion.setReleaseTime(publicationDate);

        dataset.setPublicationDate(new Timestamp(publicationDate.getTime()));

        return datasetVersion;
    }
}
