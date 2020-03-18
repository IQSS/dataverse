package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.DataverseDao;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.consent.ConsentDto;
import edu.harvard.iq.dataverse.consent.ConsentService;
import edu.harvard.iq.dataverse.persistence.ActionLogRecord;
import edu.harvard.iq.dataverse.persistence.config.ValidateEmail;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.BuiltinUser;
import edu.harvard.iq.dataverse.persistence.user.PasswordResetData;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import io.vavr.control.Option;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ViewScoped
@Named("PasswordResetPage")
public class PasswordResetPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(PasswordResetPage.class.getCanonicalName());

    @EJB
    PasswordResetServiceBean passwordResetService;
    @EJB
    DataverseDao dataverseDao;
    @EJB
    AuthenticationServiceBean authSvc;
    @Inject
    DataverseSession session;
    @EJB
    SettingsServiceBean settingsService;

    @EJB
    ActionLogServiceBean actionLogSvc;

    @EJB
    PasswordValidatorServiceBean passwordValidatorService;

    @Inject
    private ConsentService consentService;

    private List<ConsentDto> consents = new ArrayList<>();

    /**
     * The unique string used to look up a user and continue the password reset
     * process.
     */
    String token;

    /**
     * The user looked up by the token who will be setting a new password.
     */
    BuiltinUser user;

    /**
     * The email address that is entered to initiate the password reset process.
     */

    @NotBlank(message = "{user.invalidEmail}")
    @ValidateEmail(message = "{password.validate}")
    String emailAddress;

    /**
     * The link that is emailed to the user to reset the password that contains
     * a token.
     */
    String passwordResetUrl;

    /**
     * The new password the user enters.
     */
    String newPassword;

    PasswordResetData passwordResetData;

    private List<String> passwordErrors;

    public void init() {
        if (token != null) {
            PasswordResetExecResponse passwordResetExecResponse = passwordResetService.processToken(token);
            passwordResetData = passwordResetExecResponse.getPasswordResetData();

            Option.of(passwordResetData)
                    .peek(resetData -> user = resetData.getBuiltinUser())
                    .onEmpty(() -> FacesContext.getCurrentInstance()
                            .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                               BundleUtil.getStringFromBundle(
                                                                       "passwdVal.passwdReset.resetLinkTitle"),
                                                               BundleUtil.getStringFromBundle(
                                                                       "passwdVal.passwdReset.resetLinkDesc"))))
                    .filter(resetData -> resetData.getReason() == PasswordResetData.Reason.UPGRADE_REQUIRED)
                    .peek(resetData -> consents = consentService.prepareConsentsForView(session.getLocale()));
        }
    }

    public String sendPasswordResetLink() {

        actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "passwordResetRequest")
                                 .setInfo("Email Address: " + emailAddress));
        try {
            PasswordResetInitResponse passwordResetInitResponse = passwordResetService.requestReset(emailAddress);
            PasswordResetData passwordResetData = passwordResetInitResponse.getPasswordResetData();
            if (passwordResetData != null) {
                BuiltinUser foundUser = passwordResetData.getBuiltinUser();
                passwordResetUrl = passwordResetInitResponse.getResetUrl();
                actionLogSvc.log(new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "passwordResetSent")
                                         .setInfo("Email Address: " + emailAddress));
            } else {
                /**
                 * @todo remove "single" when it's no longer necessary. See
                 * https://github.com/IQSS/dataverse/issues/844 and
                 * https://github.com/IQSS/dataverse/issues/1141
                 */
                logger.log(Level.INFO, "Couldn''t find single account using {0}", emailAddress);
            }
            FacesContext.getCurrentInstance().addMessage(null,
                                                         new FacesMessage(FacesMessage.SEVERITY_INFO,
                                                                          BundleUtil.getStringFromBundle(
                                                                                  "passwdVal.passwdReset.resetInitiated"),
                                                                          ""));
        } catch (PasswordResetException ex) {
            /**
             * @todo do we really need a special exception for this??
             */
            logger.log(Level.WARNING, "Error While resetting password: " + ex.getMessage(), ex);
        }
        return "";
    }

    public String resetPassword() {
        PasswordChangeAttemptResponse response = passwordResetService.attemptPasswordReset(user,
                                                                                           newPassword,
                                                                                           this.token);
        if (response.isChanged()) {
            FacesContext.getCurrentInstance().addMessage(null,
                                                         new FacesMessage(FacesMessage.SEVERITY_INFO,
                                                                          response.getMessageSummary(),
                                                                          response.getMessageDetail()));
            String builtinAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
            AuthenticatedUser au = authSvc.lookupUser(builtinAuthProviderId, user.getUserName());
            session.setUser(au);
            consentService.executeActionsAndSaveAcceptedConsents(consents, au);
            return "/dataverse.xhtml?alias=" + dataverseDao.findRootDataverse().getAlias() + "faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                                                         new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                                          response.getMessageSummary(),
                                                                          response.getMessageDetail()));
            return null;
        }
    }

    //Note: Ported from DataverseUserPage
    public void validateNewPassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        if (StringUtils.isBlank(password)) {
            logger.log(Level.WARNING, BundleUtil.getStringFromBundle("passwdVal.passwdReset.valBlankLog"));

            ((UIInput) toValidate).setValid(false);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                                    BundleUtil.getStringFromBundle("passwdVal.passwdReset.valFacesError"),
                                                    BundleUtil.getStringFromBundle(
                                                            "passwdVal.passwdReset.valFacesErrorDesc"));
            context.addMessage(toValidate.getClientId(context), message);
            return;

        }

        final List<String> errors = passwordValidatorService.validate(password, new Date(), false);
        this.passwordErrors = errors;
        if (!errors.isEmpty()) {
            ((UIInput) toValidate).setValid(false);
        }
    }

    public String getPasswordRequirements() {
        return passwordValidatorService.getGoodPasswordDescription(passwordErrors);
    }

    public boolean isAccountUpgrade() {
        return passwordResetData.getReason() == PasswordResetData.Reason.UPGRADE_REQUIRED;
    }

    public boolean isPasswordCompliant() {
        return passwordResetData.getReason() == PasswordResetData.Reason.NON_COMPLIANT_PASSWORD;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public BuiltinUser getUser() {
        return user;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPasswordResetUrl() {
        return passwordResetUrl;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getCustomPasswordResetAlertMessage() {
        String customPasswordResetAlertMessage = settingsService.getValueForKey(SettingsServiceBean.Key.PVCustomPasswordResetAlertMessage);
        if (customPasswordResetAlertMessage != null && !customPasswordResetAlertMessage.isEmpty()) {
            return customPasswordResetAlertMessage;
        } else {
            String defaultPasswordResetAlertMessage = BundleUtil.getStringFromBundle("passwdReset.newPasswd.details");
            return defaultPasswordResetAlertMessage;
        }
    }

    public List<ConsentDto> getConsents() {
        return consents;
    }
}
