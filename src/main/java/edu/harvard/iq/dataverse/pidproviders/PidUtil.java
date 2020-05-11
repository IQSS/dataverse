package edu.harvard.iq.dataverse.pidproviders;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Base64;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class PidUtil {

    public static JsonObjectBuilder queryDoi(String persistentId, String baseUrl, String username, String password) {
        URL url;
        try {
            url = new URL(baseUrl + persistentId);
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
            JsonObject out = Json.createReader(connection.getErrorStream()).readObject();
            return Json.createObjectBuilder().add("response", out);
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

}
