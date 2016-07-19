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
import static edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil.getRandomUserStatic;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
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
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
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
    @EJB
    SettingsServiceBean settingsService;

    /**
     * "Production" means "don't mess with the HTTP request".
     */
    public enum DevShibAccountType {

        PRODUCTION,
        RANDOM,
        TESTSHIB1,
        HARVARD1,
        HARVARD2,
        TWO_EMAILS,
        INVALID_EMAIL,
        EMAIL_WITH_LEADING_SPACE,
        UID_WITH_LEADING_SPACE,
        IDENTIFIER_WITH_LEADING_SPACE,
        MISSING_REQUIRED_ATTR,
    };

    public DevShibAccountType getDevShibAccountType() {
        DevShibAccountType saneDefault = DevShibAccountType.PRODUCTION;
        String settingReturned = settingsService.getValueForKey(SettingsServiceBean.Key.DebugShibAccountType);
        logger.fine("setting returned: " + settingReturned);
        if (settingReturned != null) {
            try {
                DevShibAccountType parsedValue = DevShibAccountType.valueOf(settingReturned);
                return parsedValue;
            } catch (IllegalArgumentException ex) {
                logger.info("Couldn't parse value: " + ex + " - returning a sane default: " + saneDefault);
                return saneDefault;
            }
        } else {
            logger.fine("Shibboleth dev mode has not been configured. Returning a sane default: " + saneDefault);
            return saneDefault;
        }
    }

    /**
     * This method exists so developers don't have to run Shibboleth locally.
     * You can populate the request with Shibboleth attributes by changing a
     * setting like this:
     *
     * curl -X PUT -d RANDOM
     * http://localhost:8080/api/admin/settings/:DebugShibAccountType
     *
     * When you're done, feel free to delete the setting:
     *
     * curl -X DELETE
     * http://localhost:8080/api/admin/settings/:DebugShibAccountType
     *
     * Note that setting ShibUseHeaders to true will make this "dev mode" stop
     * working.
     */
    public void possiblyMutateRequestInDev(HttpServletRequest request) {
        switch (getDevShibAccountType()) {
            case PRODUCTION:
                logger.fine("Request will not be mutated");
                break;

            case RANDOM:
                mutateRequestForDevRandom(request);
                break;

            case TESTSHIB1:
                ShibUtil.mutateRequestForDevConstantTestShib1(request);
                break;

            case HARVARD1:
                ShibUtil.mutateRequestForDevConstantHarvard1(request);
                break;

            case HARVARD2:
                ShibUtil.mutateRequestForDevConstantHarvard2(request);
                break;

            case TWO_EMAILS:
                ShibUtil.mutateRequestForDevConstantTwoEmails(request);
                break;

            case INVALID_EMAIL:
                ShibUtil.mutateRequestForDevConstantInvalidEmail(request);
                break;

            case EMAIL_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantEmailWithLeadingSpace(request);
                break;

            case UID_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantUidWithLeadingSpace(request);
                break;

            case IDENTIFIER_WITH_LEADING_SPACE:
                ShibUtil.mutateRequestForDevConstantIdentifierWithLeadingSpace(request);
                break;

            case MISSING_REQUIRED_ATTR:
                ShibUtil.mutateRequestForDevConstantMissingRequiredAttributes(request);
                break;

            default:
                logger.info("Should never reach here");
                break;
        }
    }

    public AuthenticatedUser findAuthUserByEmail(String emailToFind) {
        return authSvc.getAuthenticatedUserByEmail(emailToFind);
    }

    public BuiltinUser findBuiltInUserByAuthUserIdentifier(String authUserIdentifier) {
        return builtinUserService.findByUserName(authUserIdentifier);
    }

    public AuthenticatedUser canLogInAsBuiltinUser(String username, String password) {
        logger.fine("checking to see if " + username + " knows the password...");
        if (password == null) {
            logger.info("password was null");
            return null;
        }

        AuthenticationRequest authReq = new AuthenticationRequest();
        /**
         * @todo Should this really be coming from a bundle like this? Added
         * because that's what BuiltinAuthenticationProvider does.
         */
        authReq.putCredential(BundleUtil.getStringFromBundle("login.builtin.credential.usernameOrEmail"), username);
        authReq.putCredential(BundleUtil.getStringFromBundle("login.builtin.credential.password"), password);
        /**
         * @todo Should probably set IP address here.
         */
//        authReq.setIpAddress(session.getUser().getRequestMetadata().getIpAddress());

        String credentialsAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
        try {
            AuthenticatedUser au = authSvc.authenticate(credentialsAuthProviderId, authReq);
            logger.fine("User authenticated:" + au.getEmail());
            return au;
        } catch (AuthenticationFailedException ex) {
            logger.info("The username and/or password entered is invalid: " + ex.getResponse().getMessage());
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

    public String getAffiliation(String shibIdp, DevShibAccountType devShibAccountType) {
        JsonArray emptyJsonArray = new JsonArray();
        String discoFeedJson = emptyJsonArray.toString();
        String discoFeedUrl;
        if (devShibAccountType.equals(DevShibAccountType.PRODUCTION)) {
            discoFeedUrl = systemConfig.getDataverseSiteUrl() + "/Shibboleth.sso/DiscoFeed";
        } else {
            String devUrl = "http://localhost:8080/resources/dev/sample-shib-identities.json";
            discoFeedUrl = devUrl;
        }
        logger.fine("Trying to get affiliation from disco feed URL: " + discoFeedUrl);
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

    private void mutateRequestForDevRandom(HttpServletRequest request) {
        Map<String, String> randomUser = getRandomUser();
        request.setAttribute(ShibUtil.lastNameAttribute, randomUser.get("lastName"));
        request.setAttribute(ShibUtil.firstNameAttribute, randomUser.get("firstName"));
        request.setAttribute(ShibUtil.emailAttribute, randomUser.get("email"));
        request.setAttribute(ShibUtil.shibIdpAttribute, randomUser.get("idp"));
        // eppn
        request.setAttribute(ShibUtil.uniquePersistentIdentifier, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * For testing, don't expect this to work well.
     */
    public Map<String, String> getRandomUser() throws JsonSyntaxException, JsonIOException {
        Map<String, String> fakeUser = new HashMap<>();
        String sURL = "http://api.randomuser.me/0.8";
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
