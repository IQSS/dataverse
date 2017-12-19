package edu.harvard.iq.dataverse.metadata.tabular;

import edu.harvard.iq.dataverse.DataFile;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.dataaccess.DataAccess;
import edu.harvard.iq.dataverse.dataaccess.StorageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.json.Json;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class TabularMetadataServiceBeanTest {

    private static TabularMetadataServiceBean tabularMetadataServiceBean;
    private static Dataset dataset;
    private static DataFile dataFile;

    @BeforeClass
    public static void setUpClass() {
        tabularMetadataServiceBean = new TabularMetadataServiceBean();
        dataset = new Dataset();
        dataset.setAuthority("10.5072/FK2");
        dataset.setIdentifier("ABCDE");
        dataFile = new DataFile();
        dataFile.setOwner(dataset);
        dataFile.setStorageIdentifier("12345");
        try {
            StorageIO<DataFile> storageIO = DataAccess.createNewStorageIO(dataFile, dataFile.getStorageIdentifier());
            storageIO.saveInputStream(new ByteArrayInputStream("data".getBytes()));
        } catch (IOException ioex) {
            ioex.printStackTrace();
        }
    }

    @Test
    public void testProcessPrepFileValid() throws IOException {
        String emptyJsonObject = "{}";
        assertEquals(true, tabularMetadataServiceBean.processDataSummary(emptyJsonObject, dataFile, true, null));
    }

    @Test
    public void testProcessPrepFileJsonArray() {
        String emptyJsonArray = Json.createArrayBuilder().build().toString();
        assertEquals(false, tabularMetadataServiceBean.processDataSummary(emptyJsonArray, dataFile, true, null));
    }

    @Test
    public void testProcessPrepFileNull() {
        assertEquals(false, tabularMetadataServiceBean.processDataSummary(null, dataFile, true, null));
    }

    @Test
    public void testProcessPrepFileEmptyString() {
        assertEquals(false, tabularMetadataServiceBean.processDataSummary("", dataFile, true, null));
    }

    @Test
    public void testProcessPrepFileNotJson() {
        assertEquals(false, tabularMetadataServiceBean.processDataSummary("This isn't JSON!", dataFile, true, null));
    }

}

