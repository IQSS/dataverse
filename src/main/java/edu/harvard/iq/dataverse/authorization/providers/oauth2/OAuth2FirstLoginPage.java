package edu.harvard.iq.dataverse.authorization.providers.oauth2;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.ValidateEmail;
import edu.harvard.iq.dataverse.authorization.AuthTestDataServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationRequest;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.CredentialsAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.exceptions.AuthenticationFailedException;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.inject.Named;
import javax.inject.Inject;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Backing bean for {@code oauth/welcome.xhtml}, the page that greets new users
 * when they first login through OAuth2.
 *
 * @author michael
 */
@Named("OAuth2FirstLoginPage")
@SessionScoped
public class OAuth2FirstLoginPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(OAuth2FirstLoginPage.class.getCanonicalName());

    @EJB
    AuthenticationServiceBean authenticationSvc;

    @EJB
    BuiltinUserServiceBean builtinUserSvc;

    @EJB
    SystemConfig systemConfig;

    @EJB
    AuthTestDataServiceBean authTestDataSvc;

    @Inject
    DataverseSession session;

    OAuth2UserRecord newUser;

    @NotBlank(message = "Please enter your username.")
    String username;

    @NotBlank(message = "Please enter a valid email address.")
    @ValidateEmail(message = "Please enter a valid email address.")
    String selectedEmail;

    String password;

    boolean authenticationFailed = false;

    /**
     * Attempts to init the page. Redirects the user to {@code /} in case
     * the initialization fails.
     * @throws IOException If the redirection fails to be sent. Should not happen*
     * 
     * 
     * 
     * * Famous last sentences etc.
     */
    public void init() throws IOException {
        logger.fine("init called");
        
        if ( newUser == null ) {
            // There's no new user to welcome, so we're out of the "normal" OAuth2 flow.
            // e.g., someone might have directly accessed this page.
            // return to sanity be redirection to /index
            FacesContext.getCurrentInstance().getExternalContext().redirect("/");
            return;
        }
        
        /**
         * @todo Add something like SettingsServiceBean.Key.DebugShibAccountType
         */
        boolean devMode = false;
        if (devMode) {
            Map<String, String> randomUser = authTestDataSvc.getRandomUser();
            String lastName = randomUser.get("lastName");
            String firstName = randomUser.get("firstName");
            String email = randomUser.get("email");
            String randomUsername = randomUser.get("username");
            String eppn = randomUser.get("eppn");
            String accessToken = "qwe-addssd-iiiiie";
            setNewUser(new OAuth2UserRecord("github", eppn, randomUsername, accessToken,
                    new AuthenticatedUserDisplayInfo(firstName, lastName, email, "myAffiliation", "myPosition"),
                    Arrays.asList("extra1@example.com", "extra2@example.com", "extra3@example.com")));
        }
    }

    public String createNewAccount() {

        AuthenticatedUserDisplayInfo newAud = new AuthenticatedUserDisplayInfo(newUser.getDisplayInfo().getFirstName(),
                newUser.getDisplayInfo().getLastName(),
                getSelectedEmail(),
                newUser.getDisplayInfo().getAffiliation(),
                newUser.getDisplayInfo().getPosition());
        final AuthenticatedUser user = authenticationSvc.createAuthenticatedUser(newUser.getUserRecordIdentifier(), getUsername(), newAud, true);
        session.setUser(user);

        return "/dataverse.xhtml?faces-redirect=true";
    }

    public String convertExistingAccount() {
        BuiltinAuthenticationProvider biap = new BuiltinAuthenticationProvider(builtinUserSvc);
        AuthenticationRequest auReq = new AuthenticationRequest();
        final List<CredentialsAuthenticationProvider.Credential> creds = biap.getRequiredCredentials();
        auReq.putCredential(creds.get(0).getTitle(), getUsername());
        auReq.putCredential(creds.get(1).getTitle(), getPassword());
        try {
            AuthenticatedUser existingUser = authenticationSvc.authenticate(BuiltinAuthenticationProvider.PROVIDER_ID, auReq);
            authenticationSvc.updateProvider(existingUser, newUser.getServiceId(), newUser.getIdInService());
            builtinUserSvc.removeUser(existingUser.getUserIdentifier());

            session.setUser(existingUser);
            AuthenticationProvider authProvider = authenticationSvc.getAuthenticationProvider(newUser.getServiceId());
            JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("oauth2.convertAccount.success", Arrays.asList(authProvider.getInfo().getTitle())));

            return "/dataverse.xhtml?faces-redirect=true";

        } catch (AuthenticationFailedException ex) {
            setAuthenticationFailed(true);
            return null;
        }
    }

    public String testAction() {
        Logger.getLogger(OAuth2FirstLoginPage.class.getName()).log(Level.INFO, "testAction");
        return "dataverse.xhtml";
    }

    public boolean isEmailAvailable() {
        return authenticationSvc.isEmailAddressAvailable(getSelectedEmail());
    }

    /**
     * @todo This was copied from DataverseUserPage and modified so consider
     * consolidating common code (DRY).
     */
    public void validateUserName(FacesContext context, UIComponent toValidate, Object value) {
        String userName = (String) value;
        logger.fine("Validating username: " + userName);
        boolean userNameFound = authenticationSvc.identifierExists(userName);
        if (userNameFound) {
            ((UIInput) toValidate).setValid(false);
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, BundleUtil.getStringFromBundle("user.username.taken"), null);
            context.addMessage(toValidate.getClientId(context), message);
        }
    }

    public void suggestedEmailChanged(ValueChangeEvent evt) {
        setSelectedEmail(evt.getNewValue().toString());
    }

    /**
     * @return A textual reference to the user.
     */
    public String getUserReference() {
        AuthenticatedUserDisplayInfo udi = newUser.getDisplayInfo();
        return (udi.getFirstName() + " " + udi.getLastName()).trim();
    }

    public OAuth2UserRecord getNewUser() {
        return newUser;
    }

    public void setNewUser(OAuth2UserRecord newUser) {
        this.newUser = newUser;
        // uncomment to suggest username to user
        //setUsername(newUser.getUsername());
        setSelectedEmail(newUser.getDisplayInfo().getEmailAddress());
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setSelectedEmail(String selectedEmail) {
        this.selectedEmail = selectedEmail;
    }

    public String getSelectedEmail() {
        return selectedEmail;
    }

    public boolean areExtraEmailsAvailable() {
        return newUser.getAvailableEmailAddresses().size() > 1;
    }

    public List<String> getExtraEmails() {
        return newUser.getAvailableEmailAddresses();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAuthenticationFailed() {
        return authenticationFailed;
    }

    public void setAuthenticationFailed(boolean authenticationFailed) {
        this.authenticationFailed = authenticationFailed;
    }

    public String getCreateFromWhereTip() {
        AbstractOAuth2AuthenticationProvider authProvider = authenticationSvc.getOAuth2Provider(newUser.getServiceId());
        return BundleUtil.getStringFromBundle("oauth2.newAccount.explanation", Arrays.asList(authProvider.getTitle(), systemConfig.getNameOfInstallation()));
    }

    public String getSuggestConvertInsteadOfCreate() {
        return BundleUtil.getStringFromBundle("oauth2.newAccount.suggestConvertInsteadOfCreate", Arrays.asList(systemConfig.getNameOfInstallation()));
    }

    public String getConvertTip() {
        AbstractOAuth2AuthenticationProvider authProvider = authenticationSvc.getOAuth2Provider(newUser.getServiceId());
        return BundleUtil.getStringFromBundle("oauth2.convertAccount.explanation", Arrays.asList(systemConfig.getNameOfInstallation(), authProvider.getTitle()));
    }

}
