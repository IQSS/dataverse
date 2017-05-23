package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.logging.Logger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataCaptureModuleUrl;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonObject;

/**
 * This class contains all the methods that have external runtime dependencies
 * such as the Data Capture Module itself and PostgreSQL.
 */
@Stateless
@Named
public class DataCaptureModuleServiceBean implements Serializable {

    private static final Logger logger = Logger.getLogger(DataCaptureModuleServiceBean.class.getCanonicalName());

    @EJB
    SettingsServiceBean settingsService;

    /**
     * @param user AuthenticatedUser
     * @return Unirest response as JSON or null.
     * @throws Exception if Data Capture Module URL hasn't been configured or if
     * the POST failed for any reason.
     */
    // TODO: Do we care about authenticating to the DCM? If not, no need for AuthenticatedUser here.
    public HttpResponse<String> requestRsyncScriptCreation(AuthenticatedUser user, Dataset dataset, JsonObject jab) throws Exception {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new Exception("Problem POSTing JSON to Data Capture Module. The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        String jsonString = jab.toString();
        // curl -H 'Content-Type: application/json' -X POST -d '{"datasetId":"42", "userId":"1642","datasetIdentifier":"42"}' http://localhost/ur.py
        logger.info("JSON to send to Data Capture Module: " + jsonString);
        HttpResponse<String> uploadRequest = Unirest.post(dcmBaseUrl + "/ur.py")
                .body(jsonString)
                .asString();
        return uploadRequest;
    }

    // TODO: Do we care about authenticating to the DCM?
    public ScriptRequestResponse retreiveRequestedRsyncScript(Dataset dataset) {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new RuntimeException("Problem GETing JSON to Data Capture Module for dataset " + dataset.getId() + " The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        return getRsyncScriptForDataset(dcmBaseUrl, dataset.getId());

    }

    public static ScriptRequestResponse getRsyncScriptForDataset(String dcmBaseUrl, long datasetId) {
        String scriptRequestUrl = dcmBaseUrl + "/sr.py";
        try {
            HttpResponse<JsonNode> scriptRequest = Unirest.post(scriptRequestUrl)
                    .field("datasetIdentifier", datasetId)
                    .asJson();
            return DataCaptureModuleUtil.getScriptFromRequest(scriptRequest);
        } catch (UnirestException ex) {
            logger.info("Error calling " + scriptRequestUrl + ": " + ex);
            return null;
        }
    }

}
