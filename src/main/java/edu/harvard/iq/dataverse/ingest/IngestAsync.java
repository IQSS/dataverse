package edu.harvard.iq.dataverse.ingest;

import edu.harvard.iq.dataverse.Dataset;
import java.util.List;
import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;

public class IngestAsync {

    @Asynchronous
    public static Future<JsonObjectBuilder> getVersionsWithMissingUNFs(List<Dataset> datasets, String logFile) {
        JsonObjectBuilder response = Json.createObjectBuilder();
        JsonArray datasetVersionsWithMissingUNFs = IngestUtil.getVersionsWithMissingUNFs(datasets, logFile).build();
        response.add("datasetVersionsWithMissingUNFs", datasetVersionsWithMissingUNFs);
        return new AsyncResult<>(response);

    }

}
