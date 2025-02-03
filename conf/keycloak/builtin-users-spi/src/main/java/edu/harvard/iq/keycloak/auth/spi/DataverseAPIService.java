package edu.harvard.iq.keycloak.auth.spi;

import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Provides API interaction methods for Dataverse authentication.
 */
public class DataverseAPIService {

    private static final Logger logger = Logger.getLogger(DataverseAPIService.class);
    private static final String DATAVERSE_API_URL = "http://dataverse:8080/api/builtin-users/%s/canLoginWithGivenCredentials?password=%s";

    /**
     * Validates if a Dataverse built-in user can log in with the given credentials.
     *
     * @param username The username of the Dataverse built-in user.
     * @param password The password to be validated.
     * @return {@code true} if the user can log in, {@code false} otherwise.
     */
    public static boolean canLogInAsBuiltinUser(String username, String password) {
        HttpURLConnection connection = null;

        try {
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
            String requestUrl = String.format(DATAVERSE_API_URL, encodedUsername, encodedPassword);

            URL url = new URL(requestUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            logger.infof("Dataverse API response code for user '%s': %d", username, responseCode);

            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            logger.errorf(e, "Error occurred while validating login for user '%s'", username);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
