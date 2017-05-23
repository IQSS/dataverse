package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import java.util.logging.Logger;

public class DataCaptureModuleUtil {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleUtil.class.getCanonicalName());

    public static ScriptRequestResponse getScriptFromRequest(HttpResponse<JsonNode> uploadRequest) {
        int status = uploadRequest.getStatus();
        JsonNode body = uploadRequest.getBody();
        logger.info("Got " + status + " with body: " + body);
        if (status == 404) {
            return new ScriptRequestResponse(status);
        }
        int httpStatusCode = uploadRequest.getStatus();
        String script = body.getObject().getString("script");
        long datasetId = body.getObject().getLong("datasetIdentifier");
        long userId = body.getObject().getLong("userId");
        ScriptRequestResponse scriptRequestResponse = new ScriptRequestResponse(httpStatusCode, datasetId, userId, script);
        return scriptRequestResponse;
    }

}
