package edu.harvard.iq.dataverse.authorization.providers.common;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthTestDataServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.common.ExternalIdpUserRecord;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.DevOAuthAccountType;
import edu.harvard.iq.dataverse.authorization.providers.oauth2.OAuth2TokenDataServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.saml.SamlAuthenticationServlet;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.consent.ConsentDto;
import edu.harvard.iq.dataverse.consent.ConsentService;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.config.EMailValidator;
import edu.harvard.iq.dataverse.persistence.config.ValidateEmail;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.OAuth2TokenData;
import edu.harvard.iq.dataverse.settings.InstallationConfigService;
import edu.harvard.iq.dataverse.settings.SettingsWrapper;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import io.vavr.control.Option;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Backing bean for the page that greets new users
 * when they first login through OAuth2 or SAML.
 *
 * @author michael
 */
@Named("ExternalIdpFirstLoginPage")
@SessionScoped
public class ExternalIdpFirstLoginPage implements Serializable {

    private static final Logger logger = Logger.getLogger(ExternalIdpFirstLoginPage.class.getCanonicalName());

    @EJB
    AuthenticationServiceBean authenticationSvc;

    @EJB
    BuiltinUserServiceBean builtinUserSvc;

    @EJB
    UserNotificationService userNotificationService;

    @EJB
    SystemConfig systemConfig;

    @EJB
    AuthTestDataServiceBean authTestDataSvc;

    @EJB
    OAuth2TokenDataServiceBean oauth2Tokens;

    @EJB
    InstallationConfigService installationConfigService;

    @Inject
    DataverseSession session;

    @Inject
    private SettingsWrapper settingsWrapper;

    @Inject
    private ConsentService consentService;


    ExternalIdpUserRecord newUser;

    @NotBlank(message = "{oauth.username}")
    String username;

    @NotBlank(message = "{user.invalidEmail}")
    @ValidateEmail(message = "{user.invalidEmail}")
    String selectedEmail;

    String password;

    String installationName;

    private List<ConsentDto> consents = new ArrayList<>();

    boolean authenticationFailed = false;

    private AuthenticationProvider authProvider;

    private PasswordValidatorServiceBean passwordValidatorService;

    private Locale preferredNotificationsLanguage;

    private Boolean notificationLanguageSelectionEnabled;

    // -------------------- GETTERS --------------------

    public AuthenticationProvider getAuthProvider() {
        return authProvider;
    }

    public List<ConsentDto> getConsents() {
        return consents;
    }

    public ExternalIdpUserRecord getNewUser() {
        return newUser;
    }

    public String getPassword() {
        return password;
    }

    public String getSelectedEmail() {
        return selectedEmail;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAuthenticationFailed() {
        return authenticationFailed;
    }

    public Boolean getNotificationLanguageSelectionEnabled() {
        return notificationLanguageSelectionEnabled;
    }

    // -------------------- LOGIC --------------------

    /**
     * Attempts to init the page. Redirects the user to {@code /} in case the
     * initialization fails.
     *
     * @throws IOException If the redirection fails to be sent. Should not
     *                     happen* <p> <p> <p> * Famous last sentences etc.
     */
    public String init() throws IOException {
        notificationLanguageSelectionEnabled = settingsWrapper.isLocalesConfigured();
        if (systemConfig.isReadonlyMode()) {
            return "/403.xhtml";
        }
        DevOAuthAccountType devMode = systemConfig.getDevOAuthAccountType();
        logger.log(Level.FINEST, () -> "devMode: " + devMode);

        if (!DevOAuthAccountType.PRODUCTION.equals(devMode)) {
            createRandomAuthentication(devMode);
        }
        if (newUser != null && session.getUser().isAuthenticated()) {
            // Do not show this page if user is already registered
            // and the newUser object is somehow cached in viewscope
            return redirectToHome();
        }
        if (newUser == null) {
            // If new user tries to sign up with SAML then
            // the user data is stored in the http session
            HttpSession httpSession = (HttpSession) FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getSession(false);
            if (httpSession != null) {
                newUser = (ExternalIdpUserRecord) httpSession.getAttribute(SamlAuthenticationServlet.NEW_USER_SESSION_PARAM);
                httpSession.removeAttribute(SamlAuthenticationServlet.NEW_USER_SESSION_PARAM);
            }
        }
        if (newUser == null) {
            // There's no new user to welcome, so we're out of the "normal" OAuth2 flow.
            // e.g., someone might have directly accessed this page.
            // return to sanity be redirection to /index
            return redirectToHome();
        }

        // Suggest the best email we can.
        String emailFromDisplayInfo = newUser.getDisplayInfo().getEmailAddress();

        String emailToSuggest = StringUtils.isNotBlank(emailFromDisplayInfo)
                ? emailFromDisplayInfo
                : Optional.ofNullable(newUser.getAvailableEmailAddresses())
                    .orElse(Collections.emptyList()).stream()
                    .findFirst()
                    .orElse(null);

        setSelectedEmail(emailToSuggest);

        if (!notificationLanguageSelectionEnabled) {
            preferredNotificationsLanguage = Locale.forLanguageTag(getSupportedLanguages().get(0));
        }
        authProvider = authenticationSvc.getAuthenticationProvider(newUser.getServiceId());
        installationName = installationConfigService.getNameOfInstallation();
        consents = consentService.prepareConsentsForView(session.getLocale());

        return StringUtils.EMPTY;
    }

    public String createNewAccount() {
        AuthenticatedUserDisplayInfo displayInfo = newUser.getDisplayInfo();
        AuthenticatedUserDisplayInfo newAuthenticatedUserDisplayInfo = new AuthenticatedUserDisplayInfo(
                displayInfo.getFirstName(), displayInfo.getLastName(), getSelectedEmail(), displayInfo.getAffiliation(),
                displayInfo.getPosition());

        final AuthenticatedUser user = authenticationSvc.createAuthenticatedUser(newUser.toUserRecordIdentifier(),
                getUsername(), newAuthenticatedUserDisplayInfo, true, preferredNotificationsLanguage).getOrNull();

        session.setUser(user);
        userNotificationService.sendNotificationWithEmail(user, new Timestamp(new Date().getTime()), NotificationType.CREATEACC,
                null, NotificationObjectType.AUTHENTICATED_USER);
        consentService.executeActionsAndSaveAcceptedConsents(consents, user);
        final OAuth2TokenData tokenData = newUser.getTokenData();
        if (tokenData != null) {
            tokenData.setUser(user);
            tokenData.setOauthProviderId(newUser.getServiceId());
            oauth2Tokens.store(tokenData);
        }
        return "/dataverse.xhtml?faces-redirect=true";
    }

    public String convertExistingAccount() {
        BuiltinAuthenticationProvider builtinAuthenticationProvider =
                new BuiltinAuthenticationProvider(builtinUserSvc, passwordValidatorService, authenticationSvc);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        final List<CredentialsAuthenticationProvider.Credential> creds = builtinAuthenticationProvider.getRequiredCredentials();
        authenticationRequest.putCredential(creds.get(0).getKey(), getUsername());
        authenticationRequest.putCredential(creds.get(1).getKey(), getPassword());

        try {
            AuthenticatedUser existingUser = authenticationSvc.getUpdateAuthenticatedUser(
                    BuiltinAuthenticationProvider.PROVIDER_ID, authenticationRequest);
            authenticationSvc.updateProvider(existingUser, newUser.getServiceId(), newUser.getIdInService());
            builtinUserSvc.removeUser(existingUser.getUserIdentifier());

            session.setUser(existingUser);
            AuthenticationProvider newUserAuthProvider = authenticationSvc.getAuthenticationProvider(newUser.getServiceId());
            JsfHelper.addFlashSuccessMessage(
                    BundleUtil.getStringFromBundle("oauth2.convertAccount.success", newUserAuthProvider.getInfo().getTitle()));
            return "/dataverse.xhtml?faces-redirect=true";
        } catch (AuthenticationFailedException ex) {
            setAuthenticationFailed(true);
            return null;
        }
    }

    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        logger.log(Level.FINEST, () -> "Validating username: " + userName);
        boolean userNameFound = authenticationSvc.identifierExists(userName);
        if (userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("user.username.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void validateUserEmail(FacesContext context, UIComponent toValidate, Object value) {
        String userEmail = (String) value;
        boolean emailValid = EMailValidator.isEmailValid(userEmail, null);
        if (!emailValid) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("external.newAccount.emailInvalid"), null);
            context.addMessage(toValidate.getClientId(context), message);
            return;
        }
        boolean userEmailFound = false;
        AuthenticatedUser authenticatedUser = authenticationSvc.getAuthenticatedUserByEmail(userEmail);
        if (authenticatedUser != null) {
            userEmailFound = true;
        }
        if (userEmailFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("user.email.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public String getWelcomeMessage() {
        return BundleUtil.getStringFromBundle("external.newAccount.welcome");
    }

    public void setNewUser(ExternalIdpUserRecord newUser) {
        this.newUser = newUser;
        // uncomment to suggest username to user
        // setUsername(newUser.getUsername());
        setSelectedEmail(newUser.getDisplayInfo().getEmailAddress());
    }

    public String getCreateFromWhereTip() {
        if (authProvider == null) {
            return "Unknown identity provider. Are you a developer playing with :DebugOAuthAccountType? " +
                    "Try adding this provider to the authenticationproviderrow table: " + newUser.getServiceId();
        }
        return BundleUtil.getStringFromBundle("external.newAccount.explanation", authProvider.getInfo().getTitle(), installationName);
    }

    public boolean isConvertFromBuiltinIsPossible() {
        return authenticationSvc.getAuthenticationProvider(BuiltinAuthenticationProvider.PROVIDER_ID) != null;
    }

    public String getSuggestConvertInsteadOfCreate() {
        return BundleUtil.getStringFromBundle("external.newAccount.suggestConvertInsteadOfCreate", installationName);
    }

    public String getConvertTip() {
        return authProvider == null
                ? StringUtils.EMPTY
                : BundleUtil.getStringFromBundle(
                        "oauth2.convertAccount.explanation", installationName, authProvider.getInfo().getTitle(),
                        systemConfig.getGuidesBaseUrl(preferredNotificationsLanguage), systemConfig.getGuidesVersion());
    }

    public List<String> getEmailsToPickFrom() {
        List<String> emailsToPickFrom = new ArrayList<>();
        if (selectedEmail != null) {
            emailsToPickFrom.add(selectedEmail);
        }
        List<String> extraEmails = Optional.ofNullable(newUser.getAvailableEmailAddresses())
                .orElse(Collections.emptyList()).stream()
                .filter(e -> StringUtils.isNotBlank(e) && !e.equals(selectedEmail))
                .collect(Collectors.toList());
        emailsToPickFrom.addAll(extraEmails);
        logger.log(Level.FINEST, () -> emailsToPickFrom.size() + " emails to pick from: " + emailsToPickFrom);
        return emailsToPickFrom;
    }

    public List<String> getSupportedLanguages() {
        return new ArrayList<>(settingsWrapper.getConfiguredLocales().keySet());
    }

    public String getPreferredNotificationsLanguage() {
        return Option.of(preferredNotificationsLanguage)
                .map(Locale::getLanguage)
                .getOrNull();
    }

    public String getLocalizedDisplayNameForLanguage(String language) {
        return getLocalizedDisplayNameForLanguage(Locale.forLanguageTag(language));
    }

    public void validatePreferredNotificationsLanguage(FacesContext context, UIComponent toValidate, Object value) {
        if (Objects.isNull(value)) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.notificationsLanguage.requiredMessage"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    // -------------------- PRIVATE ---------------------

    private String redirectToHome() throws IOException {
        FacesContext.getCurrentInstance().getExternalContext().redirect("/");
        return StringUtils.EMPTY;
    }

    private String getLocalizedDisplayNameForLanguage(Locale language) {
        return language.getDisplayName(session.getLocale());
    }

    private void createRandomAuthentication(DevOAuthAccountType devMode) {
        if (devMode.toString().startsWith("RANDOM")) {
            Map<String, String> randomUser = authTestDataSvc.getRandomUser();
            String lastName = randomUser.get("lastName");
            String firstName = randomUser.get("firstName");
            String email = null;
            List<String> extraEmails = null;
            String authProviderId = "orcid";
            switch (devMode) {
                case RANDOM_EMAIL0:
                    authProviderId = "github";
                    break;
                case RANDOM_EMAIL1:
                    firstName = null;
                    lastName = null;
                    authProviderId = "google";
                    email = randomUser.get("email");
                    break;
                case RANDOM_EMAIL2:
                    firstName = null;
                    email = randomUser.get("email");
                    extraEmails = new ArrayList<>();
                    extraEmails.add("extra1@example.com");
                    break;
                case RANDOM_EMAIL3:
                    lastName = null;
                    email = randomUser.get("email");
                    extraEmails = new ArrayList<>();
                    extraEmails.add("extra1@example.com");
                    extraEmails.add("extra2@example.com");
                    break;
                default:
                    break;
            }
            String randomUsername = randomUser.get("username");
            String eppn = randomUser.get("eppn");
            OAuth2TokenData accessToken = new OAuth2TokenData();
            accessToken.setAccessToken("qwe-addssd-iiiiie");
            setNewUser(new ExternalIdpUserRecord(authProviderId, eppn, randomUsername, accessToken,
                    new AuthenticatedUserDisplayInfo(firstName, lastName, email, "myAffiliation", "myPosition"),
                    extraEmails));
        }
    }

    // -------------------- SETTERS --------------------

    public void setAuthenticationFailed(boolean authenticationFailed) {
        this.authenticationFailed = authenticationFailed;
    }

    public void setConsents(List<ConsentDto> consents) {
        this.consents = consents;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setPreferredNotificationsLanguage(String preferredNotificationsLanguage) {
        this.preferredNotificationsLanguage = Locale.forLanguageTag(preferredNotificationsLanguage);
    }

    public void setSelectedEmail(String selectedEmail) {
        this.selectedEmail = selectedEmail;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
