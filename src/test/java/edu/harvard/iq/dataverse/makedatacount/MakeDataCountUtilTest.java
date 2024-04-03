package edu.harvard.iq.dataverse.makedatacount;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MakeDataCountUtilTest {

    @Test
    public void testParseSushi() {
        JsonObject report;
        try (FileReader reader = new FileReader("src/test/java/edu/harvard/iq/dataverse/makedatacount/sushi_sample_logs.json")) {
            report = Json.createReader(reader).readObject();
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
        try (FileReader reader = new FileReader("src/test/java/edu/harvard/iq/dataverse/makedatacount/citations-for-doi-10.7910-DVN-HQZOOB.json")) {
            report = Json.createReader(reader).readObject();
            List<DatasetExternalCitations> datasetExternalCitations = MakeDataCountUtil.parseCitations(report);
            assertEquals(2, datasetExternalCitations.size());
        } catch (FileNotFoundException ex) {
            System.out.print("File not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.print("IO exception: " + ex.getMessage());
        }
    }

}
