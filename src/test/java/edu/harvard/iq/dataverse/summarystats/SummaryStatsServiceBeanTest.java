package edu.harvard.iq.dataverse.summarystats;

import edu.harvard.iq.dataverse.DataFile;
import java.io.IOException;
import javax.json.Json;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

public class SummaryStatsServiceBeanTest {

    private static SummaryStatsServiceBean summaryStatsServiceBean;
    private static DataFile dataFile;

    @BeforeClass
    public static void setUpClass() {
        summaryStatsServiceBean = new SummaryStatsServiceBean();
        dataFile = new DataFile();
    }

    @Test
    public void testProcessPrepFileValid() throws IOException {
        String emptyJsonObject = Json.createObjectBuilder().build().toString();
        assertEquals(true, summaryStatsServiceBean.processPrepFile(emptyJsonObject, dataFile));
    }

    @Test
    public void testProcessPrepFileJsonArray() {
        String emptyJsonArray = Json.createArrayBuilder().build().toString();
        assertEquals(false, summaryStatsServiceBean.processPrepFile(emptyJsonArray, dataFile));
    }

    @Test
    public void testProcessPrepFileNull() {
        assertEquals(false, summaryStatsServiceBean.processPrepFile(null, dataFile));
    }

    @Test
    public void testProcessPrepFileEmptyString() {
        assertEquals(false, summaryStatsServiceBean.processPrepFile("", dataFile));
    }

    @Test
    public void testProcessPrepFileNotJson() {
        assertEquals(false, summaryStatsServiceBean.processPrepFile("This isn't JSON!", dataFile));
    }

}
