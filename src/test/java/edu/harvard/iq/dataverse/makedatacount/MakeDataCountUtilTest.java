package edu.harvard.iq.dataverse.makedatacount;

import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.engine.TestCommandContext;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.externaltools.ExternalTool;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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


}
