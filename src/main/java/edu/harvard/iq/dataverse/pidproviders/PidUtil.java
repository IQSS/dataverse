package edu.harvard.iq.dataverse.pidproviders;

import edu.harvard.iq.dataverse.GlobalId;
import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class PidUtil {

    private static final Logger logger = Logger.getLogger(PidUtil.class.getCanonicalName());

    /**
     * @throws BadRequestException if user didn't supply a DOI.
     *
     * @throws NotFoundException if DOI not found in DataCite.
     *
     * @throws ServiceUnavailableException if non 200 or non 404 response from
     * DataCite.
     *
     * @throws InternalServerErrorException on local misconfiguration such as
     * DataCite hostname not in DNS.
     */
    public static JsonObjectBuilder queryDoi(String persistentId, String baseUrl, String username, String password) {
        try {
            // This throws an exception if this is not a DOI, which is the only
            // user-supplied param - treat this as a BadRequest in the catch statement.
            String doi = acceptOnlyDoi(persistentId);
            URL url;
            // Other errors are all internal misconfiguration (any problems creating the URL), the
            // DOI doesn't exist (404 from DataCite), or problem at DataCite (other non-200 responses).
            int status = 0;
            HttpURLConnection connection = null;
            try {
                url = new URL(baseUrl + "/dois/" + doi);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                String userpass = username + ":" + password;
                String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userpass.getBytes()));
                connection.setRequestProperty("Authorization", basicAuth);

                status = connection.getResponseCode();
            } catch (IOException ex) {
                // Hostname not in DNS, for example.
                throw new InternalServerErrorException(
                        BundleUtil.getStringFromBundle("pids.datacite.errors.noResponseCode", Arrays.asList(baseUrl)));
            }
            if (status == 404) {
                //Could check to see if Dataverse expects the DOI to be registered - that would result in a 404 from Dataverse before having to contact DataCite, and DataCite could still return a 404
                throw new NotFoundException("404 (NOT FOUND) from DataCite for DOI " + persistentId);
            }
            if (status != 200) {
                /* We could just send back whatever status code DataCite sends, but we've seen
                 * DataCite sometimes respond with 403 when the credentials were OK, and their
                 * 500 error doesn't mean a problem with Dataverse, so wrapping any of them in
                 * a 503 error, to indicate this is a temporary error, might be the better option. In any case, we need to log the
                 * issue to be able to debug it.
                 */
                logger.severe("Received " + status + " error from DataCite for DOI: " + persistentId); 
                InputStream errorStream = connection.getErrorStream();
                if (errorStream != null) {
                    JsonObject out = Json.createReader(connection.getErrorStream()).readObject();
                    logger.severe("DataCite error response: " + out.toString());
                } else {
                    logger.severe("No error stream from DataCite");
                }
                throw new ServiceUnavailableException();
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
            JsonObjectBuilder ret = Json.createObjectBuilder().add("id", id).add("state", state);
            return ret;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getLocalizedMessage());
        }
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
