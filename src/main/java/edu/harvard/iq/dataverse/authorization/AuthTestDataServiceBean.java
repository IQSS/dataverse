package edu.harvard.iq.dataverse.authorization;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import static edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil.getRandomUserStatic;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import jakarta.ejb.Stateless;
import org.apache.commons.lang3.StringUtils;

@Stateless
public class AuthTestDataServiceBean {

    private static final Logger logger = Logger.getLogger(AuthTestDataServiceBean.class.getCanonicalName());

    /**
     * For testing, don't expect this to work well.
     */
    public Map<String, String> getRandomUser() throws JsonSyntaxException, JsonIOException {
        Map<String, String> fakeUser = new HashMap<>();
        String sURL = "https://api.randomuser.me/0.8";
        URL url = null;
        try {
            url = new URL(sURL);
        } catch (MalformedURLException ex) {
            logger.info("Exception: " + ex);
        }
        HttpURLConnection randomUserRequest = null;
        try {
            randomUserRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            logger.info("Exception: " + ex);
        }
        try {
            randomUserRequest.connect();
        } catch (IOException ex) {
            logger.info("Exception: " + ex);
        }

        JsonParser jp = new JsonParser();
        JsonElement root = null;
        try {
            root = jp.parse(new InputStreamReader((InputStream) randomUserRequest.getContent()));
        } catch (IOException ex) {
            logger.info("Exception: " + ex);
        }
        if (root == null) {
            return getRandomUserStatic();
        }
        JsonObject rootObject = root.getAsJsonObject();
        logger.fine(rootObject.toString());
        JsonElement results = rootObject.get("results");
        logger.fine(results.toString());
        JsonElement firstResult = results.getAsJsonArray().get(0);
        logger.fine(firstResult.toString());
        JsonElement user = firstResult.getAsJsonObject().get("user");
        JsonElement username = user.getAsJsonObject().get("username");
        JsonElement salt = user.getAsJsonObject().get("salt");
        JsonElement email = user.getAsJsonObject().get("email");
        JsonElement password = user.getAsJsonObject().get("password");
        JsonElement name = user.getAsJsonObject().get("name");
        JsonElement firstName = name.getAsJsonObject().get("first");
        JsonElement lastName = name.getAsJsonObject().get("last");
        String firstNameString = StringUtils.capitalize(firstName.getAsString());
        String lastNameString = StringUtils.capitalize(lastName.getAsString());
        fakeUser.put("firstName", firstNameString);
        fakeUser.put("lastName", lastNameString);
        fakeUser.put("displayName", firstNameString + " " + lastNameString);
        fakeUser.put("email", email.getAsString());
        fakeUser.put("idp", "https://idp." + password.getAsString() + ".com/idp/shibboleth");
        fakeUser.put("username", username.getAsString());
        fakeUser.put("eppn", salt.getAsString());
        return fakeUser;
    }

}
