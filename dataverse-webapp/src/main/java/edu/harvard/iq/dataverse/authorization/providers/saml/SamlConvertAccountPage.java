package edu.harvard.iq.dataverse.authorization.providers.saml;

import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.SamlLoginIssue;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.consent.ConsentDto;
import edu.harvard.iq.dataverse.consent.ConsentService;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserDisplayInfo;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.util.JsfHelper;
import edu.harvard.iq.dataverse.util.SystemConfig;
import org.apache.commons.lang3.StringUtils;
import org.omnifaces.cdi.ViewScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ViewScoped
@Named("SamlConvertAccount")
public class SamlConvertAccountPage implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(SamlConvertAccountPage.class);

    private SystemConfig systemConfig;
    private AuthenticationServiceBean authenticationService;
    private BuiltinUserServiceBean builtinUserService;
    private DataverseSession dataverseSession;
    private ConsentService consentService;

    private boolean passwordRejected = false;
    private String builtinUsername;
    private String builtinPassword;
    private SamlUserData userData;
    private List<ConsentDto> consents = new ArrayList<>();

    // -------------------- CONSTRUCTORS --------------------

    public SamlConvertAccountPage() { }

    @Inject
    public SamlConvertAccountPage(SystemConfig systemConfig, AuthenticationServiceBean authenticationService,
                                  BuiltinUserServiceBean builtinUserService, DataverseSession dataverseSession,
                                  ConsentService consentService) {
        this.systemConfig = systemConfig;
        this.authenticationService = authenticationService;
        this.builtinUserService = builtinUserService;
        this.dataverseSession = dataverseSession;
        this.consentService = consentService;
    }

    // -------------------- GETTERS --------------------

    public boolean isPasswordRejected() {
        return passwordRejected;
    }

    public String getBuiltinPassword() {
        return builtinPassword;
    }

    public String getEmail() {
        return userData.getEmail();
    }

    public List<ConsentDto> getConsents() {
        return consents;
    }

    // -------------------- LOGIC --------------------

    public String init() {
        SamlUserData samlUser = getSamlUserDataFromSession();
        if (!systemConfig.isSignupAllowed() || samlUser == null) {
            return "/403.xhtml";
        }
        userData = samlUser;
        if (!samlUser.isCompleteForLogin()) {
            HttpSession session = JsfHelper.getCurrentSession();
            session.setAttribute(SamlAuthenticationServlet.SAML_LOGIN_ISSUE_SESSION_PARAM,
                    new SamlLoginIssue(SamlLoginIssue.Type.INCOMPLETE_DATA).addMessage(samlUser.printLoginData()));
            return "/failedLogin.xhtml";
        }
        AuthenticatedUser existingUser = authenticationService.getAuthenticatedUserByEmail(samlUser.getEmail());
        BuiltinUser existingBuiltinUser = builtinUserService.findByUserName(existingUser.getUserIdentifier());
        builtinUsername = existingBuiltinUser.getUserName();
        consents = consentService.prepareConsentsForView(dataverseSession.getLocale());
        return StringUtils.EMPTY;
    }

    public String confirmAndConvertAccount() {
        logger.info("builtin username: " + builtinUsername);
        AuthenticatedUser builtInUserToConvert = authenticationService.canLogInAsBuiltinUser(builtinUsername, builtinPassword);
        if (builtInUserToConvert == null) {
            passwordRejected = true;
            logger.info("Username/password combination for local account was invalid");
            return StringUtils.EMPTY;
        }
        AuthenticatedUser authenticatedUser = authenticationService.convertBuiltInUserToRemoteUser(builtInUserToConvert,
                SamlAuthenticationProvider.PROVIDER_ID, userData.getCompositeId());
        if (authenticatedUser == null) {
            logger.info("Local account validated but unable to convert to Saml account.");
            JsfHelper.addFlashErrorMessage(BundleUtil.getStringFromBundle("saml.convert.failed"));
            return StringUtils.EMPTY;
        }
        authenticationService.updateAuthenticatedUser(authenticatedUser,
                new AuthenticatedUserDisplayInfo(userData.getName(), userData.getSurname(), userData.getEmail(), null, null));
        authenticatedUser.setSamlIdPEntityId(userData.getIdpEntityId());
        dataverseSession.setUser(authenticatedUser);
        consentService.executeActionsAndSaveAcceptedConsents(consents, authenticatedUser);
        logger.info("Local account validated and successfully converted to a Saml account. The old account username was "
                + builtinUsername);
        JsfHelper.addFlashSuccessMessage(BundleUtil.getStringFromBundle("saml.convert.success"));
        return "/dataverseuser.xhtml?selectTab=accountInfo&faces-redirect=true";
    }

    // -------------------- PRIVATE --------------------

    private SamlUserData getSamlUserDataFromSession() {
        HttpSession session = JsfHelper.getCurrentSession();
        SamlUserData samlUser = (SamlUserData) session.getAttribute(SamlAuthenticationServlet.USER_TO_CONVERT_SESSION_PARAM);
        session.removeAttribute(SamlAuthenticationServlet.USER_TO_CONVERT_SESSION_PARAM);
        return samlUser;
    }

    // -------------------- SETTERS --------------------

    public void setBuiltinPassword(String builtinPassword) {
        this.builtinPassword = builtinPassword;
    }

    public void setConsents(List<ConsentDto> consents) {
        this.consents = consents;
    }
}
