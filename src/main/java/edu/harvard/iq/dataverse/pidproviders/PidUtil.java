package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PidUtil {

    private static final Logger logger = Logger.getLogger(PidUtil.class.getCanonicalName());

    public static JsonObjectBuilder queryDoi(String persistentId, String baseUrl, String username, String password) {
        String doi = acceptOnlyDoi(persistentId);
        URL url;
        try {
            url = new URL(baseUrl + "/dois/" + doi);
        } catch (MalformedURLException ex) {
            return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
        }
        try {
            connection.setRequestMethod("GET");
        } catch (ProtocolException ex) {
            return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
        }
        String userpass = username + ":" + password;
        String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
        connection.setRequestProperty("Authorization", basicAuth);
        int status = 0;
        try {
            status = connection.getResponseCode();
        } catch (IOException ex) {
            // Hostname not in DNS, for example.
            return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
        }
        if (status != 200) {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                JsonObject out = Json.createReader(connection.getErrorStream()).readObject();
                return Json.createObjectBuilder().add("response", out);
            } else {
                return Json.createObjectBuilder().add("response", BundleUtil.getStringFromBundle("pids.datacite.errors.noErrorStream"));
            }
        }
        JsonObject out;
        try {
            out = Json.createReader(connection.getInputStream()).readObject();
        } catch (IOException ex) {
            return Json.createObjectBuilder().add("response", ex.getLocalizedMessage());
        }
        JsonObject data = out.getJsonObject("data");
        String id = data.getString("id");
        JsonObject attributes = data.getJsonObject("attributes");
        String state = attributes.getString("state");
        JsonObjectBuilder ret = Json.createObjectBuilder()
                .add("id", id)
                .add("state", state);
        return ret;
    }

    /**
     * @param PID in the form doi:10.7910/DVN/TJCLKP
     * @return DOI in the form 10.7910/DVN/TJCLKP (no "doi:")
     */
    private static String acceptOnlyDoi(String persistentId) {
        GlobalId globalId = new GlobalId(persistentId);
        if (!GlobalId.DOI_PROTOCOL.equals(globalId.getProtocol())) {
            throw new IllegalArgumentException(BundleUtil.getStringFromBundle("pids.datacite.errors.DoiOnly"));
        }
        return globalId.getAuthority() + "/" + globalId.getIdentifier();
    }
}
