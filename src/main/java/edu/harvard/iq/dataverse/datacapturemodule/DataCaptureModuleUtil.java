package edu.harvard.iq.dataverse.datacapturemodule;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.DatasetVersion;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class DataCaptureModuleUtil {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleUtil.class.getCanonicalName());

    public static boolean rsyncSupportEnabled(String uploadMethodsSettings) {
        logger.fine("uploadMethodsSettings: " + uploadMethodsSettings);; 
        if (uploadMethodsSettings==null){
            return false;
        } else {
           return  Arrays.asList(uploadMethodsSettings.toLowerCase().split("\\s*,\\s*")).contains(SystemConfig.FileUploadMethods.RSYNC.toString());
        }
    }

    /**
     * generate JSON to send to DCM
     */
    public static JsonObject generateJsonForUploadRequest(AuthenticatedUser user, Dataset dataset) {
        JsonObjectBuilder jab = Json.createObjectBuilder();
        // The general rule should be to always pass the user id and dataset identifier to the DCM.
        jab.add("userId", user.getId());
        jab.add("datasetIdentifier", dataset.getIdentifier());
        return jab.build();
    }

    /**
     * transfer script from DCM
     */
    public static ScriptRequestResponse getScriptFromRequest(HttpResponse<JsonNode> uploadRequest) {
        int status = uploadRequest.getStatus();
        JsonNode body = uploadRequest.getBody();
        logger.fine("Got " + status + " with body: " + body);
        if (status == 404) {
            return new ScriptRequestResponse(status);
        }
        int httpStatusCode = uploadRequest.getStatus();
        String script = body.getObject().getString("script");
        String datasetIdentifier = body.getObject().getString("datasetIdentifier");
        long userId = body.getObject().getLong("userId");
        ScriptRequestResponse scriptRequestResponse = new ScriptRequestResponse(httpStatusCode, datasetIdentifier, userId, script);
        return scriptRequestResponse;
    }

    static UploadRequestResponse makeUploadRequest(HttpResponse<String> uploadRequest) {
        int status = uploadRequest.getStatus();
        String body = uploadRequest.getBody();
        logger.fine("Got " + status + " with body: " + body);
        return new UploadRequestResponse(uploadRequest.getStatus(), body);
    }

    public static String getMessageFromException(DataCaptureModuleException ex) {
        if (ex == null) {
            return "DataCaptureModuleException was null!";
        }
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ex.toString();
        }
        String message = ex.getMessage();
        if (message == null) {
            return cause.toString();
        }
        return message + " was caused by " + cause.getMessage();
    }

    public static String getScriptName(DatasetVersion datasetVersion) {
        return "upload-" + datasetVersion.getDataset().getIdentifier().replace("/", "_") + ".bash";
    }

}
