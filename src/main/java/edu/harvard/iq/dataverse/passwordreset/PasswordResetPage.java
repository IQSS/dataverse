package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.SettingsWrapper;
import edu.harvard.iq.dataverse.ValidateEmail;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.SystemConfig;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import edu.harvard.iq.dataverse.validation.PasswordValidatorServiceBean;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.NotBlank;

@ViewScoped
@Named("PasswordResetPage")
public class PasswordResetPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(PasswordResetPage.class.getCanonicalName());

    @EJB
    PasswordResetServiceBean passwordResetService;
    @EJB
    BuiltinUserServiceBean dataverseUserService;
    @EJB
    DataverseServiceBean dataverseService;    
    @EJB
    AuthenticationServiceBean authSvc;
    @Inject
    DataverseSession session;
    @Inject
    SettingsWrapper settingsWrapper; 
    
    @EJB
    ActionLogServiceBean actionLogSvc;

    @EJB
    PasswordValidatorServiceBean passwordValidatorService;
    
    @EJB
    SystemConfig systemConfig;

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
            if (passwordResetData != null) {
                user = passwordResetData.getBuiltinUser();
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, 
                        BundleUtil.getStringFromBundle("passwdVal.passwdReset.resetLinkTitle"),
                        BundleUtil.getStringFromBundle("passwdVal.passwdReset.resetLinkDesc")));
            }
        }
    }

    public String sendPasswordResetLink() {
            
        actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "passwordResetRequest")
                            .setInfo("Email Address: " + emailAddress) );
        try {
            PasswordResetInitResponse passwordResetInitResponse = passwordResetService.requestReset(emailAddress);
            PasswordResetData passwordResetData = passwordResetInitResponse.getPasswordResetData();
            if (passwordResetData != null) {
                BuiltinUser foundUser = passwordResetData.getBuiltinUser();
                passwordResetUrl = passwordResetInitResponse.getResetUrl(systemConfig.getDataverseSiteUrl());
                actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "passwordResetSent")
                            .setInfo("Email Address: " + emailAddress) );
            } else {
                logger.log(Level.INFO, "Cannot find account (or it's deactivated) given {0}", emailAddress);
            }
            /**
             * We show this "an email will be sent" message no matter what (if
             * the account can be found or not, if the account has been
             * deactivated or not) to prevent hackers from figuring out if you
             * have an account based on your email address. Yes, this is a white
             * lie sometimes, in the name of security.
             */
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, BundleUtil.getStringFromBundle("passwdVal.passwdReset.resetInitiated"), 
                    BundleUtil.getStringFromBundle("passwdReset.successSubmit.tip", Arrays.asList(emailAddress))));
        } catch (PasswordResetException ex) {
            /**
             * @todo do we really need a special exception for this??
             */
            logger.log(Level.WARNING, "Error While resetting password: " + ex.getMessage(), ex);
        }
        return "";
    }

    public String resetPassword() {
        PasswordChangeAttemptResponse response = passwordResetService.attemptPasswordReset(user, newPassword, this.token);
        if (response.isChanged()) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, response.getMessageSummary(), response.getMessageDetail()));
            String builtinAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
            AuthenticatedUser au = authSvc.lookupUser(builtinAuthProviderId, user.getUserName());
            session.setUser(au);
            return "/dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias() + "faces-redirect=true";
        } else {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, response.getMessageSummary(), response.getMessageDetail()));
            return null;
        }
    }

    //Note: Ported from DataverseUserPage
    public void validateNewPassword(FacesContext context, UIComponent toValidate, Object value) {
        String password = (String) value;
        if (StringUtils.isBlank(password)){
            logger.log(Level.WARNING, BundleUtil.getStringFromBundle("passwdVal.passwdReset.valBlankLog"));

            ((UIInput) toValidate).setValid(false);

            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    BundleUtil.getStringFromBundle("passwdVal.passwdReset.valFacesError"),
                    BundleUtil.getStringFromBundle("passwdVal.passwdReset.valFacesErrorDesc"));
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

    public PasswordResetData getPasswordResetData() {
        return passwordResetData;
    }

    public void setPasswordResetData(PasswordResetData passwordResetData) {
        this.passwordResetData = passwordResetData;
    }

    public String getGoodPasswordDescription() {
        // FIXME: Pass the errors in.
        return passwordValidatorService.getGoodPasswordDescription(null);
    }
    
    public String getCustomPasswordResetAlertMessage() {
        String customPasswordResetAlertMessage = settingsWrapper.getValueForKey(SettingsServiceBean.Key.PVCustomPasswordResetAlertMessage);
        if(customPasswordResetAlertMessage != null && !customPasswordResetAlertMessage.isEmpty()){
            return customPasswordResetAlertMessage;
        } else {
            String defaultPasswordResetAlertMessage = BundleUtil.getStringFromBundle("passwdReset.newPasswd.details");
            return defaultPasswordResetAlertMessage;
        }
    }
}
