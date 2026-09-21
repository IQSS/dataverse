package edu.harvard.iq.dataverse.makedatacount;

import java.io.IOException;
import java.util.List;
import edu.harvard.iq.dataverse.util.json.JsonUtil;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MakeDataCountUtilTest {

    @Test
    public void testParseSushi() {
        JsonObject report;
        try {
            report = JsonUtil.getJsonObjectFromFile("src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json");
            //  List<DatasetMetrics> datasetMetrics = parseSushiReport(report);
        } catch (IOException ex) {
            System.out.print("IO exception: " + ex.getMessage());
        } catch (Exception e) {
            System.out.print("Unspecified Exception: " + e.getMessage());
        }
    }

    @Test
    public void testParseCitations() {
        JsonObject report;
        try {
            report = JsonUtil.getJsonObjectFromFile("src/test/java/edu/harvard/iq/dataverse/makedatacount/citations-for-doi-10.7910-DVN-HQZOOB.json");
            List<DatasetExternalCitations> datasetExternalCitations = MakeDataCountUtil.parseCitations(report);
            assertEquals(2, datasetExternalCitations.size());
        } catch (IOException ex) {
            System.out.print("IO exception: " + ex.getMessage());
        }
    }

}
