package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import java.io.StringReader;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

public class DataCaptureModuleUtil {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleUtil.class.getCanonicalName());

    public static ScriptRequestResponse getScriptFromRequest(HttpResponse<String> uploadRequest) {
        int status = uploadRequest.getStatus();
        String body = uploadRequest.getBody();
        logger.info("Got " + status + " with body: " + body);
        if (status == 404) {
            return new ScriptRequestResponse(status, 0, 0, null);
        }
        logger.info("status: " + status);
        if (body == null || body.isEmpty()) {
            return new ScriptRequestResponse(uploadRequest.getStatus(), 0, 0, null);
        }
        logger.info("body: " + body);
        JsonReader jsonReader = Json.createReader(new StringReader(body));
        JsonObject jsonObject = jsonReader.readObject();
        int httpStatusCode = uploadRequest.getStatus();
        String script = jsonObject.getString("script");
        // FIXME: Why do we need to expect Strings from DatasetsIT but non-Strings from DataCaptureModuleServiceBeanIT?
        boolean expectStrings = false;
        if (expectStrings) {
            String userId = jsonObject.getString("userId");
            String datasetId = jsonObject.getString("datasetIdentifier");
            ScriptRequestResponse scriptRequestResponse = new ScriptRequestResponse(httpStatusCode, new Long(datasetId), new Long(userId), script);
            return scriptRequestResponse;
        } else {
            int userId = jsonObject.getInt("userId");
            int datasetId = jsonObject.getInt("datasetIdentifier");
            ScriptRequestResponse scriptRequestResponse = new ScriptRequestResponse(httpStatusCode, datasetId, userId, script);
            return scriptRequestResponse;
        }
    }

}
