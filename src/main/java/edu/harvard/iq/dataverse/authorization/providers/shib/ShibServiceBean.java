package edu.harvard.iq.dataverse.authorization.providers.shib;

import com.google.gson.JsonArray;
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
import edu.harvard.iq.dataverse.util.SystemConfig;
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
import javax.ejb.EJBException;
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
    @EJB
    SystemConfig systemConfig;

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
        } catch (EJBException ex) {
            Throwable cause = ex;
            StringBuilder sb = new StringBuilder();
            sb.append(ex + " ");
            while (cause.getCause() != null) {
                cause = cause.getCause();
                sb.append(cause.getClass().getCanonicalName() + " ");
                sb.append(cause.getMessage()).append(" ");
                /**
                 * @todo Investigate why authSvc.authenticate is throwing
                 * NullPointerException. If you convert a Shib user to a Builtin
                 * user, the password may be null.
                 */
                if (cause instanceof NullPointerException) {
                    for (int i = 0; i < 2; i++) {
                        StackTraceElement stacktrace = cause.getStackTrace()[i];
                        if (stacktrace != null) {
                            String classCanonicalName = stacktrace.getClass().getCanonicalName();
                            String methodName = stacktrace.getMethodName();
                            int lineNumber = stacktrace.getLineNumber();
                            String error = "at " + stacktrace.getClassName() + "." + stacktrace.getMethodName() + "(" + stacktrace.getFileName() + ":" + lineNumber + ") ";
                            sb.append(error);
                        }
                    }
                }
            }
            logger.info("When trying to validate password, exception calling authSvc.authenticate: " + sb.toString());
            return null;
        }
    }

    public String getAffiliation(String shibIdp, Shib.DevShibAccountType devShibAccountType) {
        JsonArray emptyJsonArray = new JsonArray();
        String discoFeedJson = emptyJsonArray.toString();
        String discoFeedUrl;
        if (devShibAccountType.equals(Shib.DevShibAccountType.PRODUCTION)) {
            discoFeedUrl = systemConfig.getDataverseSiteUrl() + "/Shibboleth.sso/DiscoFeed";
        } else {
            String devUrl = "http://localhost:8080/resources/dev/sample-shib-identities.json";
            discoFeedUrl = devUrl;
        }
        logger.info("Trying to get affiliation from disco feed URL: " + discoFeedUrl);
        URL url = null;
        try {
            url = new URL(discoFeedUrl);
        } catch (MalformedURLException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (url == null) {
            logger.info("url object was null after parsing " + discoFeedUrl);
            return null;
        }
        HttpURLConnection discoFeedRequest = null;
        try {
            discoFeedRequest = (HttpURLConnection) url.openConnection();
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (discoFeedRequest == null) {
            logger.info("disco feed request was null");
            return null;
        }
        try {
            discoFeedRequest.connect();
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        JsonParser jp = new JsonParser();
        JsonElement root = null;
        try {
            root = jp.parse(new InputStreamReader((InputStream) discoFeedRequest.getInputStream()));
        } catch (IOException ex) {
            logger.info(ex.toString());
            return null;
        }
        if (root == null) {
            logger.info("root was null");
            return null;
        }
        JsonArray rootArray = root.getAsJsonArray();
        if (rootArray == null) {
            logger.info("Couldn't get JSON Array from URL");
            return null;
        }
        discoFeedJson = rootArray.toString();
        logger.fine("Dump of disco feed:" + discoFeedJson);
        String affiliation = ShibUtil.getDisplayNameFromDiscoFeed(shibIdp, discoFeedJson);
        if (affiliation != null) {
            return affiliation;
        } else {
            logger.info("Couldn't find an affiliation from  " + shibIdp);
            return null;
        }
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
        if (root == null) {
            String shortRandomString = UUID.randomUUID().toString().substring(0, 8);
            fakeUser.put("firstName", shortRandomString);
            fakeUser.put("lastName", shortRandomString);
            fakeUser.put("displayName", shortRandomString + " " + shortRandomString);
            fakeUser.put("email", shortRandomString + "@mailinator.com");
            fakeUser.put("idp", "https://idp." + shortRandomString + ".com/idp/shibboleth");
            fakeUser.put("username", shortRandomString);
            fakeUser.put("eppn", shortRandomString);
            return fakeUser;
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
