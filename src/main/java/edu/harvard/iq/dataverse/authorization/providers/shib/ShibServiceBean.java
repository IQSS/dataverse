package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import edu.harvard.iq.dataverse.Shib;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.authorization.users.User;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Named;
import org.apache.commons.lang.StringUtils;

@Named
@Stateless
public class ShibServiceBean {
    
    private static final Logger logger = Logger.getLogger(ShibServiceBean.class.getCanonicalName());
    
    @EJB
    AuthenticationServiceBean authSvc;
    @EJB
    BuiltinUserServiceBean builtinUserService;
    
    public AuthenticatedUser findAuthUserByEmail(String emailToFind) {
        return authSvc.getAuthenticatedUserByEmail(emailToFind);
    }
    
    public BuiltinUser findBuiltInUserByAuthUserIdentifier(String authUserIdentifier) {
        return builtinUserService.findByUserName(authUserIdentifier);
    }
    
    public AuthenticatedUser canLogInAsBuiltinUser(String username, String password) {
        logger.info("checking to see if " + username + " knows the password...");
        if (password == null) {
            logger.info("password was null");
            return null;
        }
        
        AuthenticationRequest authReq = new AuthenticationRequest();
        authReq.putCredential("Username", username);
        authReq.putCredential("Password", password);
        /**
         * @todo Should probably set IP address here.
         */
//        authReq.setIpAddress(session.getUser().getRequestMetadata().getIpAddress());

        String credentialsAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
        try {
            AuthenticatedUser au = authSvc.authenticate(credentialsAuthProviderId, authReq);
            logger.log(Level.INFO, "User authenticated: {0}", au.getEmail());
            return au;
        } catch (AuthenticationFailedException ex) {
            logger.info("The username and/or password you entered is invalid. Need assistance accessing your account?" + ex.getResponse().getMessage());
            return null;
        }
    }

    /**
     * @todo Move the getAffiliation method from the Shib JSF backing bean to
     * here.
     */
    public String getFriendlyInstitutionName(String entityId) {
        /**
         * @todo Look for the entityId (i.e.
         * "https://idp.testshib.org/idp/shibboleth") for find "TestShib Test
         * IdP" in (for example)
         * https://dataverse-demo.iq.harvard.edu/Shibboleth.sso/DiscoFeed
         *
         * It looks something like this: [ { "entityID":
         * "https://idp.testshib.org/idp/shibboleth", "DisplayNames": [ {
         * "value": "TestShib Test IdP", "lang": "en" } ], "Descriptions": [ {
         * "value": "TestShib IdP. Use this as a source of attributes\n for your
         * test SP.", "lang": "en" } ], "Logos": [ { "value":
         * "https://www.testshib.org/testshibtwo.jpg", "height": "88", "width":
         * "253" } ] } ]
         */
        return null;
    }

    /**
     * For testing, don't expect this to work well.
     */
    public Map<String, String> getRandomUser() throws JsonSyntaxException, JsonIOException {
        Map<String, String> fakeUser = new HashMap<>();
        String sURL = "http://api.randomuser.me";
        URL url = null;
        try {
            url = new URL(sURL);
        } catch (MalformedURLException ex) {
            Logger.getLogger(Shib.class.getName()).log(Level.SEVERE, null, ex);
        }
        HttpURLConnection randomUserRequest = null;
        try {
            randomUserRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            Logger.getLogger(Shib.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            randomUserRequest.connect();
        } catch (IOException ex) {
            Logger.getLogger(Shib.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        JsonParser jp = new JsonParser();
        JsonElement root = null;
        try {
            root = jp.parse(new InputStreamReader((InputStream) randomUserRequest.getContent()));
        } catch (IOException ex) {
            Logger.getLogger(Shib.class.getName()).log(Level.SEVERE, null, ex);
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
        fakeUser.put("firstName", firstName.getAsString());
        fakeUser.put("lastName", lastName.getAsString());
        fakeUser.put("displayName", StringUtils.capitalise(firstName.getAsString()) + " " + StringUtils.capitalise(lastName.getAsString()));
        fakeUser.put("email", email.getAsString());
        fakeUser.put("idp", "https://idp." + password.getAsString() + ".com/idp/shibboleth");
        fakeUser.put("username", username.getAsString());
        fakeUser.put("eppn", salt.getAsString());
        return fakeUser;
    }
}
