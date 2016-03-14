package edu.harvard.iq.dataverse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.UserIdentifier;
import edu.harvard.iq.dataverse.authorization.UserRecordIdentifier;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.shib.ShibGroupServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUserNameFields;
import edu.harvard.iq.dataverse.authorization.providers.shib.ShibUtil;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

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
    @EJB
    SystemConfig systemConfig;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    GroupServiceBean groupService;

    HttpServletRequest request;

    List<String> shibValues = new ArrayList<>();
    /**
     * @todo make this configurable? See
     * https://github.com/IQSS/dataverse/issues/2129
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
    private String affiliationToDisplayAtConfirmation = null;
    /**
     * @todo Once we can persist "position" to the authenticateduser table, we
     * can revisit this. Maybe we'll use ORCID instead. Dunno.
     */
//    private String positionToPersist = "Position not provided by institution log in";
    /**
     * @todo localize this
     */
    private String friendlyNameForInstitution = "your institution";
    private State state;
    private String debugSummary;
    /**
     * After a successful login, we will redirect users to this page (unless
     * it's a new account).
     */
    private String redirectPage;
//    private boolean debug = false;
    private String emailAddress;
    private boolean useHeaders;
    private final String testShibIdpEntityId = "https://idp.testshib.org/idp/shibboleth";

    public enum State {

        INIT,
        REGULAR_LOGIN_INTO_EXISTING_SHIB_ACCOUNT,
        PROMPT_TO_CREATE_NEW_ACCOUNT,
        PROMPT_TO_CONVERT_EXISTING_ACCOUNT,
    };

    /**
     * These are attributes that were found to be interesting while developing
     * the Shibboleth feature. Only the ones that are defined elsewhere are
     * actually used.
     */
    List<String> shibAttrs = Arrays.asList(
            shibIdpAttribute,
            uniquePersistentIdentifier,
            usernameAttribute,
            displayNameAttribute,
            firstNameAttribute,
            lastNameAttribute,
            emailAttribute,
            "telephoneNumber",
            "affiliation",
            "unscoped-affiliation",
            "entitlement",
            "persistent-id"
    );

    public void init() {
        state = State.INIT;
        /**
         * @todo For security reasons, Dataverse shouldn't even support the
         * possibility of using headers. All the related code including
         * SettingsServiceBean.Key.ShibUseHeaders should be removed. See all the
         * scary warnings quoted from official Shib docs in
         * https://github.com/IQSS/dataverse/issues/2294
         */
        useHeaders = systemConfig.isShibUseHeaders();
        if (useHeaders) {
            printHeaders();
        }
        ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
        request = (HttpServletRequest) context.getRequest();
        printAttributes(request);

        /**
         * @todo Investigate why JkEnvVar is null since it may be useful for
         * debugging per https://github.com/IQSS/dataverse/issues/2916 . See
         * also
         * http://stackoverflow.com/questions/30193117/iterate-through-all-servletrequest-attributes#comment49933342_30193117
         * and
         * http://shibboleth.1660669.n2.nabble.com/Why-doesn-t-Java-s-request-getAttributeNames-show-Shibboleth-attributes-tp7616427p7616591.html
         */
        logger.fine("JkEnvVar: " + System.getenv("JkEnvVar"));

        possiblyMutateRequestInDev();

        try {
            shibIdp = getRequiredValueFromAssertion(shibIdpAttribute);
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
            shibUserIdentifier = getRequiredValueFromAssertion(uniquePersistentIdentifier);
        } catch (Exception ex) {
            return;
        }
        String firstName;
        try {
            firstName = getRequiredValueFromAssertion(firstNameAttribute);
        } catch (Exception ex) {
            return;
        }
        String lastName;
        try {
            lastName = getRequiredValueFromAssertion(lastNameAttribute);
        } catch (Exception ex) {
            return;
        }
        ShibUserNameFields shibUserNameFields = ShibUtil.findBestFirstAndLastName(firstName, lastName, null);
        if (shibUserNameFields != null) {
            String betterFirstName = shibUserNameFields.getFirstName();
            if (betterFirstName != null) {
                firstName = betterFirstName;
            }
            String betterLastName = shibUserNameFields.getLastName();
            if (betterLastName != null) {
                lastName = betterLastName;
            }
        }
        String emailAddressInAssertion = null;
        try {
            emailAddressInAssertion = getRequiredValueFromAssertion(emailAttribute);
        } catch (Exception ex) {
            if (shibIdp.equals(testShibIdpEntityId)) {
                logger.info("For " + testShibIdpEntityId + " (which as of this writing doesn't provide the " + emailAttribute + " attribute) setting email address to value of eppn: " + shibUserIdentifier);
                emailAddressInAssertion = shibUserIdentifier;
            } else {
                // forcing all other IdPs to send us an an email
                return;
            }
        }

        if (!EMailValidator.isEmailValid(emailAddressInAssertion, null)) {
            String msg = "The SAML assertion contained an invalid email address: \"" + emailAddressInAssertion + "\".";
            logger.info(msg);
            String singleEmailAddress = ShibUtil.findSingleValue(emailAddressInAssertion);
            if (EMailValidator.isEmailValid(singleEmailAddress, null)) {
                msg = "Multiple email addresses were asserted by the Identity Provider (" + emailAddressInAssertion + " ). These were sorted and the first was chosen: " + singleEmailAddress;
                logger.info(msg);
                emailAddress = singleEmailAddress;
            } else {
                msg += " A single valid address could not be found.";
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, identityProviderProblem, msg));
                return;
            }
        } else {
            emailAddress = emailAddressInAssertion;
        }

        String usernameAssertion = getValueFromAssertion(usernameAttribute);
        internalUserIdentifer = ShibUtil.generateFriendlyLookingUserIdentifer(usernameAssertion, emailAddress);
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
        /**
         * @todo Shouldn't we persist the displayName too? It still exists on
         * the authenticateduser table.
         */
//        String displayName = getDisplayName(displayNameAttribute, firstNameAttribute, lastNameAttribute);
        String affiliation = getAffiliation();
//        emailAddress = "willFailBeanValidation"; // for testing createAuthenticatedUser exceptions
        displayInfo = new AuthenticatedUserDisplayInfo(firstName, lastName, emailAddress, affiliation, null);

        userPersistentId = shibIdp + persistentUserIdSeparator + shibUserIdentifier;
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        AuthenticatedUser au = authSvc.lookupUser(shibAuthProvider.getId(), userPersistentId);
        if (au != null) {
            state = State.REGULAR_LOGIN_INTO_EXISTING_SHIB_ACCOUNT;
            logger.info("Found user based on " + userPersistentId + ". Logging in.");
            logger.info("Updating display info for " + au.getName());
            authSvc.updateAuthenticatedUser(au, displayInfo);
            logInUserAndSetShibAttributes(au);
            String prettyFacesHomePageString = getPrettyFacesHomePageString(false);
            try {
                FacesContext.getCurrentInstance().getExternalContext().redirect(prettyFacesHomePageString);
            } catch (IOException ex) {
                logger.info("Unable to redirect user to homepage at " + prettyFacesHomePageString);
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
        logger.info("Debug summary: " + debugSummary + " (state: " + state + ").");
        logger.fine("redirectPage: " + redirectPage);
    }

    /**
     * @todo Move this to the shib service bean.
     */
    private String getAffiliation() {
        JsonArray emptyJsonArray = new JsonArray();
        String discoFeedJson = emptyJsonArray.toString();
        String discoFeedUrl;
        if (getDevShibAccountType().equals(DevShibAccountType.PRODUCTION)) {
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
            affiliationToDisplayAtConfirmation = affiliation;
            friendlyNameForInstitution = affiliation;
            return affiliation;
        } else {
            logger.info("Couldn't find an affiliation from  " + shibIdp);
            return null;
        }
    }

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
        MISSING_REQUIRED_ATTR,
    };

    private DevShibAccountType getDevShibAccountType() {
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
    private void possiblyMutateRequestInDev() {
        switch (getDevShibAccountType()) {
            case PRODUCTION:
                logger.fine("Request will not be mutated");
                break;

            case RANDOM:
                mutateRequestForDevRandom();
                break;

            case TESTSHIB1:
                mutateRequestForDevConstantTestShib1();
                break;

            case HARVARD1:
                mutateRequestForDevConstantHarvard1();
                break;

            case HARVARD2:
                mutateRequestForDevConstantHarvard2();
                break;

            case TWO_EMAILS:
                mutateRequestForDevConstantTwoEmails();
                break;

            case INVALID_EMAIL:
                mutateRequestForDevConstantInvalidEmail();
                break;

            case MISSING_REQUIRED_ATTR:
                mutateRequestForDevConstantMissingRequiredAttributes();
                break;

            default:
                logger.info("Should never reach here");
                break;
        }
    }

    private void printHeaders() {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = (String) headerNames.nextElement();
            logger.info(headerName + " = " + request.getHeader(headerName));
        }
    }

    public String confirmAndCreateAccount() {
        ShibAuthenticationProvider shibAuthProvider = new ShibAuthenticationProvider();
        String lookupStringPerAuthProvider = userPersistentId;
        AuthenticatedUser au = null;
        try {
            au = authSvc.createAuthenticatedUser(
                    new UserRecordIdentifier(shibAuthProvider.getId(), lookupStringPerAuthProvider), internalUserIdentifer, displayInfo, true);
        } catch (EJBException ex) {
            /**
             * @todo Show the ConstraintViolationException, if any.
             */
            logger.info("Couldn't create user " + userPersistentId + " due to exception: " + ex.getCause());
        }
        if (au != null) {
            logger.info("created user " + au.getIdentifier());
            logInUserAndSetShibAttributes(au);
        } else {
            JsfHelper.addErrorMessage("Couldn't create user.");
        }
        return getPrettyFacesHomePageString(true);
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
                return "/dataverseuser.xhtml?selectTab=accountInfo&faces-redirect=true";
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
        logger.info("Groups for user " + au.getId() + " (" + au.getIdentifier() + "): " + getGroups(au));
    }

    /**
     * @todo After merging the latest from develop (before making a pull
     * request) consider removing the equivalent method from
     * DataverseHeaderFragment since we're debugging groups here. Related:
     * https://github.com/IQSS/dataverse/issues/105
     */
    public List<String> getGroups(AuthenticatedUser au) {
        List<String> groups = new ArrayList<>();
        groupService.groupsFor(au, null).stream().forEach((group) -> {
            groups.add(group.getDisplayName() + " (" + group.getIdentifier() + ")");
        });
        return groups;
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

    /**
     * These are the attributes we are getting from the IdP at testshib.org, a
     * dump from https://pdurbin.pagekite.me/Shibboleth.sso/Session
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
     *
     */
    private void printAttributes(HttpServletRequest request) {
        if (request == null) {
            logger.info("HttpServletRequest was null. No shib values to print.");
            return;
        }
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
    private String getValueFromAssertion(String key) {
        Object attributeOrHeader = getAttributeOrHeader(key);
        if (attributeOrHeader != null) {
            String attributeValue = attributeOrHeader.toString();
            logger.info("The SAML assertion for \"" + key + "\" (optional) was \"" + attributeValue + "\".");
            if (!attributeValue.isEmpty()) {
                return attributeValue;
            }
        }
        logger.info("The SAML assertion for \"" + key + "\" (optional) was null.");
        return null;
    }

    private String getRequiredValueFromAssertion(String key) throws Exception {
        Object attributeOrHeader = getAttributeOrHeader(key);
        if (attributeOrHeader == null) {
            String msg = "The SAML assertion for \"" + key + "\" was null. Please contact support.";
            logger.info(msg);
            boolean showMessage = true;
            if (shibIdp.equals(testShibIdpEntityId) && key.equals(emailAttribute)) {
                showMessage = false;
            }
            if (showMessage) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, identityProviderProblem, msg));
            }
            throw new Exception(msg);
        }
        String attributeValue = attributeOrHeader.toString();
        if (attributeValue.isEmpty()) {
            throw new Exception(key + " was empty");
        }
        logger.info("The SAML assertion for \"" + key + "\" (required) was \"" + attributeValue + "\".");
        return attributeValue;
    }

    private Object getAttributeOrHeader(String attribute) {
        /**
         * @todo Should the prefix be configurable?
         */
        String prefix = "ajp_";
        Object attributeOrHeader;
        if (useHeaders) {
            attributeOrHeader = request.getHeader(prefix + attribute);
        } else {
            attributeOrHeader = request.getAttribute(attribute);
        }
        return attributeOrHeader;
    }

    public String getRootDataverseAlias() {
        Dataverse rootDataverse = dataverseService.findRootDataverse();
        if (rootDataverse != null) {
            String rootDvAlias = rootDataverse.getAlias();
            if (rootDvAlias != null) {
                return rootDvAlias;
            }
        }
        return null;
    }

    /**
     * @param includeFacetDashRedirect if true, include "faces-redirect=true" in
     * the string
     *
     * @todo Once https://github.com/IQSS/dataverse/issues/1519 is done, revisit
     * this method and have the home page be "/" rather than "/dataverses/root".
     *
     * @todo Like builtin users, Shibboleth should benefit from redirectPage
     * logic per https://github.com/IQSS/dataverse/issues/1551
     */
    public String getPrettyFacesHomePageString(boolean includeFacetDashRedirect) {
        if (redirectPage != null) {
            return redirectPage;
        }
        String plainHomepageString = "/dataverse.xhtml";
        String rootDvAlias = getRootDataverseAlias();
        if (includeFacetDashRedirect) {
            if (rootDvAlias != null) {
                return plainHomepageString + "?alias=" + rootDvAlias + "&faces-redirect=true";
            } else {
                return plainHomepageString + "?faces-redirect=true";
            }
        } else if (rootDvAlias != null) {
            /**
             * @todo Is there a constant for "/dataverse/" anywhere? I guess
             * we'll just hard-code it here.
             */
            return "/dataverse/" + rootDvAlias;
        } else {
            return plainHomepageString;
        }
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

    public String getAffiliationToDisplayAtConfirmation() {
        return affiliationToDisplayAtConfirmation;
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

    public String getRedirectPage() {
        return redirectPage;
    }

    public void setRedirectPage(String redirectPage) {
        this.redirectPage = redirectPage;
    }

    private void mutateRequestForDevRandom() {
        Map<String, String> randomUser = shibService.getRandomUser();
        request.setAttribute(lastNameAttribute, randomUser.get("lastName"));
        request.setAttribute(firstNameAttribute, randomUser.get("firstName"));
        request.setAttribute(emailAttribute, randomUser.get("email"));
        request.setAttribute(shibIdpAttribute, randomUser.get("idp"));
        // eppn
        request.setAttribute(uniquePersistentIdentifier, UUID.randomUUID().toString().substring(0, 8));
    }

    private void mutateRequestForDevConstantTestShib1() {
        request.setAttribute(shibIdpAttribute, testShibIdpEntityId);
        // the TestShib "eppn" looks like an email address
        request.setAttribute(uniquePersistentIdentifier, "saml@testshib.org");
//        request.setAttribute(displayNameAttribute, "Sam El");
        request.setAttribute(firstNameAttribute, "Samuel;Sam");
        request.setAttribute(lastNameAttribute, "El");
        // TestShib doesn't send "mail" attribute so let's mimic that.
//        request.setAttribute(emailAttribute, "saml@mailinator.com");
        request.setAttribute(usernameAttribute, "saml");
    }

    private void mutateRequestForDevConstantHarvard1() {
        /**
         * Harvard's IdP doesn't send a username (uid).
         */
        request.setAttribute(shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantHarvard");
        /**
         * @todo Does Harvard really send displayName? At one point they didn't.
         * Let's simulate the non-sending of displayName here.
         */
//        request.setAttribute(displayNameAttribute, "John Harvard");
        request.setAttribute(firstNameAttribute, "John");
        request.setAttribute(lastNameAttribute, "Harvard");
        request.setAttribute(emailAttribute, "jharvard@mailinator.com");
        request.setAttribute(usernameAttribute, "jharvard");
    }

    private void mutateRequestForDevConstantHarvard2() {
        request.setAttribute(shibIdpAttribute, "https://fed.huit.harvard.edu/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "constantHarvard2");
//        request.setAttribute(displayNameAttribute, "Grace Hopper");
        request.setAttribute(firstNameAttribute, "Grace");
        request.setAttribute(lastNameAttribute, "Hopper");
        request.setAttribute(emailAttribute, "ghopper@mailinator.com");
        request.setAttribute(usernameAttribute, "ghopper");
    }

    private void mutateRequestForDevConstantTwoEmails() {
        request.setAttribute(shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "twoEmails");
        request.setAttribute(firstNameAttribute, "Eric");
        request.setAttribute(lastNameAttribute, "Allman");
        request.setAttribute(emailAttribute, "eric1@mailinator.com;eric2@mailinator.com");
        request.setAttribute(usernameAttribute, "eallman");
    }

    private void mutateRequestForDevConstantInvalidEmail() {
        request.setAttribute(shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        request.setAttribute(uniquePersistentIdentifier, "invalidEmail");
        request.setAttribute(firstNameAttribute, "Invalid");
        request.setAttribute(lastNameAttribute, "Email");
        request.setAttribute(emailAttribute, "invalidEmail");
        request.setAttribute(usernameAttribute, "invalidEmail");

    }

    private void mutateRequestForDevConstantMissingRequiredAttributes() {
        request.setAttribute(shibIdpAttribute, "https://fake.example.com/idp/shibboleth");
        /**
         * @todo When shibIdpAttribute is set to null why don't we see the error
         * in the GUI?
         */
//        request.setAttribute(shibIdpAttribute, null);
        request.setAttribute(uniquePersistentIdentifier, "missing");
        request.setAttribute(uniquePersistentIdentifier, null);
        request.setAttribute(firstNameAttribute, "Missing");
        request.setAttribute(lastNameAttribute, "Required");
        request.setAttribute(emailAttribute, "missing@mailinator.com");
        request.setAttribute(usernameAttribute, "missing");
    }

}
