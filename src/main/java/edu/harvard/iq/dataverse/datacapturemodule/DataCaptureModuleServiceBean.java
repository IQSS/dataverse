package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.logging.Logger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataCaptureModuleUrl;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.JsonObjectBuilder;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    /**
     * @param user AuthenticatedUser
     * @return Unirest response as JSON or null.
     * @throws Exception if Data Capture Module URL hasn't been configured or if
     * the POST failed for any reason.
     */
    public HttpResponse<JsonNode> requestRsyncScriptCreation(AuthenticatedUser user, Dataset dataset, JsonObjectBuilder jab) throws Exception {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new Exception("Problem POSTing JSON to Data Capture Module. The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        String jsonString = jab.build().toString();
        logger.fine("JSON to send to Data Capture Module: " + jsonString);
        HttpResponse<JsonNode> uploadRequest = Unirest.post(dcmBaseUrl + "/ur.py")
                .body(jsonString)
                .asJson();
        return uploadRequest;
    }

    public HttpResponse<JsonNode> retreiveRequestedRsyncScript(AuthenticatedUser user, Dataset dataset) throws Exception {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new Exception("Problem GETing JSON to Data Capture Module for dataset " + dataset.getId() + " The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        HttpResponse<JsonNode> scriptRequest = Unirest
                .get(dcmBaseUrl + "/sr.py/" + dataset.getId())
                .asJson();
        return scriptRequest;
    }

    public Dataset persistRsyncScript(Dataset dataset, String script) {
        dataset.setRsyncScript(script);
        return em.merge(dataset);
    }

}
