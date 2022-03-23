package edu.harvard.iq.dataverse.datacapturemodule;

import java.util.logging.Logger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.Serializable;
import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * This class contains all the methods that have external runtime dependencies
 * such as the Data Capture Module itself.
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
    public static String uploadRequestPath = "/ur.py";
    public static String scriptRequestPath = "/sr.py";

    // TODO: Do we care about authenticating to the DCM? If not, no need for AuthenticatedUser here.
    public UploadRequestResponse requestRsyncScriptCreation(String jsonString, String uploadRequestUrl) throws DataCaptureModuleException {
        logger.fine("requestRsyncScriptCreation using JSON string: " + jsonString + " and sending to " + uploadRequestUrl);
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
    public ScriptRequestResponse retreiveRequestedRsyncScript(String datasetIdentifier, String scriptRequestUrl) throws DataCaptureModuleException
    {
        logger.fine("retreiveRequestedRsyncScript using dataset identifier + " + datasetIdentifier + " to " + scriptRequestUrl);
        try
        {
            //When the result is an error, html is returned from DCM instead of json. This causes the parser to blow up unhelpfully.
            //Stock unirest hasn't been updated in years, but in a fork this issue seems to be improved: https://github.com/OpenUnirest/unirest-java/issues/10
            HttpResponse<JsonNode> scriptRequest = Unirest.post(scriptRequestUrl)
                                                   .field("datasetIdentifier", datasetIdentifier)
                                                   .asJson();
            return DataCaptureModuleUtil.getScriptFromRequest(scriptRequest);
        }
        catch( UnirestException ex)
        {
                        String error = "Error calling " + scriptRequestUrl + ". This likely indicates the DCM service returned an error page and not valid json. Unirest parsing error: " + ex;
            logger.info(error);
            throw new DataCaptureModuleException(error, ex);
        }
    }

}
