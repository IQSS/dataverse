package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.logging.Logger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.Dataset;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataCaptureModuleUrl;
import java.io.Serializable;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * This class contains all the methods that have external runtime dependencies
 * such as the Data Capture Module itself and PostgreSQL.
 */
@Stateless
@Named
public class DataCaptureModuleServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBean.class.getCanonicalName());
    /**
     * You've got to give the DCM time to cook up an rsync script after making
     * an upload request. In dev, 400 milliseconds has been enough. Consider
     * making this configurable.
     */
    public static long millisecondsToSleepBetweenUploadRequestAndScriptRequestCalls = 500;

    // TODO: Do we care about authenticating to the DCM? If not, no need for AuthenticatedUser here.
    public UploadRequestResponse requestRsyncScriptCreation(AuthenticatedUser user, Dataset dataset, String dcmBaseUrl) throws DataCaptureModuleException {
        if (dcmBaseUrl == null) {
            throw new RuntimeException("Problem POSTing JSON to Data Capture Module. The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        return makeUploadRequest(dcmBaseUrl, user, dataset);
    }

    public static UploadRequestResponse makeUploadRequest(String dcmBaseUrl, AuthenticatedUser user, Dataset dataset) throws DataCaptureModuleException {
        String uploadRequestUrl = dcmBaseUrl + "/ur.py";
        String jsonString = DataCaptureModuleUtil.generateJsonForUploadRequest(user, dataset).toString();
        // curl -H 'Content-Type: application/json' -X POST -d '{"datasetId":"42", "userId":"1642","datasetIdentifier":"42"}' http://localhost/ur.py
        logger.info("JSON to send to Data Capture Module: " + jsonString);
        try {
            HttpResponse<String> uploadRequest = Unirest.post(uploadRequestUrl)
                    .body(jsonString)
                    .asString();
            UploadRequestResponse uploadRequestResponse = DataCaptureModuleUtil.makeUploadRequest(uploadRequest);
            return uploadRequestResponse;
        } catch (UnirestException ex) {
            String error = "Error calling " + uploadRequestUrl + ": " + ex;
            logger.info(error);
            throw new DataCaptureModuleException(error, ex);
        }
    }

    public ScriptRequestResponse retreiveRequestedRsyncScript(Dataset dataset, String dcmBaseUrl) throws DataCaptureModuleException {
        if (dcmBaseUrl == null) {
            throw new RuntimeException("Problem GETing JSON to Data Capture Module for dataset " + dataset.getId() + " The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        return getRsyncScriptForDataset(dcmBaseUrl, dataset.getId());

    }

    public static ScriptRequestResponse getRsyncScriptForDataset(String dcmBaseUrl, long datasetId) throws DataCaptureModuleException {
        String scriptRequestUrl = dcmBaseUrl + "/sr.py";
        try {
            HttpResponse<JsonNode> scriptRequest = Unirest.post(scriptRequestUrl)
                    .field("datasetIdentifier", datasetId)
                    .asJson();
            return DataCaptureModuleUtil.getScriptFromRequest(scriptRequest);
        } catch (UnirestException ex) {
            String error = "Error calling " + scriptRequestUrl + ": " + ex;
            logger.info(error);
            throw new DataCaptureModuleException(error, ex);
        }
    }

}
