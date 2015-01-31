package edu.harvard.iq.dataverse;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;

@ViewScoped
@Named("Shib")
public class Shib implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(Shib.class.getCanonicalName());

    @Inject
    DataverseSession session;

    @EJB
    AuthenticationServiceBean authSvc;

    @EJB
    ShibGroupServiceBean shibGroupService;

    HttpServletRequest request;

    /**
     * @todo these are the attributes we are getting from the IdP at
     * testshib.org. What other attributes should we expect?
     *
     * Here is a dump from https://pdurbin.pagekite.me/Shibboleth.sso/Session
     *
     * Miscellaneous
     *
     * Session Expiration (barring inactivity): 479 minute(s)
     *
     * Client Address: 10.0.2.2
     *
     * SSO Protocol: urn:oasis:names:tc:SAML:2.0:protocol
     *
     * Identity Provider: https://idp.testshib.org/idp/shibboleth
     *
     * Authentication Time: 2014-09-12T17:07:36.137Z
     *
     * Authentication Context Class:
     * urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport
     *
     * Authentication Context Decl: (none)
     *
     *
     *
     * Attributes
     *
     * affiliation: Member@testshib.org;Staff@testshib.org
     *
     * cn: Me Myself And I
     *
     * entitlement: urn:mace:dir:entitlement:common-lib-terms
     *
     * eppn: myself@testshib.org
     *
     * givenName: Me Myself
     *
     * persistent-id:
     * https://idp.testshib.org/idp/shibboleth!https://pdurbin.pagekite.me/shibboleth!zylzL+NruovU5OOGXDOL576jxfo=
     *
     * sn: And I
     *
     * telephoneNumber: 555-5555
     *
     * uid: myself
     *
     * unscoped-affiliation: Member;Staff
     */
    /**
     * @todo Resolve potential confusing of having attibutes like "eppn" defined
     * twice in this class.
     */
    List<String> shibAttrs = Arrays.asList(
            "Shib-Identity-Provider",
            "uid",
            "cn",
            "sn",
            "givenName",
            "telephoneNumber",
            "eppn",
            "affiliation",
            "unscoped-affiliation",
            "entitlement",
            "persistent-id"
    );

    List<String> shibValues = new ArrayList<>();
    /**
     * @todo make this configurable?
     */
    private final String shibIdpAttribute = "Shib-Identity-Provider";
    private final String uniquePersistentIdentifier = "eppn";
    private String userPersistentId;
    private final String displayNameAttribute = "cn";
    private final String firstNameAttribute = "givenName";
    private final String lastNameAttribute = "sn";
    private final String emailAttribute = "mail";
    RoleAssigneeDisplayInfo displayInfo;
    private boolean visibleTermsOfUse;
    private final String homepage = "/dataverse.xhtml";
    private final String identityProviderProblem = "Problem with Identity Provider";

    /**
     * We only have one field in which to store a unique
     * useridentifier/persistentuserid so we have to jam the the "entityId" for
     * a Shibboleth Identity Provider (IdP) and the unique persistent identifier
     * per user into the same field and a separator between these two would be
     * nice, in case we ever want to answer questions like "How many users
     * logged in from Harvard's Identity Provider?".
     *
     * A pipe ("|") is used as a separator because it's considered "unwise" to
     * use in a URL and the "entityId" for a Shibboleth Identity Provider (IdP)
     * looks like a URL:
     * http://stackoverflow.com/questions/1547899/which-characters-make-a-url-invalid
     */
    private String persistentUserIdSeparator = "|";

    /**
     * The Shibboleth Identity Provider (IdP), an "entityId" which often but not
     * always looks like a URL.
     */
    String shibIdp;

    private boolean debug = false;

    public void init() {
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        request = (HttpServletRequest) context.getRequest();

        // set one of these to true in dev to avoid needing Shibboleth set up locally
        boolean devRandom = false;
        boolean devConstantTestShib = false;
        boolean devConstantHarvard = false;
        if (devRandom) {
            mutateRequestForDevRandom();
        }
        if (devConstantTestShib) {
            mutateRequestForDevConstantTestShib();
        }
        if (devConstantHarvard) {
            mutateRequestForDevConstantHarvard();
        }

        try {
            shibIdp = getRequiredValueFromAttribute(shibIdpAttribute);
        } catch (Exception ex) {
            /**
             * @todo is in an antipattern to throw exceptions to control flow?
             * http://c2.com/cgi/wiki?DontUseExceptionsForFlowControl
             */
            return;
        }
        String userIdentifier;
        try {
            userIdentifier = getRequiredValueFromAttribute(uniquePersistentIdentifier);
        } catch (Exception ex) {
            return;
        }

        /**
         * @todo Remove, longer term. For now, commenting out special logic for
         * always showing Terms of Use for TestShib accounts. The Terms of Use
         * workflow is captured at
         * http://datascience.iq.harvard.edu/blog/try-out-single-sign-shibboleth-40-beta
         */
//        if (shibIdp.equals("https://idp.testshib.org/idp/shibboleth")) {
//            StringBuilder sb = new StringBuilder();
//            String freshNewShibUser = sb.append(userIdentifier).append(UUID.randomUUID()).toString();
//            logger.info("Will create a new, unique user so the account Terms of Use will be displayed.");
//            userIdentifier = freshNewShibUser;
//        }
        String displayName = getDisplayName(displayNameAttribute, firstNameAttribute, lastNameAttribute);
        /**
         * @todo is it ok if email address is null? What will blow up?
         */
        String emailAddress = getValueFromAttribute(emailAttribute);
        displayInfo = new RoleAssigneeDisplayInfo(displayName, emailAddress);

        userPersistentId = shibIdp + persistentUserIdSeparator + userIdentifier;
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        AuthenticatedUser au = authSvc.lookupUser(shibAuthProvider.getId(), userPersistentId);
        if (au != null) {
            logger.info("Found user based on " + userPersistentId + ". Logging in.");
            logger.info("Updating display info for " + au.getName());
            authSvc.updateAuthenticatedUser(au, displayInfo);
            logInUserAndSetShibAttributes(au);
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(homepage);
            } catch (IOException ex) {
                logger.info("Unable to redirect user to " + homepage);
            }
        } else {
            logger.info("Couldn't find authenticated user based on " + userPersistentId);
            visibleTermsOfUse = true;
        }
        if (debug) {
            printAttributes(request);
        }
    }

    public String confirm() {
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        AuthenticatedUser au = authSvc.createAuthenticatedUser(shibAuthProvider.getId(), userPersistentId, displayInfo);
        if (au != null) {
            logger.info("created user " + au.getIdentifier());
        } else {
            logger.info("couldn't create user " + userPersistentId);
        }
        logInUserAndSetShibAttributes(au);
        return homepage + "?faces-redirect=true";
    }

    private void logInUserAndSetShibAttributes(AuthenticatedUser au) {
        au.setShibIdentityProvider(shibIdp);
        session.setUser(au);
    }

    public List<String> getShibValues() {
        return shibValues;
    }

    private void printAttributes(HttpServletRequest request) {
        for (String attr : shibAttrs) {

            /**
             * @todo explain in Installers Guide that in order for these
             * attributes to be found attributePrefix="AJP_" must be added to
             * /etc/shibboleth/shibboleth2.xml like this:
             *
             * <ApplicationDefaults entityID="https://dataverse.org/shibboleth"
             * REMOTE_USER="eppn" attributePrefix="AJP_">
             *
             */
            Object attrObject = request.getAttribute(attr);
            if (attrObject != null) {
                shibValues.add(attr + ": " + attrObject.toString());
            }
        }
        logger.info("shib values: " + shibValues);
    }

    /**
     * @return The value of a Shib attribute (if non-empty) or null.
     */
    private String getValueFromAttribute(String attribute) {
        Object attributeObject = request.getAttribute(attribute);
        if (attributeObject != null) {
            String attributeValue = attributeObject.toString();
            if (!attributeValue.isEmpty()) {
                return attributeValue;
            }
        }
        return null;
    }

    private String getRequiredValueFromAttribute(String attribute) throws Exception {
        Object attributeObject = request.getAttribute(attribute);
        if (attributeObject == null) {
            String msg = attribute + " was null";
            logger.info(msg);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, identityProviderProblem, msg));
            throw new Exception(msg);
        }
        String attributeValue = attributeObject.toString();
        if (attributeValue.isEmpty()) {
            throw new Exception(attribute + " was empty");
        }
        return attributeValue;
    }

    /**
     * @return The best display name we can retrieve or construct based on
     * attributes received from Shibboleth. Shouldn't be null, maybe "Unknown"
     */
    private String getDisplayName(String displayNameAttribute, String firstNameAttribute, String lastNameAttribute) {
        Object displayNameObject = request.getAttribute(displayNameAttribute);
        if (displayNameObject != null) {
            String displayName = displayNameObject.toString();
            if (!displayName.isEmpty()) {
                return displayName;
            } else {
                return getDisplayNameFromFirstNameLastName(firstNameAttribute, lastNameAttribute);
            }
        } else {
            return getDisplayNameFromFirstNameLastName(firstNameAttribute, lastNameAttribute);
        }
    }

    /**
     * @return First name plus last name if available, just first name or just
     * last name or "Unknown".
     */
    private String getDisplayNameFromFirstNameLastName(String firstNameAttribute, String lastNameAttribute) {
        String firstName = getValueFromAttribute(firstNameAttribute);
        String lastName = getValueFromAttribute(lastNameAttribute);
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return "Unknown";
        }
    }

    public boolean isVisibleTermsOfUse() {
        return visibleTermsOfUse;
    }

    private void mutateRequestForDevRandom() throws JsonSyntaxException, JsonIOException {
        // set *something*, at least, even if it's just shortened UUIDs
        for (String attr : shibAttrs) {
            // in dev we don't care if a new, random user is created each time
            request.setAttribute(attr, UUID.randomUUID().toString().substring(0, 8));
        }

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

        JsonParser jp = new JsonParser(); //from gson
        JsonElement root = null;
        try {
            root = jp.parse(new InputStreamReader((InputStream) randomUserRequest.getContent())); //convert the input stream to a json element
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
        JsonElement email = user.getAsJsonObject().get("email");
        JsonElement password = user.getAsJsonObject().get("password");
        JsonElement name = user.getAsJsonObject().get("name");
        JsonElement firstName = name.getAsJsonObject().get("first");
        JsonElement lastName = name.getAsJsonObject().get("last");
        request.setAttribute(displayNameAttribute, StringUtils.capitalise(firstName.getAsString()) + " " + StringUtils.capitalise(lastName.getAsString()));
        request.setAttribute(emailAttribute, email.getAsString());
        // random IDP
        request.setAttribute(shibIdpAttribute, "https://idp." + password.getAsString() + ".com/idp/shibboleth");
    }

    private void mutateRequestForDevConstantTestShib() {
        request.setAttribute(shibIdpAttribute, "https://idp.testshib.org/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantTestShib");
        request.setAttribute(displayNameAttribute, "Sam El");
        request.setAttribute(emailAttribute, "saml@mailinator.com");
    }

    private void mutateRequestForDevConstantHarvard() {
        request.setAttribute(shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantHarvard");
        request.setAttribute(displayNameAttribute, "John Harvard");
        request.setAttribute(emailAttribute, "jharvard@mailinator.com");
    }

}
