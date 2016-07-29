package edu.harvard.iq.dataverse.confirmemail;

import edu.harvard.iq.dataverse.DataverseServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.ValidateEmail;
import edu.harvard.iq.dataverse.actionlogging.ActionLogServiceBean;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.providers.builtin.BuiltinAuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.util.BundleUtil;
import edu.harvard.iq.dataverse.util.JsfHelper;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import org.hibernate.validator.constraints.NotBlank;

/**
 *
 * @author bsilverstein
 */
@ViewScoped
@Named("ConfirmEmailPage")
public class ConfirmEmailPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(ConfirmEmailPage.class.getCanonicalName());

    @EJB
    ConfirmEmailServiceBean confirmEmailService;
    @EJB
    AuthenticationServiceBean dataverseUserService;
    @EJB
    DataverseServiceBean dataverseService;
    @EJB
    AuthenticationServiceBean authSvc;
    @Inject
    DataverseSession session;

    @EJB
    ActionLogServiceBean actionLogSvc;

    /**
     * The unique string used to look up a user and continue the email
     * confirmation.
     */
    String token;

    /**
     * The user looked up by the token who will be confirming their email.
     */
    AuthenticatedUser user;

    /**
     * The email address that is entered to be confirmed.
     */
    @NotBlank(message = "Please enter a valid email address.")
    @ValidateEmail(message = "Confirm email page default email message.")
    String emailAddress;

    /**
     * The link that is emailed to the user to confirm the email that contains a
     * token.
     */
    String confirmEmailUrl;

    ConfirmEmailData confirmEmailData;

    public String init() {
        if (token != null) {
            ConfirmEmailExecResponse confirmEmailExecResponse = confirmEmailService.processToken(token);
            confirmEmailData = confirmEmailExecResponse.getConfirmEmailData();
            if (confirmEmailData != null) {
                user = confirmEmailData.getAuthenticatedUser();
                session.setUser(user);
                JsfHelper.addSuccessMessage(BundleUtil.getStringFromBundle("confirmEmail.details.success"));
                return "/dataverse.xhtml?faces-redirect=true";
            }
        }
        JsfHelper.addErrorMessage(BundleUtil.getStringFromBundle("confirmEmail.details.failure"));
        /**
         * @todo It would be nice to send a 404 response but if we enable this
         * then the user sees the contents of 404.xhtml rather than the contents
         * of JsfHelper.addErrorMessage above!
         */
//        try {
//            FacesContext.getCurrentInstance().getExternalContext().responseSendError(HttpServletResponse.SC_NOT_FOUND, null);
//        } catch (IOException ex) {
//        }
        return null;
    }

    public String sendEmailConfirmLink() {
        try {
            ConfirmEmailInitResponse confirmEmailInitResponse = confirmEmailService.beginConfirm(emailAddress);
            ConfirmEmailData confirmEmailData = confirmEmailInitResponse.getConfirmEmailData();
            if (confirmEmailData != null) {
                AuthenticatedUser foundUser = confirmEmailData.getAuthenticatedUser();
                confirmEmailUrl = confirmEmailInitResponse.getConfirmUrl();
            } else {
                logger.log(Level.INFO, "Couldn''t find single account using {0}", emailAddress);
            }
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Email Confirmation Initiated", ""));
        } catch (ConfirmEmailException ex) {
            logger.log(Level.WARNING, "Error While confirming email: " + ex.getMessage(), ex);
        }
        return "";
    }
//might work now...

    public String confirmEmail() throws ConfirmEmailException {
        ConfirmEmailAttemptResponse response = confirmEmailService.attemptEmailConfirm(user, token, token);
        try {
            if (response.isConfirmed()) {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, response.getMessageSummary(), response.getMessageDetail()));
                String builtinAuthProviderId = BuiltinAuthenticationProvider.PROVIDER_ID;
                AuthenticatedUser au = authSvc.lookupUser(builtinAuthProviderId, user.getIdentifier());
                session.setUser(au);
                return "/dataverse.xhtml?alias=" + dataverseService.findRootDataverse().getAlias() + "faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, response.getMessageSummary(), response.getMessageDetail()));
                return null;
            }

        } catch (Exception ex) {
            String msg = "Unable to save token for " + user.getEmail();
            throw new ConfirmEmailException(msg, ex);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AuthenticatedUser getUser() {
        return user;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getConfirmEmailUrl() {
        return confirmEmailUrl;
    }

    public ConfirmEmailData getConfirmEmailData() {
        return confirmEmailData;
    }

    public void setConfirmEmailData(ConfirmEmailData confirmEmailData) {
        this.confirmEmailData = confirmEmailData;
    }

    public boolean isInvalidToken() {
        if (confirmEmailData == null) {
            return true;
        } else {
            return false;
        }
    }

    public String getRedirectToAccountInfoTab() {
        return "/dataverseuser.xhtml?selectTab=accountInfo&faces-redirect=true";
    }

}
