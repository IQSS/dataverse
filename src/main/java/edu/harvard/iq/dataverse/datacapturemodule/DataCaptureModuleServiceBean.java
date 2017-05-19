package edu.harvard.iq.dataverse.datacapturemodule;

import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.logging.Logger;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import edu.harvard.iq.dataverse.Dataset;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.DataCaptureModuleUrl;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
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
    // TODO: Do we care about authenticating to the DCM? If not, no need for AuthenticatedUser here.
    public HttpResponse<String> requestRsyncScriptCreation(AuthenticatedUser user, Dataset dataset, JsonObject jab) throws Exception {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new Exception("Problem POSTing JSON to Data Capture Module. The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        String jsonString = jab.toString();
        // JSON to send to Data Capture Module: {"userId":1642,"datasetId":3813}]]
        logger.info("JSON to send to Data Capture Module: " + jsonString);
        HttpResponse<String> uploadRequest = Unirest.post(dcmBaseUrl + "/ur.py")
                .body(jsonString)
                .asString();
        return uploadRequest;
    }

    // TODO: Do we care about authenticating to the DCM?
    public JsonObject retreiveRequestedRsyncScript(Dataset dataset) throws MalformedURLException, ProtocolException, IOException {
        String dcmBaseUrl = settingsService.getValueForKey(DataCaptureModuleUrl);
        if (dcmBaseUrl == null) {
            throw new RuntimeException("Problem GETing JSON to Data Capture Module for dataset " + dataset.getId() + " The '" + DataCaptureModuleUrl + "' setting has not been configured.");
        }
        String bodyString = "datasetIdentifier=" + dataset.getId();
        // FIXME: Remove this hard-coded value of "3813"! It's the only one that works!
        bodyString = "datasetIdentifier=" + 3813;
        byte[] postData = bodyString.getBytes(StandardCharsets.UTF_8);
        int postDataLength = postData.length;
//        String urlString = "http://localhost:8888/sr.py";
        String urlString = dcmBaseUrl + "/sr.py";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("charset", "utf-8");
        conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
        conn.setUseCaches(false);
        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }
        InputStreamReader inputStreamReader = new InputStreamReader((InputStream) conn.getContent());
        String result = new BufferedReader(inputStreamReader)
                .lines().collect(Collectors.joining("\n"));
        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonObject jsonObject = jsonReader.readObject();
        return jsonObject;

    }

    public Dataset persistRsyncScript(Dataset dataset, String script) {
        dataset.setRsyncScript(script);
        return em.merge(dataset);
    }

}
