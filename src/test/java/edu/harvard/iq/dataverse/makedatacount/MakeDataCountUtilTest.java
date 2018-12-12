package edu.harvard.iq.dataverse.makedatacount;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import javax.json.Json;
import javax.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;

public class MakeDataCountUtilTest {

    @Test
    public void testParseSushi() {
        JsonObject report;
        // TODO: Replace this Dash example JSON from counter-processor with one from Dataverse once our logger is ready.
        try (FileReader reader = new FileReader("src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json")) {
            report = Json.createReader(reader).readObject();
            List<DatasetMetrics> datasetMetrics = MakeDataCountUtil.parseSushiReport(report);
            Assert.assertEquals(1, datasetMetrics.size());
        } catch (IOException ex) {
        }
    }

}
