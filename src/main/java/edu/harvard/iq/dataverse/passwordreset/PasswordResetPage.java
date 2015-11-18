package edu.harvard.iq.dataverse.passwordreset;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.ValidateEmail;
import edu.harvard.iq.dataverse.actionlogging.ActionLogRecord;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUser;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinUserServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
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
    
    @EJB
    ActionLogServiceBean actionLogSvc;
    
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
    @NotBlank(message = "{passwordReset.notBlank}")
    @ValidateEmail(message = "{passwordReset.validateEmail}")    
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

    public void init() {
        if (token != null) {
            PasswordResetExecResponse passwordResetExecResponse = passwordResetService.processToken(token);
            passwordResetData = passwordResetExecResponse.getPasswordResetData();
            if (passwordResetData != null) {
                user = passwordResetData.getBuiltinUser();
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Password Reset Link", "Your password reset link is not valid."));
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
                passwordResetUrl = passwordResetInitResponse.getResetUrl();
                actionLogSvc.log( new ActionLogRecord(ActionLogRecord.ActionType.BuiltinUser, "passwordResetSent")
                            .setInfo("Email Address: " + emailAddress) );
            } else {
                /**
                 * @todo remove "single" when it's no longer necessary. See
                 * https://github.com/IQSS/dataverse/issues/844 and
                 * https://github.com/IQSS/dataverse/issues/1141
                 */
                logger.log(Level.INFO, "Couldn''t find single account using {0}", emailAddress);
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, ResourceBundle.getBundle("Bundle").getString("passwordReset.initiated"), ""));
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

    public boolean isAccountUpgrade() {
        return passwordResetData.getReason() == PasswordResetData.Reason.UPGRADE_REQUIRED;
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

}
