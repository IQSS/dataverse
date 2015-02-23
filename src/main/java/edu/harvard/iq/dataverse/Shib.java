package edu.harvard.iq.dataverse;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.RoleAssigneeDisplayInfo;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
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
    ShibServiceBean shibService;
    @EJB
    ShibGroupServiceBean shibGroupService;
    @EJB
    SettingsServiceBean settingsService;

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
     *
     * This was used early on in development and should be removed at some
     * point.
     */
    @Deprecated
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
    /**
     * @todo Make attribute used (i.e. "eppn") configurable:
     * https://github.com/IQSS/dataverse/issues/1422
     *
     * OR *maybe* we can rely on people installing Dataverse to configure shibd
     * to always send "eppn" as an attribute, via attribute mappings or what
     * have you.
     */
    private final String uniquePersistentIdentifier = "eppn";
    private String userPersistentId;
    private String internalUserIdentifer;
    private final String usernameAttribute = "uid";
    private final String displayNameAttribute = "cn";
    private final String firstNameAttribute = "givenName";
    private final String lastNameAttribute = "sn";
    private final String emailAttribute = "mail";
    AuthenticatedUserDisplayInfo displayInfo;
    /**
     * @todo Remove this boolean some day? Now the mockups show a popup. Should
     * be re-worked. See also the comment about the lack of a Cancel button.
     */
    private boolean visibleTermsOfUse;
    private final String homepage = "/dataverse.xhtml";
    private final String loginpage = "/loginpage.xhtml";
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
    private String builtinUsername;
    private String builtinPassword;
    private String existingEmail;
    private String existingDisplayName;
    private boolean passwordRejected;
    private String displayNameToPersist = "(Blank: display name not received from Institution Log In)";
//    private String firstNameToPersist = "(Blank: first name not received from Institution Log In)";
//    private String lastNameToPersist = "(Blank: last name not received from Institution Log In)";
    private String emailToPersist = "(Blank: email received from Institution Log In)";
    /**
     * @todo We're not really doing anything with affiliation yet, even though
     * the mockups show it. The plan is to parse the JSON from
     * https://dataverse.harvard.edu/Shibboleth.sso/DiscoFeed for example. Check
     * the "ShibUtil" class
     */
    private String affiliationToPersist = "Affiliation not provided by institution log in";
    /**
     * @todo Once we can persist "position" to the authenticateduser table, we
     * can revisit this. Maybe we'll use ORCID instead. Dunno.
     */
//    private String positionToPersist = "Position not provided by institution log in";
    private String friendlyNameForInstitution = "your institution";
    private State state;
    private String debugSummary;
//    private boolean debug = false;
    private String emailAddress;

    public enum State {

        INIT,
        REGULAR_LOGIN_INTO_EXISTING_SHIB_ACCOUNT,
        PROMPT_TO_CREATE_NEW_ACCOUNT,
        PROMPT_TO_CONVERT_EXISTING_ACCOUNT,
    };

    public void init() {
        state = State.INIT;
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        request = (HttpServletRequest) context.getRequest();

        // set one of these to true in dev to avoid needing Shibboleth set up locally
        boolean devRandom = false;
        boolean devConstantTestShib = false;
        boolean devConstantHarvard1 = false;
        boolean devConstantHarvard2 = false;
        if (devRandom) {
            mutateRequestForDevRandom();
        }
        if (devConstantTestShib) {
            mutateRequestForDevConstantTestShib();
        }
        if (devConstantHarvard1) {
            mutateRequestForDevConstantHarvard1();
        }
        if (devConstantHarvard2) {
            mutateRequestForDevConstantHarvard2();
        }

        try {
            shibIdp = getRequiredValueFromAttribute(shibIdpAttribute);
        } catch (Exception ex) {
            /**
             * @todo is in an antipattern to throw exceptions to control flow?
             * http://c2.com/cgi/wiki?DontUseExceptionsForFlowControl
             *
             * All this exception handling should be handled in the new
             * ShibServiceBean so it's consistently handled by the API as well.
             */
            return;
        }
        String shibUserIdentifier;
        try {
            shibUserIdentifier = getRequiredValueFromAttribute(uniquePersistentIdentifier);
        } catch (Exception ex) {
            return;
        }
        try {
            emailAddress = getRequiredValueFromAttribute(emailAttribute);
        } catch (Exception ex) {
            String shipIdp = "https://idp.testshib.org/idp/shibboleth";
            if (shibIdp.equals(shipIdp)) {
                logger.info("For " + shipIdp + " setting email address to value of eppn: " + shibUserIdentifier);
                emailAddress = shibUserIdentifier;
            } else {
                // forcing all other IdPs to send us an an email
                return;
            }
        }
        internalUserIdentifer = generateFriendlyLookingUserIdentifer(usernameAttribute, emailAttribute);
        logger.info("friendly looking identifer (backend will enforce uniqueness):" + internalUserIdentifer);

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
         * @todo Update affiliation with "Harvard University". This is not
         * commonly sent as an attribute in the Shibboleth world but we might
         * need to parse something like
         * https://dataverse-demo.iq.harvard.edu/Shibboleth.sso/DiscoFeed
         */
        /**
         * @todo Add position and review firstname, lastname
         */
        String affiliation = "FIXME";
        displayInfo = new AuthenticatedUserDisplayInfo(firstNameAttribute, lastNameAttribute, emailAddress, affiliation, null);

        userPersistentId = shibIdp + persistentUserIdSeparator + shibUserIdentifier;
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        AuthenticatedUser au = authSvc.lookupUser(shibAuthProvider.getId(), userPersistentId);
        if (au != null) {
            state = State.REGULAR_LOGIN_INTO_EXISTING_SHIB_ACCOUNT;
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
            state = State.PROMPT_TO_CREATE_NEW_ACCOUNT;
            displayNameToPersist = displayInfo.getTitle();
//            firstNameToPersist = "foo";
//            lastNameToPersist = "bar";
            emailToPersist = emailAddress;
            /**
             * @todo For Harvard at least, we plan to use "Harvard University"
             * for affiliation because it's what we get from
             * https://dataverse.harvard.edu/Shibboleth.sso/DiscoFeed
             */
//            affiliationToPersist = "FIXME";
            /**
             * @todo for Harvard we plan to use the value(s) from
             * eduPersonScopedAffiliation which
             * http://iam.harvard.edu/resources/saml-shibboleth-attributes says
             * can be One or more of the following values: faculty, staff,
             * student, affiliate, and member.
             *
             * http://dataverse.nl plans to use
             * urn:mace:dir:attribute-def:eduPersonAffiliation per
             * http://irclog.iq.harvard.edu/dataverse/2015-02-13#i_16265 . Can
             * they configure shibd to map eduPersonAffiliation to
             * eduPersonScopedAffiliation?
             */
//            positionToPersist = "FIXME";
            logger.info("Couldn't find authenticated user based on " + userPersistentId);
            visibleTermsOfUse = true;
            /**
             * Using the email address from the IdP, try to find an existing
             * user. For TestShib we convert the "eppn" to an email address.
             *
             * If found, prompt for password and offer to convert.
             *
             * If not found, create a new account. It must be a new user.
             */
            String emailAddressToLookUp = emailAddress;
            if (existingEmail != null) {
                emailAddressToLookUp = existingEmail;
            }
            AuthenticatedUser existingAuthUserFoundByEmail = shibService.findAuthUserByEmail(emailAddressToLookUp);
            BuiltinUser existingBuiltInUserFoundByEmail = null;
            if (existingAuthUserFoundByEmail != null) {
                existingDisplayName = existingAuthUserFoundByEmail.getName();
                existingBuiltInUserFoundByEmail = shibService.findBuiltInUserByAuthUserIdentifier(existingAuthUserFoundByEmail.getUserIdentifier());
                if (existingBuiltInUserFoundByEmail != null) {
                    state = State.PROMPT_TO_CONVERT_EXISTING_ACCOUNT;
                    existingDisplayName = existingBuiltInUserFoundByEmail.getDisplayName();
                    debugSummary = "getting username from the builtin user we looked up via email";
                    builtinUsername = existingBuiltInUserFoundByEmail.getUserName();
                } else {
                    debugSummary = "Could not find a builtin account based on the username. Here we should simply create a new Shibboleth user";
                }
            } else {
                debugSummary = "Could not find an auth user based on email address";
            }

        }
//        if (debug) {
//            printAttributes(request);
//        }
    }

    public String confirmAndCreateAccount() {
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        String lookupStringPerAuthProvider = userPersistentId;
        UserIdentifier userIdentifier = new UserIdentifier(lookupStringPerAuthProvider, internalUserIdentifer);
        AuthenticatedUser au = authSvc.createAuthenticatedUserWithDecoupledIdentifiers(shibAuthProvider.getId(), userIdentifier, displayInfo);
        if (au != null) {
            logger.info("created user " + au.getIdentifier());
        } else {
            logger.info("couldn't create user " + userPersistentId);
        }
        logInUserAndSetShibAttributes(au);
        return homepage + "?faces-redirect=true";
    }

    public String confirmAndConvertAccount() {
        visibleTermsOfUse = false;
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        String lookupStringPerAuthProvider = userPersistentId;
        UserIdentifier userIdentifier = new UserIdentifier(lookupStringPerAuthProvider, internalUserIdentifer);
        logger.info("builtin username: " + builtinUsername);
        AuthenticatedUser builtInUserToConvert = shibService.canLogInAsBuiltinUser(builtinUsername, builtinPassword);
        if (builtInUserToConvert != null) {
            AuthenticatedUser au = authSvc.convertBuiltInToShib(builtInUserToConvert, shibAuthProvider.getId(), userIdentifier);
            if (au != null) {
                authSvc.updateAuthenticatedUser(au, displayInfo);
                logInUserAndSetShibAttributes(au);
                debugSummary = "Local account validated and successfully converted to a Shibboleth account. The old account username was " + builtinUsername;
                JsfHelper.addSuccessMessage("Your Dataverse account is now associated with your institutional account.");
                return homepage + "?faces-redirect=true";
            } else {
                debugSummary = "Local account validated but unable to convert to Shibboleth account.";
            }
        } else {
            passwordRejected = true;
            debugSummary = "Username/password combination for local account was invalid";
        }
        return null;
    }

    private void logInUserAndSetShibAttributes(AuthenticatedUser au) {
        au.setShibIdentityProvider(shibIdp);
        session.setUser(au);
    }

    /**
     * @todo The mockups show a Cancel button but because we're using the
     * "requiredCheckboxValidator" you are forced to agree to Terms of Use
     * before clicking Cancel! Argh! The mockups show how we want to display
     * Terms of Use in a popup anyway so this should all be re-done. No time
     * now. Here's the mockup:
     * https://iqssharvard.mybalsamiq.com/projects/loginwithshibboleth-version3-dataverse40/Dataverse%20Account%20III%20-%20Agree%20Terms%20of%20Use
     */
    public String cancel() {
        return loginpage + "?faces-redirect=true";
    }

    public List<String> getShibValues() {
        return shibValues;
    }

//    private void printAttributes(HttpServletRequest request) {
//        for (String attr : shibAttrs) {
//
//            /**
//             * @todo explain in Installers Guide that in order for these
//             * attributes to be found attributePrefix="AJP_" must be added to
//             * /etc/shibboleth/shibboleth2.xml like this:
//             *
//             * <ApplicationDefaults entityID="https://dataverse.org/shibboleth"
//             * REMOTE_USER="eppn" attributePrefix="AJP_">
//             *
//             */
//            Object attrObject = request.getAttribute(attr);
//            if (attrObject != null) {
//                shibValues.add(attr + ": " + attrObject.toString());
//            }
//        }
//        logger.info("shib values: " + shibValues);
//    }
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
            String msg = " the attribute \"" + attribute + "\" was null. Please contact support.";
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
     * @todo Move logic to ShibServiceBean
     */
    private String generateFriendlyLookingUserIdentifer(String usernameNameAttribute, String emailAttribute) {
        Object usernameObject = request.getAttribute(usernameNameAttribute);
        if (usernameObject != null) {
            String userIdentifier = usernameObject.toString();
            if (!userIdentifier.isEmpty()) {
                return userIdentifier;
            }
        } else {
            logger.info("username attribute not sent by IdP");
        }
        Object emailObject = request.getAttribute(emailAttribute);
        if (emailObject != null) {
            String email = emailObject.toString();
            if (!email.isEmpty()) {
                /**
                 * @todo Just grab the first part of the email
                 */
                String[] parts = email.split("@");
                try {
                    String firstPart = parts[0];
                    return firstPart;
                } catch (ArrayIndexOutOfBoundsException ex) {
                    logger.info("odd email address. no @ sign: " + email);
                }
            }
        } else {
            logger.info("email attribute not sent by IdP");
        }
        logger.info("the best we can do is generate a random UUID");
        return UUID.randomUUID().toString();
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
        /**
         * @todo Should the first name attribute be required?
         */
        String firstName = getValueFromAttribute(firstNameAttribute);
        /**
         * @todo Should the last name attribute be required?
         */
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

    public boolean isDebug() {
        boolean safeDefaultIfKeyNotFound = false;
        return settingsService.isTrueForKey(SettingsServiceBean.Key.Debug, safeDefaultIfKeyNotFound);
    }

    public boolean isInit() {
        return state.equals(State.INIT);
    }

    public boolean isOfferToCreateNewAccount() {
        return state.equals(State.PROMPT_TO_CREATE_NEW_ACCOUNT);
    }

    public boolean isOfferToConvertExistingAccount() {
        return state.equals(State.PROMPT_TO_CONVERT_EXISTING_ACCOUNT);
    }

//    curl -X PUT -d@/tmp/apptos.txt http://localhost:8080/api/s/settings/:ApplicationTermsOfUse
    public String getApplicationTermsOfUse() {
        String saneDefaultForAppTermsOfUse = "There are no Terms of Use for this Dataverse installation.";
        String appTermsOfUse = settingsService.getValueForKey(SettingsServiceBean.Key.ApplicationTermsOfUse, saneDefaultForAppTermsOfUse);
        return appTermsOfUse;
    }

    public String getDisplayNameToPersist() {
        return displayNameToPersist;
    }

//    public String getFirstNameToPersist() {
//        return firstNameToPersist;
//    }
//    public String getLastNameToPersist() {
//        return lastNameToPersist;
//    }
    public String getEmailToPersist() {
        return emailToPersist;
    }

    public String getAffiliationToPersist() {
        return affiliationToPersist;
    }

//    public String getPositionToPersist() {
//        return positionToPersist;
//    }
    public String getExistingEmail() {
        return existingEmail;
    }

    public void setExistingEmail(String existingEmail) {
        this.existingEmail = existingEmail;
    }

    public String getExistingDisplayName() {
        return existingDisplayName;
    }

    public boolean isPasswordRejected() {
        return passwordRejected;
    }

    public String getFriendlyNameForInstitution() {
        return friendlyNameForInstitution;
    }

    public void setFriendlyNameForInstitution(String friendlyNameForInstitution) {
        this.friendlyNameForInstitution = friendlyNameForInstitution;
    }

    public State getState() {
        return state;
    }

    public boolean isVisibleTermsOfUse() {
        return visibleTermsOfUse;
    }

    public String getBuiltinUsername() {
        return builtinUsername;
    }

    public void setBuiltinUsername(String builtinUsername) {
        this.builtinUsername = builtinUsername;
    }

    public String getBuiltinPassword() {
        return builtinPassword;
    }

    public void setBuiltinPassword(String builtinPassword) {
        this.builtinPassword = builtinPassword;
    }

    public String getDebugSummary() {
        return debugSummary;
    }

    public void setDebugSummary(String debugSummary) {
        this.debugSummary = debugSummary;
    }

    private void mutateRequestForDevRandom() throws JsonSyntaxException, JsonIOException {
        // set *something*, at least, even if it's just shortened UUIDs
//        for (String attr : shibAttrs) {
        // in dev we don't care if a new, random user is created each time
//            request.setAttribute(attr, UUID.randomUUID().toString().substring(0, 8));
//        }

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
        JsonElement username = user.getAsJsonObject().get("username");
        JsonElement email = user.getAsJsonObject().get("email");
        JsonElement password = user.getAsJsonObject().get("password");
        JsonElement name = user.getAsJsonObject().get("name");
        JsonElement firstName = name.getAsJsonObject().get("first");
        JsonElement lastName = name.getAsJsonObject().get("last");
        /**
         * @todo Does Harvard really send displayName? At one point they didn't.
         * Let's simulate the non-sending of displayName here.
         */
//        request.setAttribute(displayNameAttribute, StringUtils.capitalise(firstName.getAsString()) + " " + StringUtils.capitalise(lastName.getAsString()));
        request.setAttribute(lastNameAttribute, StringUtils.capitalise(lastName.getAsString()));
        request.setAttribute(firstNameAttribute, StringUtils.capitalise(firstName.getAsString()));
        request.setAttribute(emailAttribute, email.getAsString());
        // random IDP
        request.setAttribute(shibIdpAttribute, "https://idp." + password.getAsString() + ".com/idp/shibboleth");
        /**
         * Harvard's IdP doesn't send a username so let's test without it by
         * commenting it out here.
         */
//        request.setAttribute(usernameAttribute, username.getAsString());
        // eppn
        request.setAttribute(uniquePersistentIdentifier, UUID.randomUUID().toString().substring(0, 8));
    }

    private void mutateRequestForDevConstantTestShib() {
        request.setAttribute(shibIdpAttribute, "https://idp.testshib.org/idp/shibboleth");
        // the TestShib "eppn" looks like an email address
        request.setAttribute(uniquePersistentIdentifier, "constant@testshib.org");
        request.setAttribute(displayNameAttribute, "Sam El");
        // TestShib doesn't send "mail" attribute so let's mimic that.
//        request.setAttribute(emailAttribute, "saml@mailinator.com");
        request.setAttribute(usernameAttribute, "saml");
    }

    private void mutateRequestForDevConstantHarvard1() {
        request.setAttribute(shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantHarvard");
        request.setAttribute(displayNameAttribute, "John Harvard");
        request.setAttribute(emailAttribute, "jharvard@mailinator.com");
        request.setAttribute(usernameAttribute, "jharvard");
    }

    private void mutateRequestForDevConstantHarvard2() {
        request.setAttribute(shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantHarvard2");
        request.setAttribute(displayNameAttribute, "Grace Hopper");
        request.setAttribute(emailAttribute, "ghopper@mailinator.com");
        request.setAttribute(usernameAttribute, "ghopper");
    }

}
